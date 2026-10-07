package com.antigone.rh.service;

import com.antigone.rh.dto.ResultatNetDTO;
import com.antigone.rh.dto.ResumeChargesDTO;
import com.antigone.rh.dto.TotauxPaieDTO;
import com.antigone.rh.dto.VueDecaissementsDTO;
import com.antigone.rh.dto.VueEncaissementsDTO;
import com.antigone.rh.entity.AutreRevenu;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.repository.AutreRevenuRepository;
import com.antigone.rh.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/** Tableaux de bord : encaissements, décaissements et résultat net. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardFinanceService {

    private final FactureRepository factureRepository;
    private final AutreRevenuRepository autreRevenuRepository;
    private final PayrollService payrollService;
    private final ChargesService chargesService;
    private final DetteService detteService;

    // ── Encaissements ────────────────────────────────────────────────────────

    public VueEncaissementsDTO getVueEncaissements(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        List<Facture> factures = factureRepository.findByTypeAndPeriode(
                TypeDocument.FACTURE, ym.atDay(1), ym.atEndOfMonth());

        double totalFacture = 0, totalEncaisse = 0, totalPending = 0;
        for (Facture f : factures) {
            totalFacture += nz(f.getTotalTtc());
            if (f.getStatut() == StatutFacture.PAYEE) {
                totalEncaisse += nz(f.getTotalTtc());
            } else if (f.getStatut() == StatutFacture.PARTIEL) {
                totalEncaisse += nz(f.getMontantPaye());
            } else {
                totalPending += nz(f.getTotalTtc());
            }
        }

        double totalAutresRevenus = autreRevenuRepository.findByMoisOrderByDateDesc(mois).stream()
                .mapToDouble(AutreRevenu::getMontant).sum();

        double grandTotal = totalEncaisse + totalAutresRevenus;

        return VueEncaissementsDTO.builder()
                .mois(mois)
                .totalFacture(round2(totalFacture))
                .totalEncaisse(round2(totalEncaisse))
                .totalPending(round2(totalPending))
                .totalRemaining(round2(totalFacture - totalEncaisse))
                .totalAutresRevenus(round2(totalAutresRevenus))
                .grandTotal(round2(grandTotal))
                .partEncaisse(part(totalEncaisse, grandTotal))
                .partAutresRevenus(part(totalAutresRevenus, grandTotal))
                .build();
    }

    // ── Décaissements ────────────────────────────────────────────────────────

    public VueDecaissementsDTO getVueDecaissements(String mois) {
        TotauxPaieDTO paie = payrollService.getTotaux(mois);
        ResumeChargesDTO charges = chargesService.getResume(mois);

        double irpp = nz(paie.getIrppTotal());
        double tfp = nz(paie.getTfpTotal());
        double foprolos = nz(paie.getFoprolosTotal());

        double totalDecaissements = nz(paie.getCoutTotal())
                + nz(charges.getTotalCharges());

        return VueDecaissementsDTO.builder()
                .mois(mois)
                .masseBrute(nz(paie.getMasseBrute()))
                .masseNette(nz(paie.getMasseNette()))
                .chargesPatronales(round2(nz(paie.getCoutTotal()) - nz(paie.getMasseBrute())))
                .coutTotalSalaires(nz(paie.getCoutTotal()))
                .netRestantAPayer(nz(paie.getNetRestant()))
                .netReporte(payrollService.getNetReporte(mois))
                .chargesFixesDues(nz(charges.getTotalFixesDues()))
                .chargesFixesPayees(nz(charges.getTotalFixesPayees()))
                .chargesFixesRestantes(nz(charges.getTotalFixesRestantes()))
                .chargesVariables(nz(charges.getTotalVariables()))
                .tvaSurCharges(nz(charges.getTvaDeductible()))
                .dettesSoldeRestant(detteService.getTotalSoldeRestant())
                .irpp(irpp).tfp(tfp).foprolos(foprolos)
                .totalTaxesDues(round2(irpp + tfp + foprolos))
                .totalDecaissements(round2(totalDecaissements))
                .build();
    }

    // ── Résultat net ─────────────────────────────────────────────────────────

    /** Croise revenus et dépenses du mois — le calcul absent du produit d'origine. */
    public ResultatNetDTO getResultatNet(String mois) {
        VueEncaissementsDTO encaissements = getVueEncaissements(mois);
        VueDecaissementsDTO decaissements = getVueDecaissements(mois);

        double revenus = nz(encaissements.getGrandTotal());
        double depenses = nz(decaissements.getTotalDecaissements());
        double resultat = round2(revenus - depenses);

        return ResultatNetDTO.builder()
                .mois(mois)
                .totalRevenus(revenus)
                .totalDepenses(depenses)
                .resultatNet(resultat)
                .margePourcent(part(resultat, revenus))
                .build();
    }

    private double part(double valeur, double total) {
        if (total == 0) return 0;
        return Math.round((valeur / total) * 1000.0) / 10.0;
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
