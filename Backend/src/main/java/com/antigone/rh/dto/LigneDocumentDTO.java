package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne de prestation d'une facture ou d'un devis. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneDocumentDTO {
    private String designation;
    private Double quantite;
    private Double prixUnitaire;
    /** Coché = la ligne entre dans le total HT. */
    private Boolean selectionnee;
}
