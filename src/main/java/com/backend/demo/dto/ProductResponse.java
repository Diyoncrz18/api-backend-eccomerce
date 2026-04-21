package com.backend.demo.dto;

import com.backend.demo.model.Category;
import com.backend.demo.model.Collection;
import com.backend.demo.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private Long id;
    
    private String sku;
    
    private String name;
    
    private String description;
    
    private BigDecimal price;
    
    private BigDecimal salePrice;
    
    private Integer stock;
    
    private Boolean isActive;
    
    private Boolean isNew;
    
    private String imageUrl;
    
    private String material;
    
    private String dimensions;
    
    private BigDecimal weightKg;
    
    private Boolean assemblyRequired;
    
    private Integer warrantyMonths;
    
    private BigDecimal rating;
    
    private Integer reviewCount;
    
    private CategoryResponse category;
    
    private CollectionResponse collection;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    public static ProductResponse from(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .stock(product.getStock())
                .isActive(product.getIsActive())
                .isNew(product.getIsNew())
                .imageUrl(product.getImageUrl())
                .material(product.getMaterial())
                .dimensions(product.getDimensions())
                .weightKg(product.getWeightKg())
                .assemblyRequired(product.getAssemblyRequired())
                .warrantyMonths(product.getWarrantyMonths())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());
        
        if (product.getCategory() != null) {
            builder.category(CategoryResponse.from(product.getCategory()));
        }
        
        if (product.getCollection() != null) {
            builder.collection(CollectionResponse.from(product.getCollection()));
        }
        
        return builder.build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        
        public static CategoryResponse from(Category category) {
            return CategoryResponse.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .description(category.getDescription())
                    .imageUrl(category.getImageUrl())
                    .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionResponse {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        
        public static CollectionResponse from(Collection collection) {
            return CollectionResponse.builder()
                    .id(collection.getId())
                    .name(collection.getName())
                    .description(collection.getDescription())
                    .imageUrl(collection.getImageUrl())
                    .build();
        }
    }
}