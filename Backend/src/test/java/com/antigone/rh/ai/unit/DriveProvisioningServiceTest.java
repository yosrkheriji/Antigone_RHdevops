package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.service.DriveProvisioningService;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.service.GoogleDriveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scenario 12 : resilience Google Drive.
 *
 * <p>Une indisponibilite de Drive ne doit jamais faire perdre une generation deja
 * payee au LLM. Les lignes sont persistees avec {@code lienDrive = "PENDING"}, et la
 * reprise ne rejoue que l'etape Drive.
 */
@ExtendWith(MockitoExtension.class)
class DriveProvisioningServiceTest {

    @Mock
    private GoogleDriveService googleDriveService;

    @Mock
    private MediaPlanRepository mediaPlanRepository;

    @InjectMocks
    private DriveProvisioningService service;

    private MediaPlan plan(long id, LocalDate date, String lienDrive) {
        return MediaPlan.builder()
                .id(id)
                .datePublication(date)
                .titre("Publication " + id)
                .lienDrive(lienDrive)
                .client(Client.builder().id(1L).nom("Marque Alpha").build())
                .build();
    }

    @Test
    @DisplayName("Un lien est retourne quand Drive repond")
    void returnsLinkWhenDriveAnswers() throws IOException {
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenReturn("https://drive.google.com/drive/folders/abc");

        Optional<String> link = service.resolveMonthFolderLink("Marque Alpha", YearMonth.of(2026, 7));

        assertThat(link).contains("https://drive.google.com/drive/folders/abc");
        verify(googleDriveService, times(1)).getOrCreateClientMonthFolder(anyString(), any());
    }

    @Test
    @DisplayName("Un echec transitoire est retente puis reussit")
    void retriesOnceThenSucceeds() throws IOException {
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenThrow(new IOException("503 Drive indisponible"))
                .thenReturn("https://drive.google.com/drive/folders/ok");

        Optional<String> link = service.resolveMonthFolderLink("Marque Alpha", YearMonth.of(2026, 7));

        assertThat(link).contains("https://drive.google.com/drive/folders/ok");
        verify(googleDriveService, times(2)).getOrCreateClientMonthFolder(anyString(), any());
    }

    @Test
    @DisplayName("Un echec persistant rend un resultat vide, sans exception")
    void persistentFailureYieldsEmptyWithoutThrowing() throws IOException {
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenThrow(new IOException("quota depasse"));

        Optional<String> link = service.resolveMonthFolderLink("Marque Alpha", YearMonth.of(2026, 7));

        assertThat(link).isEmpty();
        verify(googleDriveService, times(2)).getOrCreateClientMonthFolder(anyString(), any());
    }

    @Test
    @DisplayName("Un nom de marque absent n'appelle meme pas Drive")
    void blankClientNameSkipsDrive() throws IOException {
        assertThat(service.resolveMonthFolderLink(null, YearMonth.of(2026, 7))).isEmpty();
        assertThat(service.resolveMonthFolderLink("  ", YearMonth.of(2026, 7))).isEmpty();

        verify(googleDriveService, never()).getOrCreateClientMonthFolder(anyString(), any());
    }

    @Test
    @DisplayName("PENDING, vide et null comptent tous comme non approvisionnes")
    void pendingDetectionCoversAllEmptyForms() {
        assertThat(DriveProvisioningService.isPending("PENDING")).isTrue();
        assertThat(DriveProvisioningService.isPending(null)).isTrue();
        assertThat(DriveProvisioningService.isPending("   ")).isTrue();
        assertThat(DriveProvisioningService.isPending("https://drive.google.com/x")).isFalse();
    }

    @Test
    @DisplayName("La reprise ne met a jour que les lignes du mois restees en attente")
    void retryOnlyTouchesPendingRowsOfTheMonth() throws IOException {
        MediaPlan pendingJuly = plan(1L, LocalDate.of(2026, 7, 5), "PENDING");
        MediaPlan alreadyLinkedJuly = plan(2L, LocalDate.of(2026, 7, 12), "https://drive/existing");
        MediaPlan pendingAugust = plan(3L, LocalDate.of(2026, 8, 3), "PENDING");

        when(mediaPlanRepository.findByClientId(1L))
                .thenReturn(List.of(pendingJuly, alreadyLinkedJuly, pendingAugust));
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenReturn("https://drive.google.com/drive/folders/july");

        int updated = service.retryPending(1L, "2026-07");

        assertThat(updated).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MediaPlan>> captor = ArgumentCaptor.forClass(List.class);
        verify(mediaPlanRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).extracting(MediaPlan::getId).containsExactly(1L);
        assertThat(pendingJuly.getLienDrive()).isEqualTo("https://drive.google.com/drive/folders/july");
        assertThat(alreadyLinkedJuly.getLienDrive()).isEqualTo("https://drive/existing");
        assertThat(pendingAugust.getLienDrive()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Sans ligne en attente, la reprise n'appelle pas Drive")
    void retryWithNothingPendingSkipsDrive() throws IOException {
        when(mediaPlanRepository.findByClientId(1L))
                .thenReturn(List.of(plan(1L, LocalDate.of(2026, 7, 5), "https://drive/ok")));

        assertThat(service.retryPending(1L, "2026-07")).isZero();

        verify(googleDriveService, never()).getOrCreateClientMonthFolder(anyString(), any());
        verify(mediaPlanRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Une reprise infructueuse laisse les lignes en attente, sans rien ecrire")
    void failedRetryLeavesRowsPending() throws IOException {
        MediaPlan pending = plan(1L, LocalDate.of(2026, 7, 5), "PENDING");
        when(mediaPlanRepository.findByClientId(1L)).thenReturn(List.of(pending));
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenThrow(new IOException("toujours indisponible"));

        assertThat(service.retryPending(1L, "2026-07")).isZero();

        assertThat(pending.getLienDrive()).isEqualTo("PENDING");
        verify(mediaPlanRepository, never()).saveAll(any());
    }
}
