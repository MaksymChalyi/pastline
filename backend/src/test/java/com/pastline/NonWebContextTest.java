package com.pastline;

import static org.assertj.core.api.Assertions.assertThat;

import com.pastline.benchmark.BenchmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Контекст без веб-сервера, як у профілі benchmark. Окремий профіль тут не вмикається: його раннер завершує JVM.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.main.web-application-type=none", "pastline.embedding.worker.enabled=false"})
@Import(TestcontainersConfiguration.class)
class NonWebContextTest {

    @Autowired
    BenchmarkService benchmarkService;

    @Test
    void contextStartsWithoutWebServer() {
        assertThat(benchmarkService).isNotNull();
    }
}
