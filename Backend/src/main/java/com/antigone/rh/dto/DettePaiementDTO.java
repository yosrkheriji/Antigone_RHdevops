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
public class DettePaiementDTO {
    private Long id;
    private Long detteId;
    private Double montant;
    private LocalDate datePaiement;
    private String note;
}
