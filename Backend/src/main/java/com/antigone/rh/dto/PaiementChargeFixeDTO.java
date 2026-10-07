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
public class PaiementChargeFixeDTO {
    private Long id;
    private Long chargeFixeId;
    private String mois;
    private Double montant;
    private LocalDate datePaiement;
}
