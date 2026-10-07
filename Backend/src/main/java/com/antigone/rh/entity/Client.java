package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String email;

    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Profil de marque (contexte RAG de l'assistant IA) ─────────────────
    /**
     * Ces quatre champs alimentent la generation de media plan : ils sont indexes
     * comme chunk BRAND et injectes en contexte. Laisses vides, l'assistant se
     * rabat sur {@link #notes} et {@link #description}, avec une qualite moindre.
     */
    @Column(columnDefinition = "TEXT")
    private String identite;

    @Column(columnDefinition = "TEXT")
    private String activite;

    @Column(columnDefinition = "TEXT")
    private String positionnement;

    @Column(columnDefinition = "TEXT")
    private String objectifs;

    // ── Contact principal ──────────────────────────────────────────────────────
    private String contactNom;
    private String contactPoste;
    private String contactEmail;
    private String contactTelephone;

    // ── Identité fiscale (facturation) ────────────────────────────────────────
    /** Matricule fiscal tunisien */
    private String matriculeFiscale;

    /** Registre National des Entreprises */
    private String rne;

    /** Périodicité de facturation en mois (1 = mensuel, 3 = trimestriel...) */
    private Integer cycleFacturation;

    /** Nom du destinataire des e-mails de facture (pour la civilité) */
    private String emailReceiverNom;

    /** Genre du destinataire : M | F — personnalise la civilité */
    private String emailReceiverGenre;

    // ── Credentials du client (login/mdp) ─────────────────────────────────────
    private String loginClient;
    private String passwordClient;

    /**
     * Comma-separated list of page keys the client is allowed to access.
     * Known keys: MEDIA_PLANS, PROJETS, FICHIERS
     * Empty/null means no portal pages are accessible.
     */
    @Column(columnDefinition = "TEXT")
    private String clientPages;

    // ── Legacy fields (kept for backward compat) ──────────────────────────────
    /** Description libre (anciennement utilisé) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Responsible person name (free text) */
    private String responsable;

    /** Relative path to the stored file (pdf/png/jpeg) */
    private String filePath;

    private String logoPath;

    /** Original filename as uploaded by the user */
    private String fileName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
