package com.edstem.interviewprep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TaskVersionConflictException extends ErrorResponseException {

    public TaskVersionConflictException(Long id, Long requestedVersion, Long currentVersion) {
        super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Task " + id + " is at version " + currentVersion + " but the update was based on version "
                        + requestedVersion + "; reload it and retry"), null);
    }
}
