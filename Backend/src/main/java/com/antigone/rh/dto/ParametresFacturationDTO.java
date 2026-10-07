package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Paramètres de facturation et listes de valeurs, tous issus des référentiels. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametresFacturationDTO {
    /** Taux de TVA par défaut, en %. */
    private Double tvaDefaut;
    /** Timbre fiscal par défaut, en DT. */
    private Double timbreFiscalDefaut;
    /** Périodicités proposées pour les charges fixes, en mois. */
    private List<Integer> cyclesChargeFixe;
    private List<String> categoriesRevenu;
    private List<String> categoriesCharge;
}
