package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sortie structuree du LLM pour une generation de media plan.
 *
 * <p>{@code syntheseEditoriale} porte la prose (angle retenu, ce qui a ete evite par
 * rapport aux mois precedents) ; {@code publications} porte la donnee. Le cahier
 * des charges impose cette separation : aucune donnee finale en texte libre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMediaPlan {

    /** Explication en clair du parti pris editorial du mois. */
    private String syntheseEditoriale;

    /** Thematiques volontairement ecartees pour ne pas repeter les mois passes. */
    private List<String> thematiquesEvitees = new ArrayList<>();

    private List<GeneratedMediaPlanItem> publications = new ArrayList<>();
}
