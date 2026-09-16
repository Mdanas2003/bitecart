package com.bitecart.service.impl;

import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CartItemDTO;
import com.bitecart.dto.CouponDTO;
import com.bitecart.dto.OrderDTO;
import com.bitecart.dto.OrderItemDTO;
import com.bitecart.dto.WishlistDTO;
import com.bitecart.entity.OrderStatus;
import com.bitecart.entity.DiscountType;
import com.bitecart.entity.PaymentMethod;
import com.bitecart.entity.PaymentStatus;
import com.bitecart.service.CartService;
import com.bitecart.service.CouponService;
import com.bitecart.service.OrderService;
import com.bitecart.service.ReviewService;
import com.bitecart.service.WishlistService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalContextServiceImplTest {

    private final CartService cartService = mock(CartService.class);
    private final WishlistService wishlistService = mock(WishlistService.class);
    private final OrderService orderService = mock(OrderService.class);
    private final CouponService couponService = mock(CouponService.class);
    private final ReviewService reviewService = mock(ReviewService.class);
    private final PersonalContextServiceImpl service = new PersonalContextServiceImpl(cartService, wishlistService,
            orderService, couponService, reviewService);

    @Test
    void readsExistingCartOnlyThroughTheOwnershipScopedNonCreatingService() {
        when(cartService.findCurrentUsersCart()).thenReturn(Optional.of(new CartDTO(1L,
                List.of(new CartItemDTO(10L, 100L, "Owner's Biryani", null, 2,
                        new BigDecimal("250.00"), new BigDecimal("500.00"))), 2, new BigDecimal("500.00"))));

        String context = service.findPersonalContext("What is in my cart?");

        assertThat(context).contains("Owner's Biryani").contains("₹500.00");
        verify(cartService).findCurrentUsersCart();
        verify(cartService, never()).getCurrentUsersCart();
        verifyNoPersonalServicesExceptCart();
    }

    @Test
    void includesOnlyTheOrdersReturnedByTheCurrentUsersScopedOrderService() {
        OrderDTO ownersOrder = new OrderDTO(11L, 1L, new BigDecimal("300.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("300.00"), PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING,
                OrderStatus.PREPARING, null, LocalDateTime.now(), List.of(new OrderItemDTO(1L, 5L,
                "Owner's Shawarma", 1, new BigDecimal("300.00"), new BigDecimal("300.00"))));
        when(orderService.getCurrentUsersOrders()).thenReturn(List.of(ownersOrder));

        String context = service.findPersonalContext("What is my order status?");

        assertThat(context).contains("Order #11: PREPARING").doesNotContain("other customer");
        verify(orderService).getCurrentUsersOrders();
        verify(cartService, never()).findCurrentUsersCart();
        verify(wishlistService, never()).getCurrentUsersWishlist();
        verify(couponService, never()).getApplicableCoupons(org.mockito.ArgumentMatchers.any());
        verify(reviewService, never()).getCurrentUsersReviewableOrders(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void derivesCouponAndReviewContextOnlyFromTheCurrentUsersScopedSnapshots() {
        when(cartService.findCurrentUsersCart()).thenReturn(Optional.of(new CartDTO(1L, List.of(), 0,
                new BigDecimal("500.00"))));
        when(couponService.getApplicableCoupons(new BigDecimal("500.00"))).thenReturn(List.of(new CouponDTO(1L,
                "OWNER50", "Owner's eligible offer", DiscountType.FIXED, new BigDecimal("50.00"), BigDecimal.ZERO,
                null, LocalDateTime.now().plusDays(1), null, 0, true)));
        OrderDTO ownersOrder = new OrderDTO(8L, 1L, new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("250.00"), PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, OrderStatus.DELIVERED,
                null, LocalDateTime.now(), List.of(new OrderItemDTO(2L, 42L, "Owner's Waffle", 1,
                new BigDecimal("250.00"), new BigDecimal("250.00"))));
        when(orderService.getCurrentUsersOrders()).thenReturn(List.of(ownersOrder));
        when(reviewService.getCurrentUsersReviewableOrders(42L)).thenReturn(List.of());

        String context = service.findPersonalContext("Which coupons can I use and what can I review?");

        assertThat(context).contains("OWNER50").contains("Owner's Waffle: not currently eligible");
        verify(cartService).findCurrentUsersCart();
        verify(couponService).getApplicableCoupons(new BigDecimal("500.00"));
        verify(orderService).getCurrentUsersOrders();
        verify(reviewService).getCurrentUsersReviewableOrders(42L);
        verify(wishlistService, never()).getCurrentUsersWishlist();
    }

    private void verifyNoPersonalServicesExceptCart() {
        verify(wishlistService, never()).getCurrentUsersWishlist();
        verify(orderService, never()).getCurrentUsersOrders();
        verify(couponService, never()).getApplicableCoupons(org.mockito.ArgumentMatchers.any());
        verify(reviewService, never()).getCurrentUsersReviewableOrders(org.mockito.ArgumentMatchers.anyLong());
    }
}
