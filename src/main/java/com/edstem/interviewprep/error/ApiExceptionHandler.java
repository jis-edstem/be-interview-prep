package com.edstem.interviewprep.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Request has invalid fields");
        problem.setProperty(ERRORS_PROPERTY, errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request body is malformed");
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            problem.setDetail("Request has invalid fields");
            problem.setProperty(ERRORS_PROPERTY, List.of(fieldError(mismatch)));
        }
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static FieldErrorResponse fieldError(MismatchedInputException mismatch) {
        String field = mismatch.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .reduce((parent, child) -> parent + "." + child)
                .orElseThrow();
        Class<?> targetType = mismatch.getTargetType();
        String message;
        if (targetType != null && targetType.isEnum()) {
            message = field + " must be one of " + Arrays.toString(targetType.getEnumConstants());
        } else if (mismatch instanceof InvalidFormatException invalid) {
            message = field + " has an invalid value '" + invalid.getValue() + "'";
        } else {
            message = field + " has the wrong type";
        }
        return new FieldErrorResponse(field, message);
    }
}
