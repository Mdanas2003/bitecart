package com.bitecart.repository;

import com.bitecart.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
