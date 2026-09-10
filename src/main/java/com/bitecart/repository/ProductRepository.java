package com.bitecart.repository;

import com.bitecart.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :productId and product.active = true")
    Optional<Product> findActiveByIdForUpdate(Long productId);

    List<Product> findTop6ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    List<Product> findTop4ByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(Long categoryId, Long productId,
                                                                                  Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
