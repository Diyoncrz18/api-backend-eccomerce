package com.backend.demo.controller;

import com.backend.demo.dto.LoginRequest;
import com.backend.demo.dto.RegisterRequest;
import com.backend.demo.dto.ResponseData;
import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import com.backend.demo.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("gender", user.getGender());
        userInfo.put("birthdate", user.getBirthdate());
        userInfo.put("tier", user.getTier());
        userInfo.put("points", user.getRewardPoints());
        userInfo.put("totalOrders", user.getTotalOrders());
        userInfo.put("joinDate", user.getJoinDate());
        userInfo.put("roles", user.getRoles().stream().map(r -> r.getName()).toList());

        return ResponseEntity.ok(userInfo);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        if (request.containsKey("name") && request.get("name") != null && !request.get("name").isBlank()) {
            user.setName(request.get("name"));
        }
        if (request.containsKey("phone") && request.get("phone") != null) {
            user.setPhone(request.get("phone"));
        }
        if (request.containsKey("avatarUrl") && request.get("avatarUrl") != null) {
            user.setAvatarUrl(request.get("avatarUrl"));
        }
        if (request.containsKey("gender") && request.get("gender") != null) {
            user.setGender(request.get("gender"));
        }
        if (request.containsKey("birthdate") && request.get("birthdate") != null) {
            String birthdate = request.get("birthdate");
            if (birthdate.isBlank()) {
                user.setBirthdate(null);
            } else {
                try {
                    user.setBirthdate(LocalDate.parse(birthdate));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid birthdate format. Use YYYY-MM-DD"));
                }
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
    }
}
