package com.pastline;

import com.pastline.support.FakeChatModel;
import com.pastline.support.FakeEmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Спільна конфігурація всіх інтеграційних тестів: один контекст і один контейнер на всі класи. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        // Той самий образ, що в compose.yaml: потрібне розширення vector
        return new PostgreSQLContainer(
                DockerImageName.parse("pgvector/pgvector:0.8.2-pg17").asCompatibleSubstituteFor("postgres"));
    }

    /** Ollama в тестах немає: порт ембедінгів замінено детермінованим фейком. */
    @Bean
    @Primary
    public FakeEmbeddingModel fakeEmbeddingModel() {
        return new FakeEmbeddingModel();
    }

    /** Генеративної моделі в тестах теж немає: заготовлені відповіді перевіряють пост-валідацію детерміновано. */
    @Bean
    @Primary
    public FakeChatModel fakeChatModel() {
        return new FakeChatModel();
    }
}
