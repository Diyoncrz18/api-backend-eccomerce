package com.backend.demo.service.ai;

import com.backend.demo.model.Order;
import com.backend.demo.model.OrderStatus;
import com.backend.demo.model.User;
import com.backend.demo.model.Voucher;
import com.backend.demo.model.VoucherType;
import com.backend.demo.repository.CartItemRepository;
import com.backend.demo.repository.OrderRepository;
import com.backend.demo.repository.UserRepository;
import com.backend.demo.repository.VoucherRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Builds a snapshot of the authenticated user's account data so the chatbot can
 * answer personalized questions like "berapa pesanan saya?", "voucher apa
 * yang aktif untuk saya?", or "berapa item di keranjang saya?".
 *
 * Defensive: any failure produces an empty Optional — chat must never crash
 * because user-context lookup failed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextProvider {

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
        OrderStatus.MENUNGGU,
        OrderStatus.DIKEMAS,
        OrderStatus.DIKIRIM
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;

    @Transactional(readOnly = true)
    public Optional<UserContext> buildFor(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        try {
            return userRepository.findWithWishlistByEmail(email).map(this::snapshot);
        } catch (Exception e) {
            log.warn("UserContextProvider failed for email={}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    private UserContext snapshot(User user) {
        Long uid = user.getId();

        long totalOrders = safeCount(() -> orderRepository.countByUserId(uid));
        long activeOrders = safeCount(() -> orderRepository.countByUserIdAndStatusIn(uid, ACTIVE_STATUSES));
        long completedOrders = safeCount(() -> orderRepository.countByUserIdAndStatusIn(uid, List.of(OrderStatus.SELESAI)));

        List<Order> recent = orderRepository
            .findByUserId(uid, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent();

        long cartCount = safeCount(() -> cartItemRepository.countByUserId(uid));
        int wishlistCount = user.getWishlist() == null ? 0 : user.getWishlist().size();

        List<Voucher> activeVouchers = voucherRepository.findByIsActiveTrueOrderByValidUntilAsc()
            .stream()
            .filter(this::stillValid)
            .limit(5)
            .toList();

        return UserContext.builder()
            .name(user.getName())
            .email(user.getEmail())
            .tier(user.getTier() == null ? "REGULAR" : user.getTier().name())
            .points(user.getRewardPoints() == null ? 0 : user.getRewardPoints())
            .totalSpent(user.getTotalSpent() == null ? BigDecimal.ZERO : user.getTotalSpent())
            .totalOrders(totalOrders)
            .activeOrders(activeOrders)
            .completedOrders(completedOrders)
            .recentOrders(recent.stream().map(this::toRecentOrder).toList())
            .cartItemCount(cartCount)
            .wishlistCount(wishlistCount)
            .activeVouchers(activeVouchers.stream().map(this::toVoucherInfo).toList())
            .build();
    }

    private boolean stillValid(Voucher v) {
        if (v.getValidUntil() == null) return true;
        return !v.getValidUntil().isBefore(LocalDate.now());
    }

    private RecentOrder toRecentOrder(Order o) {
        return RecentOrder.builder()
            .orderNumber(o.getOrderNumber())
            .status(o.getStatus() == null ? "?" : o.getStatus().name())
            .total(o.getTotal() == null ? BigDecimal.ZERO : o.getTotal())
            .createdAt(o.getCreatedAt())
            .build();
    }

    private VoucherInfo toVoucherInfo(Voucher v) {
        return VoucherInfo.builder()
            .code(v.getCode())
            .type(v.getType() == null ? VoucherType.PERSEN : v.getType())
            .value(v.getValue())
            .minOrderValue(v.getMinOrderValue())
            .pointsCost(v.getPointsCost() == null ? 0 : v.getPointsCost())
            .validUntil(v.getValidUntil())
            .build();
    }

    private long safeCount(java.util.function.LongSupplier op) {
        try { return op.getAsLong(); } catch (Exception e) { return 0L; }
    }

    // ─── DTOs (immutable snapshots passed to PromptBuilder) ──────────────────

    @Data
    @Builder
    public static class UserContext {
        private String name;
        private String email;
        private String tier;
        private int points;
        private BigDecimal totalSpent;
        private long totalOrders;
        private long activeOrders;
        private long completedOrders;
        private List<RecentOrder> recentOrders;
        private long cartItemCount;
        private int wishlistCount;
        private List<VoucherInfo> activeVouchers;
    }

    @Data
    @Builder
    public static class RecentOrder {
        private String orderNumber;
        private String status;
        private BigDecimal total;
        private java.time.Instant createdAt;
    }

    @Data
    @Builder
    public static class VoucherInfo {
        private String code;
        private VoucherType type;
        private BigDecimal value;
        private BigDecimal minOrderValue;
        private int pointsCost;
        private LocalDate validUntil;
    }

    // unused helper kept for completeness (scope: future filters)
    @SuppressWarnings("unused")
    private static Collection<OrderStatus> activeStatuses() { return ACTIVE_STATUSES; }
}
