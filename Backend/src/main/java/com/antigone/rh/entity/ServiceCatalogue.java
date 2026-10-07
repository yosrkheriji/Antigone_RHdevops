package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

/** Catalogue de prestations réutilisables dans les lignes de facture. */
@Entity
@Table(name = "services_catalogue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private Double prixDefaut;

    private String categorie;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
