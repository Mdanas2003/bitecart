package com.bitecart.dto;

import java.math.BigDecimal;

public record CouponApplicationDTO(
        Long couponId,
        String couponCode,
        BigDecimal discountAmount
) {
}
