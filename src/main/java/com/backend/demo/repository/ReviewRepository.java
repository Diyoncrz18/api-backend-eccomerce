package com.backend.demo.repository;

import com.backend.demo.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double averageRatingByProductId(@Param("productId") Long productId);

    long countByProductIdAndIsApprovedTrue(Long productId);
}
