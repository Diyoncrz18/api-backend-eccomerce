package com.backend.demo.controller;

import com.backend.demo.dto.OrderRequest;
import com.backend.demo.dto.OrderResponse;
import com.backend.demo.model.OrderStatus;
import com.backend.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("User {} creating order", userId);
        
        OrderResponse order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.info("Getting order by id: {}", id);
        
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        
        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting orders for user: {}, page: {}, size: {}", userId, page, size);
        
        Page<OrderResponse> orders = orderService.getUserOrders(userId, page, size);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        
        log.info("Getting all orders, page: {}, size: {}, status: {}", page, size, status);
        
        Page<OrderResponse> orders = orderService.getAllOrders(page, size, status);
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        
        log.info("Updating order status: {}, new status: {}", id, status);
        
        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {
        
        log.info("Updating order: {}", id);
        
        OrderResponse order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(order);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.info("Cancelling order: {}", id);
        
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching orders with keyword: {}, page: {}, size: {}", keyword, page, size);
        
        Page<OrderResponse> orders = orderService.searchOrders(keyword, page, size);
        return ResponseEntity.ok(orders);
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.backend.demo.security.user.UserDetail userDetail) {
            return userDetail.getId();
        }
        
        throw new SecurityException("Unable to extract user ID from authentication");
    }
}
