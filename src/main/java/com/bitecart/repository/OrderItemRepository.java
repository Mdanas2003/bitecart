package com.bitecart.repository;

import com.bitecart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Optional<OrderItem> findByIdAndOrderUserEmailIgnoreCase(Long id, String email);
}
