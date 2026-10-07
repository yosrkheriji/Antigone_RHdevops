package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Revenu hors facturation. Le montant saisi est TTC :
 * HT = montant / (1 + tauxTva / 100).
 */
@Entity
@Table(name = "autres_revenus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutreRevenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format "YYYY-MM" */
    @Column(nullable = false)
    private String mois;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Double montant;

    @Builder.Default
    private Double tauxTva = 0.0;

    private LocalDate date;

    /** consulting | vente | subvention | remboursement | loyer | autre */
    private String categorie;

    @Column(columnDefinition = "TEXT")
    private String description;
}
