package com.bitecart.payment;

public record RazorpayGatewayOrder(String id, long amountInPaise, String currency) {
}
