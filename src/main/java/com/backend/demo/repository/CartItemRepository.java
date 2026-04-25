package com.backend.demo.repository;

import com.backend.demo.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndProductIdAndVariant(Long userId, Long productId, String variant);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
