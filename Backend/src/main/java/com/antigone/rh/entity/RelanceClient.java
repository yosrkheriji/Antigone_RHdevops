package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Relance programmée sur une facture impayée. */
@Entity
@Table(name = "relances_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelanceClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    @ToString.Exclude
    private Facture facture;

    @Column(nullable = false)
    private LocalDate dateRelance;

    @Column(nullable = false)
    @Builder.Default
    private Boolean envoyee = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    // ── Brouillon redige par l'assistant IA ──────────────────────────────────
    /**
     * Objet et corps sont stockes separement de {@link #note} : c'est ce qui permet
     * de reexpedier exactement le message valide par l'utilisateur, plutot que de
     * tenter de le reconstituer a partir d'un champ de commentaire libre.
     */
    private String objet;

    @Column(columnDefinition = "TEXT")
    private String corps;

    /** SOFT, FIRM ou FORMAL — derive des jours de retard, pas choisi par le modele. */
    @Column(length = 20)
    private String ton;

    /** Destinataire retenu au moment de la redaction. */
    private String destinataireEmail;

    private String destinataireNom;

    private java.time.LocalDateTime dateEnvoi;
}
