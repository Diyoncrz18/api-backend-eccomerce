package com.backend.demo.service;

import com.backend.demo.dto.ProductRequest;
import com.backend.demo.dto.ProductResponse;
import com.backend.demo.handler.GlobalExceptionHandler.EntityNotFoundException;
import com.backend.demo.model.Category;
import com.backend.demo.model.Collection;
import com.backend.demo.model.Product;
import com.backend.demo.repository.CategoryRepository;
import com.backend.demo.repository.CollectionRepository;
import com.backend.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    
    public Page<ProductResponse> list(int page, int size, Long categoryId, Long collectionId, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products;
        
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else if (collectionId != null) {
            products = productRepository.findByCollectionId(collectionId, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            products = productRepository.findAllActive(pageable);
        }
        
        return products.map(ProductResponse::from);
    }
    
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return ProductResponse.from(product);
    }
    
    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating new product: {}", request.getSku());
        
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product with SKU already exists: " + request.getSku());
        }
        
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stock(request.getStock())
                .isActive(request.getIsActive())
                .isNew(request.getIsNew())
                .imageUrl(request.getImageUrl())
                .material(request.getMaterial())
                .dimensions(request.getDimensions())
                .weightKg(request.getWeightKg())
                .assemblyRequired(request.getAssemblyRequired() != null ? request.getAssemblyRequired() : false)
                .warrantyMonths(request.getWarrantyMonths())
                .build();
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }
        
        if (request.getCollectionId() != null) {
            Collection collection = collectionRepository.findById(request.getCollectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Collection not found with id: " + request.getCollectionId()));
            product.setCollection(collection);
        }
        
        Product saved = productRepository.save(product);
        log.info("Product created successfully: {}", saved.getId());
        
        return ProductResponse.from(saved);
    }
    
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Updating product: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product with SKU already exists: " + request.getSku());
        }
        
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice());
        product.setStock(request.getStock());
        product.setIsActive(request.getIsActive());
        product.setIsNew(request.getIsNew());
        product.setImageUrl(request.getImageUrl());
        product.setMaterial(request.getMaterial());
        product.setDimensions(request.getDimensions());
        product.setWeightKg(request.getWeightKg());
        product.setAssemblyRequired(request.getAssemblyRequired() != null ? request.getAssemblyRequired() : false);
        product.setWarrantyMonths(request.getWarrantyMonths());
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        
        if (request.getCollectionId() != null) {
            Collection collection = collectionRepository.findById(request.getCollectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Collection not found with id: " + request.getCollectionId()));
            product.setCollection(collection);
        } else {
            product.setCollection(null);
        }
        
        Product updated = productRepository.save(product);
        log.info("Product updated successfully: {}", id);
        
        return ProductResponse.from(updated);
    }
    
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        
        productRepository.deleteById(id);
        log.info("Product deleted: {}", id);
    }
}
