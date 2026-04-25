package com.backend.demo.controller;

import com.backend.demo.model.Voucher;
import com.backend.demo.model.VoucherType;
import com.backend.demo.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/vouchers")
public class VoucherController {

    private final VoucherRepository voucherRepository;

    @GetMapping
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(Map.of(
                "vouchers",
                voucherRepository.findByIsActiveTrueOrderByValidUntilAsc()
                        .stream()
                        .map(this::toVoucherMap)
                        .toList()
        ));
    }

    @GetMapping("/my-vouchers")
    public ResponseEntity<?> getMyVouchers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        return getAllVouchers();
    }

    @PostMapping("/redeem")
    @Transactional
    public ResponseEntity<?> redeemVoucher(@RequestBody Map<String, Object> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String code = String.valueOf(request.getOrDefault("code", "")).trim();
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElse(null);
        if (voucher == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid voucher code"));
        }
        if (!isVoucherUsable(voucher)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Voucher is not active"));
        }

        voucher.setUsedCount((voucher.getUsedCount() != null ? voucher.getUsedCount() : 0) + 1);
        voucherRepository.save(voucher);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Voucher redeemed successfully",
                "voucher", toVoucherMap(voucher)
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateVoucher(@RequestBody Map<String, Object> request) {
        String code = String.valueOf(request.getOrDefault("code", "")).trim();
        BigDecimal orderValue = new BigDecimal(String.valueOf(request.getOrDefault("orderValue", "0")));
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElse(null);

        if (voucher == null) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid voucher code"));
        }
        if (!isVoucherUsable(voucher)) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Voucher is not active"));
        }
        if (orderValue.compareTo(voucher.getMinOrderValue()) < 0) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Minimum order value not met"));
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discountType", toDiscountType(voucher.getType()),
                "discountValue", voucher.getValue()
        ));
    }

    private boolean isVoucherUsable(Voucher voucher) {
        boolean active = Boolean.TRUE.equals(voucher.getIsActive());
        boolean underLimit = voucher.getUsageLimit() == null || voucher.getUsedCount() == null
                || voucher.getUsedCount() < voucher.getUsageLimit();
        boolean notExpired = voucher.getValidUntil() == null || !voucher.getValidUntil().isBefore(LocalDate.now());
        return active && underLimit && notExpired;
    }

    private Map<String, Object> toVoucherMap(Voucher voucher) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", voucher.getId());
        item.put("code", voucher.getCode());
        item.put("discountType", toDiscountType(voucher.getType()));
        item.put("discountValue", voucher.getValue());
        item.put("pointsCost", voucher.getPointsCost());
        item.put("minOrderValue", voucher.getMinOrderValue());
        item.put("validUntil", voucher.getValidUntil());
        item.put("isActive", voucher.getIsActive());
        item.put("usedCount", voucher.getUsedCount());
        item.put("usageLimit", voucher.getUsageLimit());
        return item;
    }

    private String toDiscountType(VoucherType type) {
        return type == VoucherType.PERSEN ? "PERCENT" : "FIXED";
    }
}
