package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.MediaPlanAssignment;
import com.antigone.rh.repository.MediaPlanAssignmentRepository;
import com.antigone.rh.security.AuthPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/**
 * Scenarios 3, 6 et 8 : cloisonnement par role, par marque et par employe.
 *
 * <p>Ces regles sont testees ici sans Spring ni base : elles ne doivent dependre
 * d'aucun contexte, puisque ce sont elles que les outils invoquent depuis un thread
 * de callback LangChain4j.
 */
@ExtendWith(MockitoExtension.class)
class AiAccessScopeTest {

    @Mock
    private MediaPlanAssignmentRepository assignmentRepository;

    @InjectMocks
    private AiAccessScope accessScope;

    private static AuthPrincipal principal(Set<String> roles, Set<String> permissions, Long employeId) {
        return AuthPrincipal.builder()
                .principalType("EMPLOYEE")
                .accountId(1L)
                .employeId(employeId)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    private static AuthPrincipal socialMedia(Long employeId) {
        return principal(Set.of("SOCIAL_MEDIA"), Set.of("VIEW_MEDIA_PLAN"), employeId);
    }

    private static AuthPrincipal admin() {
        return principal(Set.of("ADMIN"), Set.of("VIEW_FINANCE"), 99L);
    }

    private void assign(Long employeId, Long... clientIds) {
        List<MediaPlanAssignment> assignments = java.util.Arrays.stream(clientIds)
                .map(clientId -> MediaPlanAssignment.builder()
                        .employe(Employe.builder().id(employeId).build())
                        .client(Client.builder().id(clientId).build())
                        .build())
                .toList();
        lenient().when(assignmentRepository.findByEmployeId(employeId)).thenReturn(assignments);
    }

    @Nested
    @DisplayName("Media plan — scenario 3")
    class MediaPlanAccess {

        @Test
        @DisplayName("VIEW_MEDIA_PLAN autorise l'assistant media plan")
        void socialMediaIsAllowed() {
            assertThat(accessScope.canUseMediaPlanAssistant(socialMedia(10L))).isTrue();
        }

        @Test
        @DisplayName("Un compte sans permission media plan est refuse")
        void plainEmployeeIsDenied() {
            AuthPrincipal employee = principal(Set.of("EMPLOYE"), Set.of("VIEW_MES_DEMANDES"), 10L);

            assertThat(accessScope.canUseMediaPlanAssistant(employee)).isFalse();
            assertThatThrownBy(() -> accessScope.requireMediaPlanAssistant(employee))
                    .isInstanceOf(AiForbiddenException.class)
                    .hasMessageContaining("VIEW_MEDIA_PLAN");
        }

        @Test
        @DisplayName("VIEW_TOUS_MEDIA_PLAN ouvre toutes les marques")
        void allMediaPlansPermissionIsUnrestricted() {
            AuthPrincipal manager = principal(Set.of("SM_MANAGER"), Set.of("VIEW_TOUS_MEDIA_PLAN"), 11L);

            AiAccessScope.ClientScope scope = accessScope.mediaPlanClientScope(manager);

            assertThat(scope.unrestricted()).isTrue();
            assertThat(scope.allows(1234L)).isTrue();
        }

        @Test
        @DisplayName("Le perimetre se limite aux marques assignees")
        void scopeIsLimitedToAssignedClients() {
            assign(10L, 1L, 2L);

            AiAccessScope.ClientScope scope = accessScope.mediaPlanClientScope(socialMedia(10L));

            assertThat(scope.unrestricted()).isFalse();
            assertThat(scope.clientIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(scope.allows(1L)).isTrue();
            assertThat(scope.allows(3L)).isFalse();
        }

        @Test
        @DisplayName("Une marque hors perimetre est refusee, meme demandee par le LLM")
        void foreignBrandIsRejected() {
            assign(10L, 1L);
            AuthPrincipal user = socialMedia(10L);

            assertThatCode(() -> accessScope.requireClientAllowed(user, 1L)).doesNotThrowAnyException();
            assertThatThrownBy(() -> accessScope.requireClientAllowed(user, 2L))
                    .isInstanceOf(AiForbiddenException.class)
                    .hasMessageContaining("perimetre");
        }

        @Test
        @DisplayName("Sans aucune assignation, la sentinelle evite un IN () invalide")
        void emptyScopeUsesSentinel() {
            assign(10L);

            AiAccessScope.ClientScope scope = accessScope.mediaPlanClientScope(socialMedia(10L));

            assertThat(scope.clientIds()).isNotEmpty();
            assertThat(scope.clientIds()).containsExactly(-1L);
            assertThat(scope.allows(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Relances — scenario 6")
    class ReminderAccess {

        @Test
        @DisplayName("L'administrateur peut rediger une relance")
        void adminIsAllowed() {
            assertThat(accessScope.canUseReminderAssistant(admin())).isTrue();
        }

        @Test
        @DisplayName("Un profil social media est refuse")
        void socialMediaIsDenied() {
            AuthPrincipal user = socialMedia(10L);

            assertThat(accessScope.canUseReminderAssistant(user)).isFalse();
            assertThatThrownBy(() -> accessScope.requireReminderAssistant(user))
                    .isInstanceOf(AiForbiddenException.class)
                    .hasMessageContaining("administrateurs");
        }

        @Test
        @DisplayName("VIEW_FINANCE vaut acces administrateur pour cette capacite")
        void financePermissionIsEnough() {
            AuthPrincipal comptable = principal(Set.of("COMPTABLE"), Set.of("VIEW_FINANCE"), 12L);

            assertThat(accessScope.canUseReminderAssistant(comptable)).isTrue();
        }
    }

    @Nested
    @DisplayName("Bulletin de paie — scenario 8")
    class PayslipAccess {

        @Test
        @DisplayName("L'administrateur interroge n'importe quel employe")
        void adminMayTargetAnyone() {
            assertThat(accessScope.resolvePayslipEmployeId(admin(), 42L)).isEqualTo(42L);
        }

        @Test
        @DisplayName("Un employe est ramene a son propre identifiant")
        void employeeIsForcedToOwnId() {
            AuthPrincipal employee = principal(Set.of("EMPLOYE"), Set.of(), 10L);

            assertThat(accessScope.resolvePayslipEmployeId(employee, null)).isEqualTo(10L);
            assertThat(accessScope.resolvePayslipEmployeId(employee, 10L)).isEqualTo(10L);
        }

        @Test
        @DisplayName("Viser le bulletin d'un collegue est refuse, quelle que soit la formulation")
        void employeeCannotTargetColleague() {
            AuthPrincipal employee = principal(Set.of("EMPLOYE"), Set.of(), 10L);

            assertThatThrownBy(() -> accessScope.resolvePayslipEmployeId(employee, 11L))
                    .isInstanceOf(AiForbiddenException.class)
                    .hasMessageContaining("votre propre bulletin");
        }

        @Test
        @DisplayName("Un compte sans employe rattache est refuse")
        void accountWithoutEmployeeIsDenied() {
            AuthPrincipal orphan = principal(Set.of("EMPLOYE"), Set.of(), null);

            assertThatThrownBy(() -> accessScope.resolvePayslipEmployeId(orphan, null))
                    .isInstanceOf(AiForbiddenException.class);
        }

        @Test
        @DisplayName("L'administrateur doit tout de meme designer un employe")
        void adminMustProvideTarget() {
            assertThatThrownBy(() -> accessScope.resolvePayslipEmployeId(admin(), null))
                    .isInstanceOf(AiForbiddenException.class);
        }
    }
}
