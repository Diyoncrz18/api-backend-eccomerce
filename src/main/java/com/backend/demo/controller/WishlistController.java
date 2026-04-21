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
@RequestMapping("${api.prefix}/wishlist")
public class WishlistController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getWishlist(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        List<Map<String, Object>> wishlist = user.getWishlist() != null 
            ? user.getWishlist().stream().map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("price", p.getPrice());
                item.put("imageUrl", p.getImageUrl());
                item.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
                return item;
            }).collect(Collectors.toList())
            : List.of();

        return ResponseEntity.ok(Map.of("items", wishlist, "total", wishlist.size()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        // Product will be added later - for now return success
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Product added to wishlist"
        ));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Product removed from wishlist"
        ));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("inWishlist", false));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.ok(Map.of("inWishlist", false));
        }

        return ResponseEntity.ok(Map.of("inWishlist", false));
    }
}