package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Brouillon de relance produit par le LLM : objet et corps, rien d'autre. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderDraft {

    /** Objet de l'email, sans prefixe technique. */
    private String subject;

    /** Corps de l'email en texte brut, civilite et signature comprises. */
    private String body;
}
