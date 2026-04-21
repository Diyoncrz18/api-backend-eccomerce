package com.backend.demo.controller;

import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/cart")
public class CartController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> guestCart = new HashMap<>();
            guestCart.put("items", new ArrayList<>());
            guestCart.put("isGuest", true);
            return ResponseEntity.ok(guestCart);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", new ArrayList<>());
        cartData.put("userId", user != null ? user.getId() : null);
        cartData.put("isGuest", false);

        return ResponseEntity.ok(cartData);
    }

    @PostMapping
    public ResponseEntity<?> addToCart(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Item added to cart");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long itemId) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Item removed from cart");
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("quantity", request.get("quantity"));
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart() {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cart cleared");
        
        return ResponseEntity.ok(response);
    }
}