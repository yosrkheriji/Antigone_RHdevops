package com.antigone.rh.ai.security;

import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.repository.MediaPlanAssignmentRepository;
import com.antigone.rh.security.AuthPrincipal;
import com.antigone.rh.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Point unique de décision d'accès pour l'assistant IA.
 *
 * <p>Le spec raisonne en rôles {@code SOCIAL_MEDIA} / {@code ADMIN} / {@code EMPLOYEE}.
 * L'application, elle, utilise des rôles dynamiques en base plus un catalogue de
 * permissions. La correspondance retenue :
 * <ul>
 *   <li>« SOCIAL_MEDIA » → permission {@code VIEW_MEDIA_PLAN} (périmètre restreint
 *       aux clients assignés) ou {@code VIEW_TOUS_MEDIA_PLAN} (tous les clients) ;</li>
 *   <li>« ADMIN » → rôle {@code ADMIN} ou permission {@code VIEW_FINANCE} ;</li>
 *   <li>« EMPLOYEE » → tout compte employé authentifié, limité à ses propres données.</li>
 * </ul>
 *
 * <p>Aucune décision d'accès n'est déléguée au prompt : chaque outil et chaque
 * endpoint repasse par cette classe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiAccessScope {

    public static final String PERM_MEDIA_PLAN = "VIEW_MEDIA_PLAN";
    public static final String PERM_ALL_MEDIA_PLANS = "VIEW_TOUS_MEDIA_PLAN";
    public static final String PERM_FINANCE = "VIEW_FINANCE";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String PRINCIPAL_EMPLOYEE = "EMPLOYEE";

    /** Sentinelle : un {@code IN ()} vide est invalide en SQL natif. */
    public static final List<Long> NO_CLIENT = List.of(-1L);

    private final MediaPlanAssignmentRepository assignmentRepository;

    /** Périmètre client d'une recherche RAG. */
    public record ClientScope(boolean unrestricted, List<Long> clientIds) {

        public static ClientScope all() {
            return new ClientScope(true, NO_CLIENT);
        }

        public static ClientScope of(List<Long> ids) {
            return new ClientScope(false, ids == null || ids.isEmpty() ? NO_CLIENT : ids);
        }

        public boolean allows(Long clientId) {
            return unrestricted || (clientId != null && clientIds.contains(clientId));
        }
    }

    public AuthPrincipal current() {
        return SecurityUtils.getCurrentPrincipal();
    }

    public boolean isAdmin(AuthPrincipal principal) {
        return principal.hasRole(ROLE_ADMIN) || hasPermission(principal, PERM_FINANCE);
    }

    public boolean isEmployee(AuthPrincipal principal) {
        return PRINCIPAL_EMPLOYEE.equals(principal.getPrincipalType()) && principal.getEmployeId() != null;
    }

    public boolean hasPermission(AuthPrincipal principal, String permission) {
        Set<String> permissions = principal.getPermissions();
        return permissions != null && permissions.contains(permission);
    }

    // ── Media plan ────────────────────────────────────────────────────────────

    public boolean canUseMediaPlanAssistant(AuthPrincipal principal) {
        return hasPermission(principal, PERM_MEDIA_PLAN)
                || hasPermission(principal, PERM_ALL_MEDIA_PLANS)
                || principal.hasRole(ROLE_ADMIN);
    }

    public void requireMediaPlanAssistant(AuthPrincipal principal) {
        if (!canUseMediaPlanAssistant(principal)) {
            throw new AiForbiddenException(
                    "Acces refuse : la generation de media plan requiert la permission VIEW_MEDIA_PLAN.");
        }
    }

    /**
     * Périmètre client de l'utilisateur. {@code VIEW_TOUS_MEDIA_PLAN} et le rôle
     * ADMIN ouvrent tout ; sinon seules les marques explicitement assignées via
     * {@code media_plan_assignments} sont visibles.
     */
    @Transactional(readOnly = true)
    public ClientScope mediaPlanClientScope(AuthPrincipal principal) {
        if (hasPermission(principal, PERM_ALL_MEDIA_PLANS) || principal.hasRole(ROLE_ADMIN)) {
            return ClientScope.all();
        }
        Long employeId = principal.getEmployeId();
        if (employeId == null) {
            return ClientScope.of(List.of());
        }
        List<Long> clientIds = assignmentRepository.findByEmployeId(employeId).stream()
                .map(assignment -> assignment.getClient().getId())
                .distinct()
                .toList();
        return ClientScope.of(clientIds);
    }

    /**
     * Vérifie qu'une marque précise est dans le périmètre. Appelé par les outils
     * <em>avant</em> toute lecture, y compris quand l'identifiant vient du LLM.
     */
    @Transactional(readOnly = true)
    public void requireClientAllowed(AuthPrincipal principal, Long clientId) {
        if (clientId == null) {
            throw new AiForbiddenException("Identifiant de client manquant.");
        }
        if (!mediaPlanClientScope(principal).allows(clientId)) {
            log.warn("Acces client refuse : compte={} client={}", principal.getAccountId(), clientId);
            throw new AiForbiddenException(
                    "Acces refuse : la marque " + clientId + " n'est pas dans votre perimetre.");
        }
    }

    // ── Relances clients ──────────────────────────────────────────────────────

    public boolean canUseReminderAssistant(AuthPrincipal principal) {
        return isAdmin(principal);
    }

    public void requireReminderAssistant(AuthPrincipal principal) {
        if (!canUseReminderAssistant(principal)) {
            throw new AiForbiddenException(
                    "Acces refuse : la redaction de relances est reservee aux administrateurs.");
        }
    }

    // ── Bulletin de paie ──────────────────────────────────────────────────────

    public boolean canUsePayslipAssistant(AuthPrincipal principal) {
        return isAdmin(principal) || isEmployee(principal);
    }

    /**
     * Résout l'employé dont le bulletin peut être lu.
     *
     * <p>Un administrateur interroge librement. Tout autre compte est ramené à son
     * propre {@code employeId} issu du JWT : la valeur du corps de requête est
     * ignorée, et une demande explicite sur un tiers est refusée. C'est ce qui rend
     * inopérante toute reformulation de prompt visant le bulletin d'un collègue.
     */
    public Long resolvePayslipEmployeId(AuthPrincipal principal, Long requestedEmployeId) {
        if (isAdmin(principal)) {
            if (requestedEmployeId == null) {
                throw new AiForbiddenException("Identifiant d'employe requis.");
            }
            return requestedEmployeId;
        }
        Long ownEmployeId = principal.getEmployeId();
        if (ownEmployeId == null) {
            throw new AiForbiddenException("Acces refuse : compte employe requis.");
        }
        if (requestedEmployeId != null && !requestedEmployeId.equals(ownEmployeId)) {
            log.warn("Tentative d'acces au bulletin d'un tiers : compte={} demande={} autorise={}",
                    principal.getAccountId(), requestedEmployeId, ownEmployeId);
            throw new AiForbiddenException(
                    "Acces refuse : vous ne pouvez consulter que votre propre bulletin de paie.");
        }
        return ownEmployeId;
    }
}
