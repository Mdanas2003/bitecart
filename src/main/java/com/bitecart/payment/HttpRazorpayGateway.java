package com.bitecart.payment;

import com.bitecart.config.RazorpayProperties;
import com.bitecart.exception.PaymentGatewayException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class HttpRazorpayGateway implements RazorpayGateway {

    private final RestClient restClient;
    private final RazorpayProperties properties;

    public HttpRazorpayGateway(RestClient.Builder restClientBuilder, RazorpayProperties properties) {
        this.restClient = restClientBuilder.baseUrl("https://api.razorpay.com").build();
        this.properties = properties;
    }

    @Override
    public RazorpayGatewayOrder createOrder(long amountInPaise, String receipt) {
        if (!properties.isConfigured()) {
            throw new PaymentGatewayException("Online payment is not configured. Please choose cash on delivery or try again later.");
        }
        try {
            RazorpayGatewayOrder response = restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBasicAuth(properties.getKeyId(), properties.getKeySecret()))
                    .body(Map.of("amount", amountInPaise, "currency", "INR", "receipt", receipt))
                    .retrieve()
                    .body(RazorpayGatewayOrder.class);
            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new PaymentGatewayException("Online payment could not be started. Please try again.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new PaymentGatewayException("Online payment could not be started. Please try again.");
        }
    }
}
