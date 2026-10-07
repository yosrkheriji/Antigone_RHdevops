package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Une ligne de media plan persistee.
 *
 * <p>Les noms de champs sont ceux de l'entite {@code MediaPlan} reelle, pour que le
 * frontend media plan existant puisse consommer ces objets sans traduction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaPlanItemDTO {
    private Long id;
    private LocalDate datePublication;
    private String heure;
    private String titre;
    private String texteSurVisuel;
    private String inspiration;
    private String autresElements;
    private String platforme;
    private String format;
    private String type;
    /** Lien du dossier Drive, ou "PENDING" si Drive etait indisponible. */
    private String lienDrive;
    private String etatPublication;
    private String statut;
    /** Justification editoriale produite par l'assistant. */
    private String remarques;
}
