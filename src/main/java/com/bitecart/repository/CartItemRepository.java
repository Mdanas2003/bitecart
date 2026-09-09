package com.bitecart.repository;

import com.bitecart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartUserEmailIgnoreCase(Long id, String email);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
