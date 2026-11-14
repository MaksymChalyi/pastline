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
 * application-benchmark.properties): прогін, звіт, вихід. pastline.benchmark.task: search (пошук, за замовчуванням)
 * або ask (Ask Pastline, H5).
 */
@Slf4j
@Component
@Profile("benchmark")
@RequiredArgsConstructor
public class BenchmarkRunner implements ApplicationRunner {

    private final BenchmarkService benchmarkService;
    private final AskBenchmarkService askBenchmarkService;
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

    @Value("${pastline.benchmark.task:search}")
    private String task;

    @Value("${pastline.benchmark.ask-questions}")
    private Path askQuestions;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0;
        try {
            switch (task) {
                case "search" ->
                    benchmarkService.run(new BenchmarkConfig(corpus, queries, output, user, warmup, repeats));
                case "ask" -> askBenchmarkService.run(corpus, queries, askQuestions, output, user);
                default -> throw new IllegalArgumentException("pastline.benchmark.task: search або ask, а не " + task);
            }
        } catch (Exception e) {
            log.error("Бенчмарк не виконано: {}", e.toString());
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
