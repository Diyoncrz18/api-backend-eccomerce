package com.backend.demo.controller;

import com.backend.demo.model.Product;
import com.backend.demo.model.Review;
import com.backend.demo.model.User;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.ReviewRepository;
import com.backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/reviews")
public class ReviewController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toReviewMap));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {
        List<Map<String, Object>> reviews = reviewRepository
                .findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toReviewMap)
                .toList();
        double averageRating = reviewRepository.averageRatingByProductId(productId) != null
                ? reviewRepository.averageRatingByProductId(productId)
                : 0.0;

        return ResponseEntity.ok(Map.of(
            "reviews", reviews,
            "averageRating", averageRating,
            "totalReviews", reviews.size()
        ));
    }

    @PostMapping("/product/{productId}")
    @Transactional
    public ResponseEntity<?> addReview(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        User user = userRepository.findByEmail(authentication.getName());

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        }

        int rating = Integer.parseInt(String.valueOf(request.getOrDefault("rating", "0")));
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5"));
        }

        Review review = reviewRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseGet(() -> Review.builder()
                        .user(user)
                        .product(product)
                        .build());
        review.setRating(rating);
        review.setComment((String) request.getOrDefault("comment", ""));
        review.setIsApproved(true);
        reviewRepository.save(review);
        refreshProductRating(product);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Review submitted successfully",
            "approved", true
        ));
    }

    @DeleteMapping("/{reviewId}")
    @Transactional
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Review not found"));
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        refreshProductRating(product);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Review deleted successfully"
        ));
    }

    @PutMapping("/{reviewId}/status")
    @Transactional
    public ResponseEntity<?> updateReviewStatus(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Review not found"));
        }

        String status = String.valueOf(request.getOrDefault("status", "approved"));
        review.setIsApproved(!status.equalsIgnoreCase("rejected"));
        Review saved = reviewRepository.save(review);
        refreshProductRating(saved.getProduct());

        return ResponseEntity.ok(Map.of("success", true, "review", toReviewMap(saved)));
    }

    private Map<String, Object> toReviewMap(Review review) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", review.getId());
        item.put("userId", review.getUser().getId());
        item.put("userName", review.getUser().getName());
        item.put("productId", review.getProduct().getId());
        item.put("productName", review.getProduct().getName());
        item.put("rating", review.getRating());
        item.put("comment", review.getComment());
        item.put("isApproved", review.getIsApproved());
        item.put("createdAt", review.getCreatedAt());
        return item;
    }

    private void refreshProductRating(Product product) {
        Double avg = reviewRepository.averageRatingByProductId(product.getId());
        long count = reviewRepository.countByProductIdAndIsApprovedTrue(product.getId());
        product.setRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        product.setReviewCount((int) count);
        productRepository.save(product);
    }
}
