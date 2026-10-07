package com.antigone.rh.service;

import com.antigone.rh.dto.ElementSalaireDTO;
import com.antigone.rh.dto.FichePaieDTO;
import com.antigone.rh.dto.ParametresPaieDTO;
import com.antigone.rh.dto.TrancheIrppDTO;
import com.antigone.rh.entity.Employe;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Moteur de calcul de la paie tunisienne (CNSS, contribution de solidarité, IRPP progressif,
 * charges patronales). Port fidèle de la logique historiquement implémentée côté frontend
 * dans le projet Antigone_finance (useSalaires.ts / usePayrollRates.ts).
 */
@Component
public class PayrollCalculator {

    /** Types de contrat exonérés de CNSS/IRPP/solidarité. */
    private static final Set<String> TYPES_EXONERES = Set.of("CIVP", "Freelance", "Stage");

    /**
     * true si le type de contrat est exonéré de toute retenue sociale et fiscale.
     *
     * <p>Le type de contrat est facultatif sur un employé ; le test de nullité est
     * indispensable car {@code Set.of(...).contains(null)} lève une NPE.
     */
    public boolean estExonere(String typeContrat) {
        return typeContrat != null && TYPES_EXONERES.contains(typeContrat);
    }

    // ── Jours ouvrés ─────────────────────────────────────────────────────────

    private int compterJoursOuvres(LocalDate debut, LocalDate fin) {
        int count = 0;
        LocalDate d = debut;
        while (!d.isAfter(fin)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    public int joursOuvresDuMois(String mois) {
        YearMonth ym = YearMonth.parse(mois);
        return compterJoursOuvres(ym.atDay(1), ym.atEndOfMonth());
    }

    /** Jours travaillés lors du mois d'embauche si celle-ci tombe après le 1er du mois. */
    private Integer joursTravaillesPremierMois(Employe emp, String mois) {
        if (emp.getDateEmbauche() == null) return null;
        YearMonth ym = YearMonth.parse(mois);
        LocalDate debutMois = ym.atDay(1);
        LocalDate finMois = ym.atEndOfMonth();
        LocalDate dateEmbauche = emp.getDateEmbauche();
        if (!dateEmbauche.isAfter(debutMois) || dateEmbauche.isAfter(finMois)) return null;
        return compterJoursOuvres(dateEmbauche, finMois);
    }

    /** Jours travaillés lors du mois de départ (archivage) si celui-ci tombe avant la fin du mois. */
    private Integer joursTravaillesDernierMois(Employe emp, String mois) {
        if (emp.getArchivedAt() == null) return null;
        YearMonth ym = YearMonth.parse(mois);
        LocalDate debutMois = ym.atDay(1);
        LocalDate finMois = ym.atEndOfMonth();
        LocalDate archiveDate = emp.getArchivedAt().toLocalDate();
        if (archiveDate.isBefore(debutMois) || !archiveDate.isBefore(finMois)) return null;
        return compterJoursOuvres(debutMois, archiveDate);
    }

    /** Un employé est actif pour un mois donné si son contrat couvre au moins une partie de ce mois. */
    public boolean estActifPourMois(Employe emp, String mois) {
        YearMonth ym = YearMonth.parse(mois);
        LocalDate debutMois = ym.atDay(1);
        LocalDate finMois = ym.atEndOfMonth();

        if (emp.getDateEmbauche() != null && emp.getDateEmbauche().isAfter(finMois)) return false;
        if (emp.getDateFinContrat() != null && emp.getDateFinContrat().isBefore(debutMois)) return false;
        if (emp.getArchivedAt() != null && emp.getArchivedAt().toLocalDate().isBefore(debutMois)) return false;
        return true;
    }

    // ── IRPP ─────────────────────────────────────────────────────────────────

    /** Barème IRPP annuel progressif : impôt total sur un revenu imposable annuel donné. */
    public double irppAnnuel(double imposableAnnuel, List<TrancheIrppDTO> tranches) {
        double impot = 0;
        double reste = imposableAnnuel;
        double prev = 0;
        for (TrancheIrppDTO tranche : tranches) {
            double plafond = tranche.getPlafond() == null ? Double.POSITIVE_INFINITY : tranche.getPlafond();
            double part = Math.min(reste, plafond - prev);
            if (part <= 0) break;
            impot += part * tranche.getTaux();
            reste -= part;
            prev = plafond;
        }
        return impot;
    }

    /**
     * Recherche par dichotomie du salaire brut tel que le net calculé égale le net cible.
     * L'IRPP étant progressif, cette relation ne peut pas être inversée analytiquement.
     */
    public double brutDepuisNet(double netCible, ParametresPaieDTO taux, List<TrancheIrppDTO> tranches) {
        if (netCible <= 0) return 0;
        double low = netCible;
        double high = netCible * 3;

        for (int i = 0; i < 64; i++) {
            double mid = (low + high) / 2;
            double cnss = mid * taux.getCnssSalarie();
            double imposable = mid - cnss;
            double tauxAbattement = taux.getAbattement() != null ? taux.getAbattement() : 0;
            double abattementMontant = imposable * tauxAbattement;
            double revenuNetImposable = imposable - abattementMontant;
            double baseContribution = tauxAbattement > 0 ? revenuNetImposable : imposable;
            double solidarite = baseContribution * taux.getSolidariteSalarie();
            double irpp = irppAnnuel(revenuNetImposable * 12, tranches) / 12;
            double netCalcule = mid - cnss - solidarite - irpp;

            if (Math.abs(netCalcule - netCible) < 0.0005) break;
            if (netCalcule < netCible) low = mid; else high = mid;
        }
        return round3((low + high) / 2);
    }

    // ── Éléments de salaire dynamiques (primes, absences, acomptes...) ────────

    private double appliquerAjustements(double base, List<ElementSalaireDTO> elements, String flag, int joursOuvres) {
        if (elements == null || elements.isEmpty()) return base;
        double v = base;
        for (ElementSalaireDTO el : elements) {
            if (!estActif(el, flag) || "jours".equals(el.getUnit())) continue;
            if ("gain".equals(el.getType())) v += el.getAmount();
            else if ("deduction".equals(el.getType())) v -= el.getAmount();
        }
        double ref = v;
        for (ElementSalaireDTO el : elements) {
            if (!estActif(el, flag) || !"jours".equals(el.getUnit())) continue;
            double montant = ref / joursOuvres * el.getAmount();
            if ("gain".equals(el.getType())) v += montant;
            else if ("deduction".equals(el.getType())) v -= montant;
        }
        return v;
    }

    private boolean estActif(ElementSalaireDTO el, String flag) {
        if (!Boolean.TRUE.equals(el.getEnabled())) return false;
        return switch (flag) {
            case "affectsBrut" -> Boolean.TRUE.equals(el.getAffectsBrut());
            case "affectsCnss" -> Boolean.TRUE.equals(el.getAffectsCnss());
            case "affectsIrpp" -> Boolean.TRUE.equals(el.getAffectsIrpp());
            default -> false;
        };
    }

    private double totalNetOnly(List<ElementSalaireDTO> elements) {
        if (elements == null) return 0;
        return elements.stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()) && "net_only".equals(e.getType()))
                .mapToDouble(ElementSalaireDTO::getAmount)
                .sum();
    }

