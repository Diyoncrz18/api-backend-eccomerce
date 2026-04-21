package com.backend.demo.dto;

import com.backend.demo.model.Order;
import com.backend.demo.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    
    private String orderNumber;
    
    private UserResponse user;
    
    private OrderStatus status;
    
    private BigDecimal subtotal;
    
    private BigDecimal tax;
    
    private BigDecimal shippingFee;
    
    private BigDecimal discount;
    
    private BigDecimal total;
    
    private String shippingAddress;
    
    private String billingAddress;
    
    private String customerNote;
    
    private String adminNote;
    
    private List<OrderItemResponse> orderItems;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    public static OrderResponse from(Order order) {
        OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .customerNote(order.getCustomerNote())
                .adminNote(order.getAdminNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt());
        
        if (order.getUser() != null) {
            builder.user(UserResponse.from(order.getUser()));
        }
        
        if (order.getOrderItems() != null) {
            builder.orderItems(order.getOrderItems().stream()
                    .map(OrderItemResponse::from)
                    .collect(Collectors.toList()));
        }
        
        return builder.build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        
        private Long id;
        
        private ProductResponse product;
        
        private Integer quantity;
        
        private BigDecimal unitPrice;
        
        private BigDecimal salePrice;
        
        private BigDecimal subtotal;
        
        public static OrderItemResponse from(com.backend.demo.model.OrderItem orderItem) {
            OrderItemResponseBuilder builder = OrderItemResponse.builder()
                    .id(orderItem.getId())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .salePrice(orderItem.getSalePrice())
                    .subtotal(orderItem.getSubtotal());
            
            if (orderItem.getProduct() != null) {
                builder.product(ProductResponse.from(orderItem.getProduct()));
            }
            
            return builder.build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        
public static UserResponse from(com.backend.demo.model.User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .build();
    }
    }
}