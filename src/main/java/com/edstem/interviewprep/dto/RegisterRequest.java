package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = User.EMAIL_MAX_LENGTH, message = "email must be at most {max} characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = User.PASSWORD_MIN_LENGTH, message = "password must be at least {min} characters")
        @MaxUtf8Bytes(value = User.PASSWORD_MAX_BYTES, message = "password must be at most {value} bytes in UTF-8")
        String password) {}
