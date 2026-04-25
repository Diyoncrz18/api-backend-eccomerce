package com.backend.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Persisted Gemini embedding for a product.
 *
 * Stored as a JSON-encoded float array inside a TEXT column. At ~768 dims × 7 chars
 * per float ≈ 5.5 KB per row — well within MySQL TEXT (65 KB limit).
 *
 * Key design decisions:
 * - Uses productId as primary key (1:1 relationship — one embedding per product).
 * - {@code sourceHash} stores a SHA-256 (first 16 hex chars) of the text that was
 *   embedded; if product data changes, the hash changes → we know to re-embed.
 * - {@code model} records which embedding model generated this vector, so switching
 *   models triggers automatic re-embed of all rows.
 */
@Entity
@Table(
    name = "product_embeddings",
    indexes = {
        @Index(name = "idx_product_emb_model", columnList = "model"),
        @Index(name = "idx_product_emb_hash", columnList = "source_hash")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEmbedding {

    /** Same ID as the linked product (1:1) */
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "vector_json", nullable = false, columnDefinition = "TEXT")
    private String vectorJson;

    @Column(name = "model", nullable = false, length = 80)
    private String model;

    @Column(name = "source_hash", nullable = false, length = 32)
    private String sourceHash;

    @Column(name = "dimensions")
    private Integer dimensions;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
