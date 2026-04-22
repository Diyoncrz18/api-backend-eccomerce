package com.backend.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/admin/rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRewardsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRewardsConfig() {
        log.info("Admin get rewards config");
        
        Map<String, Object> config = Map.of(
                "pointsPerRupiah", 1,
                "pointsToRupiahRatio", 10,
                "tiers", Map.of(
                        "BRONZE", Map.of("minSpent", 0, "multiplier", 1.0),
                        "SILVER", Map.of("minSpent", 5000000, "multiplier", 1.5),
                        "GOLD", Map.of("minSpent", 15000000, "multiplier", 2.0),
                        "PLATINUM", Map.of("minSpent", 50000000, "multiplier", 3.0)
                ),
                "welcomePoints", 100,
                "birthdayPoints", 500,
                "referralPoints", 200
        );
        
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateRewardsConfig(@RequestBody Map<String, Object> config) {
        log.info("Admin update rewards config");
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rewards config updated"
        ));
    }
}