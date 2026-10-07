package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Paramètres globaux de calcul de la paie (taux CNSS, IRPP, charges patronales...).
 * Table à une seule ligne (id fixe = 1).
 */
@Entity
@Table(name = "parametres_paie")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametresPaie {

    @Id
    private Long id;

    /** Part salariale CNSS — ex: 0.0918 */
    @Column(nullable = false)
    private Double cnssSalarie;

    /** Contribution sociale de solidarité (CSS) salariale — ex: 0.0050 */
    @Column(nullable = false)
    private Double solidariteSalarie;

    /** Part patronale CNSS consolidée — ex: 0.1657 */
    @Column(nullable = false)
    private Double cnssPatronale;

    /** Taxe de formation professionnelle — ex: 0.01 */
    @Column(nullable = false)
    private Double tfp;

    /** FOPROLOS — ex: 0.01 */
    @Column(nullable = false)
    private Double foprolos;

    /** Accidents du travail — ex: 0.004 */
    @Column(nullable = false)
    private Double at;

    /** Déduction forfaitaire sur salaire imposable — ex: 0.10 */
    @Column(nullable = false)
    private Double abattement;

    private String notes;

    private LocalDateTime dateMiseAJour;
}
