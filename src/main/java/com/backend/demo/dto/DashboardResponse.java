package com.backend.demo.dto;

import com.backend.demo.model.UserTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private UserSummary user;

    private DashboardStats stats;

    private List<OrderSummary> recentOrders;

    private List<ProductSummary> wishlist;

    private List<ProductSummary> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private UserTier tier;
        private Integer points;
        private Integer pointsNext;
        private BigDecimal totalSpent;
        private Instant joinDate;
        private List<String> roles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private long totalOrders;
        private long activeOrders;
        private long wishlistCount;
        private int rewardPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private Long id;
        private String orderNumber;
        private String productName;
        private String productImage;
        private String status;
        private String statusCode;
        private BigDecimal total;
        private Instant createdAt;
        private String eta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private Long id;
        private String name;
        private BigDecimal price;
        private BigDecimal salePrice;
        private String imageUrl;
        private String tag;
        private BigDecimal rating;
        private String category;
        private Boolean inWishlist;
    }
}
