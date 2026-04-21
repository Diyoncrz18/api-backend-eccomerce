package com.backend.demo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    
    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    private String sku;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
    
    @DecimalMin(value = "0.0", message = "Sale price must be greater than or equal to 0")
    private BigDecimal salePrice;
    
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    private Integer stock;
    
    @NotNull(message = "Active status is required")
    private Boolean isActive;
    
    @NotNull(message = "New status is required")
    private Boolean isNew;
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    @Size(max = 100, message = "Material must not exceed 100 characters")
    private String material;
    
    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensions;
    
    @DecimalMin(value = "0.0", message = "Weight must be greater than or equal to 0")
    private BigDecimal weightKg;
    
    private Boolean assemblyRequired;
    
    @Min(value = 0, message = "Warranty months must be greater than or equal to 0")
    private Integer warrantyMonths;
    
    private Long categoryId;
    
    private Long collectionId;
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getSalePrice() {
        return salePrice;
    }
    
    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
}