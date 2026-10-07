package com.antigone.rh.dto;

import com.antigone.rh.enums.StatutPaie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulletinPaieDTO {
    private Long id;
    private Long employeId;
    private String employeNom;
    private String employeMatricule;
    private String mois;
    private List<ElementSalaireDTO> elements;

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

    private StatutPaie statut;
    private LocalDate datePaiement;
    private LocalDateTime dateCalcul;
}
