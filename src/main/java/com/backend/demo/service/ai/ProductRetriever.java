package com.backend.demo.service.ai;

import com.backend.demo.model.Product;
import com.backend.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword-based retriever for the RAG chatbot.
 *
 * Strategy for ~53 products:
 *  1. Load all active products once (in-memory is fine at this scale).
 *  2. Parse user query for hints: category, price ceiling, keywords.
 *  3. Score each product with weighted feature matches.
 *  4. Return top-K products sorted by score (tie-break by rating desc).
 *
 * Zero external dependencies — pure Java + JPA. Can be upgraded to
 * vector-embedding retrieval in Phase 2 without changing the public API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRetriever {

    private final ProductRepository productRepository;

    /** Common Indonesian stopwords we strip before scoring. */
    private static final Set<String> STOPWORDS = Set.of(
        "saya", "aku", "kami", "kita", "kamu", "anda",
        "yang", "untuk", "dengan", "dari", "dan", "atau", "juga",
        "ada", "mau", "ingin", "butuh", "cari", "sedang", "ini", "itu",
        "lagi", "bisa", "boleh", "tolong", "mohon", "apa", "apakah",
        "aja", "saja", "pun", "sih", "dong", "kok", "gimana", "bagaimana",
        "kasih", "berikan", "tunjukkan", "lihat", "rekomendasi", "rekomendasikan",
        "produk", "barang", "punya", "under", "below", "max", "maks",
        "the", "are", "for", "with", "and", "please", "show", "give",
        "want", "need", "looking", "buat"
    );

    /** Canonical category name → hint keywords that should map to it. */
    private static final Map<String, List<String>> CATEGORY_SYNONYMS = Map.ofEntries(
        Map.entry("Kursi", List.of(
            "kursi", "sofa", "chair", "couch", "duduk", "bangku", "stool",
            "armchair", "lounge", "dudukan", "seating", "loveseat", "sectional"
        )),
        Map.entry("Meja", List.of(
            "meja", "table", "desk", "console", "counter", "coffee table", "side table",
            "dining table", "kerja", "makan"
        )),
        Map.entry("Lampu", List.of(
            "lampu", "lamp", "light", "lighting", "pencahayaan", "chandelier",
            "sconce", "pendant", "lantera", "floor lamp", "table lamp"
        )),
        Map.entry("Dekorasi", List.of(
            "dekorasi", "decor", "ornamen", "hiasan", "vas", "vase", "cermin", "mirror",
            "frame", "bingkai", "lilin", "candle", "karpet", "rug", "throw", "selimut",
            "panel", "art", "lukisan"
        )),
        Map.entry("Penyimpanan", List.of(
            "rak", "shelf", "shelving", "bookshelf", "lemari", "wardrobe", "closet",
            "cabinet", "sideboard", "storage", "penyimpanan", "keranjang", "basket",
            "chest", "drawer", "laci", "buku", "sepatu"
        )),
        Map.entry("Ruang Tamu", List.of(
            "ruang tamu", "living room", "living", "tamu", "santai", "family room"
        )),
        Map.entry("Ruang Makan", List.of(
            "ruang makan", "dining room", "dining", "makan"
        )),
        Map.entry("Kamar Tidur", List.of(
            "kamar", "kamar tidur", "bedroom", "tidur", "sleep", "nightstand", "bed"
        ))
    );

    /** Style / material keywords that boost matching products. */
    private static final Map<String, List<String>> STYLE_HINTS = Map.ofEntries(
        Map.entry("japandi", List.of("japandi", "minimalis", "minimal", "zen")),
        Map.entry("rattan", List.of("rotan", "rattan", "anyaman")),
        Map.entry("oak", List.of("oak", "kayu", "wood")),
        Map.entry("marble", List.of("marmer", "marble", "travertine")),
        Map.entry("velvet", List.of("velvet", "beludru")),
        Map.entry("linen", List.of("linen", "kain")),
        Map.entry("leather", List.of("kulit", "leather")),
        Map.entry("brass", List.of("brass", "kuningan", "gold", "emas")),
        Map.entry("ceramic", List.of("keramik", "ceramic", "terracotta", "gerabah"))
    );

    /**
     * Main entry point: return the top-K most relevant products for the query.
     * Never throws — returns empty list on failure (caller must handle gracefully).
     */
    public List<Product> findRelevant(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) return List.of();

        String normalized = normalize(query);
        List<Product> pool = loadActiveProducts();
        if (pool.isEmpty()) return List.of();

        // Parse hints
        Set<String> targetCategories = detectCategories(normalized);
        BigDecimal maxPrice = detectMaxPrice(normalized);
        Set<String> keywords = extractKeywords(normalized);
        boolean wantsNew = mentionsNew(normalized);
        boolean wantsBudget = mentionsBudget(normalized);

        log.debug("Retrieval query='{}' → categories={}, maxPrice={}, keywords={}, wantsNew={}, wantsBudget={}",
            query, targetCategories, maxPrice, keywords, wantsNew, wantsBudget);

        // Score all products
        List<ScoredProduct> scored = new ArrayList<>(pool.size());
        for (Product p : pool) {
            double score = score(p, targetCategories, maxPrice, keywords, wantsNew, wantsBudget);
            scored.add(new ScoredProduct(p, score));
        }

        // Sort by score desc, tie-break by rating desc
        scored.sort((a, b) -> {
            int byScore = Double.compare(b.score, a.score);
            if (byScore != 0) return byScore;
            BigDecimal ra = a.product.getRating() != null ? a.product.getRating() : BigDecimal.ZERO;
            BigDecimal rb = b.product.getRating() != null ? b.product.getRating() : BigDecimal.ZERO;
            return rb.compareTo(ra);
        });

        // If nobody scored > 0, return top-rated products as fallback
        boolean anyMatch = scored.stream().anyMatch(s -> s.score > 0);
        if (!anyMatch) {
            log.debug("No keyword match, falling back to top-rated products");
            return topByRating(pool, topK);
        }

        return scored.stream()
            .filter(s -> s.score > 0)
            .limit(topK)
            .map(s -> s.product)
            .toList();
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    private double score(Product p, Set<String> targetCategories, BigDecimal maxPrice,
                         Set<String> keywords, boolean wantsNew, boolean wantsBudget) {
        double score = 0;

        // Category match (strongest signal)
        if (!targetCategories.isEmpty() && p.getCategory() != null) {
            if (targetCategories.contains(p.getCategory().getName())) {
                score += 10;
            }
        }

        // Budget match
        if (maxPrice != null) {
            BigDecimal effectivePrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            if (effectivePrice != null && effectivePrice.compareTo(maxPrice) <= 0) {
                score += 8;
            } else if (wantsBudget) {
                // user explicitly asked cheap, penalise over-budget
                score -= 4;
            }
        } else if (wantsBudget) {
            // "murah" without specific budget — prefer under 3 juta
            BigDecimal threshold = new BigDecimal("3000000");
            BigDecimal effectivePrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            if (effectivePrice != null && effectivePrice.compareTo(threshold) <= 0) {
                score += 5;
            }
        }

        // Keyword matches in name, description, material
        String name = lower(p.getName());
        String desc = lower(p.getDescription());
        String material = lower(p.getMaterial());
        for (String kw : keywords) {
            if (name.contains(kw)) score += 5;
            if (desc.contains(kw)) score += 2;
            if (material.contains(kw)) score += 3;
        }

        // "baru" / "new" boost
        if (wantsNew && Boolean.TRUE.equals(p.getIsNew())) {
            score += 4;
        }

        // Small quality boost (rating 0..5 → 0..2 points)
        if (p.getRating() != null) {
            score += p.getRating().doubleValue() * 0.4;
        }

        // Out-of-stock penalty
        if (p.getStock() != null && p.getStock() <= 0) {
            score -= 3;
        }

        return score;
    }

    // ─── Hint parsing ─────────────────────────────────────────────────────────

    private Set<String> detectCategories(String normalized) {
        Set<String> matched = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> e : CATEGORY_SYNONYMS.entrySet()) {
            for (String hint : e.getValue()) {
                if (normalized.contains(hint)) {
                    matched.add(e.getKey());
                    break;
                }
            }
        }
        return matched;
    }

    /** Parse "10 juta", "5jt", "3.000.000", "dibawah 2 juta", "under 5m", etc. */
    private BigDecimal detectMaxPrice(String normalized) {
        // Pattern 1: "X juta" or "Xjt"
        Pattern juta = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(juta|jt|million|m\\b)");
        Matcher m1 = juta.matcher(normalized);
        if (m1.find()) {
            double n = parseNum(m1.group(1));
            return BigDecimal.valueOf((long)(n * 1_000_000));
        }
        // Pattern 2: "X ribu" or "Xrb"
        Pattern ribu = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(ribu|rb|k\\b)");
        Matcher m2 = ribu.matcher(normalized);
        if (m2.find()) {
            double n = parseNum(m2.group(1));
            return BigDecimal.valueOf((long)(n * 1_000));
        }
        // Pattern 3: raw number >= 100000 treated as IDR price
        Pattern rawBig = Pattern.compile("(\\d{6,})");
        Matcher m3 = rawBig.matcher(normalized.replaceAll("[.,]", ""));
        if (m3.find()) {
            return new BigDecimal(m3.group(1));
        }
        return null;
    }

    private Set<String> extractKeywords(String normalized) {
        Set<String> out = new LinkedHashSet<>();
        for (String tok : normalized.split("[^a-zA-Z0-9]+")) {
            if (tok.length() < 3) continue;
            if (STOPWORDS.contains(tok)) continue;
            out.add(tok);
        }
        // Add style hint canonical forms (e.g. "minimalis" → "japandi")
        for (Map.Entry<String, List<String>> e : STYLE_HINTS.entrySet()) {
            for (String hint : e.getValue()) {
                if (normalized.contains(hint)) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    private boolean mentionsBudget(String normalized) {
        return normalized.contains("murah")
            || normalized.contains("terjangkau")
            || normalized.contains("hemat")
            || normalized.contains("budget")
            || normalized.contains("affordable")
            || normalized.contains("cheap")
            || normalized.contains("di bawah")
            || normalized.contains("dibawah")
            || normalized.contains("under")
            || normalized.contains("maks")
            || normalized.contains("max");
    }

    private boolean mentionsNew(String normalized) {
        return normalized.contains("baru")
            || normalized.contains("terbaru")
            || normalized.contains("new")
            || normalized.contains("latest")
            || normalized.contains("koleksi terbaru");
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private List<Product> loadActiveProducts() {
        // At ~53 products this is one query — fast and simple.
        // If catalog grows > 1000, switch to specification-based narrowing.
        return productRepository.findAllActive(
            PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "rating"))
        ).getContent();
    }

    private List<Product> topByRating(List<Product> pool, int k) {
        return pool.stream()
            .sorted((a, b) -> {
                BigDecimal ra = a.getRating() != null ? a.getRating() : BigDecimal.ZERO;
                BigDecimal rb = b.getRating() != null ? b.getRating() : BigDecimal.ZERO;
                return rb.compareTo(ra);
            })
            .limit(k)
            .toList();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                             .replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static double parseNum(String s) {
        return Double.parseDouble(s.replace(",", "."));
    }

    private record ScoredProduct(Product product, double score) {}
}
