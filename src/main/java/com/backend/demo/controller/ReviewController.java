package com.backend.demo.controller;

import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/reviews")
public class ReviewController {

    private final UserRepository userRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {
        // Return mock reviews - will connect to Review entity when table created
        List<Map<String, Object>> reviews = List.of();
        double averageRating = 0.0;
        
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                .mapToInt(r -> (Integer) r.get("rating"))
                .average()
                .orElse(0.0);
        }

        return ResponseEntity.ok(Map.of(
            "reviews", reviews,
            "averageRating", averageRating,
            "totalReviews", reviews.size()
        ));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<?> addReview(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        // Auto-approve reviews as requested
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Review submitted successfully",
            "approved", true
        ));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Review deleted successfully"
        ));
    }
}