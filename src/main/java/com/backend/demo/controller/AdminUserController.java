package com.backend.demo.controller;

import com.backend.demo.dto.AdminUserResponse;
import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tier) {
        
        log.info("Admin get all users: page={}, size={}, search={}, tier={}", page, size, search, tier);
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage;
        
        if (search != null && !search.isEmpty()) {
            usersPage = userRepository.findByNameContainingOrEmailContaining(search, search, pageRequest);
        } else {
            usersPage = userRepository.findAll(pageRequest);
        }
        
        Page<AdminUserResponse> response = usersPage.map(AdminUserResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        log.info("Admin get user by id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        log.info("Admin update user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.containsKey("name")) {
            user.setName((String) request.get("name"));
        }
        if (request.containsKey("phone")) {
            user.setPhone((String) request.get("phone"));
        }
        if (request.containsKey("tier")) {
            user.setTier(com.backend.demo.model.UserTier.valueOf((String) request.get("tier")));
        }
        if (request.containsKey("rewardPoints")) {
            user.setRewardPoints((Integer) request.get("rewardPoints"));
        }
        if (request.containsKey("isActive")) {
            user.setIsActive((Boolean) request.get("isActive"));
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Admin delete user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Admin get user stats");
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers
        ));
    }
}
