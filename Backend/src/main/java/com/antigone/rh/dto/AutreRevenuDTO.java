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
public class AutreRevenuDTO {
    private Long id;
    private String mois;
    private String label;
    /** Montant TTC saisi. */
    private Double montant;
    private Double tauxTva;
    private LocalDate date;
    private String categorie;
    private String description;
    /** HT = montant / (1 + tauxTva / 100) */
    private Double montantHt;
    private Double montantTva;
}
