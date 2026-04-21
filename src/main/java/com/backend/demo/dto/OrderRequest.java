package com.backend.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;
    
    @Size(max = 500, message = "Billing address must not exceed 500 characters")
    private String billingAddress;
    
    @Size(max = 1000, message = "Customer note must not exceed 1000 characters")
    private String customerNote;
    
    @NotNull(message = "Tax is required")
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;
    
    @NotNull(message = "Shipping fee is required")
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    private BigDecimal discount;
    
    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}