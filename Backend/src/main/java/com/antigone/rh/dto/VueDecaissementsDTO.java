package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vue d'ensemble des décaissements pour un mois. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VueDecaissementsDTO {
    private String mois;

    // ── Masse salariale ──────────────────────────────────────────────────────
    private Double masseBrute;
    private Double masseNette;
    private Double chargesPatronales;
    private Double coutTotalSalaires;
    private Double netRestantAPayer;
    /** Net reporté des mois antérieurs impayés. */
    private Double netReporte;

    // ── Charges ──────────────────────────────────────────────────────────────
    private Double chargesFixesDues;
    private Double chargesFixesPayees;
    private Double chargesFixesRestantes;
    private Double chargesVariables;
    private Double tvaSurCharges;

    // ── Dettes ───────────────────────────────────────────────────────────────
    private Double dettesSoldeRestant;

    // ── Taxes dues ───────────────────────────────────────────────────────────
    private Double irpp;
    private Double tfp;
    private Double foprolos;
    /** IRPP + TFP + FOPROLOS */
    private Double totalTaxesDues;

    /** Somme de tous les décaissements du mois. */
    private Double totalDecaissements;
}
