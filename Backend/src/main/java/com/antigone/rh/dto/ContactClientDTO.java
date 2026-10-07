package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactClientDTO {
    private Long id;
    private Long clientId;
    private String nom;
    private String poste;
    private String email;
    private String telephone;
    private String genre;
    private String notes;
}
