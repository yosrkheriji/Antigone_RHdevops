package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Totaux agrégés de la paie d'un mois — pour le dashboard finance. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotauxPaieDTO {
    private String mois;
    private Integer nbEmployes;
    private Double masseBrute;
    private Double masseNette;
    private Double cnssSalarie;
    private Double cnssEmployeur;
    private Double cnssTotal;
    private Double irppTotal;
    private Double tfpTotal;
    private Double foprolosTotal;
    private Double coutTotal;
    private Double netPaye;
    private Double netRestant;
}
