package com.bitecart.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Your cart is empty.");
    }
}
