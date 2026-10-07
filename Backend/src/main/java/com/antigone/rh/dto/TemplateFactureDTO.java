package com.antigone.rh.dto;

import com.antigone.rh.enums.TypeDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateFactureDTO {
    private Long id;
    private String nom;
    private TypeDocument type;
    private List<LigneDocumentDTO> lignes;
    private Double tauxTva;
    private Double timbreFiscal;
    private String notes;
}
