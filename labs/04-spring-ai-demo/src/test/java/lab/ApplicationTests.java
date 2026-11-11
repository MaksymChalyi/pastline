package lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Без індексації на старті: тест не повинен залежати від запущеної Ollama.
@SpringBootTest(properties = "demo.index-on-startup=false")
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
