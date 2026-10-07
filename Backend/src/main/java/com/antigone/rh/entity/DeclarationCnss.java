package com.antigone.rh.entity;

import com.antigone.rh.enums.StatutPaie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Déclaration CNSS trimestrielle de l'agence. */
@Entity
@Table(name = "declarations_cnss", uniqueConstraints = @UniqueConstraint(columnNames = { "annee", "trimestre" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationCnss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer annee;

    /** 1 à 4 */
    @Column(nullable = false)
    private Integer trimestre;

    @Column(nullable = false)
    @Builder.Default
    private Double montantSalarie = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double montantEmployeur = 0.0;

    @Builder.Default
    private Double montantPenalite = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double montantTotal = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPaie statut = StatutPaie.IMPAYE;

    private LocalDate datePaiement;
}
