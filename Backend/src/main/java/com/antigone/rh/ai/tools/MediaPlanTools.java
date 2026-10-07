package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.dto.MediaPlanGenerationResponse;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.MediaPlanGenerationService;
import com.antigone.rh.ai.util.MonthParser;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.entity.Projet;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.repository.ProjetRepository;
import com.antigone.rh.security.AuthPrincipal;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Outils media plan exposes au LLM : identite de marque, projets en cours et
 * recherche hybride dans l'historique editorial.
 *
 * <p>Chaque outil revalide le perimetre via {@link AiAccessScope} avant toute
 * lecture. L'identifiant de marque provient du modele, donc potentiellement d'un
 * prompt manipule : il n'est jamais considere comme fiable.
 */
@Slf4j
@lombok.AllArgsConstructor
public class MediaPlanTools {

    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final MediaPlanRepository mediaPlanRepository;
    private final HybridRetriever hybridRetriever;
    private final MediaPlanGenerationService generationService;
    private final AiAccessScope accessScope;
    private final ToolAuditService audit;
    /** Appelant de cette instance d'outil. Une instance = un utilisateur. */
    private final AiCallContext context;

    // ---- BrandInfoTool -----------------------------------------------------

    @Tool(name = "BrandInfoTool", value = """
            Recupere l'identite, l'activite, le positionnement et les objectifs d'une marque \
            (client) a partir de son identifiant. A appeler en premier avant toute generation \
            de media plan. Retourne aussi la liste des marques accessibles si l'identifiant \
            est inconnu ou hors perimetre.""")
    public String brandInfo(@P("Identifiant numerique de la marque (client)") Long clientId) {
        return audit.execute(context, "BrandInfoTool", "clientId=" + clientId, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireMediaPlanAssistant(principal);
            accessScope.requireClientAllowed(principal, clientId);

            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                return "Aucune marque trouvee pour l'identifiant " + clientId + ".";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Marque : ").append(client.getNom()).append(" (id ").append(client.getId()).append(")\n");
            appendField(sb, "Identite", client.getIdentite());
            appendField(sb, "Activite", client.getActivite());
            appendField(sb, "Positionnement", client.getPositionnement());
            appendField(sb, "Objectifs", client.getObjectifs());
            appendField(sb, "Notes", client.getNotes());
            appendField(sb, "Description", client.getDescription());
            if (isBrandProfileEmpty(client)) {
                sb.append("\nAttention : le profil de marque n'est pas renseigne. Fonde-toi sur les "
                        + "projets en cours et l'historique, et signale ce manque dans ta reponse.\n");
            }
            return sb.toString();
        });
    }

    /** Liste les marques accessibles, pour resoudre un nom en identifiant. */
    @Tool(name = "ListBrandsTool", value = """
            Liste les marques (clients) accessibles a l'utilisateur connecte, avec leur \
            identifiant. A utiliser quand l'utilisateur designe une marque par son nom.""")
    public String listBrands() {
        return audit.execute(context, "ListBrandsTool", "", () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireMediaPlanAssistant(principal);
            AiAccessScope.ClientScope scope = accessScope.mediaPlanClientScope(principal);

            List<Client> clients = scope.unrestricted()
                    ? clientRepository.findAll()
                    : clientRepository.findAllById(scope.clientIds());
            if (clients.isEmpty()) {
                return "Aucune marque n'est accessible avec votre perimetre actuel.";
            }
            StringBuilder sb = new StringBuilder("Marques accessibles :\n");
            clients.stream()
                    .sorted(Comparator.comparing(Client::getNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .forEach(client -> sb.append("- ").append(client.getNom())
                            .append(" (id ").append(client.getId()).append(")\n"));
            return sb.toString();
        });
    }

    // ---- ProjectInfoTool ---------------------------------------------------

    @Tool(name = "ProjectInfoTool", value = """
            Liste les projets et actions en cours d'une marque, avec leur statut et leurs \
            dates. Sert a ancrer le media plan sur ce qui se passe reellement sur la periode.""")
    public String projectInfo(@P("Identifiant numerique de la marque (client)") Long clientId) {
        return audit.execute(context, "ProjectInfoTool", "clientId=" + clientId, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireMediaPlanAssistant(principal);
            accessScope.requireClientAllowed(principal, clientId);

            List<Projet> projets = projetRepository.findByClientId(clientId);
            if (projets.isEmpty()) {
                return "Aucun projet enregistre pour cette marque.";
            }
            StringBuilder sb = new StringBuilder("Projets de la marque :\n");
            for (Projet projet : projets) {
                sb.append("- ").append(projet.getNom());
                if (projet.getStatut() != null) {
                    sb.append(" [").append(projet.getStatut().name()).append(']');
                }
                if (projet.getDateDebut() != null) {
                    sb.append(" du ").append(projet.getDateDebut());
                }
                if (projet.getDateFin() != null) {
                    sb.append(" au ").append(projet.getDateFin());
                }
                if (projet.getDescription() != null && !projet.getDescription().isBlank()) {
                    sb.append(" : ").append(projet.getDescription().trim());
                }
                sb.append('\n');
            }
            return sb.toString();
        });
    }

    // ---- PreviousMediaPlansTool --------------------------------------------

    @Tool(name = "PreviousMediaPlansTool", value = """
            Recherche dans les media plans passes d'une marque, par recherche hybride \
            (semantique + mots-cles). Utilise-le pour assurer la continuite editoriale et \
            surtout pour eviter de repeter des thematiques, formats ou angles deja publies.""")
    public String previousMediaPlans(
            @P("Identifiant numerique de la marque (client)") Long clientId,
            @P("Ce que tu cherches : thematique, format, angle editorial, periode") String recherche) {
        return audit.execute(context, "PreviousMediaPlansTool",
                "clientId=" + clientId + " recherche=" + recherche, () -> {
                    AuthPrincipal principal = context.principal();
                    accessScope.requireMediaPlanAssistant(principal);
                    accessScope.requireClientAllowed(principal, clientId);

                    List<RagHit> hits = hybridRetriever.search(new RagQuery(
                            recherche,
                            AiAccessScope.ClientScope.of(List.of(clientId)),
                            Set.of(AiSourceType.MEDIA_PLAN, AiSourceType.CONTENT),
                            10));
                    if (hits.isEmpty()) {
                        return "Aucun media plan anterieur ne correspond. La marque n'a probablement "
                                + "pas encore d'historique : appuie-toi sur son identite et ses projets.";
                    }
                    StringBuilder sb = new StringBuilder("Publications anterieures pertinentes :\n\n");
                    for (RagHit hit : hits) {
                        sb.append("--- (source ").append(hit.provenance()).append(")\n")
                                .append(hit.chunk().getContent()).append("\n\n");
                    }
                    return sb.toString();
                });
    }

    /** Historique brut d'un mois, sans passer par la recherche semantique. */
    @Tool(name = "MediaPlanHistoryTool", value = """
            Retourne toutes les publications d'une marque sur les N derniers mois precedant \
            un mois donne. Vue exhaustive et chronologique, complementaire de la recherche \
            PreviousMediaPlansTool.""")
    public String mediaPlanHistory(
            @P("Identifiant numerique de la marque (client)") Long clientId,
            @P("Mois de reference au format YYYY-MM") String mois,
            @P("Nombre de mois d'historique a remonter (1 a 12)") Integer nombreDeMois) {
        return audit.execute(context, "MediaPlanHistoryTool",
                "clientId=" + clientId + " mois=" + mois + " n=" + nombreDeMois, () -> {
                    AuthPrincipal principal = context.principal();
                    accessScope.requireMediaPlanAssistant(principal);
                    accessScope.requireClientAllowed(principal, clientId);

                    YearMonth reference = MonthParser.parseOrCurrent(mois);
                    int months = nombreDeMois == null ? 3 : Math.max(1, Math.min(12, nombreDeMois));
                    LocalDate from = reference.minusMonths(months).atDay(1);
                    LocalDate to = reference.atDay(1);

                    List<MediaPlan> plans = mediaPlanRepository.findByClientId(clientId).stream()
                            .filter(plan -> plan.getDatePublication() != null)
                            .filter(plan -> !plan.getDatePublication().isBefore(from)
                                    && plan.getDatePublication().isBefore(to))
                            .sorted(Comparator.comparing(MediaPlan::getDatePublication))
                            .toList();

                    if (plans.isEmpty()) {
                        return "Aucune publication sur les " + months + " mois precedant " + mois
                                + ". Premiere generation pour cette marque.";
                    }
                    StringBuilder sb = new StringBuilder("Historique des " + months + " mois precedents ("
                            + plans.size() + " publications) :\n");
                    for (MediaPlan plan : plans) {
                        sb.append("- ").append(plan.getDatePublication())
                                .append(" | ").append(nullSafe(plan.getPlatforme()))
                                .append(" | ").append(nullSafe(plan.getFormat()))
                                .append(" | ").append(nullSafe(plan.getType()))
                                .append(" | ").append(nullSafe(plan.getTitre()))
                                .append('\n');
                    }
                    return sb.toString();
                });
    }

    // ---- GenerateMediaPlanDraftTool ----------------------------------------

    @Tool(name = "GenerateMediaPlanDraftTool", value = """
            Construit une proposition de media plan structuree pour une marque et un mois, a \
            partir de son identite, ses projets et son historique. Ne persiste rien en base et \
            ne provisionne aucun dossier Drive : c'est un apercu, affiche a l'utilisateur sous \
            forme de cartes, qu'il devra revoir puis valider depuis la page Media Plan. A \
            appeler apres BrandInfoTool, ProjectInfoTool, MediaPlanHistoryTool et \
            PreviousMediaPlansTool, jamais avant : c'est ce contexte qui ancre la proposition sur \
            des donnees reelles. Ne redige jamais toi-meme la liste des publications en reponse : \
            cet outil s'en charge.""")
    public String generateDraft(
            @P("Identifiant numerique de la marque (client)") Long clientId,
            @P("Mois cible au format YYYY-MM") String mois) {
        return audit.execute(context, "GenerateMediaPlanDraftTool",
                "clientId=" + clientId + " mois=" + mois, () -> {
                    AuthPrincipal principal = context.principal();
                    accessScope.requireMediaPlanAssistant(principal);
                    accessScope.requireClientAllowed(principal, clientId);

                    MediaPlanGenerationService.Draft draft = generationService.generateDraft(
                            principal, clientId, mois, MediaPlanGenerationService.ProgressListener.NOOP);

                    // Depose pour ChatOrchestratorService : fait apparaitre la grille de cartes
                    // et le bouton « Partager » sous la reponse du modele.
                    context.structuredResult().set(MediaPlanGenerationResponse.fromDraft(draft));

                    int count = draft.generated().getPublications() == null
                            ? 0 : draft.generated().getPublications().size();
                    return count + " publications proposees pour " + draft.client().getNom() + " en "
                            + draft.month() + ", deja affichees a l'utilisateur sous forme de cartes. "
                            + "Resume en deux ou trois phrases le parti pris editorial et ce qui a ete "
                            + "evite, sans reciter le detail de chaque publication ni pretendre qu'elle "
                            + "est enregistree : elle ne l'est pas tant que l'utilisateur ne l'a pas "
                            + "validee depuis la page Media Plan.";
                });
    }

    // ---- Helpers -----------------------------------------------------------

    private boolean isBrandProfileEmpty(Client client) {
        return isBlank(client.getIdentite()) && isBlank(client.getActivite())
                && isBlank(client.getPositionnement()) && isBlank(client.getObjectifs());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(" : ").append(value.trim()).append('\n');
        }
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
