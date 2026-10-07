package com.antigone.rh.service;

import com.antigone.rh.dto.*;
import com.antigone.rh.entity.AcompteSalaire;
import com.antigone.rh.entity.BaremeIrpp;
import com.antigone.rh.entity.BulletinPaie;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.ParametresPaie;
import com.antigone.rh.enums.StatutPaie;
import com.antigone.rh.repository.AcompteSalaireRepository;
import com.antigone.rh.repository.BaremeIrppRepository;
import com.antigone.rh.repository.BulletinPaieRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.ParametresPaieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private static final Long PARAMETRES_ID = 1L;

    private final EmployeRepository employeRepository;
    private final BulletinPaieRepository bulletinPaieRepository;
    private final AcompteSalaireRepository acompteSalaireRepository;
    private final BaremeIrppRepository baremeIrppRepository;
    private final ParametresPaieRepository parametresPaieRepository;
    private final PayrollCalculator calculator;

    // ── Paramètres globaux ───────────────────────────────────────────────────

    public ParametresPaieDTO getParametres() {
        return toDTO(findOrCreateParametres());
    }

    public ParametresPaieDTO updateParametres(ParametresPaieDTO dto) {
        ParametresPaie p = findOrCreateParametres();
        p.setCnssSalarie(dto.getCnssSalarie());
        p.setSolidariteSalarie(dto.getSolidariteSalarie());
        p.setCnssPatronale(dto.getCnssPatronale());
        p.setTfp(dto.getTfp());
        p.setFoprolos(dto.getFoprolos());
        p.setAt(dto.getAt());
        p.setAbattement(dto.getAbattement());
        p.setNotes(dto.getNotes());
        p.setDateMiseAJour(LocalDateTime.now());
        return toDTO(parametresPaieRepository.save(p));
    }

    private ParametresPaie findOrCreateParametres() {
        return parametresPaieRepository.findById(PARAMETRES_ID).orElseGet(() -> parametresPaieRepository.save(
                ParametresPaie.builder()
                        .id(PARAMETRES_ID)
                        .cnssSalarie(0.0918).solidariteSalarie(0.0050).cnssPatronale(0.1657)
                        .tfp(0.01).foprolos(0.01).at(0.004).abattement(0.10)
                        .notes("Taux par défaut — Tunisie").dateMiseAJour(LocalDateTime.now())
                        .build()));
    }

    // ── Barème IRPP ──────────────────────────────────────────────────────────

    public List<BaremeIrppDTO> listBaremes() {
        return baremeIrppRepository.findAllByOrderByEffectiveFromDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public BaremeIrppDTO createBareme(BaremeIrppDTO dto) {
        BaremeIrpp b = BaremeIrpp.builder()
                .effectiveFrom(dto.getEffectiveFrom())
                .tranches(dto.getTranches())
                .notes(dto.getNotes())
                .build();
        return toDTO(baremeIrppRepository.save(b));
    }

    private List<TrancheIrppDTO> getBaremeEffectif(String mois) {
        LocalDate premierJour = YearMonth.parse(mois).atDay(1);
        return baremeIrppRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(premierJour)
                .map(BaremeIrpp::getTranches)
                .orElseThrow(() -> new RuntimeException("Aucun barème IRPP effectif pour " + mois));
    }

    // ── Calcul / génération des bulletins ───────────────────────────────────

    public FichePaieDTO calculerApercu(Long employeId, String mois, List<ElementSalaireDTO> elements) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return calculator.calculerFichePaie(employe, mois, getParametres(), getBaremeEffectif(mois), elements);
    }

    public BulletinPaieDTO genererBulletin(Long employeId, String mois, List<ElementSalaireDTO> elements) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        return toDTO(genererOuMettreAJourBulletin(employe, mois, elements));
    }

    public List<BulletinPaieDTO> genererBulletinsDuMois(String mois) {
        List<Employe> employesActifs = employeRepository.findAll().stream()
                .filter(e -> calculator.estActifPourMois(e, mois))
                .toList();
        List<BulletinPaie> resultats = new ArrayList<>();
        for (Employe e : employesActifs) {
            resultats.add(genererOuMettreAJourBulletin(e, mois, Collections.emptyList()));
        }
        return resultats.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private BulletinPaie genererOuMettreAJourBulletin(Employe employe, String mois, List<ElementSalaireDTO> elements) {
        BulletinPaie bulletin = bulletinPaieRepository.findByEmployeIdAndMois(employe.getId(), mois)
                .orElse(BulletinPaie.builder().employe(employe).mois(mois).statut(StatutPaie.IMPAYE).build());

        List<ElementSalaireDTO> elementsAppliques = (elements == null || elements.isEmpty())
                ? bulletin.getElements() : elements;

        FichePaieDTO fiche = calculator.calculerFichePaie(employe, mois, getParametres(), getBaremeEffectif(mois), elementsAppliques);
        appliquerFicheSurBulletin(bulletin, fiche, elementsAppliques);
        bulletin.setDateCalcul(LocalDateTime.now());
        return bulletinPaieRepository.save(bulletin);
    }

    private void appliquerFicheSurBulletin(BulletinPaie bulletin, FichePaieDTO fiche, List<ElementSalaireDTO> elements) {
        bulletin.setElements(elements != null ? elements : Collections.emptyList());
        bulletin.setSalaireBrut(fiche.getSalaireBrut());
        bulletin.setBonus(fiche.getBonus());
        bulletin.setDeductionAbsences(fiche.getDeductionAbsences());
        bulletin.setBrutEffectif(fiche.getBrutEffectif());
        bulletin.setCnssSalarie(fiche.getCnssSalarie());
        bulletin.setSalaireImposable(fiche.getSalaireImposable());
        bulletin.setAbattementMontant(fiche.getAbattementMontant());
        bulletin.setRevenuNetImposable(fiche.getRevenuNetImposable());
        bulletin.setIrppMensuel(fiche.getIrppMensuel());
        bulletin.setSolidariteSalarie(fiche.getSolidariteSalarie());
        bulletin.setNet(fiche.getNet());
        bulletin.setChargesEmployeur(fiche.getChargesEmployeur());
        bulletin.setCoutTotal(fiche.getCoutTotal());
        bulletin.setCnssEmployeurDetail(fiche.getCnssEmployeurDetail());
        bulletin.setTfpDetail(fiche.getTfpDetail());
        bulletin.setFoprolosDetail(fiche.getFoprolosDetail());
        bulletin.setAtDetail(fiche.getAtDetail());

        // L'acompte déjà versé (audit AcompteSalaire) reste la source de vérité, pas le total des
        // éléments "net_only" recalculé — on ne l'écrase que s'il n'y a pas encore d'acompte enregistré.
        double acompteDejaVerse = bulletin.getAcompte() != null ? bulletin.getAcompte() : 0.0;
        double acompte = acompteDejaVerse > 0 ? acompteDejaVerse : fiche.getAcompte();
        bulletin.setAcompte(acompte);
        bulletin.setNetAPayer(Math.round((fiche.getNet() - acompte) * 1000.0) / 1000.0);
        recalculerStatut(bulletin);
    }

    private void recalculerStatut(BulletinPaie bulletin) {
        if (bulletin.getStatut() == StatutPaie.PAYE) return; // déjà soldé manuellement
        double acompte = bulletin.getAcompte() != null ? bulletin.getAcompte() : 0.0;
        if (acompte <= 0) bulletin.setStatut(StatutPaie.IMPAYE);
        else if (bulletin.getNetAPayer() != null && bulletin.getNetAPayer() <= 0) bulletin.setStatut(StatutPaie.PAYE);
        else bulletin.setStatut(StatutPaie.PARTIEL);
    }

    // ── Consultation ─────────────────────────────────────────────────────────

    public List<BulletinPaieDTO> getBulletins(String mois) {
        return bulletinPaieRepository.findByMois(mois).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BulletinPaieDTO> getBulletinsByEmploye(Long employeId) {
        return bulletinPaieRepository.findByEmployeIdOrderByMoisDesc(employeId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<BulletinPaieDTO> getImpayes(String avantMois) {
        return bulletinPaieRepository.findByMoisLessThanAndStatutNot(avantMois, StatutPaie.PAYE).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Net reporté des mois antérieurs non soldés. Les bulletins étant persistés avec
     * leurs montants figés, le report reflète les taux et le barème en vigueur au moment
     * du calcul, et non les taux actuels.
     */
    public double getNetReporte(String avantMois) {
        return round2(bulletinPaieRepository.findByMoisLessThanAndStatutNot(avantMois, StatutPaie.PAYE).stream()
                .mapToDouble(b -> nz(b.getNetAPayer()))
                .sum());
    }

    // ── Paiement ─────────────────────────────────────────────────────────────

    public BulletinPaieDTO marquerPaye(Long bulletinId, LocalDate date) {
        BulletinPaie bulletin = bulletinPaieRepository.findById(bulletinId)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouvé"));
        bulletin.setStatut(StatutPaie.PAYE);
        bulletin.setDatePaiement(date != null ? date : LocalDate.now());
        bulletin.setAcompte(bulletin.getNet());
        bulletin.setNetAPayer(0.0);
        return toDTO(bulletinPaieRepository.save(bulletin));
    }

    public BulletinPaieDTO enregistrerAcompte(Long employeId, String mois, Double montant, LocalDate date, String note) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        BulletinPaie bulletin = genererOuMettreAJourBulletin(employe, mois, null);

        double nouvelAcompte = Math.round(((bulletin.getAcompte() != null ? bulletin.getAcompte() : 0.0) + montant) * 1000.0) / 1000.0;
        bulletin.setAcompte(nouvelAcompte);
        bulletin.setNetAPayer(Math.round((bulletin.getNet() - nouvelAcompte) * 1000.0) / 1000.0);
        if (bulletin.getStatut() != StatutPaie.PAYE) {
            recalculerStatut(bulletin);
            if (bulletin.getStatut() == StatutPaie.PAYE) bulletin.setDatePaiement(date != null ? date : LocalDate.now());
        }
        bulletinPaieRepository.save(bulletin);

        acompteSalaireRepository.save(AcompteSalaire.builder()
                .employe(employe).mois(mois).montant(montant)
                .date(date != null ? date : LocalDate.now()).note(note)
                .build());

        return toDTO(bulletin);
    }

    public List<AcompteSalaireDTO> getAcomptes(Long employeId, String mois) {
        return acompteSalaireRepository.findByEmployeIdAndMoisOrderByDateAsc(employeId, mois).stream()
                .map(a -> AcompteSalaireDTO.builder()
                        .id(a.getId()).employeId(a.getEmploye().getId()).mois(a.getMois())
                        .montant(a.getMontant()).date(a.getDate()).note(a.getNote())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Totaux / dashboard ───────────────────────────────────────────────────

    public TotauxPaieDTO getTotaux(String mois) {
        List<BulletinPaie> bulletins = bulletinPaieRepository.findByMois(mois);

        double masseBrute = 0, masseNette = 0, cnssSal = 0, cnssEmp = 0, irpp = 0, tfp = 0, foprolos = 0,
                cout = 0, netPaye = 0, netRestant = 0;
        for (BulletinPaie b : bulletins) {
            masseBrute += nz(b.getBrutEffectif());
            masseNette += nz(b.getNet());
            cnssSal += nz(b.getCnssSalarie()) + nz(b.getSolidariteSalarie());
            cnssEmp += nz(b.getCnssEmployeurDetail());
            irpp += nz(b.getIrppMensuel());
            tfp += nz(b.getTfpDetail());
            foprolos += nz(b.getFoprolosDetail());
            cout += nz(b.getCoutTotal());
            if (b.getStatut() == StatutPaie.PAYE) netPaye += nz(b.getNet());
            else netRestant += nz(b.getNetAPayer());
        }

        return TotauxPaieDTO.builder()
                .mois(mois).nbEmployes(bulletins.size())
                .masseBrute(round2(masseBrute)).masseNette(round2(masseNette))
                .cnssSalarie(round2(cnssSal)).cnssEmployeur(round2(cnssEmp)).cnssTotal(round2(cnssSal + cnssEmp))
                .irppTotal(round2(irpp)).tfpTotal(round2(tfp)).foprolosTotal(round2(foprolos))
                .coutTotal(round2(cout)).netPaye(round2(netPaye)).netRestant(round2(netRestant))
                .build();
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ParametresPaieDTO toDTO(ParametresPaie p) {
        return ParametresPaieDTO.builder()
                .cnssSalarie(p.getCnssSalarie()).solidariteSalarie(p.getSolidariteSalarie())
                .cnssPatronale(p.getCnssPatronale()).tfp(p.getTfp()).foprolos(p.getFoprolos())
                .at(p.getAt()).abattement(p.getAbattement()).notes(p.getNotes())
                .build();
    }

    private BaremeIrppDTO toDTO(BaremeIrpp b) {
        return BaremeIrppDTO.builder()
                .id(b.getId()).effectiveFrom(b.getEffectiveFrom()).tranches(b.getTranches()).notes(b.getNotes())
                .build();
    }

    private BulletinPaieDTO toDTO(BulletinPaie b) {
        return BulletinPaieDTO.builder()
                .id(b.getId())
                .employeId(b.getEmploye().getId())
                .employeNom(b.getEmploye().getNom() + " " + b.getEmploye().getPrenom())
                .employeMatricule(b.getEmploye().getMatricule())
                .mois(b.getMois())
                .elements(b.getElements())
                .salaireBrut(b.getSalaireBrut()).bonus(b.getBonus()).deductionAbsences(b.getDeductionAbsences())
                .brutEffectif(b.getBrutEffectif()).cnssSalarie(b.getCnssSalarie())
                .salaireImposable(b.getSalaireImposable()).abattementMontant(b.getAbattementMontant())
                .revenuNetImposable(b.getRevenuNetImposable()).irppMensuel(b.getIrppMensuel())
                .solidariteSalarie(b.getSolidariteSalarie()).net(b.getNet()).acompte(b.getAcompte())
                .netAPayer(b.getNetAPayer()).chargesEmployeur(b.getChargesEmployeur()).coutTotal(b.getCoutTotal())
                .cnssEmployeurDetail(b.getCnssEmployeurDetail()).tfpDetail(b.getTfpDetail())
                .foprolosDetail(b.getFoprolosDetail()).atDetail(b.getAtDetail())
                .statut(b.getStatut()).datePaiement(b.getDatePaiement()).dateCalcul(b.getDateCalcul())
                .build();
    }
}
