package com.edstem.interviewprep.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERRORS_PROPERTY = "errors";

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
        if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
            problem.setDetail("Request has invalid fields");
            problem.setProperty(ERRORS_PROPERTY, List.of(fieldError(invalid)));
        }
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static FieldErrorResponse fieldError(InvalidFormatException invalid) {
        String field = invalid.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .reduce((parent, child) -> parent + "." + child)
                .orElseThrow();
        Class<?> targetType = invalid.getTargetType();
        String message = targetType.isEnum()
                ? field + " must be one of " + Arrays.toString(targetType.getEnumConstants())
                : field + " has an invalid value '" + invalid.getValue() + "'";
        return new FieldErrorResponse(field, message);
    }
}
