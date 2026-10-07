package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Élément dynamique de paie (prime, bonus, absence, acompte...).
 * type: "gain" | "deduction" | "net_only"
 * unit: "montant" | "jours"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementSalaireDTO {
    private String id;
    private String label;
    private String type;
    private String unit;
    private Double amount;
    private Boolean enabled;
    private Boolean affectsBrut;
    private Boolean affectsCnss;
    private Boolean affectsIrpp;
}
