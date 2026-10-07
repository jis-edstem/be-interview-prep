package com.edstem.interviewprep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "short_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortLink {

    public static final int CODE_LENGTH = 8;
    public static final int URL_MAX_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = CODE_LENGTH)
    private String code;

    @Column(nullable = false, length = URL_MAX_LENGTH)
    private String originalUrl;

    private Instant expiresAt;

    @Column(nullable = false)
    private long visitCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ShortLink(String code, String originalUrl, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    public boolean isExpiredAt(Instant instant) {
        return expiresAt != null && !instant.isBefore(expiresAt);
    }
}
