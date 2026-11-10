package lab;

import lab.dto.ChatMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String model = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen3:4b");
        OllamaClient client = new OllamaClient("http://localhost:11434", Duration.ofMinutes(10));
        List<ChatMessage> history = new ArrayList<>();

        try {
            ask(client, model, history, "Поясни семантичний пошук у трьох реченнях.");
            ask(client, model, history, "А тепер коротше, одним реченням.");  // спирається на попередню відповідь
        } catch (OllamaException e) {
            System.err.println("Помилка: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void ask(OllamaClient client, String model, List<ChatMessage> history, String question) {
        history.add(new ChatMessage("user", question));
        long start = System.nanoTime();
        String answer = client.chat(model, history);
        long ms = (System.nanoTime() - start) / 1_000_000;
        history.add(new ChatMessage("assistant", answer));  // без цього модель не "пам'ятатиме" першу відповідь

        System.out.println("> " + question);
        System.out.println(answer);
        System.out.println("[" + ms + " мс]" + System.lineSeparator());
    }
}
