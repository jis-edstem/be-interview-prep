package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.ShortLink;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    private static final int SAMPLE_SIZE = 1_000;

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void codesAreUrlSafeAndAtMostEightCharacters() {
        IntStream.range(0, SAMPLE_SIZE)
                .mapToObj(i -> generator.next())
                .forEach(code -> assertThat(code).matches("[A-Za-z0-9]{" + ShortLink.CODE_LENGTH + "}"));
    }
}
