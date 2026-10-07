package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Charge fixe récurrente de l'agence (loyer, abonnement, assurance...). */
@Entity
@Table(name = "charges_fixes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeFixe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Double montant;

    private Double tauxTva;

    /** Jour du mois d'échéance (1-28) */
    private Integer jourEcheance;

    /** Périodicité en mois (1, 3, 6, 12, 24...) */
    @Column(nullable = false)
    @Builder.Default
    private Integer cycleMois = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean archived = false;

    private LocalDateTime archivedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
