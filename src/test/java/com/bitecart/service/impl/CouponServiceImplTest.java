package com.bitecart.service.impl;

import com.bitecart.dto.CouponDTO;
import com.bitecart.entity.Coupon;
import com.bitecart.entity.DiscountType;
import com.bitecart.repository.CouponRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CouponServiceImplTest {

    @Test
    void returnsOnlyCouponsApplicableToTheReadOnlyCartSubtotal() {
        Coupon applicable = coupon("SAVE50", new BigDecimal("100.00"), null, 0);
        Coupon belowMinimum = coupon("BIGORDER", new BigDecimal("1000.00"), null, 0);
        Coupon exhausted = coupon("USED", BigDecimal.ZERO, 1, 1);
        CouponRepository repository = mock(CouponRepository.class);
        when(repository.findByActiveTrueAndExpiryDateAfterOrderByExpiryDateAsc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(applicable, belowMinimum, exhausted));

        List<CouponDTO> coupons = new CouponServiceImpl(repository).getApplicableCoupons(new BigDecimal("500.00"));

        assertThat(coupons).extracting(CouponDTO::code).containsExactly("SAVE50");
    }

    private Coupon coupon(String code, BigDecimal minimum, Integer usageLimit, int usedCount) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(code);
        coupon.setDiscountType(DiscountType.FIXED);
        coupon.setDiscountValue(new BigDecimal("50.00"));
        coupon.setMinimumOrderAmount(minimum);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(usedCount);
        coupon.setActive(true);
        return coupon;
    }
}
