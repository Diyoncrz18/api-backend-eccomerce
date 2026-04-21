package com.backend.demo.controller;

import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/vouchers")
public class VoucherController {

    private final UserRepository userRepository;

    // In-memory voucher storage for demo (will use DB in production)
    private static final List<Map<String, Object>> VOUCHERS = new ArrayList<>();

    static {
        VOUCHERS.add(Map.of(
            "id", 1L,
            "code", "MAISON10",
            "discountType", "PERCENT",
            "discountValue", 10,
            "minOrderValue", 500000,
            "validUntil", "2026-12-31",
            "isActive", true
        ));
        VOUCHERS.add(Map.of(
            "id", 2L,
            "code", "WELCOME15",
            "discountType", "PERCENT",
            "discountValue", 15,
            "minOrderValue", 0,
            "validUntil", "2026-12-31",
            "isActive", true
        ));
        VOUCHERS.add(Map.of(
            "id", 3L,
            "code", "MEMBER20",
            "discountType", "PERCENT",
            "discountValue", 20,
            "minOrderValue", 1000000,
            "validUntil", "2026-12-31",
            "isActive", true
        ));
    }

    @GetMapping
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(Map.of("vouchers", VOUCHERS));
    }

    @GetMapping("/my-vouchers")
    public ResponseEntity<?> getMyVouchers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        // Return available vouchers for user
        return ResponseEntity.ok(Map.of("vouchers", VOUCHERS));
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeemVoucher(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String code = (String) request.get("code");
        
        // Find voucher
        Map<String, Object> voucher = null;
        for (Map<String, Object> v : VOUCHERS) {
            if (v.get("code").equals(code)) {
                voucher = v;
                break;
            }
        }

        if (voucher == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid voucher code"
            ));
        }

        if (Boolean.FALSE.equals(voucher.get("isActive"))) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Voucher is not active"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Voucher redeemed successfully",
            "voucher", voucher
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateVoucher(
            @RequestBody Map<String, Object> request) {
        
        String code = (String) request.get("code");
        BigDecimal orderValue = new BigDecimal(request.getOrDefault("orderValue", "0").toString());
        
        // Find voucher
        Map<String, Object> voucher = null;
        for (Map<String, Object> v : VOUCHERS) {
            if (v.get("code").equals(code)) {
                voucher = v;
                break;
            }
        }

        if (voucher == null) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Invalid voucher code"
            ));
        }

        if (Boolean.FALSE.equals(voucher.get("isActive"))) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Voucher is not active"
            ));
        }

        BigDecimal minOrderValue = new BigDecimal(voucher.getOrDefault("minOrderValue", "0").toString());
        if (orderValue.compareTo(minOrderValue) < 0) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Minimum order value not met"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "valid", true,
            "discountType", voucher.get("discountType"),
            "discountValue", voucher.get("discountValue")
        ));
    }
}