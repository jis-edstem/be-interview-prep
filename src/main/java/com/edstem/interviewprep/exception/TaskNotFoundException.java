package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TaskNotFoundException extends ErrorResponseException {

    public TaskNotFoundException(Long id) {
        super(
                HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Task " + id + " not found"),
                null);
    }
}
