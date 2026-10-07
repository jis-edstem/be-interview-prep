package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ProductVersionConflictException extends ErrorResponseException {

    public ProductVersionConflictException(Long id, Long requestedVersion, Long currentVersion) {
        super(
                HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Product " + id + " is at version " + currentVersion + " but the update was based on version "
                                + requestedVersion + "; reload it and retry"),
                null);
    }
}
