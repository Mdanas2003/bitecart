package com.bitecart.dto;

public record RazorpayCheckoutDTO(Long orderId, String razorpayOrderId, long amountInPaise, String currency,
                                  String keyId) {
}
