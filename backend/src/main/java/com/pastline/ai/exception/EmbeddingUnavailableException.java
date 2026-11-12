package com.pastline.ai.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Модель недоступна (Ollama не запущена, таймаут): тимчасова проблема, не вада конкретного запису. */
public class EmbeddingUnavailableException extends BaseException {

    public EmbeddingUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "EMBEDDING_UNAVAILABLE", "Модель ембедінгів зараз недоступна");
    }
}
