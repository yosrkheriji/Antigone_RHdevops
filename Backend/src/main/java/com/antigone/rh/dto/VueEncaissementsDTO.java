package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vue d'ensemble des encaissements pour un mois. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VueEncaissementsDTO {
    private String mois;
    /** Σ totalTtc de toutes les factures du mois. */
    private Double totalFacture;
    /** Σ totalTtc des factures payées + Σ montantPaye des factures partielles. */
    private Double totalEncaisse;
    /** Σ totalTtc des factures encore en attente. */
    private Double totalPending;
    /** totalFacture − totalEncaisse */
    private Double totalRemaining;
    /** Σ montants TTC des autres revenus. */
    private Double totalAutresRevenus;
    /** totalEncaisse + totalAutresRevenus */
    private Double grandTotal;
    /** Part des factures encaissées dans le grandTotal (%). */
    private Double partEncaisse;
    /** Part des autres revenus dans le grandTotal (%). */
    private Double partAutresRevenus;
}
