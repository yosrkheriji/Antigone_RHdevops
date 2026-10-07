package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Une tranche du barème IRPP progressif. plafond=null représente l'infini (dernière tranche). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrancheIrppDTO {
    private Double plafond;
    private Double taux;
}
