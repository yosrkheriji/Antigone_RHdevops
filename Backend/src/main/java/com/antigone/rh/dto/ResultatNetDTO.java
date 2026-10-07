package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Croisement encaissements ↔ décaissements — le résultat net qui manquait
 * dans le produit d'origine (incohérence n°6 du README).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatNetDTO {
    private String mois;
    private Double totalRevenus;
    private Double totalDepenses;
    /** totalRevenus − totalDepenses */
    private Double resultatNet;
    /** Marge en % du chiffre d'affaires encaissé. */
    private Double margePourcent;
}
