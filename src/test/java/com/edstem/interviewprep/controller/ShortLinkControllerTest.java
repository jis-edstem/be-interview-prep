package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.repository.ShortLinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@WithMockUser
@RequiredArgsConstructor
class ShortLinkControllerTest {

    private static final String LINKS = "/api/links";
    private static final String LONG_URL = "https://example.com/articles/2026/very/long/path?ref=newsletter";
    private static final String CODE_PATTERN = "[A-Za-z0-9]{" + ShortLink.CODE_LENGTH + "}";

    private final MockMvcTester mvc;
    private final ShortLinkRepository repository;

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

        assertThat(repository.findAll())
                .extracting(ShortLink::getCode)
                .doesNotHaveDuplicates()
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "not a url",
                "example.com/no-scheme",
                "ftp://example.com/file",
                "javascript:alert(1)",
                "https://",
                "http://my_host.example.com/x"
            })
    void invalidUrlIsRejected(String url) {
        MvcTestResult result = shorten("""
                {"url": "%s"}
                """.formatted(url));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("url");
        assertThat(repository.count()).isZero();
    }

    @Test
    void internationalisedUrlIsStoredAndRedirectedInAsciiForm() {
        MvcTestResult created = shorten("""
                {"url": "https://例え.jp/パス"}
                """);

        assertThat(created).hasStatus(HttpStatus.CREATED);
        String ascii = "https://xn--r8jz45g.jp/%E3%83%91%E3%82%B9";
        assertThat(created).bodyJson().extractingPath("$.originalUrl").isEqualTo(ascii);
        String code = repository.findAll().getFirst().getCode();
        assertThat(mvc.get().uri("/" + code).exchange().getResponse().getHeader("Location"))
                .isEqualTo(ascii);
    }

    @Test
    void urlTooLongOnceEncodedIsRejected() {
        String url = "https://example.com/" + "é".repeat(ShortLink.URL_MAX_LENGTH / 2);

        MvcTestResult result = shorten("""
                {"url": "%s"}
                """.formatted(url));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("url must be an absolute http or https URL of at most " + ShortLink.URL_MAX_LENGTH
                        + " characters");
    }

    @Test
    void missingUrlIsRejected() {
        MvcTestResult result = shorten("{}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("url is required");
    }

    @Test
    void blankUrlReportsOnlyThatItIsRequired() {
        MvcTestResult result = shorten("""
                {"url": ""}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[*].message")
                .asArray()
                .containsExactly("url is required");
    }

    @Test
    void pastExpiryIsRejected() {
        MvcTestResult result = shorten("""
                {"url": "%s", "expiresAt": "%s"}
                """.formatted(LONG_URL, Instant.now().minus(1, ChronoUnit.HOURS)));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("expiresAt must be in the future");
    }

    @Test
    void visitRedirectsToOriginalUrlAndIsCounted() {
        repository.save(new ShortLink("abc12345", LONG_URL, null));

        MvcTestResult result = mvc.get().uri("/abc12345").exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(LONG_URL);
        assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(repository.findByCode("abc12345").orElseThrow().getVisitCount())
                .isEqualTo(1);
    }

    @Test
    void unknownCodeReturnsNotFound() {
        MvcTestResult result = mvc.get().uri("/missing1").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Short link missing1 not found");
    }

    @Test
    void shorterRootPathsAreNotTreatedAsCodes() {
        MvcTestResult result = mvc.get().uri("/login").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().doesNotStartWith("Short link");
    }

    @Test
    void expiredCodeReturnsGoneAndIsNotCounted() {
        repository.save(new ShortLink("expired1", LONG_URL, Instant.now().minus(1, ChronoUnit.MINUTES)));

        MvcTestResult result = mvc.get().uri("/expired1").exchange();

        assertThat(result).hasStatus(HttpStatus.GONE);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Short link expired1 has expired");
        assertThat(repository.findByCode("expired1").orElseThrow().getVisitCount())
                .isZero();
    }

    @Test
    void statsShowOriginalUrlVisitCountAndCreatedDate() {
        repository.save(new ShortLink("stats123", LONG_URL, null));
        mvc.get().uri("/stats123").exchange();
        mvc.get().uri("/stats123").exchange();

        MvcTestResult result = mvc.get().uri(LINKS + "/stats123/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.originalUrl").isEqualTo(LONG_URL);
        assertThat(result).bodyJson().extractingPath("$.visitCount").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.createdAt").isNotNull();
    }

    @Test
    void statsRemainAvailableAfterExpiry() {
        repository.save(new ShortLink("expired2", LONG_URL, Instant.now().minus(1, ChronoUnit.MINUTES)));

        assertThat(mvc.get().uri(LINKS + "/expired2/stats")).hasStatusOk();
    }

    @Test
    void statsForUnknownCodeReturnNotFound() {
        assertThat(mvc.get().uri(LINKS + "/missing1/stats")).hasStatus(HttpStatus.NOT_FOUND);
    }

    private MvcTestResult shorten(String body) {
        return mvc.post()
                .uri(LINKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }
}
