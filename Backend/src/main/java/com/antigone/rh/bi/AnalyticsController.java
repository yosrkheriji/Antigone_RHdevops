package com.antigone.rh.bi;

import com.antigone.rh.bi.dto.EtatEntrepotDTO;
import com.antigone.rh.bi.dto.FinanceDashboardDTO;
import com.antigone.rh.bi.dto.PresenceDashboardDTO;
import com.antigone.rh.bi.dto.ProjetsDashboardDTO;
import com.antigone.rh.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API decisionnelle : sert les trois tableaux de bord depuis l'entrepot {@code dwh}.
 *
 * <p>Le cloisonnement reprend celui des applications : la presence exige
 * VIEW_MONITORING (espace RH), la finance VIEW_FINANCE, les projets
 * VIEW_PROJETS. Un administrateur passe partout.
 *
 * <p>VIEW_TOUS_PROJETS reste acceptee par tolerance, mais ne protege rien :
 * elle est absente de RoleService.PERMISSION_LABELS, donc initPermissions()
 * la supprime a chaque demarrage et aucun compte ne peut la porter.
 *
 * <p>Sans parametre de periode, la fenetre par defaut est les douze derniers mois.
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final String ADMIN = "ROLE_ADMIN";

    private final AnalyticsService analyticsService;
    private final DwhEtlService etlService;

    @GetMapping("/presence")
    @PreAuthorize("hasAnyAuthority('" + ADMIN + "', 'VIEW_MONITORING', 'VIEW_DASHBOARD_RH')")
    public ResponseEntity<ApiResponse<PresenceDashboardDTO>> presence(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String departement) {
        return repondre(() -> analyticsService.presence(debutOuDefaut(debut), finOuDefaut(fin), departement));
    }

    @GetMapping("/departements")
    @PreAuthorize("hasAnyAuthority('" + ADMIN + "', 'VIEW_MONITORING', 'VIEW_DASHBOARD_RH')")
    public ResponseEntity<ApiResponse<List<String>>> departements() {
        return repondre(analyticsService::departements);
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyAuthority('" + ADMIN + "', 'VIEW_FINANCE')")
    public ResponseEntity<ApiResponse<FinanceDashboardDTO>> finance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return repondre(() -> analyticsService.finance(debutOuDefaut(debut), finOuDefaut(fin)));
    }

    @GetMapping("/projets")
    @PreAuthorize("hasAnyAuthority('" + ADMIN + "', 'VIEW_TOUS_PROJETS', 'VIEW_PROJETS')")
    public ResponseEntity<ApiResponse<ProjetsDashboardDTO>> projets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return repondre(() -> analyticsService.projets(debutOuDefaut(debut), finOuDefaut(fin)));
    }

    /** Fraicheur des donnees — affichee en bandeau sur chaque page Analytique. */
    @GetMapping("/etat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EtatEntrepotDTO>> etat() {
        return repondre(analyticsService::etat);
    }

    /**
     * Rechargement a la demande. Reserve a l'administrateur : le traitement
     * reconstruit l'integralite de l'entrepot.
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('" + ADMIN + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh() {
        try {
            long debut = System.currentTimeMillis();
            long lignes = etlService.rafraichir();
            return ResponseEntity.ok(ApiResponse.ok(
                    "Entrepot recharge.",
                    Map.of("lignes", lignes, "dureeMs", System.currentTimeMillis() - debut)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (DataAccessException e) {
            log.error("[BI] Rechargement manuel en echec", e);
            return ResponseEntity.status(503).body(ApiResponse.error(messageEntrepot(e)));
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Un entrepot absent (schema jamais construit, base indisponible) ne doit pas
     * remonter une trace SQL au navigateur : on renvoie 503 avec la marche a suivre.
     */
    private <T> ResponseEntity<ApiResponse<T>> repondre(java.util.function.Supplier<T> requete) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(requete.get()));
        } catch (DataAccessException e) {
            log.error("[BI] Lecture de l'entrepot impossible", e);
            return ResponseEntity.status(503).body(ApiResponse.error(messageEntrepot(e)));
        }
    }

    private String messageEntrepot(DataAccessException e) {
        return "Entrepot decisionnel indisponible. Redemarrez le backend pour le reconstruire, "
                + "ou lancez SELECT dwh.refresh_all(); en base. Detail : " + e.getMostSpecificCause().getMessage();
    }

    private LocalDate debutOuDefaut(LocalDate debut) {
        return debut != null ? debut : LocalDate.now().minusMonths(12).withDayOfMonth(1);
    }

    private LocalDate finOuDefaut(LocalDate fin) {
        return fin != null ? fin : LocalDate.now();
    }
}
