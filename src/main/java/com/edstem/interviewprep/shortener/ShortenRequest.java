package com.edstem.interviewprep.shortener;

import com.edstem.interviewprep.validation.HttpUrl;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = ShortLink.URL_MAX_LENGTH, message = "url must be at most {max} characters")
        @HttpUrl(message = "url must be an absolute http or https URL")
        String url,

        @Future(message = "expiresAt must be in the future")
        Instant expiresAt) {
}
