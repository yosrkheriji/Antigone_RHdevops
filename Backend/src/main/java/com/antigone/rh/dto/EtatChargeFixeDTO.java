package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** État d'une charge fixe pour un mois donné : échéance, paiement, reste et cumul impayé. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtatChargeFixeDTO {
    private Long id;
    private String label;
    private Double montant;
    private Double tauxTva;
    private Integer jourEcheance;
    private Integer cycleMois;

    /** true si la charge tombe à échéance ce mois-ci (multiple exact du cycle depuis le mois d'ancrage). */
    private Boolean dueCeMois;
    private Double montantPaye;
    private Double resteAPayer;
    /** PAYEE | PARTIELLE | NON_PAYEE | NON_DUE */
    private String statut;
    /** Somme des restes à payer sur les mois échus antérieurs. */
    private Double cumulImpaye;

    /** HT = montant / (1 + tauxTva / 100) */
    private Double montantHt;
    private Double montantTva;
}
