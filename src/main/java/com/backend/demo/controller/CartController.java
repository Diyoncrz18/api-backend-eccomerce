package com.backend.demo.controller;

import com.backend.demo.model.CartItem;
import com.backend.demo.model.Product;
import com.backend.demo.model.User;
import com.backend.demo.repository.CartItemRepository;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/cart")
public class CartController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    @GetMapping
    @Transactional
    public ResponseEntity<?> getCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        List<Map<String, Object>> items = cartItems
                .stream()
                .map(this::toCartItemMap)
                .toList();

        BigDecimal subtotal = cartItems
                .stream()
                .map(item -> {
                    BigDecimal price = item.getProduct().getSalePrice() != null
                            ? item.getProduct().getSalePrice()
                            : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "items", items,
                "subtotal", subtotal,
                "count", items.stream().mapToInt(item -> Number.class.cast(item.get("quantity")).intValue()).sum(),
                "isGuest", false
        ));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> addToCart(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        Long productId = Long.valueOf(String.valueOf(request.get("productId")));
        int quantity = Math.max(1, Integer.parseInt(String.valueOf(request.getOrDefault("quantity", "1"))));
        String variant = String.valueOf(request.getOrDefault("variant", "default"));

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
            return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Product is out of stock"));
        }

        CartItem item = cartItemRepository
                .findByUserIdAndProductIdAndVariant(user.getId(), productId, variant)
                .orElseGet(() -> CartItem.builder()
                        .user(user)
                        .product(product)
                        .variant(variant)
                        .quantity(0)
                        .build());
        item.setQuantity(Math.min(product.getStock(), item.getQuantity() + quantity));
        CartItem saved = cartItemRepository.save(item);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Item added to cart",
                "item", toCartItemMap(saved)
        ));
    }

    @DeleteMapping("/{itemId}")
    @Transactional
    public ResponseEntity<?> removeFromCart(
            @PathVariable Long itemId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null || !item.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(404).body(Map.of("message", "Cart item not found"));
        }

        cartItemRepository.delete(item);
        return ResponseEntity.ok(Map.of("success", true, "message", "Item removed from cart"));
    }

    @PutMapping("/{itemId}")
    @Transactional
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null || !item.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(404).body(Map.of("message", "Cart item not found"));
        }

        int quantity = Math.max(1, request.getOrDefault("quantity", 1));
        item.setQuantity(Math.min(item.getProduct().getStock(), quantity));
        CartItem saved = cartItemRepository.save(item);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "item", toCartItemMap(saved)
        ));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> clearCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        cartItemRepository.deleteByUserId(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Cart cleared"));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName());
    }

    private Map<String, Object> toCartItemMap(CartItem item) {
        Product product = item.getProduct();
        BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
        return Map.of(
                "id", item.getId(),
                "productId", product.getId(),
                "productName", product.getName(),
                "productImage", product.getImageUrl() != null ? product.getImageUrl() : "",
                "variant", item.getVariant(),
                "price", price,
                "quantity", item.getQuantity(),
                "stock", product.getStock(),
                "subtotal", price.multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
