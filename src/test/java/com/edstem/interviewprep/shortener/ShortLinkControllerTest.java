package com.edstem.interviewprep.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ShortLinkControllerTest {

    private static final String LINKS = "/api/links";
    private static final String LONG_URL = "https://example.com/articles/2026/very/long/path?ref=newsletter";
    private static final String CODE_PATTERN = "[A-Za-z0-9]{" + ShortLink.CODE_LENGTH + "}";

    private final MockMvcTester mvc;
    private final ShortLinkRepository repository;

    ShortLinkControllerTest(MockMvcTester mvc, ShortLinkRepository repository) {
        this.mvc = mvc;
        this.repository = repository;
    }

    @BeforeEach
    void clearLinks() {
        repository.deleteAll();
    }

    @Test
    void shortenReturnsCodeAndShortUrl() {
        MvcTestResult result = shorten("""
                {"url": "%s"}
                """.formatted(LONG_URL));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        ShortLink stored = repository.findAll().getFirst();
        assertThat(stored.getCode()).matches(CODE_PATTERN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(stored.getCode());
        assertThat(result).bodyJson().extractingPath("$.shortUrl").isEqualTo("http://localhost/" + stored.getCode());
        assertThat(result).bodyJson().extractingPath("$.originalUrl").isEqualTo(LONG_URL);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/" + stored.getCode());
    }

    @Test
    void shortenStoresOptionalExpiry() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        MvcTestResult result = shorten("""
                {"url": "%s", "expiresAt": "%s"}
                """.formatted(LONG_URL, expiresAt));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(repository.findAll().getFirst().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void shorteningSameUrlTwiceCreatesIndependentLinks() {
        String body = """
                {"url": "%s"}
                """.formatted(LONG_URL);

        assertThat(shorten(body)).hasStatus(HttpStatus.CREATED);
        assertThat(shorten(body)).hasStatus(HttpStatus.CREATED);

        assertThat(repository.findAll()).extracting(ShortLink::getCode).doesNotHaveDuplicates().hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a url", "example.com/no-scheme", "ftp://example.com/file", "javascript:alert(1)", "https://"})
    void invalidUrlIsRejected(String url) {
        MvcTestResult result = shorten("""
                {"url": "%s"}
                """.formatted(url));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("url");
        assertThat(repository.count()).isZero();
    }

    @Test
    void missingUrlIsRejected() {
        MvcTestResult result = shorten("{}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("url is required");
    }

    @Test
    void pastExpiryIsRejected() {
        MvcTestResult result = shorten("""
                {"url": "%s", "expiresAt": "%s"}
                """.formatted(LONG_URL, Instant.now().minus(1, ChronoUnit.HOURS)));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("expiresAt must be in the future");
    }

    private MvcTestResult shorten(String body) {
        return mvc.post().uri(LINKS).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
