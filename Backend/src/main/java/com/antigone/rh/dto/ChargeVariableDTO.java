package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeVariableDTO {
    private Long id;
    private String mois;
    private String label;
    private Double montant;
    private Double tauxTva;
    private LocalDate date;
    private String categorie;
    private String description;
    /** HT = montant / (1 + tauxTva / 100) — le montant saisi est TTC. */
    private Double montantHt;
    private Double montantTva;
}
