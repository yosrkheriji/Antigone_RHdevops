package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Charge ponctuelle / ad-hoc de l'agence pour un mois donné. */
@Entity
@Table(name = "charges_variables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeVariable {

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

    private Double tauxTva;

    private LocalDate date;

    private String categorie;

    private String description;
}