    private Double montant(List<ElementSalaireDTO> elements, String id) {
        if (elements == null) return null;
        return elements.stream()
                .filter(e -> id.equals(e.getId()) && Boolean.TRUE.equals(e.getEnabled()))
                .map(ElementSalaireDTO::getAmount)
                .findFirst().orElse(null);
    }

    // ── Calcul principal de la fiche de paie ────────────────────────────────

    /**
     * Calcule la fiche de paie d'un employé pour un mois donné.
     *
     * @param emp      l'employé (salaire de base, type de contrat, dates de contrat, mode de salaire)
     * @param mois     "YYYY-MM"
     * @param taux     paramètres globaux (taux CNSS, IRPP, charges patronales...)
     * @param tranches barème IRPP effectif pour ce mois
     * @param elements éléments dynamiques (primes, absences, acomptes) du mois, peut être vide
     */
    public FichePaieDTO calculerFichePaie(Employe emp, String mois, ParametresPaieDTO taux,
            List<TrancheIrppDTO> tranches, List<ElementSalaireDTO> elements) {

        boolean exonere = estExonere(emp.getTypeContrat());
        int joursOuvres = joursOuvresDuMois(mois);

        Integer joursDernierMois = joursTravaillesDernierMois(emp, mois);
        Integer joursPremierMois = joursTravaillesPremierMois(emp, mois);
        Integer joursTravailles = joursDernierMois != null ? joursDernierMois : joursPremierMois;

        double salaireBase = emp.getSalaire() != null ? emp.getSalaire() : 0.0;
        double salaireSaisi = joursTravailles != null
                ? round3(salaireBase * joursTravailles / joursOuvres)
                : salaireBase;

        double acompteTotal = totalNetOnly(elements);
        Double bonusAmount = montant(elements, "bonus");
        double bonus = bonusAmount != null ? bonusAmount : 0;
        Double absenceAmount = montant(elements, "absence");

        if (exonere) {
            double salaireBrut = salaireSaisi;
            double brutEffectif = round3(appliquerAjustements(salaireBrut, elements, "affectsBrut", joursOuvres));
            double deductionAbsences = absenceAmount != null
                    ? round3((salaireBrut + bonus) / joursOuvres * absenceAmount) : 0;
            double net = brutEffectif;
            double netAPayer = round3(net - acompteTotal);

            return FichePaieDTO.builder()
                    .salaireBrut(salaireBrut).bonus(bonus).deductionAbsences(deductionAbsences)
                    .brutEffectif(brutEffectif)
                    .cnssSalarie(0.0).salaireImposable(brutEffectif)
                    .abattementMontant(0.0).revenuNetImposable(brutEffectif)
                    .irppMensuel(0.0).solidariteSalarie(0.0)
                    .net(net).acompte(acompteTotal).netAPayer(netAPayer)
                    .chargesEmployeur(0.0).coutTotal(brutEffectif)
                    .cnssEmployeurDetail(0.0).tfpDetail(0.0).foprolosDetail(0.0).atDetail(0.0)
                    .build();
        }

        boolean modeNet = "NET".equalsIgnoreCase(emp.getModeSalaire());
        double salaireBrut = modeNet ? brutDepuisNet(salaireSaisi, taux, tranches) : salaireSaisi;

        double brutEffectif = round3(appliquerAjustements(salaireBrut, elements, "affectsBrut", joursOuvres));
        double cnssBase = round3(appliquerAjustements(salaireBrut, elements, "affectsCnss", joursOuvres));
        double irppPreBase = round3(appliquerAjustements(salaireBrut, elements, "affectsIrpp", joursOuvres));

        double deductionAbsences = absenceAmount != null
                ? round3((salaireBrut + bonus) / joursOuvres * absenceAmount) : 0;

        // 1. CNSS salarié
        double cnssSalarie = round3(cnssBase * taux.getCnssSalarie());

        // 2. Salaire imposable = brut (ajusté IRPP) - CNSS
        double salaireImposable = round3(irppPreBase - cnssSalarie);

        // 3. Abattement forfaitaire
        double tauxAbattement = taux.getAbattement() != null ? taux.getAbattement() : 0;
        double abattementMontant = round3(salaireImposable * tauxAbattement);

        // 4. Revenu net imposable
        double revenuNetImposable = round3(salaireImposable - abattementMontant);

        // 5. Contribution sociale de solidarité
        double baseContribution = tauxAbattement > 0 ? revenuNetImposable : salaireImposable;
        double solidariteSalarie = round3(baseContribution * taux.getSolidariteSalarie());

        // 6. IRPP mensuel
        double irppMensuel = round3(irppAnnuel(revenuNetImposable * 12, tranches) / 12);

        // 7. Net
        double net = round3(brutEffectif - cnssSalarie - solidariteSalarie - irppMensuel);
        double netAPayer = round3(net - acompteTotal);

        // Charges patronales
        double cnssEmployeur = round3(brutEffectif * taux.getCnssPatronale());
        double tfp = round3(brutEffectif * taux.getTfp());
        double foprolos = round3(brutEffectif * taux.getFoprolos());
        double at = round3(brutEffectif * taux.getAt());
        double chargesEmployeur = round3(cnssEmployeur + tfp + foprolos + at);
        double coutTotal = round3(brutEffectif + chargesEmployeur);

        return FichePaieDTO.builder()
                .salaireBrut(salaireBrut).bonus(bonus).deductionAbsences(deductionAbsences)
                .brutEffectif(brutEffectif)
                .cnssSalarie(cnssSalarie).salaireImposable(salaireImposable)
                .abattementMontant(abattementMontant).revenuNetImposable(revenuNetImposable)
                .irppMensuel(irppMensuel).solidariteSalarie(solidariteSalarie)
                .net(net).acompte(acompteTotal).netAPayer(netAPayer)
                .chargesEmployeur(chargesEmployeur).coutTotal(coutTotal)
                .cnssEmployeurDetail(cnssEmployeur).tfpDetail(tfp).foprolosDetail(foprolos).atDetail(at)
                .build();
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
