package com.pastline.ai.port;

/**
 * Межа між доменом і генеративною моделлю. Ask Pastline не знає ні про Ollama, ні про Spring AI: у тестах замість
 * моделі фейк із заготовленою відповіддю, і пост-валідацію можна перевірити детерміновано.
 */
public interface ChatModelPort {

    String modelName();

    /** Повертає текст відповіді без службових блоків моделі. */
    String generate(String systemPrompt, String userPrompt);
}
