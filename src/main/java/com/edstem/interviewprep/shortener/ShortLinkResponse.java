package com.edstem.interviewprep.shortener;

import java.time.Instant;

public record ShortLinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt) {

    static ShortLinkResponse from(ShortLink link, String baseUrl) {
        return new ShortLinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getExpiresAt(),
                link.getCreatedAt());
    }
}
