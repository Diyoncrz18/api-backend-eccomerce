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
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting order by id: {} for user: {}", id, userId);

        OrderResponse order = orderService.getOrderById(id);

        if (!order.getUser().getId().equals(userId) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.warn("User {} attempted to access order {} belonging to another user", userId, id);
            throw new SecurityException("You do not have permission to view this order");
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting order by number: {} for user: {}", orderNumber, userId);

        OrderResponse order = orderService.getOrderByNumber(orderNumber);

        if (!order.getUser().getId().equals(userId) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.warn("User {} attempted to access order {} belonging to another user", userId, orderNumber);
            throw new SecurityException("You do not have permission to view this order");
        }

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
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Updating order: {} by user: {}", id, userId);

        OrderResponse order = orderService.getOrderById(id);

        if (!order.getUser().getId().equals(userId) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.warn("User {} attempted to update order {} belonging to another user", userId, id);
            throw new SecurityException("You do not have permission to update this order");
        }

        order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Cancelling order: {} by user: {}", id, userId);

        OrderResponse order = orderService.getOrderById(id);

        if (!order.getUser().getId().equals(userId) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.warn("User {} attempted to cancel order {} belonging to another user", userId, id);
            throw new SecurityException("You do not have permission to cancel this order");
        }

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
