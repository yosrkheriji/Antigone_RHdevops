package com.antigone.rh.bi.dto;

import java.util.List;

/**
 * Etat de l'entrepot : sert de bandeau de fraicheur sur les pages Analytique
 * (« donnees arretees au ... ») et de preuve que l'ETL tourne bien.
 */
public record EtatEntrepotDTO(
        String dernierChargement,
        Long dureeMs,
        Long lignesChargees,
        String statut,
        String message,
        List<VolumeTable> volumes) {

    public record VolumeTable(String table, long lignes) {
    }
}
