package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une publication generee par le LLM.
 *
 * <p>Les noms de champs suivent l'entite {@code MediaPlan} reelle de l'application
 * ({@code texteSurVisuel}, {@code autresElements}, {@code platforme}) et non les
 * libelles generiques du cahier des charges : l'objectif est de brancher la
 * generation sur les tables existantes, pas d'introduire un second vocabulaire.
 *
 * <p>{@code lienDrive} et {@code etatPublication} sont volontairement absents : ils
 * ne sont pas du ressort du modele. Le premier est approvisionne par
 * {@code DriveProvisioningService}, le second vaut {@code PAS_ENCORE} a la creation.
 *
 * <p>Types tous scalaires et sans date native : les sorties structurees strictes
 * d'OpenAI ne gerent pas {@code LocalDate}, la date transite donc en ISO et est
 * validee cote serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMediaPlanItem {

    /** Date de publication au format ISO YYYY-MM-DD, dans le mois demande. */
    private String date;

    /** Heure de publication au format HH:mm. */
    private String heure;

    /** Titre court de la publication. */
    private String titre;

    /** Texte destine a figurer sur le visuel. */
    private String texteSurVisuel;

    /** Reference ou piste creative servant d'inspiration. */
    private String inspiration;

    /** Precisions complementaires : hashtags, mentions, contraintes. */
    private String autresElements;

    /** Plateforme visee, prise dans le referentiel PLATFORME_MEDIA_PLAN. */
    private String platforme;

    /** Format, pris dans le referentiel FORMAT_MEDIA_PLAN (Reel, Carrousel...). */
    private String format;

    /** Type de contenu, pris dans le referentiel TYPE_MEDIA_PLAN. */
    private String type;

    /**
     * Justification editoriale : pourquoi ce contenu, a cette date, sur cette
     * plateforme. Persistee dans le champ {@code remarques} de l'entite, ce qui la
     * rend consultable depuis l'interface media plan existante.
     */
    private String justification;
}
