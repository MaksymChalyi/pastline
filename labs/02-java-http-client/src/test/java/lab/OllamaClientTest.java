package lab;

import com.sun.net.httpserver.HttpServer;
import lab.dto.ChatMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Тести без Ollama: піднімаємо фейковий HTTP-сервер на вільному порту (порт 0)
class OllamaClientTest {

    private static final List<ChatMessage> HISTORY = List.of(new ChatMessage("user", "Привіт"));

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsContentAndSendsCorrectRequest() throws IOException {
        String url = startServer(200, """
                {"message":{"role":"assistant","content":"Відповідь","thinking":"..."},
                 "total_duration":123,"eval_count":5,"done":true}""", 0);

        String answer = new OllamaClient(url, Duration.ofSeconds(2)).chat("qwen3:4b", HISTORY);

        assertEquals("Відповідь", answer);
        assertTrue(receivedBody.get().contains("\"model\":\"qwen3:4b\""));
        assertTrue(receivedBody.get().contains("\"stream\":false"));
    }

    @Test
    void non2xxStatusThrowsWithStatusAndBody() throws IOException {
        String url = startServer(500, "{\"error\":\"model not found\"}", 0);

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(url, Duration.ofSeconds(2)).chat("x", HISTORY));

        assertTrue(e.getMessage().contains("500"));
        assertTrue(e.getMessage().contains("model not found"));
    }

    @Test
    void slowServerCausesTimeout() throws IOException {
        String url = startServer(200, "{}", 2_000);

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(url, Duration.ofMillis(300)).chat("x", HISTORY));

        assertTrue(e.getMessage().contains("не відповіла"));
    }

    @Test
    void invalidJsonThrows() throws IOException {
        String url = startServer(200, "це не JSON", 0);

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(url, Duration.ofSeconds(2)).chat("x", HISTORY));

        assertTrue(e.getMessage().contains("невалідний JSON"));
    }

    @Test
    void ollamaNotRunningGivesClearMessage() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();  // порт був вільний, після закриття там ніхто не слухає
        }

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient("http://127.0.0.1:" + freePort, Duration.ofSeconds(2)).chat("x", HISTORY));

        assertTrue(e.getMessage().contains("недоступна"));
    }

    private String startServer(int status, String responseBody, long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sleep(delayMs);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
