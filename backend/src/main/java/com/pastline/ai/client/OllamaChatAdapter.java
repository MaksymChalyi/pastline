package com.pastline.ai.client;

import com.pastline.ai.exception.ChatUnavailableException;
import com.pastline.ai.port.ChatModelPort;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 * Адаптер порту до Spring AI (Ollama). Модель за замовчуванням qwen3:4b-instruct: на відміну від qwen3:4b (це
 * thinking-варіант, що міркує навіть з think=false), відповідає одразу, без ходу думок.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pastline.ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaChatAdapter implements ChatModelPort {

    // Підстраховка: якщо модель усе ж видала блок міркувань, він не потрапляє у відповідь
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");

    private final ChatModel chatModel;
    private final String modelName;
    private final double temperature;
    private final int maxTokens;

    public OllamaChatAdapter(
            ChatModel chatModel,
            @Value("${spring.ai.ollama.chat.model}") String modelName,
            @Value("${pastline.ask.temperature:0.2}") double temperature,
            @Value("${pastline.ask.max-tokens:400}") int maxTokens) {
        this.chatModel = chatModel;
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                OllamaChatOptions.builder()
                        // Модель явно: опції запиту без моделі перекривають налаштування значенням за замовчуванням
                        .model(modelName)
                        .temperature(temperature)
                        // Верхня межа довжини відповіді: на CPU кожен токен коштує час
                        .numPredict(maxTokens)
                        .build());
        ChatResponse response;
        long start = System.nanoTime();
        try {
            response = chatModel.call(prompt);
        } catch (ResourceAccessException e) {
            // I/O-помилка RestClient: Ollama не відповідає або перевищено тайм-аут читання
            throw new ChatUnavailableException();
        } catch (NonTransientAiException | TransientAiException e) {
            // Ollama відповіла помилкою (немає моделі, перевантаження). У повідомленні лише відповідь Ollama, без
            // промпту
            log.warn("Модель {} повернула помилку: {}", modelName, e.getMessage());
            throw new ChatUnavailableException();
        }
        if (response == null || response.getResult() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        String raw = text == null ? "" : text;
        String answer = THINK_BLOCK.matcher(raw).replaceAll("").strip();
        // Лише числа, без тексту: видно, скільки токенів пішло на промпт і на відповідь і чи були міркування
        var usage = response.getMetadata().getUsage();
        log.info(
                "Модель {}: промпт {} ток., відповідь {} ток., {} мс, блок міркувань: {}",
                modelName,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                raw.length() != answer.length() && THINK_BLOCK.matcher(raw).find());
        return answer;
    }
}
