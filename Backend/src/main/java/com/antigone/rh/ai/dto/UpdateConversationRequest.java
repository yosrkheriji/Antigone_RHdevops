package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Renommage et epinglage. Les deux champs sont optionnels et independants. */
@Data
public class UpdateConversationRequest {

    @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
    private String title;

    private Boolean pinned;
}
