package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dette de l'agence. soldeRestant = max(0, montantTotal − montantPaye). */
@Entity
@Table(name = "dettes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dette {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    /** Organisme ou personne à qui l'agence doit de l'argent. */
    private String creancier;

    @Column(nullable = false)
    private Double montantTotal;

    /** Cumul des remboursements — dénormalisé, recalculé à chaque paiement. */
    @Column(nullable = false)
    @Builder.Default
    private Double montantPaye = 0.0;

    private LocalDate dateDebut;

    private LocalDate dateEcheance;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
