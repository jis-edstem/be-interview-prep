package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.ShortLink;
import com.edstem.interviewprep.repository.ShortLinkRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class VisitCountConcurrencyTest {

    private static final int THREADS = 20;
    private static final int VISITS = 200;

    private final ShortLinkService service;
    private final ShortLinkRepository repository;

    @Test
    void simultaneousVisitsAreAllCounted() {
        String code = repository
                .save(new ShortLink("popular1", "https://example.com/launch", null))
                .getCode();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            List<CompletableFuture<Void>> visits = IntStream.range(0, VISITS)
                    .mapToObj(i -> CompletableFuture.runAsync(
                            () -> {
                                awaitStart(start);
                                service.resolve(code);
                            },
                            pool))
                    .toList();
            start.countDown();
            CompletableFuture.allOf(visits.toArray(CompletableFuture[]::new)).join();
        }

        assertThat(repository.findByCode(code).orElseThrow().getVisitCount()).isEqualTo(VISITS);
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before visiting", e);
        }
    }
}
