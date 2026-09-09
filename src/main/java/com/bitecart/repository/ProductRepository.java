package com.bitecart.repository;

import com.bitecart.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findTop6ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    List<Product> findTop4ByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(Long categoryId, Long productId,
                                                                                  Pageable pageable);
}
