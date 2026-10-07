package com.antigone.rh.service;

import com.antigone.rh.dto.AutreRevenuDTO;
import com.antigone.rh.entity.AutreRevenu;
import com.antigone.rh.repository.AutreRevenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Revenus hors facturation. Le montant saisi est TTC, le HT en est extrait. */
@Service
@RequiredArgsConstructor
@Transactional
public class RevenuService {

    private final AutreRevenuRepository autreRevenuRepository;

    public List<AutreRevenuDTO> getByMois(String mois) {
        return autreRevenuRepository.findByMoisOrderByDateDesc(mois).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public AutreRevenuDTO create(AutreRevenuDTO dto) {
        return toDTO(autreRevenuRepository.save(AutreRevenu.builder()
                .mois(dto.getMois()).label(dto.getLabel()).montant(dto.getMontant())
                .tauxTva(dto.getTauxTva() != null ? dto.getTauxTva() : 0.0)
                .date(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .categorie(dto.getCategorie()).description(dto.getDescription())
                .build()));
    }

    public AutreRevenuDTO update(Long id, AutreRevenuDTO dto) {
        AutreRevenu r = autreRevenuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revenu non trouvé"));
        r.setLabel(dto.getLabel());
        r.setMontant(dto.getMontant());
        r.setTauxTva(dto.getTauxTva() != null ? dto.getTauxTva() : 0.0);
        r.setDate(dto.getDate());
        r.setCategorie(dto.getCategorie());
        r.setDescription(dto.getDescription());
        return toDTO(autreRevenuRepository.save(r));
    }

    public void delete(Long id) {
        autreRevenuRepository.deleteById(id);
    }

    /** Σ de la TVA contenue dans les revenus du mois. */
    public double getTotalTvaDuMois(String mois) {
        return round2(autreRevenuRepository.findByMoisOrderByDateDesc(mois).stream()
                .mapToDouble(r -> extraireTva(r.getMontant(), r.getTauxTva())).sum());
    }

    /** HT = TTC / (1 + taux / 100) */
    static double extraireHt(Double montantTtc, Double tauxTva) {
        if (montantTtc == null) return 0;
        if (tauxTva == null || tauxTva == 0) return montantTtc;
        return montantTtc / (1 + tauxTva / 100.0);
    }

    static double extraireTva(Double montantTtc, Double tauxTva) {
        if (montantTtc == null) return 0;
        return montantTtc - extraireHt(montantTtc, tauxTva);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private AutreRevenuDTO toDTO(AutreRevenu r) {
        return AutreRevenuDTO.builder()
                .id(r.getId()).mois(r.getMois()).label(r.getLabel()).montant(r.getMontant())
                .tauxTva(r.getTauxTva()).date(r.getDate()).categorie(r.getCategorie())
                .description(r.getDescription())
                .montantHt(round2(extraireHt(r.getMontant(), r.getTauxTva())))
                .montantTva(round2(extraireTva(r.getMontant(), r.getTauxTva())))
                .build();
    }
}
