package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeFixeDTO {
    private Long id;
    private String label;
    private Double montant;
    private Double tauxTva;
    private Integer jourEcheance;
    private Integer cycleMois;
    private Boolean archived;
    private LocalDateTime archivedAt;
    private LocalDateTime dateCreation;
    /** Montant mensualisé = montant / cycleMois, pratique pour un total mensuel projeté. */
    private Double montantMensualise;
}
