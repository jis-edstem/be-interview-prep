package com.edstem.interviewprep.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ShortCodeCollisionTest {

    private static final String TAKEN = "TAKEN123";
    private static final String BASE_URL = "http://localhost";

    private final ShortLinkService service;
    private final ShortLinkRepository repository;

    @MockitoBean
    private ShortCodeGenerator codeGenerator;

    ShortCodeCollisionTest(ShortLinkService service, ShortLinkRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @BeforeEach
    void storeLinkWithTakenCode() {
        repository.deleteAll();
        repository.save(new ShortLink(TAKEN, "https://example.com/existing", null));
    }

    @Test
    void collidingCodeIsRetriedWithANewOne() {
        given(codeGenerator.next()).willReturn(TAKEN, "FRESH456");

        ShortLinkResponse created = service.shorten(new ShortenRequest("https://example.com/new", null), BASE_URL);

        assertThat(created.code()).isEqualTo("FRESH456");
        assertThat(repository.findByCode(TAKEN).orElseThrow().getOriginalUrl()).isEqualTo("https://example.com/existing");
    }

    @Test
    void givesUpAfterMaxAttempts() {
        given(codeGenerator.next()).willReturn(TAKEN);

        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/new", null), BASE_URL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No unique short code after " + ShortLinkService.MAX_CODE_ATTEMPTS + " attempts");
        assertThat(repository.count()).isEqualTo(1);
    }
}
