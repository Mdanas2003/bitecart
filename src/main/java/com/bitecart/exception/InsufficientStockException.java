package com.bitecart.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException() {
        super("The requested quantity is not currently available.");
    }
}
