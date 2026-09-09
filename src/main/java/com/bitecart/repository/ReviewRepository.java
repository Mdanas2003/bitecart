package com.bitecart.repository;

import com.bitecart.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    @Query("select avg(review.rating) from Review review where review.product.id = :productId")
    Double calculateAverageRatingByProductId(Long productId);
}
