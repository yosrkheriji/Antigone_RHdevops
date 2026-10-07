package com.antigone.rh.entity;

import com.antigone.rh.converter.TranchesIrppConverter;
import com.antigone.rh.dto.TrancheIrppDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Barème IRPP progressif tunisien, versionné par date d'entrée en vigueur. */
@Entity
@Table(name = "baremes_irpp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaremeIrpp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Premier jour du mois à partir duquel ce barème s'applique. */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Convert(converter = TranchesIrppConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private List<TrancheIrppDTO> tranches = new ArrayList<>();

    private String notes;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDate dateCreation = LocalDate.now();
}
