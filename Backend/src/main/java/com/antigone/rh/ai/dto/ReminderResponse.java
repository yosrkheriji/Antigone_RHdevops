package com.antigone.rh.ai.dto;

import com.antigone.rh.ai.service.ReminderGenerationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Relance redigee. {@code tone} et {@code daysLate} viennent de la regle metier. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponse {

    /** Reference du brouillon persiste, a passer a l'endpoint d'envoi. */
    private Long reminderId;
    private Long invoiceId;
    private String invoiceNumero;
    private String clientNom;
    private String clientEmail;
    private String subject;
    private String body;
    /** Corps mis en page, tel que le client le recevra. */
    private String htmlPreview;
    /** Logo du client destinataire, deja integre a htmlPreview ; null si absent. */
    private String clientLogoUrl;
    /** SOFT, FIRM ou FORMAL — derive des jours de retard, pas choisi par le modele. */
    private String tone;
    private long daysLate;
    private double amountDue;
    /** Vrai si l'email a reellement ete expedie ; faux pour un brouillon. */
    private boolean sent;

    public static ReminderResponse from(ReminderGenerationService.Result result) {
        return ReminderResponse.builder()
                .reminderId(result.relanceId())
                .invoiceId(result.factureId())
                .invoiceNumero(result.numero())
                .clientNom(result.clientNom())
                .clientEmail(result.clientEmail())
                .subject(result.subject())
                .body(result.body())
                .htmlPreview(result.htmlPreview())
                .clientLogoUrl(result.clientLogoUrl())
                .tone(result.tone().name())
                .daysLate(result.joursDeRetard())
                .amountDue(result.resteDu())
                .sent(result.sent())
                .build();
    }
}
