package lab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.dto.ChatMessage;
import lab.dto.ChatRequest;
import lab.dto.ChatResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

public class OllamaClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient http;          // один клієнт на весь час життя об'єкта
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI chatUri;
    private final Duration requestTimeout;  // скільки чекаємо на відповідь моделі

    public OllamaClient(String baseUrl, Duration requestTimeout) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)  // скільки чекаємо на встановлення з'єднання
                .build();
        this.chatUri = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
        this.requestTimeout = requestTimeout;
    }

    public String chat(String model, List<ChatMessage> history) {
        HttpRequest request = HttpRequest.newBuilder(chatUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(new ChatRequest(model, history, false, false))))
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() / 100 != 2) {
            throw new OllamaException("Ollama повернула HTTP " + response.statusCode() + ": " + response.body());
        }
        return parse(response.body()).message().content();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpConnectTimeoutException e) {  // підклас HttpTimeoutException, тому ловимо першим
            throw new OllamaException("Не вдалося під'єднатися до Ollama за " + CONNECT_TIMEOUT.toSeconds() + " с", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaException("Ollama не відповіла за " + requestTimeout.toSeconds() + " с", e);
        } catch (ConnectException e) {
            throw new OllamaException("Ollama недоступна за адресою " + chatUri + ". Чи запущено ollama serve?", e);
        } catch (IOException e) {
            throw new OllamaException("Помилка мережі під час запиту до Ollama: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Запит до Ollama перервано", e);
        }
    }

    private String toJson(ChatRequest request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new OllamaException("Не вдалося сформувати JSON запиту", e);
        }
    }

    private ChatResponse parse(String body) {
        try {
            ChatResponse parsed = mapper.readValue(body, ChatResponse.class);
            if (parsed.message() == null) {
                throw new OllamaException("У відповіді Ollama немає поля message");
            }
            return parsed;
        } catch (JsonProcessingException e) {
            throw new OllamaException("Ollama повернула невалідний JSON", e);
        }
    }
}
