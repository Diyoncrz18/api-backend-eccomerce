package com.backend.demo.repository;

import com.backend.demo.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    Page<Product> findAllActive(Pageable pageable);
    
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    
    Page<Product> findByCollectionId(Long collectionId, Pageable pageable);
    
    Page<Product> findByIsNewTrue(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.sku LIKE %:keyword% OR p.name LIKE %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold")
    List<Product> findLowStock(@Param("threshold") Integer threshold);
    
    boolean existsBySku(String sku);
}
