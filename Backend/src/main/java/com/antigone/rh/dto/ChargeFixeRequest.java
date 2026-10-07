package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeFixeRequest {
    private String label;
    private Double montant;
    private Double tauxTva;
    private Integer jourEcheance;
    private Integer cycleMois;
}
