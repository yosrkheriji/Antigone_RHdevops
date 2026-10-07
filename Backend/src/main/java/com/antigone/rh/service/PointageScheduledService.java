package com.antigone.rh.service;

import com.antigone.rh.entity.Heartbeat;
import com.antigone.rh.entity.Pointage;
import com.antigone.rh.repository.HeartbeatRepository;
import com.antigone.rh.repository.PointageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service planifié : remplit automatiquement heure_sortie dans la table
 * pointages
 * quand un agent ne s'est pas manuellement déconnecté (PC éteint, crash réseau,
 * etc.)
 *
 * Logique :
 * Toutes les 5 minutes → pour chaque pointage du jour sans heureSortie :
 * → si le dernier heartbeat est > 10 min passé → heureSortie = timestamp du
 * dernier heartbeat
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointageScheduledService {

    private final PointageRepository pointageRepository;
    private final HeartbeatRepository heartbeatRepository;

    /**
     * Délai avant de considérer qu'un agent est parti : 10 minutes sans heartbeat
     */
    private static final int SEUIL_INACTIVITE_MINUTES = 10;

    /**
     * Tourne toutes les 5 minutes.
     * Cherche les pointages du jour sans heure_sortie et dont l'agent n'envoie
     * plus de heartbeat depuis plus de SEUIL_INACTIVITE_MINUTES minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 minutes
    @Transactional
    public void autoFillHeureSortie() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Pointage> pointagesSansSortie = pointageRepository.findByDatePointage(today)
                .stream()
                .filter(p -> p.getHeureEntree() != null && p.getHeureSortie() == null)
                .toList();

        if (pointagesSansSortie.isEmpty())
            return;

        for (Pointage pointage : pointagesSansSortie) {
            Long employeId = pointage.getEmploye().getId();
            Heartbeat dernierHeartbeat = heartbeatRepository.findLastByEmployeId(employeId);

            if (dernierHeartbeat == null)
                continue;

            LocalDateTime tsHeartbeat = dernierHeartbeat.getTimestamp();
            long minutesDepuisDernierHb = java.time.temporal.ChronoUnit.MINUTES.between(tsHeartbeat, now);

            if (minutesDepuisDernierHb >= SEUIL_INACTIVITE_MINUTES) {
                // L'agent est parti sans clock-out → on utilise le dernier heartbeat comme
                // heure de sortie
                LocalTime heureSortie = tsHeartbeat.toLocalTime()
                        .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                pointage.setHeureSortie(heureSortie);
                pointageRepository.save(pointage);
                log.info("[PointageScheduled] Employé {} → heureSortie auto : {} (dernier HB il y a {} min)",
                        employeId, heureSortie, minutesDepuisDernierHb);
            }
        }
    }
}
