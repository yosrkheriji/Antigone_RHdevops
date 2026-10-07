package com.antigone.rh.dto;

import com.antigone.rh.enums.TypeDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureRequest {
    private TypeDocument type;
    private Long clientId;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private List<LigneDocumentDTO> lignes;
    /** Plancher sur le total HT — le HT retenu est max(Σ lignes, cette valeur). */
    private Double totalHtManuel;
    /** Ignoré pour un devis. Défaut 19 %. */
    private Double tauxTva;
    /** Ignoré pour un devis. Défaut 1,000 DT. */
    private Double timbreFiscal;
    private String notes;
}
