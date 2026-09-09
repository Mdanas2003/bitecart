package com.bitecart.controller.user;

import com.bitecart.dto.AddressDTO;
import com.bitecart.dto.CartDTO;
import com.bitecart.dto.CheckoutForm;
import com.bitecart.dto.OrderDTO;
import com.bitecart.entity.Address;
import com.bitecart.exception.CouponValidationException;
import com.bitecart.exception.EmptyCartException;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.service.CartService;
import com.bitecart.service.CouponService;
import com.bitecart.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CheckoutController {

    private static final String CHECKOUT_COUPON_SESSION_KEY = "checkoutCouponCode";
    private static final BigDecimal DELIVERY_FEE = BigDecimal.ZERO;

    private final CartService cartService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final AddressRepository addressRepository;

    public CheckoutController(CartService cartService, CouponService couponService, OrderService orderService,
                              AddressRepository addressRepository) {
        this.cartService = cartService;
        this.couponService = couponService;
        this.orderService = orderService;
        this.addressRepository = addressRepository;
    }

    @GetMapping("/checkout")
    public String checkout(Model model, Authentication authentication, HttpSession session,
                           @ModelAttribute("checkoutForm") CheckoutForm checkoutForm) {
        CartDTO cart = cartService.getCurrentUsersCart();
        if (cart.items().isEmpty()) {
            return "redirect:/cart";
        }

        String couponCode = (String) session.getAttribute(CHECKOUT_COUPON_SESSION_KEY);
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponCode != null) {
            try {
                couponDiscount = couponService.calculateDiscount(couponCode, cart.subtotal());
            } catch (CouponValidationException | ResourceNotFoundException exception) {
                session.removeAttribute(CHECKOUT_COUPON_SESSION_KEY);
                model.addAttribute("checkoutError", "The applied coupon is no longer available.");
                couponCode = null;
            }
        }

        List<AddressDTO> addresses = addressesFor(authentication);
        if (checkoutForm.getAddressId() == null) {
            addresses.stream().filter(AddressDTO::defaultAddress).findFirst()
                    .or(() -> addresses.stream().findFirst())
                    .ifPresent(address -> checkoutForm.setAddressId(address.id()));
        }
        if (checkoutForm.getPaymentMethod() == null) {
            checkoutForm.setPaymentMethod(com.bitecart.entity.PaymentMethod.CASH_ON_DELIVERY);
        }

        BigDecimal finalTotal = cart.subtotal().subtract(couponDiscount).add(DELIVERY_FEE);
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addresses);
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("couponDiscount", couponDiscount);
        model.addAttribute("deliveryFee", DELIVERY_FEE);
        model.addAttribute("finalTotal", finalTotal);
        return "checkout";
    }

    @PostMapping("/checkout/coupon")
    public String applyCoupon(@RequestParam(required = false) String code, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            CartDTO cart = cartService.getCurrentUsersCart();
            if (cart.items().isEmpty()) {
                throw new EmptyCartException();
            }
            couponService.calculateDiscount(code, cart.subtotal());
            session.setAttribute(CHECKOUT_COUPON_SESSION_KEY, code.trim());
            redirectAttributes.addFlashAttribute("checkoutSuccess", "Coupon applied.");
        } catch (CouponValidationException | ResourceNotFoundException | EmptyCartException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("checkoutError", exception.getMessage());
        }
        return "redirect:/checkout";
    }

    @PostMapping("/checkout/coupon/remove")
    public String removeCoupon(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute(CHECKOUT_COUPON_SESSION_KEY);
        redirectAttributes.addFlashAttribute("checkoutSuccess", "Coupon removed.");
        return "redirect:/checkout";
    }

    @PostMapping("/checkout/order")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm checkoutForm,
                             BindingResult bindingResult, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("checkoutError", "Select a valid address and payment method.");
            return "redirect:/checkout";
        }

        try {
            String couponCode = (String) session.getAttribute(CHECKOUT_COUPON_SESSION_KEY);
            OrderDTO order = orderService.createOrder(checkoutForm.getAddressId(), checkoutForm.getPaymentMethod(), couponCode);
            session.removeAttribute(CHECKOUT_COUPON_SESSION_KEY);
            redirectAttributes.addFlashAttribute("orderSuccess", "Your order has been placed.");
            return "redirect:/orders/" + order.id() + "/success";
        } catch (ResourceNotFoundException | EmptyCartException | InsufficientStockException
                 | CouponValidationException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("checkoutError", exception.getMessage());
            return "redirect:/checkout";
        }
    }

    private List<AddressDTO> addressesFor(Authentication authentication) {
        return addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(authentication.getName()).stream()
                .map(this::toDto)
                .toList();
    }

    private AddressDTO toDto(Address address) {
        return new AddressDTO(address.getId(), address.getAddressLine1(), address.getAddressLine2(), address.getCity(),
                address.getState(), address.getPostalCode(), address.getCountry(), address.getPhone(), address.isDefaultAddress());
    }
}
