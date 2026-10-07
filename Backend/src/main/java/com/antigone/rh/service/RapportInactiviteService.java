package com.antigone.rh.service;

import com.antigone.rh.dto.RapportDecisionRequest;
import com.antigone.rh.dto.RapportInactiviteDTO;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.Pointage;
import com.antigone.rh.entity.RapportInactivite;
import com.antigone.rh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RapportInactiviteService {

    private final RapportInactiviteRepository rapportRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final PointageRepository pointageRepository;
    private final EmployeRepository employeRepository;
    private final AgentService agentService;

    /**
     * Récupérer tous les rapports
     */
    public List<RapportInactiviteDTO> getAll() {
        return rapportRepository.findAllByOrderByDateGenerationDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Générer les rapports depuis la dernière décision jusqu'à aujourd'hui pour
     * chaque employé
     */
    public List<RapportInactiviteDTO> genererSemaineCourante() {
        LocalDate today = LocalDate.now();
        List<Employe> employes = employeRepository.findAll();
        List<RapportInactiviteDTO> generated = new ArrayList<>();

        for (Employe employe : employes) {
            LocalDate debut = getDateApresDerniereDecision(employe.getId());
            if (debut.isAfter(today))
                continue;
            generated.addAll(genererRapports(debut, today));
        }

        return generated;
    }

    /**
     * Trouver la date du lendemain de la dernière décision pour un employé.
     * S'il n'y a aucune décision, retourne la date d'embauche ou il y a 30 jours.
     */
    private LocalDate getDateApresDerniereDecision(Long employeId) {
        List<RapportInactivite> lastDecided = rapportRepository.findLastDecidedByEmployeId(employeId);
        if (!lastDecided.isEmpty()) {
            return lastDecided.get(0).getSemaineFin().plusDays(1);
        }
        // Pas de décision précédente : prendre la date d'embauche ou 30 jours en
        // arrière
        Employe employe = employeRepository.findById(employeId).orElse(null);
        if (employe != null && employe.getDateEmbauche() != null) {
            return employe.getDateEmbauche();
        }
        return LocalDate.now().minusDays(30);
    }

    /**
     * Générer les rapports pour une période donnée (sans découpage en semaines)
     */
    public List<RapportInactiviteDTO> genererPeriode(LocalDate debut, LocalDate fin) {
        return genererRapports(debut, fin);
    }

    /**
     * Prendre une décision sur un rapport (DEDUIT ou ANNULE)
     */
    public RapportInactiviteDTO decider(Long rapportId, RapportDecisionRequest request) {
        RapportInactivite rapport = rapportRepository.findById(rapportId)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé: " + rapportId));

        if (!"EN_ATTENTE".equals(rapport.getDecision())) {
            throw new RuntimeException("Ce rapport a déjà été traité.");
        }

        Employe decidePar = employeRepository.findById(request.getDecideParId())
                .orElseThrow(() -> new RuntimeException("Décideur non trouvé: " + request.getDecideParId()));

        rapport.setDecision(request.getDecision());
        rapport.setDecidePar(decidePar);
        rapport.setCommentaire(request.getCommentaire());
        rapport.setDateDecision(LocalDateTime.now());

        rapportRepository.save(rapport);
        return toDTO(rapport);
    }

    // ========================================
    // LOGIQUE DE GENERATION
    // ========================================

    private List<RapportInactiviteDTO> genererRapports(LocalDate periodeDebut, LocalDate periodeFin) {
        List<Employe> employes = employeRepository.findAll();
        List<RapportInactiviteDTO> generated = new ArrayList<>();

        for (Employe employe : employes) {
            // Vérifier si un rapport en attente existe déjà pour cette période exacte
            if (rapportRepository.findPendingByEmployeAndPeriod(
                    employe.getId(), periodeDebut, periodeFin).isPresent()) {
                continue; // Déjà généré
            }

            // Compter les minutes d'inactivité uniquement sur les jours ouvrés
            long inactiveMinutes = countInactiveMinutesForWorkingDays(employe.getId(), periodeDebut, periodeFin);

            // Calculer le retard cumulé : exclure les jours congé/férié/non-travaillé
            List<Pointage> pointages = pointageRepository.findByEmployeIdAndDatePointageBetween(
                    employe.getId(), periodeDebut, periodeFin);
            int retardCumule = pointages.stream()
                    .filter(p -> isStatutAvecRetard(p.getStatut()))
                    .mapToInt(p -> p.getRetardMinutes() != null ? p.getRetardMinutes() : 0)
                    .sum();

            // Si aucun retard et aucune inactivité, pas besoin de rapport
            if (retardCumule == 0 && inactiveMinutes == 0)
                continue;

            // Tolérance = 0 (chaque minute compte)
            int toleranceMinutes = 0;
            int inactiviteExcedentaire = (int) Math.max(0, inactiveMinutes - toleranceMinutes);

            // Calculer le montant de déduction basé sur le retard cumulé uniquement
            double prixMinute = agentService.calculatePrixMinute(employe);
            double montantDeduction = retardCumule * prixMinute;

            RapportInactivite rapport = RapportInactivite.builder()
                    .employe(employe)
                    .semaineDebut(periodeDebut)
                    .semaineFin(periodeFin)
                    .totalInactiviteMinutes((int) inactiveMinutes)
                    .toleranceMinutes(toleranceMinutes)
                    .inactiviteExcedentaire(inactiviteExcedentaire)
                    .retardCumule(retardCumule)
                    .montantDeduction(montantDeduction)
                    .decision("EN_ATTENTE")
                    .build();

            rapportRepository.save(rapport);
            generated.add(toDTO(rapport));
        }

        return generated;
    }

    /**
     * Compte les minutes d'inactivité uniquement sur les jours ouvrés (hors congés,
     * jours fériés, jours non travaillés).
     */
    private long countInactiveMinutesForWorkingDays(Long employeId, LocalDate debut, LocalDate fin) {
        long total = 0;
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            if (!agentService.isJourOuvrePourEmploye(employeId, date))
                continue;
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            total += heartbeatRepository.countInactiveMinutes(employeId, startOfDay, endOfDay);
        }
        return total;
    }

    /**
     * Vérifie si un statut de pointage doit être pris en compte dans le calcul du
     * retard.
     * Les congés, jours fériés et jours non travaillés sont exclus.
     */
    private boolean isStatutAvecRetard(String statut) {
        if (statut == null)
            return false;
        return "PRESENT".equals(statut) || "RETARD".equals(statut) || "TELETRAVAIL".equals(statut);
    }

    // ========================================
    // MAPPING
    // ========================================

    private RapportInactiviteDTO toDTO(RapportInactivite r) {
        int totalInactiviteMinutes = r.getTotalInactiviteMinutes();
        int toleranceMinutes = r.getToleranceMinutes();
        int inactiviteExcedentaire = r.getInactiviteExcedentaire();
        int retardCumule = r.getRetardCumule() != null ? r.getRetardCumule() : 0;
        double montantDeduction = r.getMontantDeduction();

        // Pour les rapports EN_ATTENTE, recalculer dynamiquement depuis la base
        if ("EN_ATTENTE".equals(r.getDecision())) {
            Long employeId = r.getEmploye().getId();
            LocalDate semaineDebut = r.getSemaineDebut();
            LocalDate semaineFin = r.getSemaineFin();

            long inactiveMinutesLive = countInactiveMinutesForWorkingDays(employeId, semaineDebut, semaineFin);
            totalInactiviteMinutes = (int) inactiveMinutesLive;
            inactiviteExcedentaire = (int) Math.max(0, inactiveMinutesLive - toleranceMinutes);

            List<Pointage> pointages = pointageRepository.findByEmployeIdAndDatePointageBetween(
                    employeId, semaineDebut, semaineFin);
            retardCumule = pointages.stream()
                    .filter(p -> isStatutAvecRetard(p.getStatut()))
                    .mapToInt(p -> p.getRetardMinutes() != null ? p.getRetardMinutes() : 0)
                    .sum();

            double prixMinute = agentService.calculatePrixMinute(r.getEmploye());
            montantDeduction = retardCumule * prixMinute;

            // Mettre à jour l'entité en base aussi
            r.setTotalInactiviteMinutes(totalInactiviteMinutes);
            r.setInactiviteExcedentaire(inactiviteExcedentaire);
            r.setRetardCumule(retardCumule);
            r.setMontantDeduction(montantDeduction);
            rapportRepository.save(r);
        }

        String decideParNom = null;
        if (r.getDecidePar() != null) {
            decideParNom = r.getDecidePar().getNom() + " " + r.getDecidePar().getPrenom();
        }

        return RapportInactiviteDTO.builder()
                .id(r.getId())
                .employeId(r.getEmploye().getId())
                .employeNom(r.getEmploye().getNom())
                .employePrenom(r.getEmploye().getPrenom())
                .employeMatricule(r.getEmploye().getMatricule())
                .semaineDebut(r.getSemaineDebut().toString())
                .semaineFin(r.getSemaineFin().toString())
                .totalInactiviteMinutes(totalInactiviteMinutes)
                .toleranceMinutes(toleranceMinutes)
                .inactiviteExcedentaire(inactiviteExcedentaire)
                .retardCumule(retardCumule)
                .montantDeduction(montantDeduction)
                .decision(r.getDecision())
                .decideParNom(decideParNom)
                .commentaire(r.getCommentaire())
                .dateDecision(r.getDateDecision() != null ? r.getDateDecision().toString() : null)
                .dateGeneration(r.getDateGeneration() != null ? r.getDateGeneration().toString() : null)
                .build();
    }
}
