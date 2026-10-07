package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Paiement effectif enregistré contre une charge fixe pour un mois donné. */
@Entity
@Table(name = "paiements_charges_fixes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementChargeFixe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_fixe_id", nullable = false)
    @ToString.Exclude
    private ChargeFixe chargeFixe;

    /** Format "YYYY-MM" */
    @Column(nullable = false)
    private String mois;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDate datePaiement;
}
