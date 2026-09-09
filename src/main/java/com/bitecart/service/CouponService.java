package com.bitecart.service;

import com.bitecart.dto.CouponApplicationDTO;
import com.bitecart.dto.CouponDTO;

import java.math.BigDecimal;

public interface CouponService {

    CouponDTO getCoupon(String code);

    BigDecimal calculateDiscount(String code, BigDecimal subtotal);

    CouponApplicationDTO applyCoupon(String code, BigDecimal subtotal);
}
