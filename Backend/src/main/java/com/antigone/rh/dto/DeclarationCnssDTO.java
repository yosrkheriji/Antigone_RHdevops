package com.antigone.rh.dto;

import com.antigone.rh.enums.StatutPaie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationCnssDTO {
    private Long id;
    private Integer annee;
    private Integer trimestre;
    private Double montantSalarie;
    private Double montantEmployeur;
    private Double montantPenalite;
    private Double montantTotal;
    private StatutPaie statut;
    private LocalDate datePaiement;
}
