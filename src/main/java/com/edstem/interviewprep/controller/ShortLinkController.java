package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.dto.ShortLinkResponse;
import com.edstem.interviewprep.dto.ShortLinkStats;
import com.edstem.interviewprep.dto.ShortenRequest;
import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.service.ShortLinkService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private static final String CODE_PATH = "/{code:[A-Za-z0-9]{" + ShortLink.CODE_LENGTH + "}}";

    private final ShortLinkService service;

    @PostMapping("/api/links")
    public ResponseEntity<ShortLinkResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        ShortLinkResponse created = service.shorten(request, baseUrl);
        return ResponseEntity.created(URI.create(created.shortUrl())).body(created);
    }

    @GetMapping("/api/links" + CODE_PATH + "/stats")
    public ShortLinkStats stats(@PathVariable String code) {
        return service.stats(code);
    }

    @GetMapping(CODE_PATH)
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(service.resolve(code)))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
