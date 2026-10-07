package com.edstem.interviewprep.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class IdempotencyKeyReusedException extends ErrorResponseException {

    public IdempotencyKeyReusedException(UUID key) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Idempotency-Key " + key + " was already used for a different order"),
                null);
    }
}
