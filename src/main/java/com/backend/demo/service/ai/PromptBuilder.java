package com.backend.demo.service.ai;

import com.backend.demo.dto.chat.ChatMessage;
import com.backend.demo.model.Product;
import com.backend.demo.model.VoucherType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id", "ID"))
            .withZone(ZoneId.of("Asia/Jakarta"));

    private static final DateTimeFormatter SHORT_DATE_FMT =
        DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id", "ID"));

    /**
     * Backward-compatible build (no user context).
     */
    public String build(String userMessage,
                        List<ChatMessage> history,
                        List<Product> retrieved) {
        return build(userMessage, history, retrieved, null);
    }

    /**
     * Assemble the final prompt string. When {@code userContext} is non-null,
     * the prompt also contains a "DATA AKUN ANDA" block so the LLM can answer
     * personalized questions like "berapa pesanan saya?".
     */
    public String build(String userMessage,
                        List<ChatMessage> history,
                        List<Product> retrieved,
                        UserContextProvider.UserContext userContext) {

        StringBuilder sb = new StringBuilder(4096);
        sb.append(systemInstructions(userContext != null));

        if (userContext != null) {
            sb.append("\n\n=== DATA AKUN ANDA (USER YANG SEDANG LOGIN) ===\n");
            sb.append(formatUserContext(userContext));
        }

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
     * When the user is logged in, intent {@code account_info} is added so the
     * LLM can answer personal questions ("berapa pesanan saya?", dsb).
     */
    private String systemInstructions(boolean userLoggedIn) {
        String accountIntent = userLoggedIn ? "|account_info" : "";
        String accountRule = userLoggedIn
            ? """
              8. Untuk pertanyaan tentang akun ("pesanan saya", "voucher saya", "poin saya",
                 "cart saya", "wishlist saya"), gunakan EKSKLUSIF data dari blok DATA AKUN ANDA.
                 Jangan mengarang nomor pesanan, voucher, atau jumlah yang tidak ada di sana.
                 INTENT untuk pertanyaan akun: account_info dengan PRODUCT_IDS: [].
              """
            : """
              8. Jika user bertanya tentang data pribadi ("pesanan saya", "voucher saya",
                 "poin saya", dst), arahkan dengan sopan untuk login dulu agar Anda bisa membantu.
                 INTENT: fallback, PRODUCT_IDS: [].
              """;

        return """
            Anda adalah "Maison AI", asisten belanja furnitur & dekorasi untuk toko Maison Interior.
            Tugas Anda: memberikan rekomendasi produk berdasarkan pertanyaan user.

            ATURAN WAJIB:
            1. HANYA boleh merekomendasikan produk dari daftar KATALOG di bawah. DILARANG mengarang produk baru.
            2. Gunakan Bahasa Indonesia yang hangat, singkat, dan informatif (maks 3 kalimat untuk teks utama).
            3. Sertakan harga dalam format "Rp X.XXX.XXX" bila relevan. Jika ada salePrice, gunakan harga diskon.
            4. Di akhir jawaban, WAJIB tulis dua baris marker:
               INTENT: <greeting|product_recommendation|off_topic|fallback%s>
               PRODUCT_IDS: [id1, id2, ...]
               - Cantumkan 1-4 ID produk paling relevan dari katalog.
               - Kalau tidak ada rekomendasi (greeting / off-topic / account_info), tulis: PRODUCT_IDS: []
            5. Jika user bertanya di luar topik furnitur/interior/dekorasi rumah, jawab sopan bahwa Anda khusus membantu seputar Maison.
            6. Jangan pernah menyebut "katalog", "database", atau "daftar produk" dalam jawaban — buat terasa natural.
            7. Jangan mengulangi pertanyaan user. Langsung berikan jawaban yang bernilai.
            %s""".formatted(accountIntent, accountRule);
    }

    /**
     * Render the authenticated user's data as a compact text block for the prompt.
     */
    private String formatUserContext(UserContextProvider.UserContext c) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Nama: ").append(safe(c.getName())).append("\n");
        sb.append("Tier: ").append(c.getTier())
          .append(" | Reward Points: ").append(c.getPoints())
          .append(" | Total Belanja: ").append(formatRp(c.getTotalSpent())).append("\n");

        sb.append("\nStatistik Pesanan:\n");
        sb.append("- Total pesanan: ").append(c.getTotalOrders()).append("\n");
        sb.append("- Pesanan aktif (sedang diproses/dikirim): ").append(c.getActiveOrders()).append("\n");
        sb.append("- Pesanan selesai: ").append(c.getCompletedOrders()).append("\n");

        if (c.getRecentOrders() != null && !c.getRecentOrders().isEmpty()) {
            sb.append("\n").append(Math.min(c.getRecentOrders().size(), 3)).append(" Pesanan Terbaru:\n");
            int idx = 1;
            for (UserContextProvider.RecentOrder o : c.getRecentOrders()) {
                sb.append(idx++).append(". ")
                  .append(safe(o.getOrderNumber()))
                  .append(" | ").append(o.getStatus())
                  .append(" | ").append(formatRp(o.getTotal()));
                if (o.getCreatedAt() != null) {
                    sb.append(" (").append(DATE_FMT.format(o.getCreatedAt())).append(")");
                }
                sb.append("\n");
            }
        }

        sb.append("\nCart aktif: ").append(c.getCartItemCount()).append(" item\n");
        sb.append("Wishlist: ").append(c.getWishlistCount()).append(" item\n");

        if (c.getActiveVouchers() != null && !c.getActiveVouchers().isEmpty()) {
            sb.append("\nVoucher tersedia (aktif & belum kadaluarsa):\n");
            for (UserContextProvider.VoucherInfo v : c.getActiveVouchers()) {
                sb.append("- ").append(v.getCode()).append(": ");
                if (v.getType() == VoucherType.PERSEN) {
                    sb.append("Diskon ").append(v.getValue().stripTrailingZeros().toPlainString()).append("%");
                } else {
                    sb.append("Diskon ").append(formatRp(v.getValue()));
                }
                if (v.getMinOrderValue() != null
                        && v.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(" (min belanja ").append(formatRp(v.getMinOrderValue())).append(")");
                }
                if (v.getValidUntil() != null) {
                    sb.append(" — berlaku sampai ").append(SHORT_DATE_FMT.format(v.getValidUntil()));
                }
                sb.append("\n");
            }
        } else {
            sb.append("\nTidak ada voucher aktif saat ini.\n");
        }

        return sb.toString();
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
