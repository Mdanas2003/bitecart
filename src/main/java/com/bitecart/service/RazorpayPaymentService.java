package com.bitecart.service;

import com.bitecart.dto.RazorpayCheckoutDTO;

public interface RazorpayPaymentService {

    RazorpayCheckoutDTO startPayment(Long orderId);

    RazorpayCheckoutDTO getPendingPayment(Long orderId);

    void verifyPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String signature);
}
