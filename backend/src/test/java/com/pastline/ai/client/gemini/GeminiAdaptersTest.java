package com.pastline.ai.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pastline.ai.exception.ChatUnavailableException;
import com.pastline.ai.exception.EmbeddingUnavailableException;
import com.pastline.ai.port.EmbeddingModelPort.EmbeddingDocument;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Формат запитів і відповідей Gemini REST без мережі: MockRestServiceServer замість справжнього API. */
class GeminiAdaptersTest {

    MockRestServiceServer server;
    GeminiApi api;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gemini.test/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        api = new GeminiApi(builder.build());
    }

    private static String vector(int dims) {
        return IntStream.range(0, dims).mapToObj(i -> "0.5").collect(Collectors.joining(",", "[", "]"));
    }

    @Test
    void documentsAreEmbeddedWithTaskTypeAndDimensionality() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("\"taskType\":\"RETRIEVAL_DOCUMENT\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"outputDimensionality\":768")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"title\":\"Спорт\"")))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":" + vector(768) + "},{\"values\":" + vector(768) + "}]}",
                        MediaType.APPLICATION_JSON));

        GeminiEmbeddingAdapter adapter = new GeminiEmbeddingAdapter(api, "gemini-embedding-001");
        List<float[]> vectors = adapter.embedDocuments(
                List.of(new EmbeddingDocument("Спорт", "Пробіг 5 км"), new EmbeddingDocument(null, "Купив ОВДП")));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.getFirst()).hasSize(768);
        assertThat(adapter.modelName()).isEqualTo("gemini-embedding-001@768");
        server.verify();
    }

    @Test
    void queryUsesRetrievalQueryTaskType() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"taskType\":\"RETRIEVAL_QUERY\"")))
                .andRespond(
                        withSuccess("{\"embeddings\":[{\"values\":" + vector(768) + "}]}", MediaType.APPLICATION_JSON));

        assertThat(new GeminiEmbeddingAdapter(api, "gemini-embedding-001").embedQuery("де я бігав"))
                .hasSize(768);
    }

    @Test
    void rateLimitMeansEmbeddingsUnavailable() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> new GeminiEmbeddingAdapter(api, "gemini-embedding-001").embedQuery("x"))
                .isInstanceOf(EmbeddingUnavailableException.class);
    }

    @Test
    void wrongDimensionalityIsRejected() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":" + vector(3072) + "}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new GeminiEmbeddingAdapter(api, "gemini-embedding-001").embedQuery("x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chatSendsSystemInstructionAndJoinsParts() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"systemInstruction\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"maxOutputTokens\":400")))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"Ти бігав \"},{\"text\":\"у парку [E1].\"}]}}]}",
                        MediaType.APPLICATION_JSON));

        GeminiChatAdapter chat = new GeminiChatAdapter(api, "gemini-2.5-flash-lite", 0.2, 400);
        assertThat(chat.generate("правила", "питання")).isEqualTo("Ти бігав у парку [E1].");
    }

    @Test
    void chatServerErrorMeansAiUnavailable() {
        server.expect(requestTo("http://gemini.test/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> new GeminiChatAdapter(api, "gemini-2.5-flash-lite", 0.2, 400).generate("s", "u"))
                .isInstanceOf(ChatUnavailableException.class);
    }
}
