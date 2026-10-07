package com.antigone.rh.ai.exception;

/** Périmètre insuffisant pour la capacité ou la donnée demandée → 403. */
public class AiForbiddenException extends RuntimeException {
    public AiForbiddenException(String message) {
        super(message);
    }
}
