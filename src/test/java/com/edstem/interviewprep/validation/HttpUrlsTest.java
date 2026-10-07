package com.edstem.interviewprep.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpUrlsTest {

    @ParameterizedTest
    @CsvSource({
        "https://example.com/a%20b?q=1#top, https://example.com/a%20b?q=1#top",
        "HTTP://Example.com, HTTP://Example.com",
        "https://例え.jp/パス?q=1, https://xn--r8jz45g.jp/%E3%83%91%E3%82%B9?q=1",
        "https://user@例え.jp:8443/a, https://user@xn--r8jz45g.jp:8443/a"
    })
    void convertsValidUrlsToAscii(String url, String expected) {
        assertThat(HttpUrls.toAscii(url)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "not a url",
                "example.com/no-scheme",
                "ftp://example.com",
                "javascript:alert(1)",
                "https://",
                "mailto:someone@example.com",
                "http://my_host.example.com/x"
            })
    void rejectsNonHttpOrHostlessUrls(String url) {
        assertThat(HttpUrls.toAscii(url)).isEmpty();
    }
}
