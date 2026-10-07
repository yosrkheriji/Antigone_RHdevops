package com.antigone.rh.entity;

import com.antigone.rh.converter.LignesDocumentConverter;
import com.antigone.rh.dto.LigneDocumentDTO;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Facture ou devis émis à un client. Sert aussi d'historique : chaque document émis
 * reste en base avec ses montants figés.
 */
@Entity
@Table(name = "factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro séquentiel généré via CompteurDocument (ex. FAC-2026-0001). */
    @Column(unique = true, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @ToString.Exclude
    private Client client;

    @Column(nullable = false)
    private LocalDate dateEmission;

    private LocalDate dateEcheance;

    @Convert(converter = LignesDocumentConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<LigneDocumentDTO> lignes = new ArrayList<>();

    /**
     * Total HT saisi manuellement. Sert de plancher : le HT retenu est
     * max(somme des lignes sélectionnées, cette valeur).
     */
    private Double totalHtManuel;

    // ── Montants calculés (figés à l'enregistrement) ─────────────────────────
    private Double totalHt;
    private Double tauxTva;
    private Double montantTva;
    private Double timbreFiscal;
    private Double totalTtc;

    /** Cumul des paiements reçus — dénormalisé, recalculé à chaque paiement. */
    @Column(nullable = false)
    @Builder.Default
    private Double montantPaye = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutFacture statut = StatutFacture.EN_ATTENTE;

    private LocalDateTime paidAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
