package com.antigone.rh.service;

import com.antigone.rh.dto.DetteDTO;
import com.antigone.rh.dto.DettePaiementDTO;
import com.antigone.rh.entity.Dette;
import com.antigone.rh.entity.DettePaiement;
import com.antigone.rh.repository.DettePaiementRepository;
import com.antigone.rh.repository.DetteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Dettes de l'agence. soldeRestant = max(0, montantTotal − montantPaye). */
@Service
@RequiredArgsConstructor
@Transactional
public class DetteService {

    private final DetteRepository detteRepository;
    private final DettePaiementRepository dettePaiementRepository;

    public List<DetteDTO> getAll() {
        return detteRepository.findAllByOrderByDateEcheanceAsc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public DetteDTO create(DetteDTO dto) {
        return toDTO(detteRepository.save(Dette.builder()
                .label(dto.getLabel()).creancier(dto.getCreancier())
                .montantTotal(dto.getMontantTotal()).montantPaye(0.0)
                .dateDebut(dto.getDateDebut() != null ? dto.getDateDebut() : LocalDate.now())
                .dateEcheance(dto.getDateEcheance()).notes(dto.getNotes())
                .build()));
    }

    public DetteDTO update(Long id, DetteDTO dto) {
        Dette d = findOrThrow(id);
        d.setLabel(dto.getLabel());
        d.setCreancier(dto.getCreancier());
        d.setMontantTotal(dto.getMontantTotal());
        d.setDateDebut(dto.getDateDebut());
        d.setDateEcheance(dto.getDateEcheance());
        d.setNotes(dto.getNotes());
        return toDTO(detteRepository.save(d));
    }

    public void delete(Long id) {
        Dette d = findOrThrow(id);
        dettePaiementRepository.deleteAll(dettePaiementRepository.findByDetteIdOrderByDatePaiementDesc(id));
        detteRepository.delete(d);
    }

    public List<DettePaiementDTO> getPaiements(Long detteId) {
        return dettePaiementRepository.findByDetteIdOrderByDatePaiementDesc(detteId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public DetteDTO enregistrerPaiement(Long detteId, Double montant, LocalDate datePaiement, String note) {
        Dette dette = findOrThrow(detteId);

        dettePaiementRepository.save(DettePaiement.builder()
                .dette(dette).montant(montant)
                .datePaiement(datePaiement != null ? datePaiement : LocalDate.now())
                .note(note)
                .build());

        dette.setMontantPaye(round3(nz(dette.getMontantPaye()) + montant));
        return toDTO(detteRepository.save(dette));
    }

    /** Annule un remboursement : déduit le montant, jamais de solde négatif. */
    public DetteDTO supprimerPaiement(Long paiementId) {
        DettePaiement paiement = dettePaiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));
        Dette dette = paiement.getDette();

        dette.setMontantPaye(Math.max(0, round3(nz(dette.getMontantPaye()) - paiement.getMontant())));
        dettePaiementRepository.delete(paiement);
        return toDTO(detteRepository.save(dette));
    }

    /** Σ des soldes restants sur toutes les dettes. */
    public double getTotalSoldeRestant() {
        return round2(detteRepository.findAll().stream()
                .mapToDouble(d -> Math.max(0, nz(d.getMontantTotal()) - nz(d.getMontantPaye())))
                .sum());
    }

    private Dette findOrThrow(Long id) {
        return detteRepository.findById(id).orElseThrow(() -> new RuntimeException("Dette non trouvée"));
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private DetteDTO toDTO(Dette d) {
        double solde = Math.max(0, round3(nz(d.getMontantTotal()) - nz(d.getMontantPaye())));
        return DetteDTO.builder()
                .id(d.getId()).label(d.getLabel()).creancier(d.getCreancier())
                .montantTotal(d.getMontantTotal()).montantPaye(d.getMontantPaye())
                .soldeRestant(solde).soldee(solde <= 0)
                .dateDebut(d.getDateDebut()).dateEcheance(d.getDateEcheance()).notes(d.getNotes())
                .build();
    }

    private DettePaiementDTO toDTO(DettePaiement p) {
        return DettePaiementDTO.builder()
                .id(p.getId()).detteId(p.getDette().getId()).montant(p.getMontant())
                .datePaiement(p.getDatePaiement()).note(p.getNote())
                .build();
    }
}
