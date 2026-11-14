package com.pastline.ai.port;

/** Межа між доменом і генеративною моделлю; у тестах її замінює фейк. */
public interface ChatModelPort {

    String modelName();

    /** Повертає текст відповіді без службових блоків моделі. */
    String generate(String systemPrompt, String userPrompt);
}
