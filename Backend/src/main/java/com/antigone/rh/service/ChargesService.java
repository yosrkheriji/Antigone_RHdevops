package com.antigone.rh.service;

import com.antigone.rh.dto.ChargeFixeDTO;
import com.antigone.rh.dto.ChargeFixeRequest;
import com.antigone.rh.dto.ChargeVariableDTO;
import com.antigone.rh.dto.EtatChargeFixeDTO;
import com.antigone.rh.dto.PaiementChargeFixeDTO;
import com.antigone.rh.dto.ResumeChargesDTO;
import com.antigone.rh.entity.ChargeFixe;
import com.antigone.rh.entity.ChargeVariable;
import com.antigone.rh.entity.PaiementChargeFixe;
import com.antigone.rh.repository.ChargeFixeRepository;
import com.antigone.rh.repository.ChargeVariableRepository;
import com.antigone.rh.repository.PaiementChargeFixeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargesService {

    private final ChargeFixeRepository chargeFixeRepository;
    private final PaiementChargeFixeRepository paiementChargeFixeRepository;
    private final ChargeVariableRepository chargeVariableRepository;

    // ── Charges fixes — CRUD ────────────────────────────────────────────────

    public List<ChargeFixeDTO> getChargesFixes() {
        return chargeFixeRepository.findAllByArchivedFalse().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ChargeFixeDTO createChargeFixe(ChargeFixeRequest req) {
        ChargeFixe c = ChargeFixe.builder()
                .label(req.getLabel()).montant(req.getMontant()).tauxTva(req.getTauxTva())
                .jourEcheance(req.getJourEcheance())
                .cycleMois(req.getCycleMois() != null ? req.getCycleMois() : 1)
                .build();
        return toDTO(chargeFixeRepository.save(c));
    }

    public ChargeFixeDTO updateChargeFixe(Long id, ChargeFixeRequest req) {
        ChargeFixe c = findFixeOrThrow(id);
        c.setLabel(req.getLabel());
        c.setMontant(req.getMontant());
        c.setTauxTva(req.getTauxTva());
        c.setJourEcheance(req.getJourEcheance());
        c.setCycleMois(req.getCycleMois() != null ? req.getCycleMois() : c.getCycleMois());
        return toDTO(chargeFixeRepository.save(c));
    }

    public void archiveChargeFixe(Long id) {
        ChargeFixe c = findFixeOrThrow(id);
        c.setArchived(true);
        c.setArchivedAt(LocalDateTime.now());
        chargeFixeRepository.save(c);
    }

    public List<PaiementChargeFixeDTO> getPaiements(Long chargeFixeId) {
        return paiementChargeFixeRepository.findByChargeFixeIdOrderByMoisDesc(chargeFixeId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public PaiementChargeFixeDTO enregistrerPaiement(Long chargeFixeId, String mois, Double montant, LocalDate datePaiement) {
        ChargeFixe charge = findFixeOrThrow(chargeFixeId);
        PaiementChargeFixe p = PaiementChargeFixe.builder()
                .chargeFixe(charge).mois(mois)
                .montant(montant != null ? montant : charge.getMontant())
                .datePaiement(datePaiement != null ? datePaiement : LocalDate.now())
                .build();
        return toDTO(paiementChargeFixeRepository.save(p));
    }

    // ── Charges fixes — échéancier ──────────────────────────────────────────

    /**
     * Une charge est due un mois donné si le nombre de mois écoulés depuis son mois
     * d'ancrage (mois de création) est un multiple exact de son cycle.
     */
    private boolean estDueLeMois(ChargeFixe charge, YearMonth mois) {
        YearMonth ancre = YearMonth.from(charge.getDateCreation());
        long moisEcoules = (mois.getYear() - ancre.getYear()) * 12L + (mois.getMonthValue() - ancre.getMonthValue());
        if (moisEcoules < 0) return false;
        int cycle = cycleOuUn(charge);
        return moisEcoules % cycle == 0;
    }

    /** État de chaque charge fixe pour le mois demandé (échéance, paiement, reste, cumul impayé). */
    public List<EtatChargeFixeDTO> getEtatChargesFixes(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        List<EtatChargeFixeDTO> resultat = new ArrayList<>();

        for (ChargeFixe charge : chargeFixeRepository.findAllByArchivedFalse()) {
            List<PaiementChargeFixe> paiements = paiementChargeFixeRepository
                    .findByChargeFixeIdOrderByMoisDesc(charge.getId());

            boolean due = estDueLeMois(charge, ym);
            double paye = sommePaiements(paiements, mois);
            double reste = due ? Math.max(0, charge.getMontant() - paye) : 0;

            String statut;
            if (!due) statut = "NON_DUE";
            else if (reste <= 0) statut = "PAYEE";
            else if (paye > 0) statut = "PARTIELLE";
            else statut = "NON_PAYEE";

            resultat.add(EtatChargeFixeDTO.builder()
                    .id(charge.getId()).label(charge.getLabel()).montant(charge.getMontant())
                    .tauxTva(charge.getTauxTva()).jourEcheance(charge.getJourEcheance())
                    .cycleMois(charge.getCycleMois())
                    .dueCeMois(due).montantPaye(round2(paye)).resteAPayer(round2(reste)).statut(statut)
                    .cumulImpaye(round2(cumulImpaye(charge, ym, paiements)))
                    .montantHt(round2(extraireHt(charge.getMontant(), charge.getTauxTva())))
                    .montantTva(round2(extraireTva(charge.getMontant(), charge.getTauxTva())))
                    .build());
        }
        return resultat;
    }

    /** Somme des restes à payer sur tous les mois échus strictement antérieurs à {@code mois}. */
    private double cumulImpaye(ChargeFixe charge, YearMonth mois, List<PaiementChargeFixe> paiements) {
        YearMonth ancre = YearMonth.from(charge.getDateCreation());
        double cumul = 0;
        for (YearMonth m = ancre; m.isBefore(mois); m = m.plusMonths(cycleOuUn(charge))) {
            double paye = sommePaiements(paiements, m.toString());
            cumul += Math.max(0, charge.getMontant() - paye);
        }
        return cumul;
    }

    private double sommePaiements(List<PaiementChargeFixe> paiements, String mois) {
        return paiements.stream()
                .filter(p -> mois.equals(p.getMois()))
                .mapToDouble(PaiementChargeFixe::getMontant)
                .sum();
    }

    // ── Charges variables ────────────────────────────────────────────────────

    public List<ChargeVariableDTO> getChargesVariables(String mois) {
        return chargeVariableRepository.findByMoisOrderByDateDesc(mois).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ChargeVariableDTO createChargeVariable(ChargeVariableDTO dto) {
        ChargeVariable c = ChargeVariable.builder()
                .mois(dto.getMois()).label(dto.getLabel()).montant(dto.getMontant())
                .tauxTva(dto.getTauxTva()).date(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .categorie(dto.getCategorie()).description(dto.getDescription())
                .build();
        return toDTO(chargeVariableRepository.save(c));
    }

    public ChargeVariableDTO updateChargeVariable(Long id, ChargeVariableDTO dto) {
        ChargeVariable c = chargeVariableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Charge variable non trouvée"));
        c.setLabel(dto.getLabel());
        c.setMontant(dto.getMontant());
        c.setTauxTva(dto.getTauxTva());
        c.setDate(dto.getDate());
        c.setCategorie(dto.getCategorie());
        c.setDescription(dto.getDescription());
        return toDTO(chargeVariableRepository.save(c));
    }

    public void deleteChargeVariable(Long id) {
        chargeVariableRepository.deleteById(id);
    }

    // ── Synthèse du mois ─────────────────────────────────────────────────────

    public ResumeChargesDTO getResume(String mois) {
        List<EtatChargeFixeDTO> etats = getEtatChargesFixes(mois);
        List<ChargeVariable> variables = chargeVariableRepository.findByMoisOrderByDateDesc(mois);

        double fixesDues = etats.stream().filter(EtatChargeFixeDTO::getDueCeMois)
                .mapToDouble(EtatChargeFixeDTO::getMontant).sum();
        double fixesPayees = etats.stream().mapToDouble(EtatChargeFixeDTO::getMontantPaye).sum();
        double fixesRestantes = etats.stream().mapToDouble(EtatChargeFixeDTO::getResteAPayer).sum();
        double cumulAnterieur = etats.stream().mapToDouble(EtatChargeFixeDTO::getCumulImpaye).sum();

        double totalVariables = variables.stream().mapToDouble(ChargeVariable::getMontant).sum();

        double tvaFixes = etats.stream().filter(EtatChargeFixeDTO::getDueCeMois)
                .mapToDouble(EtatChargeFixeDTO::getMontantTva).sum();
        double tvaVariables = variables.stream()
                .mapToDouble(v -> extraireTva(v.getMontant(), v.getTauxTva())).sum();

        return ResumeChargesDTO.builder()
                .mois(mois)
                .totalFixesDues(round2(fixesDues))
                .totalFixesPayees(round2(fixesPayees))
                .totalFixesRestantes(round2(fixesRestantes))
                .cumulImpayeAnterieur(round2(cumulAnterieur))
                .totalVariables(round2(totalVariables))
                .totalCharges(round2(fixesDues + totalVariables))
                .tvaDeductible(round2(tvaFixes + tvaVariables))
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Le montant saisi est TTC : HT = TTC / (1 + taux / 100). */
    private double extraireHt(Double montantTtc, Double tauxTva) {
        if (montantTtc == null) return 0;
        if (tauxTva == null || tauxTva == 0) return montantTtc;
        return montantTtc / (1 + tauxTva / 100.0);
    }

    private double extraireTva(Double montantTtc, Double tauxTva) {
        if (montantTtc == null) return 0;
        return montantTtc - extraireHt(montantTtc, tauxTva);
    }

    private int cycleOuUn(ChargeFixe charge) {
        return charge.getCycleMois() != null && charge.getCycleMois() > 0 ? charge.getCycleMois() : 1;
    }

    private ChargeFixe findFixeOrThrow(Long id) {
        return chargeFixeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Charge fixe non trouvée"));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ChargeFixeDTO toDTO(ChargeFixe c) {
        return ChargeFixeDTO.builder()
                .id(c.getId()).label(c.getLabel()).montant(c.getMontant()).tauxTva(c.getTauxTva())
                .jourEcheance(c.getJourEcheance()).cycleMois(c.getCycleMois())
                .archived(c.getArchived()).archivedAt(c.getArchivedAt()).dateCreation(c.getDateCreation())
                .montantMensualise(round2(c.getMontant() / cycleOuUn(c)))
                .build();
    }

    private PaiementChargeFixeDTO toDTO(PaiementChargeFixe p) {
        return PaiementChargeFixeDTO.builder()
                .id(p.getId()).chargeFixeId(p.getChargeFixe().getId()).mois(p.getMois())
                .montant(p.getMontant()).datePaiement(p.getDatePaiement())
                .build();
    }

    private ChargeVariableDTO toDTO(ChargeVariable c) {
        return ChargeVariableDTO.builder()
                .id(c.getId()).mois(c.getMois()).label(c.getLabel()).montant(c.getMontant())
                .tauxTva(c.getTauxTva()).date(c.getDate()).categorie(c.getCategorie())
                .description(c.getDescription())
                .montantHt(round2(extraireHt(c.getMontant(), c.getTauxTva())))
                .montantTva(round2(extraireTva(c.getMontant(), c.getTauxTva())))
                .build();
    }
}
