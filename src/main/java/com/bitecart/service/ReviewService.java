package com.bitecart.service;

import com.bitecart.dto.ReviewDTO;
import com.bitecart.dto.ReviewSubmissionRequest;
import com.bitecart.dto.ReviewableOrderDTO;

import java.util.List;

public interface ReviewService {

    List<ReviewDTO> getProductReviews(Long productId);

    List<ReviewableOrderDTO> getCurrentUsersReviewableOrders(Long productId);

    ReviewDTO submitReview(Long productId, ReviewSubmissionRequest request);
}
