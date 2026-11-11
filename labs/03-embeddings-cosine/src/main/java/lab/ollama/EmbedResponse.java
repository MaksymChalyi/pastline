package lab.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbedResponse(String model, List<float[]> embeddings) {
}
