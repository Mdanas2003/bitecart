package com.bitecart.exception;

public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException() {
        super("We could not verify that payment. Your order remains pending; please try again.");
    }
}
