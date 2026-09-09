package com.bitecart.dto;

import com.bitecart.entity.OrderStatus;

public record OrderTrackingStepDTO(OrderStatus status, String label, boolean completed, boolean current) {
}
