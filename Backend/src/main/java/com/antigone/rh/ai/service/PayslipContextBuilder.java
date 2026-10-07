package com.antigone.rh.ai.service;

import com.antigone.rh.dto.ElementSalaireDTO;
import com.antigone.rh.dto.TrancheIrppDTO;
import com.antigone.rh.entity.BaremeIrpp;
import com.antigone.rh.entity.BulletinPaie;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.ParametresPaie;
import com.antigone.rh.repository.BaremeIrppRepository;
import com.antigone.rh.repository.BulletinPaieRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.ParametresPaieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Construit le contexte de paie transmis au modele.
 *
 * <p>Une seule implementation, partagee par l'outil conversationnel et par
 * l'endpoint d'explication : sans cela les deux chemins produiraient des
 * explications differentes pour un meme bulletin, ce qui est precisement ce qu'un
 * utilisateur ne pardonne pas sur sa fiche de paie.
 *
 * <p>Le contexte ne se limite pas aux montants : il porte aussi les <em>taux
 * appliques</em> et le <em>detail de chaque formule</em>, chiffres a partir du
 * bulletin reel. Le modele n'a donc rien a recalculer — il reformule un calcul
 * deja fait, ce qui rend impossible l'erreur arithmetique dans une explication de
 * salaire.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipContextBuilder {

    /** Contrats exoneres de cotisations : le net y est egal au brut. */
    private static final List<String> TYPES_EXONERES = List.of("CIVP", "Freelance", "Stage");

    private final BulletinPaieRepository bulletinRepository;
    private final BaremeIrppRepository baremeRepository;
    private final EmployeRepository employeRepository;
    private final ParametresPaieRepository parametresRepository;

    /** Bulletin retenu, avec le mois reellement utilise. */
    public record Resolved(BulletinPaie current, BulletinPaie previous, YearMonth month,
                           boolean fellBackToLatest) {
    }

    /**
     * Retrouve le bulletin a expliquer.
     *
     * <p>Si le mois demande n'a pas encore de bulletin — cas courant quand
     * l'utilisateur demande « ce mois-ci » alors que la paie n'est pas close — on
     * se rabat sur le plus recent disponible plutot que de repondre qu'il n'y a
     * rien. L'utilisateur veut comprendre sa derniere fiche, pas s'entendre dire
     * qu'elle n'existe pas encore.
     */
    @Transactional(readOnly = true)
    public Optional<Resolved> resolve(Long employeId, YearMonth requested) {
        Optional<BulletinPaie> exact = bulletinRepository.findByEmployeIdAndMois(employeId, requested.toString());
        if (exact.isPresent()) {
            return Optional.of(new Resolved(
                    exact.get(),
                    bulletinRepository.findByEmployeIdAndMois(employeId, requested.minusMonths(1).toString())
                            .orElse(null),
                    requested,
                    false));
        }

        List<BulletinPaie> history = bulletinRepository.findByEmployeIdOrderByMoisDesc(employeId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        BulletinPaie latest = history.get(0);
        YearMonth latestMonth = YearMonth.parse(latest.getMois());
        BulletinPaie previous = history.size() > 1 ? history.get(1) : null;

        return Optional.of(new Resolved(latest, previous, latestMonth, true));
    }

    /** Contexte complet : identite, bulletins, ecarts, taux, formules et bareme. */
    @Transactional(readOnly = true)
    public String build(Long employeId, Resolved resolved) {
        StringBuilder sb = new StringBuilder();
        Employe employe = employeRepository.findById(employeId).orElse(null);
        ParametresPaie parametres = parametresRepository.findById(1L).orElse(null);

        if (employe != null) {
            sb.append("Employe : ").append(employe.getPrenom()).append(' ').append(employe.getNom())
                    .append(" (matricule ").append(employe.getMatricule()).append(")\n");
            if (employe.getPoste() != null) {
                sb.append("Poste : ").append(employe.getPoste()).append('\n');
            }
            if (employe.getTypeContrat() != null) {
                sb.append("Type de contrat : ").append(employe.getTypeContrat());
                if (isExonere(employe.getTypeContrat())) {
                    sb.append(" — contrat EXONERE de cotisations : le net est egal au brut, "
                            + "aucune CNSS, CSS ni IRPP n'est retenue");
                }
                sb.append('\n');
            }
        }

        if (resolved.fellBackToLatest()) {
            sb.append("\nNote : le mois demande n'a pas encore de bulletin (paie non close). "
                    + "L'explication porte sur le dernier bulletin disponible, ")
                    .append(resolved.month()).append(". Signale-le a l'utilisateur en une phrase.\n");
        }

        sb.append('\n').append(renderSlip("BULLETIN DE " + resolved.month(), resolved.current()));

        if (resolved.previous() != null) {
            sb.append('\n').append(renderSlip("BULLETIN DE " + resolved.previous().getMois()
                    + " (comparaison)", resolved.previous()));
            sb.append('\n').append(renderDelta(resolved.previous(), resolved.current()));
        } else {
            sb.append("\nAucun bulletin anterieur : aucune comparaison possible, laisse "
                    + "deltaReasons vide et explique simplement la composition du net.\n");
        }

        sb.append('\n').append(renderRates(parametres));
        sb.append('\n').append(renderFormulaBreakdown(resolved.current(), parametres, employe));
        sb.append('\n').append(renderBareme(resolved.month()));
        return sb.toString();
    }

    // ---- Rendu -------------------------------------------------------------

    private String renderSlip(String title, BulletinPaie slip) {
        StringBuilder sb = new StringBuilder("=== ").append(title).append(" ===\n");
        appendAmount(sb, "Salaire brut", slip.getSalaireBrut());
        appendAmount(sb, "Bonus", slip.getBonus());
        appendAmount(sb, "Deduction absences", slip.getDeductionAbsences());
        appendAmount(sb, "Brut effectif", slip.getBrutEffectif());
        appendAmount(sb, "CNSS salarie", slip.getCnssSalarie());
        appendAmount(sb, "Salaire imposable", slip.getSalaireImposable());
        appendAmount(sb, "Abattement", slip.getAbattementMontant());
        appendAmount(sb, "Revenu net imposable", slip.getRevenuNetImposable());
        appendAmount(sb, "IRPP mensuel", slip.getIrppMensuel());
        appendAmount(sb, "Contribution sociale de solidarite (CSS)", slip.getSolidariteSalarie());
        appendAmount(sb, "Net", slip.getNet());
        appendAmount(sb, "Acompte deja verse", slip.getAcompte());
        appendAmount(sb, "NET A PAYER", slip.getNetAPayer());

        List<ElementSalaireDTO> elements = slip.getElements();
        if (elements != null && !elements.isEmpty()) {
            sb.append("Elements variables du mois :\n");
            for (ElementSalaireDTO element : elements) {
                if (Boolean.FALSE.equals(element.getEnabled())) {
                    continue;
                }
                sb.append("  - ").append(element.getLabel())
                        .append(" (").append(element.getType()).append(") : ")
                        .append(format(value(element.getAmount())))
                        .append(' ').append(element.getUnit() == null ? "" : element.getUnit())
                        .append('\n');
            }
        }
        return sb.toString();
    }

    /** Ecarts calcules ici : une soustraction fausse dans une paie ne pardonne pas. */
    private String renderDelta(BulletinPaie previous, BulletinPaie current) {
        StringBuilder sb = new StringBuilder("=== ECARTS MOIS SUR MOIS (deja calcules, reprends-les) ===\n");
        appendDelta(sb, "Salaire brut", previous.getSalaireBrut(), current.getSalaireBrut());
        appendDelta(sb, "Bonus", previous.getBonus(), current.getBonus());
        appendDelta(sb, "Deduction absences", previous.getDeductionAbsences(), current.getDeductionAbsences());
        appendDelta(sb, "Brut effectif", previous.getBrutEffectif(), current.getBrutEffectif());
        appendDelta(sb, "CNSS salarie", previous.getCnssSalarie(), current.getCnssSalarie());
        appendDelta(sb, "Salaire imposable", previous.getSalaireImposable(), current.getSalaireImposable());
        appendDelta(sb, "IRPP mensuel", previous.getIrppMensuel(), current.getIrppMensuel());
        appendDelta(sb, "CSS", previous.getSolidariteSalarie(), current.getSolidariteSalarie());
        appendDelta(sb, "Acompte", previous.getAcompte(), current.getAcompte());
        appendDelta(sb, "Net a payer", previous.getNetAPayer(), current.getNetAPayer());
        return sb.toString();
    }

    /** Taux en vigueur, pour que l'explication cite des pourcentages reels. */
    private String renderRates(ParametresPaie parametres) {
        if (parametres == null) {
            return "=== TAUX ===\nParametres de paie indisponibles.\n";
        }
        StringBuilder sb = new StringBuilder("=== TAUX EN VIGUEUR ===\n");
        sb.append("CNSS salarie : ").append(percent(parametres.getCnssSalarie())).append('\n');
        sb.append("Contribution sociale de solidarite (CSS) : ")
                .append(percent(parametres.getSolidariteSalarie())).append('\n');
        sb.append("Abattement sur salaire imposable : ").append(percent(parametres.getAbattement())).append('\n');
        sb.append("CNSS patronale : ").append(percent(parametres.getCnssPatronale())).append('\n');
        sb.append("TFP : ").append(percent(parametres.getTfp()))
                .append(" — FOPROLOS : ").append(percent(parametres.getFoprolos()))
                .append(" — Accidents du travail : ").append(percent(parametres.getAt())).append('\n');
        return sb.toString();
    }

    /**
     * Chaine de calcul du net, etape par etape, deja chiffree.
     *
     * <p>C'est ce bloc qui permet a l'assistant d'expliquer « pourquoi ce montant »
     * plutot que de se contenter de le repeter. Chaque ligne montre la formule et
     * le resultat tel qu'il figure au bulletin : le modele n'a aucun calcul a
     * refaire, donc aucune occasion de se tromper.
     */
    private String renderFormulaBreakdown(BulletinPaie slip, ParametresPaie parametres, Employe employe) {
        StringBuilder sb = new StringBuilder("=== COMPOSITION DU NET, ETAPE PAR ETAPE ===\n");

        if (employe != null && isExonere(employe.getTypeContrat())) {
            sb.append("Contrat ").append(employe.getTypeContrat())
                    .append(" : exonere de cotisations.\n")
                    .append("  Net = Brut = ").append(format(value(slip.getBrutEffectif()))).append(" DT\n");
            appendAcompteStep(sb, slip);
            return sb.toString();
        }

        double brutEffectif = value(slip.getBrutEffectif());
        double cnss = value(slip.getCnssSalarie());
        double imposable = value(slip.getSalaireImposable());
        double abattement = value(slip.getAbattementMontant());
        double netImposable = value(slip.getRevenuNetImposable());
        double css = value(slip.getSolidariteSalarie());
        double irpp = value(slip.getIrppMensuel());
        double net = value(slip.getNet());

        sb.append("1. Brut effectif = salaire brut");
        if (Math.abs(value(slip.getBonus())) > 0.001) {
            sb.append(" + bonus (").append(format(value(slip.getBonus()))).append(" DT)");
        }
        if (Math.abs(value(slip.getDeductionAbsences())) > 0.001) {
            sb.append(" − absences (").append(format(value(slip.getDeductionAbsences()))).append(" DT)");
        }
        sb.append(" = ").append(format(brutEffectif)).append(" DT\n");

        sb.append("2. CNSS salarie = base CNSS × ")
                .append(percent(parametres == null ? null : parametres.getCnssSalarie()))
                .append(" = ").append(format(cnss)).append(" DT\n");

        sb.append("3. Salaire imposable = brut ajuste IRPP − CNSS = ")
                .append(format(imposable)).append(" DT\n");

        sb.append("4. Abattement = salaire imposable × ")
                .append(percent(parametres == null ? null : parametres.getAbattement()))
                .append(" = ").append(format(abattement)).append(" DT\n");

        sb.append("5. Revenu net imposable = ").append(format(imposable)).append(" − ")
                .append(format(abattement)).append(" = ").append(format(netImposable)).append(" DT\n");

        sb.append("6. Contribution de solidarite (CSS) = base × ")
                .append(percent(parametres == null ? null : parametres.getSolidariteSalarie()))
                .append(" = ").append(format(css)).append(" DT\n");

        sb.append("7. IRPP mensuel = bareme progressif applique a (")
                .append(format(netImposable)).append(" × 12 = ").append(format(netImposable * 12))
                .append(" DT annuels), puis ÷ 12 = ").append(format(irpp)).append(" DT\n");

        sb.append("8. Net = ").append(format(brutEffectif)).append(" − ").append(format(cnss))
                .append(" (CNSS) − ").append(format(css)).append(" (CSS) − ").append(format(irpp))
                .append(" (IRPP) = ").append(format(net)).append(" DT\n");

        appendAcompteStep(sb, slip);

        // Cout employeur : hors du net, mais souvent la question suivante.
        if (slip.getChargesEmployeur() != null) {
            sb.append("\nCote employeur (n'affecte pas votre net) : charges patronales ")
                    .append(format(value(slip.getChargesEmployeur())))
                    .append(" DT, cout total ").append(format(value(slip.getCoutTotal()))).append(" DT\n");
        }
        return sb.toString();
    }

    private void appendAcompteStep(StringBuilder sb, BulletinPaie slip) {
        double acompte = value(slip.getAcompte());
        if (Math.abs(acompte) > 0.001) {
            sb.append("9. Net a payer = net − acomptes deja verses (")
                    .append(format(acompte)).append(" DT) = ")
                    .append(format(value(slip.getNetAPayer()))).append(" DT\n");
        } else {
            sb.append("9. Net a payer = net (aucun acompte ce mois-ci) = ")
                    .append(format(value(slip.getNetAPayer()))).append(" DT\n");
        }
    }

    private String renderBareme(YearMonth month) {
        Optional<BaremeIrpp> bareme = baremeRepository
                .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(month.atDay(1));
        if (bareme.isEmpty()) {
            return "=== BAREME IRPP ===\nAucun bareme en vigueur pour cette periode.\n";
        }
        BaremeIrpp value = bareme.get();
        StringBuilder sb = new StringBuilder("=== BAREME IRPP ANNUEL, en vigueur depuis ")
                .append(value.getEffectiveFrom()).append(" ===\n");
        double borneBasse = 0.0;
        for (TrancheIrppDTO tranche : value.getTranches()) {
            sb.append("  De ").append(format(borneBasse)).append(" DT a ")
                    .append(tranche.getPlafond() == null ? "au-dela" : format(tranche.getPlafond()) + " DT")
                    .append(" : ").append(percent(tranche.getTaux())).append('\n');
            if (tranche.getPlafond() == null) {
                break;
            }
            borneBasse = tranche.getPlafond();
        }
        return sb.toString();
    }

    // ---- Helpers -----------------------------------------------------------

    private boolean isExonere(String typeContrat) {
        return typeContrat != null
                && TYPES_EXONERES.stream().anyMatch(type -> type.equalsIgnoreCase(typeContrat));
    }

    private void appendAmount(StringBuilder sb, String label, Double amount) {
        if (amount != null) {
            sb.append(label).append(" : ").append(format(amount)).append(" DT\n");
        }
    }

    private void appendDelta(StringBuilder sb, String label, Double previous, Double current) {
        double before = value(previous);
        double after = value(current);
        double delta = after - before;
        if (Math.abs(delta) < 0.001) {
            return;
        }
        sb.append(label).append(" : ").append(format(before)).append(" -> ").append(format(after))
                .append(" (").append(delta > 0 ? "+" : "").append(format(delta)).append(" DT)\n");
    }

    private double value(Double amount) {
        return amount == null ? 0.0 : amount;
    }

    private String format(double amount) {
        return String.format(Locale.FRANCE, "%.3f", amount);
    }

    private String percent(Double rate) {
        return rate == null ? "-" : String.format(Locale.FRANCE, "%.2f %%", rate * 100);
    }
}
