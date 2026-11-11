package lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Потрібен запущений Postgres (docker compose up -d); Ollama не потрібна, бо записи не завантажуються.
@SpringBootTest(properties = "demo.load-on-startup=false")
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
