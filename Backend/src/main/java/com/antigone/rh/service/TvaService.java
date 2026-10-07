package com.antigone.rh.service;

import com.antigone.rh.dto.DeclarationTvaDTO;
import com.antigone.rh.dto.ResumeChargesDTO;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/** Déclaration TVA mensuelle : collectée sur les ventes, déductible sur les charges. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TvaService {

    private final FactureRepository factureRepository;
    private final ChargesService chargesService;
    private final RevenuService revenuService;

    public DeclarationTvaDTO getDeclaration(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        List<Facture> factures = factureRepository.findByTypeAndPeriode(
                TypeDocument.FACTURE, ym.atDay(1), ym.atEndOfMonth());

        // TVA collectée sur factures : au prorata du montant réellement encaissé.
        double tvaFactures = 0;
        for (Facture f : factures) {
            double ttc = nz(f.getTotalTtc());
            if (ttc <= 0) continue;
            double proportion = Math.min(nz(f.getMontantPaye()) / ttc, 1.0);
            tvaFactures += nz(f.getMontantTva()) * proportion;
        }

        double tvaAutresRevenus = revenuService.getTotalTvaDuMois(mois);
        double tvaCollectee = tvaFactures + tvaAutresRevenus;

        ResumeChargesDTO charges = chargesService.getResume(mois);
        double tvaDeductible = nz(charges.getTvaDeductible());

        double tvaNette = round2(tvaCollectee - tvaDeductible);

        return DeclarationTvaDTO.builder()
                .mois(mois)
                .tvaCollecteeFactures(round2(tvaFactures))
                .tvaCollecteeAutresRevenus(round2(tvaAutresRevenus))
                .tvaCollectee(round2(tvaCollectee))
                .tvaDeductible(round2(tvaDeductible))
                .tvaNette(tvaNette)
                .aReverser(tvaNette > 0)
                .build();
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
