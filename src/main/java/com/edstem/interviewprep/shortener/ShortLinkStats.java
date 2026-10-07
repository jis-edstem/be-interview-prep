package com.edstem.interviewprep.shortener;

import java.time.Instant;

public record ShortLinkStats(
        String code,
        String originalUrl,
        long visitCount,
        Instant createdAt,
        Instant expiresAt) {

    static ShortLinkStats from(ShortLink link) {
        return new ShortLinkStats(
                link.getCode(),
                link.getOriginalUrl(),
                link.getVisitCount(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
