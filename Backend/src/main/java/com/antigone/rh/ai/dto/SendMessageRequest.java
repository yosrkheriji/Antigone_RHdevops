package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Message envoye par l'utilisateur, declenchant l'orchestration IA. */
@Data
public class SendMessageRequest {

    @NotBlank(message = "Le message ne peut pas etre vide")
    @Size(max = 8000, message = "Le message ne peut pas depasser 8000 caracteres")
    private String content;
}
