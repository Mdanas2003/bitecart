package com.bitecart.exception;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException() {
        super("You have already reviewed this product for the selected order.");
    }
}
