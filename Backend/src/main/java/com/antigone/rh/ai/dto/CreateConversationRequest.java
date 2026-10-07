package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Titre optionnel : sinon genere depuis le premier message. */
@Data
public class CreateConversationRequest {

    @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
    private String title;
}
