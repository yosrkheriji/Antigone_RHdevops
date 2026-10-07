package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vue facturation d'un client — champs utiles à l'émission d'un document. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientFactureDTO {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private String matriculeFiscale;
    private String rne;
    private Integer cycleFacturation;
    private String emailReceiverNom;
    private String emailReceiverGenre;
    /** Contact principal, utile pour les relances. */
    private String contactNom;
    private String contactEmail;
    private String contactTelephone;
}
