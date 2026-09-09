package com.bitecart.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account already exists for this email address.");
    }
}
