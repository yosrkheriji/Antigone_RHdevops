package com.antigone.rh.service;

import com.antigone.rh.dto.DeclarationCnssDTO;
import com.antigone.rh.entity.BulletinPaie;
import com.antigone.rh.entity.DeclarationCnss;
import com.antigone.rh.enums.StatutPaie;
import com.antigone.rh.repository.BulletinPaieRepository;
import com.antigone.rh.repository.DeclarationCnssRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CnssService {

    private final DeclarationCnssRepository declarationCnssRepository;
    private final BulletinPaieRepository bulletinPaieRepository;

    public List<DeclarationCnssDTO> getByAnnee(Integer annee) {
        return declarationCnssRepository.findByAnneeOrderByTrimestreAsc(annee).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Calcule les montants CNSS suggérés pour un trimestre en sommant les bulletins de paie
     * des 3 mois du trimestre déjà générés, pénalité de retard incluse.
     */
    public DeclarationCnssDTO calculerSuggestionDepuisBulletins(Integer annee, Integer trimestre) {
        List<String> mois = moisDuTrimestre(annee, trimestre);
        double montantSalarie = 0, montantEmployeur = 0;
        for (String m : mois) {
            for (BulletinPaie b : bulletinPaieRepository.findByMois(m)) {
                montantSalarie += nz(b.getCnssSalarie()) + nz(b.getSolidariteSalarie());
                montantEmployeur += nz(b.getCnssEmployeurDetail());
            }
        }
        double totalBrut = montantSalarie + montantEmployeur;
        double penalite = calculerPenalite(annee, trimestre, totalBrut);

        return DeclarationCnssDTO.builder()
                .annee(annee).trimestre(trimestre)
                .montantSalarie(round2(montantSalarie)).montantEmployeur(round2(montantEmployeur))
                .montantPenalite(round2(penalite))
                .montantTotal(round2(totalBrut + penalite))
                .statut(StatutPaie.IMPAYE)
                .build();
    }

    /** Échéance légale de la déclaration : le 15 du mois suivant la fin du trimestre. */
    public LocalDate echeanceTrimestre(Integer annee, Integer trimestre) {
        return LocalDate.of(annee, trimestre * 3, 15).plusMonths(1);
    }

    /**
     * Pénalité de retard = montant brut × mois de retard × 1 %.
     * Les mois de retard sont arrondis, avec un minimum de 1 dès que l'échéance est dépassée.
     */
    private double calculerPenalite(Integer annee, Integer trimestre, double totalBrut) {
        long jours = ChronoUnit.DAYS.between(echeanceTrimestre(annee, trimestre), LocalDate.now());
        if (jours <= 0) return 0;
        long moisRetard = Math.max(1, Math.round(jours / 30.0));
        return totalBrut * moisRetard * 0.01;
    }

    public DeclarationCnssDTO saveDeclaration(DeclarationCnssDTO dto) {
        DeclarationCnss d = declarationCnssRepository.findByAnneeAndTrimestre(dto.getAnnee(), dto.getTrimestre())
                .orElse(DeclarationCnss.builder().annee(dto.getAnnee()).trimestre(dto.getTrimestre()).build());
        d.setMontantSalarie(dto.getMontantSalarie());
        d.setMontantEmployeur(dto.getMontantEmployeur());
        d.setMontantPenalite(dto.getMontantPenalite() != null ? dto.getMontantPenalite() : 0.0);
        d.setMontantTotal(round2(nz(dto.getMontantSalarie()) + nz(dto.getMontantEmployeur()) + nz(d.getMontantPenalite())));
        return toDTO(declarationCnssRepository.save(d));
    }

    public DeclarationCnssDTO marquerPayee(Long id, LocalDate datePaiement) {
        DeclarationCnss d = declarationCnssRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Déclaration CNSS non trouvée"));
        d.setStatut(StatutPaie.PAYE);
        d.setDatePaiement(datePaiement != null ? datePaiement : LocalDate.now());
        return toDTO(declarationCnssRepository.save(d));
    }

    private List<String> moisDuTrimestre(Integer annee, Integer trimestre) {
        int premierMois = (trimestre - 1) * 3 + 1;
        return List.of(
                YearMonth.of(annee, premierMois).toString(),
                YearMonth.of(annee, premierMois + 1).toString(),
                YearMonth.of(annee, premierMois + 2).toString());
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private DeclarationCnssDTO toDTO(DeclarationCnss d) {
        return DeclarationCnssDTO.builder()
                .id(d.getId()).annee(d.getAnnee()).trimestre(d.getTrimestre())
                .montantSalarie(d.getMontantSalarie()).montantEmployeur(d.getMontantEmployeur())
                .montantPenalite(d.getMontantPenalite()).montantTotal(d.getMontantTotal())
                .statut(d.getStatut()).datePaiement(d.getDatePaiement())
                .build();
    }
}
