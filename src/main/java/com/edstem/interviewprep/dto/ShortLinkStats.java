package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.ShortLink;
import java.time.Instant;

public record ShortLinkStats(
        String code,
        String originalUrl,
        long visitCount,
        Instant createdAt,
        Instant expiresAt) {

    public static ShortLinkStats from(ShortLink link) {
        return new ShortLinkStats(
                link.getCode(),
                link.getOriginalUrl(),
                link.getVisitCount(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
