package com.pastline.ai.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Генеративна модель недоступна (Ollama не запущена або не відповіла вчасно). */
public class ChatUnavailableException extends BaseException {

    public ChatUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "Модель відповідей зараз недоступна");
    }
}
