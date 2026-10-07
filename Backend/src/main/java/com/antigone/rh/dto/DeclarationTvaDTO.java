package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Déclaration TVA mensuelle — calculée à la volée, non persistée. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationTvaDTO {
    private String mois;

    /** TVA sur factures, au prorata du montant réellement encaissé. */
    private Double tvaCollecteeFactures;
    /** TVA contenue dans les autres revenus du mois. */
    private Double tvaCollecteeAutresRevenus;
    /** Somme des deux sources de TVA collectée. */
    private Double tvaCollectee;

    /** TVA récupérable sur les charges fixes dues et variables du mois. */
    private Double tvaDeductible;

    /** collectée − déductible */
    private Double tvaNette;
    /** true si tvaNette > 0 → à reverser à l'État ; false → crédit de TVA. */
    private Boolean aReverser;
}
