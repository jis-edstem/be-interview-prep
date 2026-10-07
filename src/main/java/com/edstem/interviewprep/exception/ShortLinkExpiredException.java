package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ShortLinkExpiredException extends ErrorResponseException {

    public ShortLinkExpiredException(String code) {
        super(
                HttpStatus.GONE,
                ProblemDetail.forStatusAndDetail(HttpStatus.GONE, "Short link " + code + " has expired"),
                null);
    }
}
