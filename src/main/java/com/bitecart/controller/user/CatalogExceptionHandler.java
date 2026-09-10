package com.bitecart.controller.user;

import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.exception.CouponValidationException;
import com.bitecart.exception.EmptyCartException;
import com.bitecart.exception.InsufficientStockException;
import com.bitecart.exception.ReviewNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String resourceNotFound(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/admin/") ? "admin/not-found" : "catalog-not-found";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class, CouponValidationException.class,
            EmptyCartException.class, InsufficientStockException.class, ReviewNotAllowedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidRequest() {
        return "error/bad-request";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden() {
        return "error/forbidden";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpectedError() {
        return "error/unavailable";
    }
}
