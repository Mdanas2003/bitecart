package com.bitecart.service.impl;

import com.bitecart.dto.CouponApplicationDTO;
import com.bitecart.dto.CouponDTO;
import com.bitecart.entity.Coupon;
import com.bitecart.entity.DiscountType;
import com.bitecart.exception.CouponValidationException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.CouponRepository;
import com.bitecart.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public CouponDTO getCoupon(String code) {
        Coupon coupon = findCoupon(code);
        return toDto(coupon);
    }

    @Override
    public BigDecimal calculateDiscount(String code, BigDecimal subtotal) {
        Coupon coupon = findCoupon(code);
        return validateAndCalculate(coupon, subtotal);
    }

    @Override
    @Transactional
    public CouponApplicationDTO applyCoupon(String code, BigDecimal subtotal) {
        Coupon coupon = findCouponWithLock(code);
        BigDecimal discount = validateAndCalculate(coupon, subtotal);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        return new CouponApplicationDTO(coupon.getId(), coupon.getCode(), discount);
    }

    private Coupon findCoupon(String code) {
        return couponRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", null));
    }

    private Coupon findCouponWithLock(String code) {
        return couponRepository.findWithLockByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", null));
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CouponValidationException("A coupon code is required.");
        }
        return code.trim();
    }

    private BigDecimal validateAndCalculate(Coupon coupon, BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal must not be negative.");
        }
        if (!coupon.isActive()) {
            throw new CouponValidationException("This coupon is not active.");
        }
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CouponValidationException("This coupon has expired.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponValidationException("This coupon has reached its usage limit.");
        }
        if (subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new CouponValidationException("This coupon requires a higher order subtotal.");
        }

        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(coupon.getDiscountValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();
        if (coupon.getMaximumDiscount() != null) {
            discount = discount.min(coupon.getMaximumDiscount());
        }
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private CouponDTO toDto(Coupon coupon) {
        return new CouponDTO(coupon.getId(), coupon.getCode(), coupon.getDescription(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMinimumOrderAmount(), coupon.getMaximumDiscount(),
                coupon.getExpiryDate(), coupon.getUsageLimit(), coupon.getUsedCount(), coupon.isActive());
    }
}
