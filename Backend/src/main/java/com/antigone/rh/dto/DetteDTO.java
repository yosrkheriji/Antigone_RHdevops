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
public class DetteDTO {
    private Long id;
    private String label;
    private String creancier;
    private Double montantTotal;
    private Double montantPaye;
    /** max(0, montantTotal − montantPaye) */
    private Double soldeRestant;
    private LocalDate dateDebut;
    private LocalDate dateEcheance;
    private String notes;
    private Boolean soldee;
}
