package com.pastline.ai.client.gemini;

import com.pastline.ai.exception.ChatUnavailableException;
import com.pastline.ai.port.ChatModelPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Відповіді Ask Pastline через Gemini (онлайн-демо). Промпт і перевірка джерел ті самі, що з Ollama. */
@Slf4j
@Component
@ConditionalOnProperty(name = "pastline.ai.provider", havingValue = "gemini")
public class GeminiChatAdapter implements ChatModelPort {

    private final GeminiApi api;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    public GeminiChatAdapter(
            GeminiApi api,
            @Value("${pastline.gemini.chat-model:gemini-2.5-flash-lite}") String model,
            @Value("${pastline.ask.temperature:0.2}") double temperature,
            @Value("${pastline.ask.max-tokens:400}") int maxTokens) {
        this.api = api;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        try {
            return api.generate(model, systemPrompt, userPrompt, temperature, maxTokens);
        } catch (ResourceAccessException e) {
            throw new ChatUnavailableException();
        } catch (RestClientResponseException e) {
            log.warn("Gemini chat: HTTP {}", e.getStatusCode().value());
            throw new ChatUnavailableException();
        }
    }
}
