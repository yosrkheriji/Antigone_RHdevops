package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Paiement partiel ou total reçu sur une facture. */
@Entity
@Table(name = "paiements_factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    @ToString.Exclude
    private Facture facture;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDate datePaiement;

    private String note;
}
