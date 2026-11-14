package com.pastline.support;

import com.pastline.ai.exception.ChatUnavailableException;
import com.pastline.ai.port.ChatModelPort;
import java.util.ArrayList;
import java.util.List;

/** Заміна генеративної моделі: повертає заготовлену відповідь і запам'ятовує промпти, які отримала. */
public class FakeChatModel implements ChatModelPort {

    private volatile String answer = "NO_ANSWER";
    private volatile boolean available = true;
    private final List<String> userPrompts = new ArrayList<>();

    public synchronized void reset() {
        answer = "NO_ANSWER";
        available = true;
        userPrompts.clear();
    }

    public void answerWith(String answer) {
        this.answer = answer;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public synchronized List<String> userPrompts() {
        return List.copyOf(userPrompts);
    }

    @Override
    public String modelName() {
        return "fake-chat";
    }

    @Override
    public synchronized String generate(String systemPrompt, String userPrompt) {
        if (!available) {
            throw new ChatUnavailableException();
        }
        userPrompts.add(userPrompt);
        return answer;
    }
}
