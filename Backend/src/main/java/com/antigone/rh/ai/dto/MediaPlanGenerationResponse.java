package com.antigone.rh.ai.dto;

import com.antigone.rh.ai.service.MediaPlanGenerationService;
import com.antigone.rh.entity.MediaPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Media plan genere.
 *
 * <p>Pas de champ {@code mediaPlanId} : dans ce schema une ligne
 * {@code media_plans} est une publication, il n'existe pas d'entite « plan »
 * englobante. Un plan mensuel se designe donc par le couple
 * ({@code clientId}, {@code month}), et c'est ce couple que reprennent les
 * endpoints de reprise Drive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaPlanGenerationResponse {

    private Long clientId;
    private String clientNom;
    private String month;
    /** Statut commun des lignes creees : toujours EN_ATTENTE a la generation. */
    private String status;
    /** Vrai si Drive etait indisponible : les liens valent "PENDING". */
    private boolean drivePending;
    private String syntheseEditoriale;
    private List<String> thematiquesEvitees;
    private List<MediaPlanItemDTO> items;

    public static MediaPlanGenerationResponse from(MediaPlanGenerationService.Result result) {
        return MediaPlanGenerationResponse.builder()
                .clientId(result.clientId())
                .clientNom(result.clientNom())
                .month(result.mois())
                .status("EN_ATTENTE")
                .drivePending(result.drivePending())
                .syntheseEditoriale(result.syntheseEditoriale())
                .thematiquesEvitees(result.thematiquesEvitees())
                .items(result.publications().stream().map(MediaPlanGenerationResponse::toItem).toList())
                .build();
    }

    /**
     * Meme forme de reponse, construite depuis une proposition non persistee
     * ({@link MediaPlanGenerationService.Draft}) plutot que depuis des lignes
     * enregistrees : c'est ce que l'outil de conversation depose comme
     * {@code structured_result} pour afficher l'apercu sous forme de cartes, sans
     * rien ecrire en base ni provisionner Drive.
     *
     * <p>Les identifiants sont synthetiques (negatifs) : ils ne designent aucune
     * ligne reelle, seulement des cles d'affichage cote frontend.
     */
    public static MediaPlanGenerationResponse fromDraft(MediaPlanGenerationService.Draft draft) {
        List<GeneratedMediaPlanItem> items = draft.generated().getPublications() == null
                ? List.of() : draft.generated().getPublications();
        List<MediaPlanItemDTO> dtoItems = new ArrayList<>();
        long syntheticId = -1;
        for (GeneratedMediaPlanItem item : items) {
            dtoItems.add(MediaPlanItemDTO.builder()
                    .id(syntheticId--)
                    .datePublication(LocalDate.parse(item.getDate()))
                    .heure(item.getHeure())
                    .titre(item.getTitre())
                    .texteSurVisuel(item.getTexteSurVisuel())
                    .inspiration(item.getInspiration())
                    .autresElements(item.getAutresElements())
                    .platforme(item.getPlatforme())
                    .format(item.getFormat())
                    .type(item.getType())
                    .lienDrive(null)
                    .etatPublication(null)
                    .statut(null)
                    .remarques(item.getJustification())
                    .build());
        }
        return MediaPlanGenerationResponse.builder()
                .clientId(draft.clientId())
                .clientNom(draft.client().getNom())
                .month(draft.month().toString())
                .status("PROPOSITION")
                .drivePending(false)
                .syntheseEditoriale(draft.generated().getSyntheseEditoriale())
                .thematiquesEvitees(draft.generated().getThematiquesEvitees() == null
                        ? List.of() : draft.generated().getThematiquesEvitees())
                .items(dtoItems)
                .build();
    }

    private static MediaPlanItemDTO toItem(MediaPlan plan) {
        return MediaPlanItemDTO.builder()
                .id(plan.getId())
                .datePublication(plan.getDatePublication())
                .heure(plan.getHeure())
                .titre(plan.getTitre())
                .texteSurVisuel(plan.getTexteSurVisuel())
                .inspiration(plan.getInspiration())
                .autresElements(plan.getAutresElements())
                .platforme(plan.getPlatforme())
                .format(plan.getFormat())
                .type(plan.getType())
                .lienDrive(plan.getLienDrive())
                .etatPublication(plan.getEtatPublication() == null ? null : plan.getEtatPublication().name())
                .statut(plan.getStatut() == null ? null : plan.getStatut().name())
                .remarques(plan.getRemarques())
                .build();
    }
}
