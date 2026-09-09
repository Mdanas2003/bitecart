package com.bitecart.dto;

import com.bitecart.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderTrackingDTO(
        Long orderId,
        OrderStatus orderStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderTrackingStepDTO> steps
) {
}
