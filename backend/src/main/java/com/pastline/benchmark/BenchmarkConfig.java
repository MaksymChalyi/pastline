package com.pastline.benchmark;

import java.nio.file.Path;

/**
 * @param warmup скільки запитів прогнати перед вимірюванням у кожному режимі (прогрів JIT, кешів, моделі)
 * @param repeats скільки разів виміряти кожен запит у кожному режимі
 */
public record BenchmarkConfig(
        Path corpus, Path queriesDir, Path outputDir, String userEmail, int warmup, int repeats) {}
