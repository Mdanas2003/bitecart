package com.bitecart.payment;

public interface RazorpayGateway {

    RazorpayGatewayOrder createOrder(long amountInPaise, String receipt);
}
