package com.antigone.rh.entity;

import com.antigone.rh.converter.LignesDocumentConverter;
import com.antigone.rh.dto.LigneDocumentDTO;
import com.antigone.rh.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Modèle réutilisable pré-remplissant les lignes et les taux d'un document. */
@Entity
@Table(name = "templates_factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    @Convert(converter = LignesDocumentConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<LigneDocumentDTO> lignes = new ArrayList<>();

    private Double tauxTva;

    private Double timbreFiscal;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
