package com.backend.demo.service;

import com.backend.demo.dto.DashboardResponse;
import com.backend.demo.model.Order;
import com.backend.demo.model.OrderItem;
import com.backend.demo.model.OrderStatus;
import com.backend.demo.model.Product;
import com.backend.demo.model.User;
import com.backend.demo.model.UserTier;
import com.backend.demo.repository.OrderRepository;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.MENUNGGU,
            OrderStatus.DIKEMAS,
            OrderStatus.DIKIRIM
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getUserDashboard(String email) {
        User user = userRepository.findWithWishlistByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Order> recentOrders = orderRepository.findByUserId(
                user.getId(),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        Set<Product> wishlistProducts = user.getWishlist() != null ? user.getWishlist() : Set.of();
        List<Long> wishlistIds = wishlistProducts.stream().map(Product::getId).toList();

        List<DashboardResponse.ProductSummary> wishlist = wishlistProducts.stream()
                .limit(4)
                .map(product -> toProductSummary(product, true))
                .toList();

        List<DashboardResponse.ProductSummary> recommendations = productRepository
                .findDashboardRecommendations(PageRequest.of(0, 6))
                .getContent()
                .stream()
                .map(product -> toProductSummary(product, wishlistIds.contains(product.getId())))
                .toList();

        return DashboardResponse.builder()
                .user(toUserSummary(user))
                .stats(DashboardResponse.DashboardStats.builder()
                        .totalOrders(orderRepository.countByUserId(user.getId()))
                        .activeOrders(orderRepository.countByUserIdAndStatusIn(user.getId(), ACTIVE_STATUSES))
                        .wishlistCount(wishlistProducts.size())
                        .rewardPoints(user.getRewardPoints() != null ? user.getRewardPoints() : 0)
                        .build())
                .recentOrders(recentOrders.stream().map(this::toOrderSummary).toList())
                .wishlist(wishlist)
                .recommendations(recommendations)
                .build();
    }

    private DashboardResponse.UserSummary toUserSummary(User user) {
        int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;

        return DashboardResponse.UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .tier(user.getTier())
                .points(points)
                .pointsNext(nextTierPoints(user.getTier(), points))
                .totalSpent(user.getTotalSpent() != null ? user.getTotalSpent() : BigDecimal.ZERO)
                .joinDate(user.getJoinDate())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream().filter(Objects::nonNull).map(role -> role.getName()).toList()
                        : List.of())
                .build();
    }

    private DashboardResponse.OrderSummary toOrderSummary(Order order) {
        OrderItem firstItem = order.getOrderItems() != null && !order.getOrderItems().isEmpty()
                ? order.getOrderItems().get(0)
                : null;
        Product product = firstItem != null ? firstItem.getProduct() : null;

        return DashboardResponse.OrderSummary.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .productName(product != null ? product.getName() : "Pesanan")
                .productImage(product != null ? product.getImageUrl() : null)
                .status(order.getStatus().name())
                .statusCode(toStatusCode(order.getStatus()))
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .eta(null)
                .build();
    }

    private DashboardResponse.ProductSummary toProductSummary(Product product, boolean inWishlist) {
        return DashboardResponse.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .imageUrl(product.getImageUrl())
                .tag(Boolean.TRUE.equals(product.getIsNew()) ? "Baru" : null)
                .rating(product.getRating() != null ? product.getRating() : BigDecimal.ZERO)
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .inWishlist(inWishlist)
                .build();
    }

    private int nextTierPoints(UserTier tier, int points) {
        if (tier == UserTier.PLATINUM || tier == UserTier.ADMIN) {
            return Math.max(points, 1);
        }
        if (tier == UserTier.GOLD) {
            return 10000;
        }
        return 1000;
    }

    private String toStatusCode(OrderStatus status) {
        return switch (status) {
            case MENUNGGU -> "pending";
            case DIKEMAS -> "packing";
            case DIKIRIM -> "shipping";
            case SELESAI -> "done";
            case DIBATALKAN -> "cancelled";
        };
    }
}
