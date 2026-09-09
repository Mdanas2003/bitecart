package com.bitecart.exception;

public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException() {
        super("Reviews are available only for products in your delivered orders.");
    }
}
