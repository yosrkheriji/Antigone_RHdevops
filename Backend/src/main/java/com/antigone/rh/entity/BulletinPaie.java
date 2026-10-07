package com.antigone.rh.entity;

import com.antigone.rh.converter.ElementsSalaireConverter;
import com.antigone.rh.dto.ElementSalaireDTO;
import com.antigone.rh.enums.StatutPaie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bulletin de paie mensuel d'un employé (résultat figé du calcul CNSS/IRPP). */
@Entity
@Table(name = "bulletins_paie", uniqueConstraints = @UniqueConstraint(columnNames = { "employe_id", "mois" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaie {

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

    @Convert(converter = ElementsSalaireConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<ElementSalaireDTO> elements = new ArrayList<>();

    // ── Résultat du calcul (snapshot) ───────────────────────────────────────
    private Double salaireBrut;
    private Double bonus;
    private Double deductionAbsences;
    private Double brutEffectif;
    private Double cnssSalarie;
    private Double salaireImposable;
    private Double abattementMontant;
    private Double revenuNetImposable;
    private Double irppMensuel;
    private Double solidariteSalarie;
    private Double net;
    private Double acompte;
    private Double netAPayer;
    private Double chargesEmployeur;
    private Double coutTotal;
    private Double cnssEmployeurDetail;
    private Double tfpDetail;
    private Double foprolosDetail;
    private Double atDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPaie statut = StatutPaie.IMPAYE;

    private LocalDate datePaiement;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateCalcul = LocalDateTime.now();
}
