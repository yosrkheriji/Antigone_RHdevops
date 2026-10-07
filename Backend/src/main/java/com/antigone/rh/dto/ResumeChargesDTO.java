package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Synthèse des charges d'un mois — pour le tableau de bord finance. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeChargesDTO {
    private String mois;
    /** Σ montant des charges fixes à échéance ce mois. */
    private Double totalFixesDues;
    /** Σ paiements de charges fixes enregistrés ce mois. */
    private Double totalFixesPayees;
    /** max(0, dues − payées) */
    private Double totalFixesRestantes;
    /** Σ des restes à payer sur les mois échus antérieurs. */
    private Double cumulImpayeAnterieur;
    private Double totalVariables;
    /** totalFixesDues + totalVariables */
    private Double totalCharges;
    /** TVA récupérable sur les charges du mois (fixes dues + variables). */
    private Double tvaDeductible;
}
