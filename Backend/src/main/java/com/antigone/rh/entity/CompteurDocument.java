package com.antigone.rh.entity;

import com.antigone.rh.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.*;

/** Compteur de numérotation séquentielle, un par type de document et par année. */
@Entity
@Table(name = "compteurs_documents", uniqueConstraints = @UniqueConstraint(columnNames = { "type", "annee" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteurDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false)
    @Builder.Default
    private Integer dernierNumero = 0;
}
