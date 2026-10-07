package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Demande de redaction d'une relance client.
 *
 * <p>Aucun indicateur d'envoi : la generation produit toujours un brouillon.
 * L'expedition passe par {@code POST /api/v1/reminders/{reminderId}/send}, apres
 * relecture par l'utilisateur.
 */
@Data
public class GenerateReminderRequest {

    @NotNull(message = "invoiceId est obligatoire")
    private Long invoiceId;

}
