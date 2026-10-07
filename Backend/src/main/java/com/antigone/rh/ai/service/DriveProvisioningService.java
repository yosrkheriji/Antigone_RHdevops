package com.antigone.rh.ai.service;

import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Approvisionnement Google Drive des media plans generes.
 *
 * <p>Drive est un service externe qui peut etre lent ou indisponible ; il ne doit
 * jamais faire echouer une generation deja payee au LLM. En cas d'echec le media
 * plan est persiste avec {@link #LIEN_DRIVE_PENDING} en guise de lien, et
 * {@link #retryPending(Long, String)} reprend uniquement les lignes concernees —
 * pas tout le pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriveProvisioningService {

    /** Marqueur pose sur {@code lienDrive} quand Drive n'a pas repondu. */
    public static final String LIEN_DRIVE_PENDING = "PENDING";

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_BACKOFF_MS = 1500;

    private final GoogleDriveService googleDriveService;
    private final MediaPlanRepository mediaPlanRepository;

    /**
     * Resout le lien du dossier Drive du mois pour une marque.
     *
     * @return le lien, ou {@link Optional#empty()} si Drive est indisponible
     */
    public Optional<String> resolveMonthFolderLink(String clientName, YearMonth month) {
        if (clientName == null || clientName.isBlank()) {
            return Optional.empty();
        }
        LocalDate anyDayOfMonth = month.atDay(1);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String link = googleDriveService.getOrCreateClientMonthFolder(clientName, anyDayOfMonth);
                if (link != null && !link.isBlank()) {
                    return Optional.of(link);
                }
                log.warn("Drive a repondu sans lien pour {} {} (tentative {}/{})",
                        clientName, month, attempt, MAX_ATTEMPTS);
            } catch (Exception e) {
                log.warn("Approvisionnement Drive en echec pour {} {} (tentative {}/{}) : {}",
                        clientName, month, attempt, MAX_ATTEMPTS, e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepQuietly();
            }
        }
        return Optional.empty();
    }

    /** Lien du dossier racine de la marque, ou null si Drive est indisponible. */
    public String clientFolderLink(String clientName) {
        try {
            return googleDriveService.getClientFolderLink(clientName);
        } catch (Exception e) {
            log.warn("Lien du dossier client indisponible pour {} : {}", clientName, e.getMessage());
            return null;
        }
    }

    /**
     * Reprend les lignes restees en attente pour une marque et un mois. Retente
     * uniquement l'etape Drive : le contenu genere n'est jamais reproduit.
     *
     * @return nombre de lignes effectivement mises a jour
     */
    @Transactional
    public int retryPending(Long clientId, String mois) {
        YearMonth month = YearMonth.parse(mois);
        List<MediaPlan> pending = new ArrayList<>();
        for (MediaPlan plan : mediaPlanRepository.findByClientId(clientId)) {
            boolean sameMonth = plan.getDatePublication() != null
                    && YearMonth.from(plan.getDatePublication()).equals(month);
            if (sameMonth && isPending(plan.getLienDrive())) {
                pending.add(plan);
            }
        }
        if (pending.isEmpty()) {
            return 0;
        }
        Client client = pending.get(0).getClient();
        Optional<String> link = resolveMonthFolderLink(client != null ? client.getNom() : null, month);
        if (link.isEmpty()) {
            log.info("Reprise Drive infructueuse : {} lignes restent en attente pour la marque {} en {}",
                    pending.size(), clientId, mois);
            return 0;
        }
        pending.forEach(plan -> plan.setLienDrive(link.get()));
        mediaPlanRepository.saveAll(pending);
        log.info("Reprise Drive : {} lignes approvisionnees pour la marque {} en {}",
                pending.size(), clientId, mois);
        return pending.size();
    }

    public static boolean isPending(String lienDrive) {
        return lienDrive == null || lienDrive.isBlank() || LIEN_DRIVE_PENDING.equals(lienDrive);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(RETRY_BACKOFF_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
