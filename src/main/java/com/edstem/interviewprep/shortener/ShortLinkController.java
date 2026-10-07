package com.edstem.interviewprep.shortener;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortLinkController {

    private final ShortLinkService service;

    public ShortLinkController(ShortLinkService service) {
        this.service = service;
    }

    @PostMapping("/api/links")
    public ResponseEntity<ShortLinkResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        ShortLinkResponse created = service.shorten(request, baseUrl);
        return ResponseEntity.created(URI.create(created.shortUrl())).body(created);
    }
}
