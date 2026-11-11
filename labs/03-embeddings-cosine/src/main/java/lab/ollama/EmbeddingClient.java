package lab.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

public class EmbeddingClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI embedUri;
    private final String model;
    private final Duration requestTimeout;

    public EmbeddingClient(String baseUrl, String model, Duration requestTimeout) {
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.embedUri = URI.create(baseUrl.replaceAll("/+$", "") + "/api/embed");
        this.model = model;
        this.requestTimeout = requestTimeout;
    }

    /** i-й вектор відповідає i-му тексту. */
    public List<float[]> embed(List<String> texts) {
        HttpRequest request = HttpRequest.newBuilder(embedUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(new EmbedRequest(model, texts))))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw new OllamaException("Ollama повернула HTTP " + response.statusCode() + ": " + response.body());
        }

        List<float[]> vectors = parse(response.body()).embeddings();
        if (vectors == null || vectors.size() != texts.size()) {
            throw new OllamaException("Очікував " + texts.size() + " векторів, отримав "
                    + (vectors == null ? 0 : vectors.size()));
        }
        return vectors;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpConnectTimeoutException e) {
            throw new OllamaException("Не вдалося під'єднатися до Ollama за " + CONNECT_TIMEOUT.toSeconds() + " с", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaException("Ollama не відповіла за " + requestTimeout.toSeconds() + " с", e);
        } catch (ConnectException e) {
            throw new OllamaException("Ollama недоступна за адресою " + embedUri + ". Чи запущено ollama serve?", e);
        } catch (IOException e) {
            throw new OllamaException("Помилка мережі під час запиту до Ollama: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Запит до Ollama перервано", e);
        }
    }

    private String toJson(EmbedRequest request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new OllamaException("Не вдалося сформувати JSON запиту", e);
        }
    }

    private EmbedResponse parse(String body) {
        try {
            return mapper.readValue(body, EmbedResponse.class);
        } catch (JsonProcessingException e) {
            throw new OllamaException("Ollama повернула невалідний JSON", e);
        }
    }
}
