package com.backend.demo.service;

import com.backend.demo.dto.OrderRequest;
import com.backend.demo.dto.OrderResponse;
import com.backend.demo.handler.GlobalExceptionHandler.EntityNotFoundException;
import com.backend.demo.model.*;
import com.backend.demo.repository.OrderRepository;
import com.backend.demo.repository.ProductRepository;
import com.backend.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        log.info("Creating order for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.MENUNGGU)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress() != null ? request.getBillingAddress() : request.getShippingAddress())
                .customerNote(request.getCustomerNote())
                .tax(request.getTax())
                .shippingFee(request.getShippingFee())
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .build();
        
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (OrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + itemRequest.getProductId()));
            
            if (!product.getIsActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getName());
            }
            
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() + 
                        ". Available: " + product.getStock() + ", Requested: " + itemRequest.getQuantity());
            }
            
            BigDecimal salePrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .salePrice(salePrice)
                    .build();
            
            orderItem.calculateSubtotal();
            order.addOrderItem(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());
            
            // Update product stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
        }
        
        order.setSubtotal(subtotal);
        order.setTotal(subtotal
                .add(order.getTax())
                .add(order.getShippingFee())
                .subtract(order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO));
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", savedOrder.getOrderNumber());
        
        return OrderResponse.from(savedOrder);
    }
    
    @Override
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order by id: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        return OrderResponse.from(order);
    }
    
    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with number: " + orderNumber));
        
        return OrderResponse.from(order);
    }
    
    @Override
    public Page<OrderResponse> getUserOrders(Long userId, int page, int size) {
        log.info("Getting orders for user: {}, page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        
        return orders.map(OrderResponse::from);
    }
    
    @Override
    public Page<OrderResponse> getAllOrders(int page, int size, OrderStatus status) {
        log.info("Getting all orders, page: {}, size: {}, status: {}", page, size, status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = status != null 
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        
        return orders.map(OrderResponse::from);
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        log.info("Updating order status: {}, new status: {}", id, status);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        if (order.getStatus() == OrderStatus.DIBATALKAN || order.getStatus() == OrderStatus.SELESAI) {
            throw new IllegalArgumentException("Cannot update status of a " + order.getStatus() + " order");
        }
        
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        
        log.info("Order status updated: {} -> {}", id, status);
        return OrderResponse.from(updated);
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        log.info("Updating order: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        if (order.getStatus() != OrderStatus.MENUNGGU) {
            throw new IllegalArgumentException("Can only update MENUNGGU orders");
        }
        
        order.setShippingAddress(request.getShippingAddress());
        if (request.getBillingAddress() != null) {
            order.setBillingAddress(request.getBillingAddress());
        }
        order.setCustomerNote(request.getCustomerNote());
        order.setTax(request.getTax());
        order.setShippingFee(request.getShippingFee());
        order.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        
        Order updated = orderRepository.save(order);
        
        log.info("Order updated: {}", id);
        return OrderResponse.from(updated);
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.info("Cancelling order: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        
        if (order.getStatus() != OrderStatus.MENUNGGU && order.getStatus() != OrderStatus.DIKEMAS) {
            throw new IllegalArgumentException("Can only cancel MENUNGGU or DIKEMAS orders");
        }
        
        // Restore product stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        
        order.setStatus(OrderStatus.DIBATALKAN);
        orderRepository.save(order);
        
        log.info("Order cancelled: {}", id);
    }
    
    @Override
    public Page<OrderResponse> searchOrders(String keyword, int page, int size) {
        log.info("Searching orders with keyword: {}, page: {}, size: {}", keyword, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = keyword != null && !keyword.trim().isEmpty()
                ? orderRepository.findByOrderNumberContainingIgnoreCase(keyword.trim(), pageable)
                : orderRepository.findAll(pageable);
        
        return orders.map(OrderResponse::from);
    }
    
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }
}