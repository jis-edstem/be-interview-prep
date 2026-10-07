package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.ShortLinkResponse;
import com.edstem.interviewprep.dto.ShortLinkStats;
import com.edstem.interviewprep.dto.ShortenRequest;
import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.exception.ShortLinkExpiredException;
import com.edstem.interviewprep.exception.ShortLinkNotFoundException;
import com.edstem.interviewprep.repository.ShortLinkRepository;
import com.edstem.interviewprep.validation.HttpUrls;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

    static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortLinkRepository repository;
    private final ShortCodeGenerator codeGenerator;

    public ShortLinkService(ShortLinkRepository repository, ShortCodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    public ShortLinkResponse shorten(ShortenRequest request, String baseUrl) {
        String url = HttpUrls.toAscii(request.url())
                .orElseThrow(() -> new IllegalArgumentException("Not an http or https URL: " + request.url()));
        for (int attempt = 1; ; attempt++) {
            ShortLink link = new ShortLink(codeGenerator.next(), url, request.expiresAt());
            try {
                return ShortLinkResponse.from(repository.saveAndFlush(link), baseUrl);
            } catch (DataIntegrityViolationException e) {
                if (!repository.existsByCode(link.getCode())) {
                    throw e;
                }
                if (attempt == MAX_CODE_ATTEMPTS) {
                    throw new IllegalStateException("No unique short code after " + MAX_CODE_ATTEMPTS + " attempts", e);
                }
            }
        }
    }

    @Transactional
    public String resolve(String code) {
        ShortLink link = find(code);
        if (link.isExpiredAt(Instant.now())) {
            throw new ShortLinkExpiredException(code);
        }
        repository.incrementVisitCount(link.getId());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortLinkStats stats(String code) {
        return ShortLinkStats.from(find(code));
    }

    private ShortLink find(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ShortLinkNotFoundException(code));
    }
}
