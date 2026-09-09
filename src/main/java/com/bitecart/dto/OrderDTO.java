package com.bitecart.dto;

import com.bitecart.entity.OrderStatus;
import com.bitecart.entity.PaymentMethod;
import com.bitecart.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Long id,
        Long addressId,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        String couponCode,
        LocalDateTime createdAt,
        List<OrderItemDTO> items
) {
}
