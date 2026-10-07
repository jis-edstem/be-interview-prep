package com.edstem.interviewprep.shortener;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
        for (int attempt = 1; ; attempt++) {
            ShortLink link = new ShortLink(codeGenerator.next(), request.url(), request.expiresAt());
            try {
                return ShortLinkResponse.from(repository.saveAndFlush(link), baseUrl);
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_CODE_ATTEMPTS) {
                    throw new IllegalStateException(
                            "No unique short code after " + MAX_CODE_ATTEMPTS + " attempts", e);
                }
            }
        }
    }
}
