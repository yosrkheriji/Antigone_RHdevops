package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Journal d'audit des paiements partiels (acomptes) sur le salaire d'un mois. */
@Entity
@Table(name = "acomptes_salaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcompteSalaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    @ToString.Exclude
    private Employe employe;

    /** Format "YYYY-MM" */
    @Column(nullable = false)
    private String mois;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDate date;

    private String note;
}
