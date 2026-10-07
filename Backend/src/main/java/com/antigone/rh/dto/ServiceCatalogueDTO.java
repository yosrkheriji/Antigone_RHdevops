package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatalogueDTO {
    private Long id;
    private String designation;
    private Double prixDefaut;
    private String categorie;
    private String description;
    private Boolean actif;
}
