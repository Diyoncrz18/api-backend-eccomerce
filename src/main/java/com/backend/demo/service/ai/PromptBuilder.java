package com.backend.demo.service.ai;

import com.backend.demo.dto.chat.ChatMessage;
import com.backend.demo.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Builds the full prompt (system + context + history + user turn)
 * that is sent to Gemini. Also owns the response-parsing conventions
 * (PRODUCT_IDS marker + intent marker).
 */
@Component
public class PromptBuilder {

    private static final NumberFormat RP =
        NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    /**
     * Assemble the final prompt string.
     */
    public String build(String userMessage,
                        List<ChatMessage> history,
                        List<Product> retrieved) {

        StringBuilder sb = new StringBuilder(4096);
        sb.append(systemInstructions());
        sb.append("\n\n=== KATALOG PRODUK YANG TERSEDIA ===\n");
        if (retrieved == null || retrieved.isEmpty()) {
            sb.append("(tidak ada produk yang cocok ditemukan)\n");
        } else {
            for (Product p : retrieved) {
                sb.append(formatProduct(p)).append("\n");
            }
        }

        if (history != null && !history.isEmpty()) {
            sb.append("\n=== PERCAKAPAN SEBELUMNYA ===\n");
            for (ChatMessage m : history) {
                String role = "user".equalsIgnoreCase(m.getRole()) ? "User" : "Maison AI";
                sb.append(role).append(": ").append(safe(m.getText())).append("\n");
            }
        }

        sb.append("\n=== PESAN SAAT INI ===\n");
        sb.append("User: ").append(safe(userMessage)).append("\n");
        sb.append("Maison AI:");
        return sb.toString();
    }

    /**
     * System instructions — keep this tight, strict rules prevent hallucination.
     */
    private String systemInstructions() {
        return """
            Anda adalah "Maison AI", asisten belanja furnitur & dekorasi untuk toko Maison Interior.
            Tugas Anda: memberikan rekomendasi produk berdasarkan pertanyaan user.

            ATURAN WAJIB:
            1. HANYA boleh merekomendasikan produk dari daftar KATALOG di bawah. DILARANG mengarang produk baru.
            2. Gunakan Bahasa Indonesia yang hangat, singkat, dan informatif (maks 3 kalimat untuk teks utama).
            3. Sertakan harga dalam format "Rp X.XXX.XXX" bila relevan. Jika ada salePrice, gunakan harga diskon.
            4. Di akhir jawaban, WAJIB tulis dua baris marker:
               INTENT: <greeting|product_recommendation|off_topic|fallback>
               PRODUCT_IDS: [id1, id2, ...]
               - Cantumkan 1-4 ID produk paling relevan dari katalog.
               - Kalau tidak ada rekomendasi (greeting / off-topic), tulis: PRODUCT_IDS: []
            5. Jika user bertanya di luar topik furnitur/interior/dekorasi rumah, jawab sopan bahwa Anda khusus membantu seputar Maison.
            6. Jangan pernah menyebut "katalog", "database", atau "daftar produk" dalam jawaban — buat terasa natural.
            7. Jangan mengulangi pertanyaan user. Langsung berikan jawaban yang bernilai.
            """;
    }

    /**
     * Format one product into a single compact line for the prompt.
     * Example: "[id:54] Linen 3-Seater Sofa | Kursi | Rp 14.800.000 (diskon Rp 13.320.000) | Material: ... | Stok: 6"
     */
    private String formatProduct(Product p) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("[id:").append(p.getId()).append("] ");
        sb.append(p.getName());
        if (p.getCategory() != null) {
            sb.append(" | ").append(p.getCategory().getName());
        }
        sb.append(" | ").append(formatRp(p.getPrice()));
        if (p.getSalePrice() != null) {
            sb.append(" (diskon ").append(formatRp(p.getSalePrice())).append(")");
        }
        if (p.getMaterial() != null && !p.getMaterial().isBlank()) {
            sb.append(" | Bahan: ").append(p.getMaterial());
        }
        if (p.getStock() != null) {
            sb.append(" | Stok: ").append(p.getStock());
        }
        if (Boolean.TRUE.equals(p.getIsNew())) {
            sb.append(" | BARU");
        }
        String desc = truncate(p.getDescription(), 180);
        if (desc != null && !desc.isBlank()) {
            sb.append("\n   Desc: ").append(desc);
        }
        return sb.toString();
    }

    private String formatRp(BigDecimal v) {
        if (v == null) return "-";
        // id_ID NumberFormat emits "Rp14.800.000,00" — shorten to "Rp 14.800.000"
        String s = RP.format(v);
        s = s.replace("Rp", "Rp ").replace(",00", "");
        return s;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").trim();
    }
}
