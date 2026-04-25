package com.backend.demo.controller;

import com.backend.demo.model.Voucher;
import com.backend.demo.model.VoucherType;
import com.backend.demo.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherRepository voucherRepository;

    @GetMapping
    public ResponseEntity<?> getAllVouchers() {
        log.info("Admin get all vouchers");

        return ResponseEntity.ok(Map.of(
                "vouchers", voucherRepository.findAll()
                        .stream()
                        .map(this::toVoucherMap)
                        .toList()
        ));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createVoucher(@RequestBody Map<String, Object> request) {
        String code = String.valueOf(request.getOrDefault("code", "")).trim().toUpperCase();
        if (code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Voucher code is required"));
        }
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Voucher code already exists"));
        }

        Voucher voucher = Voucher.builder()
                .code(code)
                .type(readVoucherType(request))
                .value(readBigDecimal(request, "discountValue", BigDecimal.ZERO))
                .pointsCost(readInteger(request, "pointsCost", 0))
                .minOrderValue(readBigDecimal(request, "minOrderValue", BigDecimal.ZERO))
                .usageLimit(readInteger(request, "usageLimit", 999))
                .usedCount(0)
                .validUntil(readDate(request, "validUntil"))
                .isActive(readBoolean(request, "isActive", true))
                .build();

        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(Map.of("success", true, "voucher", toVoucherMap(saved)));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateVoucher(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        if (request.containsKey("code")) {
            String code = String.valueOf(request.get("code")).trim().toUpperCase();
            if (!code.equalsIgnoreCase(voucher.getCode()) && voucherRepository.existsByCodeIgnoreCase(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Voucher code already exists"));
            }
            voucher.setCode(code);
        }
        if (request.containsKey("discountType") || request.containsKey("type")) {
            voucher.setType(readVoucherType(request));
        }
        if (request.containsKey("discountValue") || request.containsKey("value")) {
            voucher.setValue(readBigDecimal(request, "discountValue", readBigDecimal(request, "value", voucher.getValue())));
        }
        if (request.containsKey("pointsCost")) {
            voucher.setPointsCost(readInteger(request, "pointsCost", voucher.getPointsCost()));
        }
        if (request.containsKey("minOrderValue") || request.containsKey("minOrder")) {
            voucher.setMinOrderValue(readBigDecimal(request, "minOrderValue", readBigDecimal(request, "minOrder", voucher.getMinOrderValue())));
        }
        if (request.containsKey("usageLimit") || request.containsKey("limit")) {
            voucher.setUsageLimit(readInteger(request, "usageLimit", readInteger(request, "limit", voucher.getUsageLimit())));
        }
        if (request.containsKey("usedCount")) {
            voucher.setUsedCount(readInteger(request, "usedCount", voucher.getUsedCount()));
        }
        if (request.containsKey("validUntil")) {
            voucher.setValidUntil(readDate(request, "validUntil"));
        }
        if (request.containsKey("isActive")) {
            voucher.setIsActive(readBoolean(request, "isActive", voucher.getIsActive()));
        }

        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(Map.of("success", true, "voucher", toVoucherMap(saved)));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateVoucherStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        voucher.setIsActive(readBoolean(request, "isActive", voucher.getIsActive()));
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(Map.of("success", true, "voucher", toVoucherMap(saved)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deactivateVoucher(@PathVariable Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        voucher.setIsActive(false);
        voucherRepository.save(voucher);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private VoucherType readVoucherType(Map<String, Object> request) {
        String raw = String.valueOf(request.getOrDefault("discountType", request.getOrDefault("type", "FIXED"))).toUpperCase();
        return switch (raw) {
            case "PERCENT", "PERSEN" -> VoucherType.PERSEN;
            default -> VoucherType.NOMINAL;
        };
    }

    private BigDecimal readBigDecimal(Map<String, Object> request, String key, BigDecimal fallback) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Integer readInteger(Map<String, Object> request, String key, Integer fallback) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Number.class.isInstance(value)
                ? Number.class.cast(value).intValue()
                : Integer.parseInt(String.valueOf(value));
    }

    private Boolean readBoolean(Map<String, Object> request, String key, Boolean fallback) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDate readDate(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private Map<String, Object> toVoucherMap(Voucher voucher) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", voucher.getId());
        item.put("code", voucher.getCode());
        item.put("discountType", voucher.getType() == VoucherType.PERSEN ? "PERCENT" : "FIXED");
        item.put("discountValue", voucher.getValue());
        item.put("pointsCost", voucher.getPointsCost());
        item.put("minOrderValue", voucher.getMinOrderValue());
        item.put("validUntil", voucher.getValidUntil());
        item.put("isActive", voucher.getIsActive());
        item.put("usedCount", voucher.getUsedCount());
        item.put("usageLimit", voucher.getUsageLimit());
        item.put("createdAt", voucher.getCreatedAt());
        item.put("updatedAt", voucher.getUpdatedAt());
        return item;
    }
}
