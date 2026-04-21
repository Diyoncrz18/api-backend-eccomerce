package com.backend.demo.repository;

import com.backend.demo.model.Order;
import com.backend.demo.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByDateRange(@Param("startDate") Instant startDate,
                                @Param("endDate") Instant endDate,
                                Pageable pageable);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'SELESAI'")
    BigDecimal getRevenueByDateRange(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);
    
    Page<Order> findByOrderNumberContainingIgnoreCase(String orderNumber, Pageable pageable);
}