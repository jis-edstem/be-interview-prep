package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiSecurityTest {

    private final MockMvcTester mvc;
    private final JwtEncoder jwtEncoder;

    @Test
    void requestWithoutTokenReturnsJson401() {
        MvcTestResult result = mvc.get().uri("/api/tasks").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(401);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Authentication is required");
        assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void requestWithInvalidTokenReturnsJson401() {
        MvcTestResult result = mvc.get()
                .uri("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Access token is invalid or expired");
    }

    @Test
    void tokenIsRejectedOnceExpired() {
        Instant expiredAt = Instant.now().minusSeconds(1);
        String token = token(expiredAt.minus(Duration.ofMinutes(15)), expiredAt);

        MvcTestResult result = mvc.get()
                .uri("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Access token is invalid or expired");
    }

    @Test
    void shortLinkRedirectStaysPublic() {
        assertThat(mvc.get().uri("/abcd1234")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.head().uri("/abcd1234")).hasStatus(HttpStatus.NOT_FOUND);
    }

    private String token(Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
