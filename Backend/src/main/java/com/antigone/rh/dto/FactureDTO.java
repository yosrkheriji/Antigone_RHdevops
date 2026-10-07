package com.antigone.rh.dto;

import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureDTO {
    private Long id;
    private String numero;
    private TypeDocument type;
    private Long clientId;
    private String clientNom;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private List<LigneDocumentDTO> lignes;
    private Double totalHtManuel;

    private Double totalHt;
    private Double tauxTva;
    private Double montantTva;
    private Double timbreFiscal;
    private Double totalTtc;

    private Double montantPaye;
    /** Dérivé : totalTtc − montantPaye (jamais négatif). */
    private Double montantRestant;
    private StatutFacture statut;
    private LocalDateTime paidAt;
    private String notes;
}
