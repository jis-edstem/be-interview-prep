package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class OrderNotFoundException extends ErrorResponseException {

    public OrderNotFoundException(Long id) {
        super(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order " + id + " not found"),
                null);
    }
}
