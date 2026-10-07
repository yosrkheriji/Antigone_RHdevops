package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Vue paie d'un employé — uniquement les champs utiles au calcul et au virement. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployePaieDTO {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String cin;
    /** Numéro d'affiliation CNSS de l'employé. */
    private String cnss;
    /** RIB pour le virement du salaire. */
    private String ribBancaire;
    private String poste;
    private String departement;
    private String typeContrat;
    private LocalDate dateEmbauche;
    private LocalDate dateFinContrat;
    private Double salaire;
    /** BRUT : le salaire saisi est le brut. NET : le brut est déduit du net cible. */
    private String modeSalaire;
    private Boolean archived;
    private LocalDate dateArchivage;
    /** true si le contrat couvre au moins une partie du mois demandé. */
    private Boolean actifCeMois;
    /** true si le type de contrat est exonéré de CNSS/IRPP (CIVP, Freelance, Stage). */
    private Boolean exonere;
}
