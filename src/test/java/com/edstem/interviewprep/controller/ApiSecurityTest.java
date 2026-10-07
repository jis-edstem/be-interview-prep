package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiSecurityTest {

    private final MockMvcTester mvc;

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
    void shortLinkRedirectStaysPublic() {
        assertThat(mvc.get().uri("/abcd1234")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
