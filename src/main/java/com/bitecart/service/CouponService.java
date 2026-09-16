package com.bitecart.service;

import com.bitecart.dto.CouponApplicationDTO;
import com.bitecart.dto.CouponDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponDTO getCoupon(String code);

    BigDecimal calculateDiscount(String code, BigDecimal subtotal);

    CouponApplicationDTO applyCoupon(String code, BigDecimal subtotal);

    /**
     * Lists coupons that may be used against the supplied subtotal without recording usage.
     */
    List<CouponDTO> getApplicableCoupons(BigDecimal subtotal);
}
