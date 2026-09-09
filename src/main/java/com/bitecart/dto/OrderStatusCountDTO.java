package com.bitecart.dto;

import com.bitecart.entity.OrderStatus;

public record OrderStatusCountDTO(OrderStatus status, long count) {
}
