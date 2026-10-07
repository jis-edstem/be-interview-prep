package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ShortLinkNotFoundException extends ErrorResponseException {

    public ShortLinkNotFoundException(String code) {
        super(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Short link " + code + " not found"),
                null);
    }
}
