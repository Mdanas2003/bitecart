package com.bitecart.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RazorpayGatewayOrder(String id, @JsonProperty("amount") long amountInPaise, String currency) {
}
