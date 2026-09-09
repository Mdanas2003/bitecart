package com.bitecart.dto;

import com.bitecart.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class CheckoutForm {

    @NotNull(message = "Select a delivery address.")
    private Long addressId;

    @NotNull(message = "Select a payment method.")
    private PaymentMethod paymentMethod;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
