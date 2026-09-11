package com.bitecart.repository;

import com.bitecart.entity.Order;
import com.bitecart.entity.OrderStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByAddressId(Long addressId);

    @EntityGraph(attributePaths = {"address", "items", "items.product"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);

    @EntityGraph(attributePaths = {"address", "items", "items.product"})
    List<Order> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    @Query("select distinct orderEntity from Order orderEntity join orderEntity.items item "
            + "where lower(orderEntity.user.email) = lower(:email) and item.product.id = :productId "
            + "and orderEntity.orderStatus = :orderStatus order by orderEntity.createdAt desc")
    List<Order> findByUserEmailAndProductAndStatus(@Param("email") String email,
                                                    @Param("productId") Long productId,
                                                    @Param("orderStatus") OrderStatus orderStatus);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime startInclusive,
                                                                     LocalDateTime endExclusive);
}
