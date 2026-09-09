package com.bitecart.dto;

import java.time.LocalDateTime;

public record ReviewDTO(
        Long id,
        Long productId,
        Long orderId,
        String reviewerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
