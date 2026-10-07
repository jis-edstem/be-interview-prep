package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InsufficientStockException extends ErrorResponseException {

    public InsufficientStockException(Long productId, int quantity) {
        super(
                HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Product " + productId + " does not have " + quantity + " in stock; no items were reserved"),
                null);
    }
}
