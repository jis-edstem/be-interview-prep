package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.ShortLink;
import java.time.Instant;

public record ShortLinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt) {

    public static ShortLinkResponse from(ShortLink link, String baseUrl) {
        return new ShortLinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getExpiresAt(),
                link.getCreatedAt());
    }
}
