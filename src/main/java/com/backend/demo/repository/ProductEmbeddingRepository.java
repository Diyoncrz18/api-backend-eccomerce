package com.backend.demo.repository;

import com.backend.demo.model.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {

    /**
     * Find embeddings only for products with matching model (others will be refreshed).
     */
    List<ProductEmbedding> findByModel(String model);

    /**
     * Return IDs of products whose embedding is missing, outdated (wrong model),
     * or whose source hash differs from the supplied map entries.
     * Used by EmbeddingBootstrap to decide which products need embedding.
     */
    @Query("SELECT e.productId FROM ProductEmbedding e WHERE e.model = :model")
    Set<Long> findProductIdsWithCurrentModel(@Param("model") String model);
}
