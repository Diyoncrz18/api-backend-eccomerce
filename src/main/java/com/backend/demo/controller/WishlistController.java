package com.backend.demo.controller;

import com.backend.demo.model.User;
import com.backend.demo.model.Product;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/wishlist")
public class WishlistController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<?> getWishlist(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        User user = userRepository.findWithWishlistByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        List<Map<String, Object>> wishlist = user.getWishlist() != null 
            ? user.getWishlist().stream().map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("price", p.getPrice());
                item.put("salePrice", p.getSalePrice());
                item.put("imageUrl", p.getImageUrl());
                item.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
                item.put("rating", p.getRating());
                item.put("stock", p.getStock());
                item.put("isNew", p.getIsNew());
                return item;
            }).collect(Collectors.toList())
            : List.of();

        return ResponseEntity.ok(Map.of("items", wishlist, "total", wishlist.size()));
    }

    @PostMapping("/{productId}")
    @Transactional
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        User user = userRepository.findWithWishlistByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
            return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        }

        user.getWishlist().add(product);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Product added to wishlist"
        ));
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        User user = userRepository.findWithWishlistByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        user.getWishlist().removeIf(product -> product.getId().equals(productId));
        userRepository.save(user);

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

        User user = userRepository.findWithWishlistByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Map.of("inWishlist", false));
        }

        boolean inWishlist = user.getWishlist().stream()
                .anyMatch(product -> product.getId().equals(productId));

        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }
}
