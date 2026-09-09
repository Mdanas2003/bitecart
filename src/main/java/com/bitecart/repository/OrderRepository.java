package com.bitecart.repository;

import com.bitecart.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"address", "items", "items.product"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
