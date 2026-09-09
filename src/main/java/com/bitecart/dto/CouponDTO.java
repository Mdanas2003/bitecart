package com.bitecart.dto;

import com.bitecart.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDTO(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscount,
        LocalDateTime expiryDate,
        Integer usageLimit,
        Integer usedCount,
        boolean active
) {
}
