package com.bitecart.repository;

import com.bitecart.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = "product")
    List<Wishlist> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Wishlist> findByIdAndUserEmailIgnoreCase(Long id, String email);

    Optional<Wishlist> findByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    boolean existsByUserEmailIgnoreCaseAndProductId(String email, Long productId);
}
