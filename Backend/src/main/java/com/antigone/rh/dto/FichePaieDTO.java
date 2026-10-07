package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Résultat du calcul d'une fiche de paie pour un employé/mois donné (non persisté). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichePaieDTO {
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
}
