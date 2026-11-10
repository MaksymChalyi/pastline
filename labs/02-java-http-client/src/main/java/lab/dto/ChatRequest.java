package lab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// think = null не потрапляє в JSON, тоді діє налаштування моделі за замовчуванням
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(String model, List<ChatMessage> messages, boolean stream, Boolean think) {
}
