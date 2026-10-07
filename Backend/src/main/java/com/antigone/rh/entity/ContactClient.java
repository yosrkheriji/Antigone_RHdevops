package com.antigone.rh.entity;

import jakarta.persistence.*;
import lombok.*;

/** Interlocuteur additionnel chez un client — un client peut en avoir plusieurs. */
@Entity
@Table(name = "contacts_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    private Client client;

    @Column(nullable = false)
    private String nom;

    private String poste;

    private String email;

    private String telephone;

    private String genre;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
