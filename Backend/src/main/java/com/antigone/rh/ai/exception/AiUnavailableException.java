package com.antigone.rh.ai.exception;

/** L'assistant est désactivé ou aucune clé LLM n'est configurée → 503. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }
}
