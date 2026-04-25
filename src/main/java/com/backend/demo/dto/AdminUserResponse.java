package com.backend.demo.dto;

import com.backend.demo.model.User;
import com.backend.demo.model.UserTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserTier tier;
    private Integer rewardPoints;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private Boolean isActive;
    private Instant joinDate;
    private List<String> roles;
    private Instant createdAt;
    private Instant updatedAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .tier(user.getTier())
                .rewardPoints(user.getRewardPoints())
                .totalOrders(user.getTotalOrders())
                .totalSpent(user.getTotalSpent())
                .isActive(user.getIsActive())
                .joinDate(user.getJoinDate())
                .roles(user.getRoles().stream().map(r -> r.getName()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
