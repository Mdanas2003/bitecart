package com.bitecart.service.impl;

import com.bitecart.config.RazorpayProperties;
import com.bitecart.dto.RazorpayCheckoutDTO;
import com.bitecart.entity.Order;
import com.bitecart.entity.PaymentMethod;
import com.bitecart.entity.PaymentStatus;
import com.bitecart.exception.PaymentGatewayException;
import com.bitecart.exception.PaymentVerificationException;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.payment.RazorpayGateway;
import com.bitecart.payment.RazorpayGatewayOrder;
import com.bitecart.repository.OrderRepository;
import com.bitecart.service.RazorpayPaymentService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@Transactional(readOnly = true)
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final String CURRENCY = "INR";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OrderRepository orderRepository;
    private final RazorpayGateway razorpayGateway;
    private final RazorpayProperties properties;

    public RazorpayPaymentServiceImpl(OrderRepository orderRepository, RazorpayGateway razorpayGateway,
                                      RazorpayProperties properties) {
        this.orderRepository = orderRepository;
        this.razorpayGateway = razorpayGateway;
        this.properties = properties;
    }

    @Override
    @Transactional
    public RazorpayCheckoutDTO startPayment(Long orderId) {
        Order order = ownedOnlinePendingOrder(orderId);
        if (order.getRazorpayOrderId() == null) {
            long amountInPaise = amountInPaise(order.getTotalAmount());
            RazorpayGatewayOrder gatewayOrder = razorpayGateway.createOrder(amountInPaise, receipt(order));
            if (gatewayOrder.amountInPaise() != amountInPaise || !CURRENCY.equals(gatewayOrder.currency())) {
                throw new PaymentGatewayException("Online payment could not be started. Please try again.");
            }
            order.setRazorpayOrderId(gatewayOrder.id());
        }
        return checkout(order);
    }

    @Override
    public RazorpayCheckoutDTO getPendingPayment(Long orderId) {
        Order order = ownedOnlinePendingOrder(orderId);
        if (order.getRazorpayOrderId() == null) {
            throw new PaymentGatewayException("Online payment has not started yet. Please retry.");
        }
        return checkout(order);
    }

    @Override
    @Transactional
    public void verifyPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String signature) {
        Order order = ownedOnlinePendingOrder(orderId);
        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(signature)
                || !MessageDigest.isEqual(order.getRazorpayOrderId().getBytes(StandardCharsets.UTF_8),
                razorpayOrderId.getBytes(StandardCharsets.UTF_8))
                || !signatureMatches(order.getRazorpayOrderId(), razorpayPaymentId, signature)) {
            throw new PaymentVerificationException();
        }
        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaymentStatus(PaymentStatus.PAID);
    }

    private Order ownedOnlinePendingOrder(Long orderId) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (order.getPaymentMethod() != PaymentMethod.ONLINE || order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentGatewayException("This online payment is no longer available.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentGatewayException("This payment cannot be retried.");
        }
        return order;
    }

    private RazorpayCheckoutDTO checkout(Order order) {
        return new RazorpayCheckoutDTO(order.getId(), order.getRazorpayOrderId(), amountInPaise(order.getTotalAmount()),
                CURRENCY, properties.getKeyId());
    }

    private boolean signatureMatches(String serverOrderId, String paymentId, String signature) {
        if (!properties.isConfigured()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getKeySecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] expected = mac.doFinal((serverOrderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            byte[] received = hexToBytes(signature);
            return received != null && MessageDigest.isEqual(expected, received);
        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] hexToBytes(String value) {
        if (value.length() % 2 != 0 || !value.matches("[0-9a-fA-F]+")) {
            return null;
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            bytes[index / 2] = (byte) Integer.parseInt(value.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private long amountInPaise(BigDecimal amount) {
        try {
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentGatewayException("Online payment could not be started. Please try again.");
        }
    }

    private String receipt(Order order) {
        return "bitecart-" + order.getId();
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to pay for an order.");
        }
        return authentication.getName();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
