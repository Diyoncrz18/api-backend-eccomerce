package com.backend.demo.service;

import com.backend.demo.dto.OrderRequest;
import com.backend.demo.dto.OrderResponse;
import com.backend.demo.model.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {
    
    OrderResponse createOrder(OrderRequest request, Long userId);
    
    OrderResponse getOrderById(Long id);
    
    OrderResponse getOrderByNumber(String orderNumber);
    
    Page<OrderResponse> getUserOrders(Long userId, int page, int size);
    
    Page<OrderResponse> getAllOrders(int page, int size, OrderStatus status);
    
    OrderResponse updateOrderStatus(Long id, OrderStatus status);
    
    OrderResponse updateOrder(Long id, OrderRequest request);
    
    void cancelOrder(Long id);
    
    Page<OrderResponse> searchOrders(String keyword, int page, int size);
}