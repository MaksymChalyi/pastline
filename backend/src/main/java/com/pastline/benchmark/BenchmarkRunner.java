package com.pastline.benchmark;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Запуск: {@code .\mvnw spring-boot:run "-Dspring-boot.run.profiles=benchmark"} з папки backend. Без веб-сервера (див.
 * application-benchmark.properties): прогін, звіт, вихід.
 */
@Slf4j
@Component
@Profile("benchmark")
@RequiredArgsConstructor
public class BenchmarkRunner implements ApplicationRunner {

    private final BenchmarkService benchmarkService;
    private final ConfigurableApplicationContext context;

    @Value("${pastline.benchmark.corpus}")
    private Path corpus;

    @Value("${pastline.benchmark.queries}")
    private Path queries;

    @Value("${pastline.benchmark.output}")
    private Path output;

    @Value("${pastline.benchmark.user}")
    private String user;

    @Value("${pastline.benchmark.warmup}")
    private int warmup;

    @Value("${pastline.benchmark.repeats}")
    private int repeats;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0;
        try {
            benchmarkService.run(new BenchmarkConfig(corpus, queries, output, user, warmup, repeats));
        } catch (Exception e) {
            log.error("Бенчмарк не виконано: {}", e.toString());
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
