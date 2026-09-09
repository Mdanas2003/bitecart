package com.bitecart.service.impl;

import com.bitecart.dto.ReviewDTO;
import com.bitecart.dto.ReviewSubmissionRequest;
import com.bitecart.dto.ReviewableOrderDTO;
import com.bitecart.entity.Order;
import com.bitecart.entity.OrderStatus;
import com.bitecart.entity.Product;
import com.bitecart.entity.Review;
import com.bitecart.exception.DuplicateReviewException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.exception.ReviewNotAllowedException;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.ReviewRepository;
import com.bitecart.service.ReviewService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository,
                             OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<ReviewDTO> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream().map(this::toDto).toList();
    }

    @Override
    public List<ReviewableOrderDTO> getCurrentUsersReviewableOrders(Long productId) {
        String email = currentAuthenticatedEmail();
        return orderRepository.findByUserEmailAndProductAndStatus(email, productId, OrderStatus.DELIVERED).stream()
                .filter(order -> !reviewRepository.existsByUserIdAndProductIdAndOrderId(
                        order.getUser().getId(), productId, order.getId()))
                .map(order -> new ReviewableOrderDTO(order.getId(), order.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public ReviewDTO submitReview(Long productId, ReviewSubmissionRequest request) {
        if (productId == null || request == null || request.getOrderId() == null || request.getRating() == null
                || request.getComment() == null || request.getRating() < 1 || request.getRating() > 5
                || request.getComment().trim().length() < 3 || request.getComment().trim().length() > 1000) {
            throw new IllegalArgumentException("Use a rating from 1 to 5 and a review comment between 3 and 1000 characters.");
        }

        String email = currentAuthenticatedEmail();
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(request.getOrderId(), email)
                .orElseThrow(ReviewNotAllowedException::new);
        if (order.getOrderStatus() != OrderStatus.DELIVERED || order.getItems().stream()
                .noneMatch(item -> item.getProduct().getId().equals(productId))) {
            throw new ReviewNotAllowedException();
        }
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(order.getUser().getId(), productId, order.getId())) {
            throw new DuplicateReviewException();
        }

        Product product = productRepository.findActiveByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Review review = new Review();
        review.setUser(order.getUser());
        review.setProduct(product);
        review.setOrder(order);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        Review savedReview = reviewRepository.saveAndFlush(review);

        Double average = reviewRepository.calculateAverageRatingByProductId(productId);
        product.setRating(average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP));
        return toDto(savedReview);
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to submit a review.");
        }
        return authentication.getName();
    }

    private ReviewDTO toDto(Review review) {
        return new ReviewDTO(review.getId(), review.getProduct().getId(), review.getOrder().getId(),
                review.getUser().getFirstName(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
