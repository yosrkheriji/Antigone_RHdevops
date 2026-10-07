package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Explication d'un bulletin de paie.
 *
 * <p>{@code previousNet} et {@code currentNet} sont renseignes par le serveur depuis
 * la base, jamais par le modele : l'explication doit citer les chiffres reels, et
 * les exposer separement rend toute divergence immediatement visible.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipExplanation {

    /** Explication en langage clair, destinee a l'employe. */
    private String explanation;

    private Comparison comparison;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comparison {
        private Double previousNet;
        private Double currentNet;
        private Double delta;
        /** Causes de l'ecart, une par entree. */
        private List<String> deltaReasons = new ArrayList<>();
    }
}
