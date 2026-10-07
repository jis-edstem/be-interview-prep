package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.edstem.interviewprep.dto.ShortLinkResponse;
import com.edstem.interviewprep.dto.ShortenRequest;
import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ShortCodeCollisionTest {

    private static final String TAKEN = "TAKEN123";
    private static final String BASE_URL = "http://localhost";

    private final ShortLinkService service;
    private final ShortLinkRepository repository;

    @MockitoBean
    private ShortCodeGenerator codeGenerator;

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
        assertThat(repository.findByCode(TAKEN).orElseThrow().getOriginalUrl())
                .isEqualTo("https://example.com/existing");
    }

    @Test
    void givesUpAfterMaxAttempts() {
        given(codeGenerator.next()).willReturn(TAKEN);

        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/new", null), BASE_URL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No unique short code after " + ShortLinkService.MAX_CODE_ATTEMPTS + " attempts");
        then(codeGenerator).should(times(ShortLinkService.MAX_CODE_ATTEMPTS)).next();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void otherConstraintViolationsAreNotRetried() {
        given(codeGenerator.next()).willReturn("FRESH456");
        String tooLong = "https://example.com/" + "a".repeat(ShortLink.URL_MAX_LENGTH);

        assertThatThrownBy(() -> service.shorten(new ShortenRequest(tooLong, null), BASE_URL))
                .isInstanceOf(DataIntegrityViolationException.class);
        then(codeGenerator).should(times(1)).next();
    }
}
