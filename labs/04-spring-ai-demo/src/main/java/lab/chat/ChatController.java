package lab.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public ChatAnswer chat(@RequestParam String q) {
        long start = System.nanoTime();
        String answer = chatClient.prompt()
                .user(q)
                // Без цього qwen3 спершу "думає" хвилинами на CPU (як у лабах 1–2).
                .options(OllamaChatOptions.builder().disableThinking())
                .call()
                .content();
        return new ChatAnswer(answer, (System.nanoTime() - start) / 1_000_000);
    }

    public record ChatAnswer(String answer, long tookMs) {
    }
}
