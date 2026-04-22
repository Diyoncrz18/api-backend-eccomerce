package com.backend.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        log.info("Admin get settings");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("storeName", "Maison Furniture");
        settings.put("storeEmail", "hello@maison.id");
        settings.put("storePhone", "+6281234567890");
        settings.put("storeAddress", "Jl. Dago No. 123, Bandung, Jawa Barat");
        settings.put("shippingFreeMin", 5000000);
        settings.put("shippingCost", 150000);
        settings.put("taxRate", 0.11);
        settings.put("whatsappNumber", "+6281234567890");
        settings.put("instagramUrl", "https://instagram.com/maison");
        settings.put("facebookUrl", "https://facebook.com/maison");
        settings.put("tokopediaUrl", "https://tokopedia.com/maison");
        settings.put("shopeeUrl", "https://shopee.com/maison");
        
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info("Admin update settings");
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Settings updated successfully"
        ));
    }
}
