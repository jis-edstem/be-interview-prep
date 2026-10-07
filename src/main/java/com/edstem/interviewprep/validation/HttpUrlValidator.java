package com.edstem.interviewprep.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    private int maxLength;

    @Override
    public void initialize(HttpUrl annotation) {
        maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return HttpUrls.toAscii(value)
                .filter(ascii -> ascii.length() <= maxLength)
                .isPresent();
    }
}
