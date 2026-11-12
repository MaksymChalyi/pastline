package com.pastline.search.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Фонова індексація. fixedDelay: наступна пачка лише після завершення попередньої, без накладання. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pastline.embedding.worker.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingScheduler {

    private final EmbeddingIndexer indexer;

    @Scheduled(fixedDelayString = "${pastline.embedding.worker.delay-ms:2000}")
    void indexPending() {
        indexer.indexPending();
    }
}
