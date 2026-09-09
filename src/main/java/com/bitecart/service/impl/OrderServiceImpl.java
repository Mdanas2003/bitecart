package com.bitecart.service.impl;

import com.bitecart.dto.CouponApplicationDTO;
import com.bitecart.dto.OrderDTO;
import com.bitecart.dto.OrderItemDTO;
import com.bitecart.entity.Address;
import com.bitecart.entity.Cart;
import com.bitecart.entity.CartItem;
import com.bitecart.entity.Order;
import com.bitecart.entity.OrderItem;
import com.bitecart.entity.PaymentMethod;
import com.bitecart.entity.Product;
import com.bitecart.entity.User;
import com.bitecart.exception.EmptyCartException;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.ProductRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.service.CouponService;
import com.bitecart.service.OrderService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal DELIVERY_FEE = BigDecimal.ZERO;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository,
                            AddressRepository addressRepository, ProductRepository productRepository,
                            UserRepository userRepository, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
    }

    @Override
    @Transactional
    public OrderDTO createOrder(Long addressId, PaymentMethod paymentMethod, String couponCode) {
        if (addressId == null || paymentMethod == null) {
            throw new IllegalArgumentException("Address and payment method are required.");
        }

        String email = currentAuthenticatedEmail();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user was not found."));
        Address address = addressRepository.findByIdAndUserEmailIgnoreCase(addressId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        Cart cart = cartRepository.findByUserEmailIgnoreCase(email).orElseThrow(EmptyCartException::new);
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setPaymentMethod(paymentMethod);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : new ArrayList<>(cart.getItems())) {
            Product product = productRepository.findActiveByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", cartItem.getProduct().getId()));
            validateStock(product, cartItem.getQuantity());

            BigDecimal price = currentPrice(product);
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(price);
            item.setSubtotal(itemSubtotal);
            order.addItem(item);
        }

        CouponApplicationDTO couponApplication = applyCoupon(couponCode, subtotal);
        BigDecimal discountAmount = couponApplication == null ? BigDecimal.ZERO : couponApplication.discountAmount();
        BigDecimal deliveryFee = calculateDeliveryFee(subtotal, discountAmount);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(deliveryFee);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setCouponCode(couponApplication == null ? null : couponApplication.couponCode());

        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        return toDto(savedOrder);
    }

    @Override
    public OrderDTO getCurrentUsersOrder(Long orderId) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, currentAuthenticatedEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDto(order);
    }

    private CouponApplicationDTO applyCoupon(String couponCode, BigDecimal subtotal) {
        return couponCode == null || couponCode.isBlank() ? null : couponService.applyCoupon(couponCode, subtotal);
    }

    private BigDecimal calculateDeliveryFee(BigDecimal subtotal, BigDecimal discountAmount) {
        return DELIVERY_FEE;
    }

    private void validateStock(Product product, Integer quantity) {
        if (quantity == null || quantity <= 0 || product.getStockQuantity() == null || quantity > product.getStockQuantity()) {
            throw new InsufficientStockException();
        }
    }

    private BigDecimal currentPrice(Product product) {
        return product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(product.getPrice()) < 0
                ? product.getDiscountPrice() : product.getPrice();
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to access orders.");
        }
        return authentication.getName();
    }

    private OrderDTO toDto(Order order) {
        List<OrderItemDTO> items = order.getItems().stream().map(item -> new OrderItemDTO(
                item.getId(), item.getProduct().getId(), item.getProductName(), item.getQuantity(), item.getPrice(), item.getSubtotal()
        )).toList();
        return new OrderDTO(order.getId(), order.getAddress().getId(), order.getSubtotal(), order.getDiscountAmount(),
                order.getDeliveryFee(), order.getTotalAmount(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getOrderStatus(), order.getCouponCode(), order.getCreatedAt(), items);
    }
}
