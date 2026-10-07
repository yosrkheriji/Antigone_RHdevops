package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Remboursement effectué sur une dette. */
@Entity
@Table(name = "dette_paiements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DettePaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dette_id", nullable = false)
    @ToString.Exclude
    private Dette dette;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDate datePaiement;

    private String note;
}
