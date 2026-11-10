package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignoreUnknown: у відповіді Ollama в message є ще поле "thinking"
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(String role, String content) {
}
