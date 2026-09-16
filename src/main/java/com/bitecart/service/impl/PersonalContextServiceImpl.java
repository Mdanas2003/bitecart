package com.bitecart.service.impl;

import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CartItemDTO;
import com.bitecart.dto.CouponDTO;
import com.bitecart.dto.OrderDTO;
import com.bitecart.dto.OrderItemDTO;
import com.bitecart.dto.ReviewableOrderDTO;
import com.bitecart.dto.WishlistDTO;
import com.bitecart.dto.WishlistItemDTO;
import com.bitecart.service.CartService;
import com.bitecart.service.CouponService;
import com.bitecart.service.OrderService;
import com.bitecart.service.PersonalContextService;
import com.bitecart.service.ReviewService;
import com.bitecart.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PersonalContextServiceImpl implements PersonalContextService {

    private static final int MAX_ITEMS = 6;
    private static final int MAX_ORDERS = 5;

    private final CartService cartService;
    private final WishlistService wishlistService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final ReviewService reviewService;

    public PersonalContextServiceImpl(CartService cartService, WishlistService wishlistService,
                                      OrderService orderService, CouponService couponService,
                                      ReviewService reviewService) {
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.reviewService = reviewService;
    }

    @Override
    public String findPersonalContext(String customerQuestion) {
        String question = customerQuestion == null ? "" : customerQuestion.toLowerCase(Locale.ROOT);
        boolean cartQuestion = containsAny(question, "cart", "basket");
        boolean wishlistQuestion = containsAny(question, "wishlist", "wish list", "saved");
        boolean orderQuestion = containsAny(question, "order", "delivery", "delivered", "status", "tracking");
        boolean couponQuestion = containsAny(question, "coupon", "discount", "offer", "promo");
        boolean reviewQuestion = containsAny(question, "review", "rate", "rating");

        if (!cartQuestion && !wishlistQuestion && !orderQuestion && !couponQuestion && !reviewQuestion) {
            return "No personal BiteCart data was requested for this question.";
        }

        StringBuilder context = new StringBuilder("Read-only private context for the authenticated BiteCart customer only:");
        Optional<CartDTO> cart = Optional.empty();
        if (cartQuestion || couponQuestion) {
            cart = cartService.findCurrentUsersCart();
            appendCart(context, cart);
        }
        if (wishlistQuestion) {
            appendWishlist(context, wishlistService.getCurrentUsersWishlist());
        }

        List<OrderDTO> orders = List.of();
        if (orderQuestion || reviewQuestion) {
            orders = orderService.getCurrentUsersOrders();
            if (orderQuestion) appendOrders(context, orders);
        }
        if (couponQuestion) appendCoupons(context, cart);
        if (reviewQuestion) appendReviewEligibility(context, orders);

        context.append("\nThis context is read-only. Do not claim to perform cart, wishlist, coupon, order, payment, or review actions.");
        return context.toString();
    }

    private void appendCart(StringBuilder context, Optional<CartDTO> cart) {
        if (cart.isEmpty() || cart.get().items().isEmpty()) {
            context.append("\nCart: empty.");
            return;
        }
        CartDTO cartDto = cart.get();
        context.append("\nCart (subtotal ₹").append(money(cartDto.subtotal())).append(", ")
                .append(cartDto.totalItems()).append(" item(s)):");
        cartDto.items().stream().limit(MAX_ITEMS).forEach(item -> context.append("\n- ").append(item.productName())
                .append(" × ").append(item.quantity()).append(" (₹").append(money(item.lineTotal())).append(")"));
    }

    private void appendWishlist(StringBuilder context, WishlistDTO wishlist) {
        if (wishlist.items().isEmpty()) {
            context.append("\nWishlist: empty.");
            return;
        }
        context.append("\nWishlist:");
        wishlist.items().stream().limit(MAX_ITEMS).forEach(item -> context.append("\n- ").append(item.productName())
                .append(" (current price ₹").append(money(currentPrice(item))).append(")"));
    }

    private void appendOrders(StringBuilder context, List<OrderDTO> orders) {
        if (orders.isEmpty()) {
            context.append("\nOrders: no orders found.");
            return;
        }
        context.append("\nRecent orders:");
        orders.stream().limit(MAX_ORDERS).forEach(order -> context.append("\n- Order #").append(order.id())
                .append(": ").append(order.orderStatus()).append(" | total ₹")
                .append(money(order.totalAmount())));
    }

    private void appendCoupons(StringBuilder context, Optional<CartDTO> cart) {
        if (cart.isEmpty() || cart.get().subtotal().compareTo(BigDecimal.ZERO) <= 0) {
            context.append("\nApplicable coupons: add items to the cart to check eligibility.");
            return;
        }
        List<CouponDTO> coupons = couponService.getApplicableCoupons(cart.get().subtotal());
        if (coupons.isEmpty()) {
            context.append("\nApplicable coupons: none for the current cart subtotal.");
            return;
        }
        context.append("\nApplicable coupons for the current cart:");
        coupons.stream().limit(MAX_ITEMS).forEach(coupon -> context.append("\n- ").append(coupon.code())
                .append(": ").append(coupon.discountType()).append(" ").append(money(coupon.discountValue())));
    }

    private void appendReviewEligibility(StringBuilder context, List<OrderDTO> orders) {
        Map<Long, String> purchasedProducts = new LinkedHashMap<>();
        for (OrderDTO order : orders) {
            for (OrderItemDTO item : order.items()) {
                if (purchasedProducts.size() >= MAX_ITEMS) break;
                if (item.productId() != null) purchasedProducts.putIfAbsent(item.productId(), item.productName());
            }
            if (purchasedProducts.size() >= MAX_ITEMS) break;
        }
        if (purchasedProducts.isEmpty()) {
            context.append("\nReview eligibility: no purchased products are available to review.");
            return;
        }
        context.append("\nReview eligibility:");
        purchasedProducts.forEach((productId, productName) -> {
            List<ReviewableOrderDTO> reviewableOrders = reviewService.getCurrentUsersReviewableOrders(productId);
            context.append("\n- ").append(productName).append(": ")
                    .append(reviewableOrders.isEmpty() ? "not currently eligible" : "eligible to review");
        });
    }

    private BigDecimal currentPrice(WishlistItemDTO item) {
        return item.discountPrice() != null && item.discountPrice().compareTo(item.price()) < 0
                ? item.discountPrice() : item.price();
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private boolean containsAny(String question, String... terms) {
        for (String term : terms) {
            if (question.contains(term)) return true;
        }
        return false;
    }
}
