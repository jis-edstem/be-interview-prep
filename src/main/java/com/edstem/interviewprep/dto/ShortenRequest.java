package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.validation.HttpUrl;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record ShortenRequest(
        @NotBlank(message = "url is required")
        @HttpUrl(
                maxLength = ShortLink.URL_MAX_LENGTH,
                message = "url must be an absolute http or https URL of at most {maxLength} characters")
        String url,

        @Future(message = "expiresAt must be in the future") Instant expiresAt) {}
