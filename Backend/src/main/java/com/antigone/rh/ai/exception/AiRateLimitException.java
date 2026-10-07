package com.antigone.rh.ai.exception;

/** Quota d'appels IA dépassé pour la fenêtre courante → 429. */
public class AiRateLimitException extends RuntimeException {
    public AiRateLimitException(String message) {
        super(message);
    }
}
