package com.antigone.rh.ai.service;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.GeneratedMediaPlan;
import com.antigone.rh.ai.dto.GeneratedMediaPlanItem;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.exception.AiUnavailableException;
import com.antigone.rh.ai.rag.EmbeddingIndexService;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.util.MonthParser;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.entity.Projet;
import com.antigone.rh.entity.Referentiel;
import com.antigone.rh.enums.EtatPublication;
import com.antigone.rh.enums.StatutMediaPlan;
import com.antigone.rh.enums.TypeReferentiel;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.repository.ProjetRepository;
import com.antigone.rh.repository.ReferentielRepository;
import com.antigone.rh.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pipeline de generation d'un media plan mensuel.
 *
 * <p>Deliberement deterministe et non agentique : le cahier des charges impose un
 * ordre de retrieval precis, et une boucle d'outils autonome ne le garantirait pas.
 * Le LLM n'intervient qu'a l'etape 5, sur un contexte deja constitue et deja filtre
 * par le controle d'acces — il ne peut donc pas elargir son propre perimetre.
 *
 * <p>Etapes : marque, projets, media plans passes, contenus realises, generation
 * structuree, persistance et approvisionnement Drive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaPlanGenerationService {

    /** Etapes rapportees au frontend via les evenements SSE de progression. */
    public enum Step {
        BRAND("BrandInfoTool"),
        PROJECTS("ProjectInfoTool"),
        HISTORY("PreviousMediaPlansTool"),
        CONTENT("RealizedContentTool"),
        GENERATION("MediaPlanGenerator"),
        DRIVE("GoogleDriveTool"),
        PERSIST("MediaPlanPersistence");

        private final String toolName;

        Step(String toolName) {
            this.toolName = toolName;
        }

        public String toolName() {
            return toolName;
        }
    }

    /** Rapporte la progression au flux SSE ; sans effet en usage synchrone. */
    public interface ProgressListener {
        void start(Step step, Object arguments);

        void end(Step step, boolean success, String detail);

        ProgressListener NOOP = new ProgressListener() {
            @Override
            public void start(Step step, Object arguments) {
            }

            @Override
            public void end(Step step, boolean success, String detail) {
            }
        };
    }

    /** Resultat complet : les lignes persistees plus la prose editoriale. */
    public record Result(Long clientId,
                         String clientNom,
                         String mois,
                         String syntheseEditoriale,
                         List<String> thematiquesEvitees,
                         List<MediaPlan> publications,
                         boolean drivePending) {
    }

    /**
     * Sortie des etapes 1 a 5 seules : contexte construit et modele appele, mais
     * rien approvisionne ni persiste. Partagee par {@link #generate} (qui enchaine
     * Drive et persistance) et par l'outil de conversation, qui presente cette
     * meme proposition a l'utilisateur sans rien engager en base.
     */
    public record Draft(Long clientId, Client client, YearMonth month, GeneratedMediaPlan generated) {
    }

    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final MediaPlanRepository mediaPlanRepository;
    private final ReferentielRepository referentielRepository;
    private final EmployeRepository employeRepository;
    private final HybridRetriever hybridRetriever;
    private final DriveProvisioningService driveProvisioning;
    private final EmbeddingIndexService indexService;
    private final AiAccessScope accessScope;
    private final ObjectProvider<StructuredAgents.MediaPlanStructuredAgent> agentProvider;

    /** Nombre de mois d'historique injectes pour garantir la non-repetition. */
    private static final int HISTORY_MONTHS = 3;

    public Draft generateDraft(AuthPrincipal principal, Long clientId, String mois, ProgressListener listener) {
        accessScope.requireMediaPlanAssistant(principal);
        accessScope.requireClientAllowed(principal, clientId);

        StructuredAgents.MediaPlanStructuredAgent agent = agentProvider.getIfAvailable();
        if (agent == null) {
            throw new AiUnavailableException(
                    "L'assistant IA n'est pas configure (cle API manquante).");
        }

        YearMonth month = MonthParser.parse(mois);
        StringBuilder context = new StringBuilder();

        // 1 - Identite de marque
        listener.start(Step.BRAND, java.util.Map.of("clientId", clientId));
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        context.append(renderBrand(client)).append('\n');
        listener.end(Step.BRAND, true, client.getNom());

        // 2 - Projets et actions sur la periode
        listener.start(Step.PROJECTS, java.util.Map.of("clientId", clientId));
        List<Projet> projets = projetRepository.findByClientId(clientId);
        context.append(renderProjects(projets, month)).append('\n');
        listener.end(Step.PROJECTS, true, projets.size() + " projets");

        // 3 - Media plans anterieurs, via la recherche hybride
        listener.start(Step.HISTORY, java.util.Map.of("clientId", clientId, "mois", month.toString()));
        List<MediaPlan> history = previousMonths(clientId, month);
        context.append(renderHistory(history, month)).append('\n');
        context.append(renderSemanticHistory(client, clientId)).append('\n');
        listener.end(Step.HISTORY, true, history.size() + " publications anterieures");

        // 4 - Contenus effectivement publies
        listener.start(Step.CONTENT, java.util.Map.of("clientId", clientId));
        context.append(renderPublishedContent(history)).append('\n');
        listener.end(Step.CONTENT, true, null);

        // Referentiels : le modele doit choisir dans les valeurs reelles de l'app,
        // sinon les lignes generees seraient inexploitables par l'interface existante.
        context.append(renderReferentiels()).append('\n');
        context.append(renderInstructions(client, month, history.isEmpty()));

        // 5 - Generation structuree
        listener.start(Step.GENERATION, java.util.Map.of("mois", month.toString()));
        GeneratedMediaPlan generated;
        try {
            generated = agent.generate(context.toString());
        } catch (Exception e) {
            listener.end(Step.GENERATION, false, e.getMessage());
            throw e;
        }
        int count = generated.getPublications() == null ? 0 : generated.getPublications().size();
        listener.end(Step.GENERATION, true, count + " publications generees");

        // Dates normalisees une seule fois ici : persist() et l'apercu presente en
        // conversation peuvent ensuite s'y fier sans reimplementer le recalage.
        if (generated.getPublications() != null) {
            for (GeneratedMediaPlanItem item : generated.getPublications()) {
                item.setDate(resolveDate(item.getDate(), month).toString());
            }
        }

        return new Draft(clientId, client, month, generated);
    }

    @Transactional
    public Result generate(AuthPrincipal principal, Long clientId, String mois, ProgressListener listener) {
        Draft draft = generateDraft(principal, clientId, mois, listener);

        // 6 - Approvisionnement Drive, puis persistance
        listener.start(Step.DRIVE, java.util.Map.of("clientNom", draft.client().getNom(), "mois", draft.month().toString()));
        Optional<String> driveLink = driveProvisioning.resolveMonthFolderLink(draft.client().getNom(), draft.month());
        listener.end(Step.DRIVE, driveLink.isPresent(),
                driveLink.orElse("Drive indisponible : liens marques PENDING"));

        int count = draft.generated().getPublications() == null ? 0 : draft.generated().getPublications().size();
        listener.start(Step.PERSIST, java.util.Map.of("count", count));
        List<MediaPlan> saved = persist(draft.generated(), draft.client(), draft.month(), driveLink.orElse(null), principal);
        listener.end(Step.PERSIST, true, saved.size() + " lignes enregistrees");

        // L'index est mis a jour dans la foulee : la generation du mois suivant doit
        // pouvoir eviter ce qui vient d'etre cree.
        saved.forEach(this::reindexQuietly);

        return new Result(
                clientId,
                draft.client().getNom(),
                draft.month().toString(),
                draft.generated().getSyntheseEditoriale(),
                draft.generated().getThematiquesEvitees() == null ? List.of() : draft.generated().getThematiquesEvitees(),
                saved,
                driveLink.isEmpty());
    }

    // ---- Persistance -------------------------------------------------------

    private List<MediaPlan> persist(GeneratedMediaPlan generated, Client client, YearMonth month,
                                    String driveLink, AuthPrincipal principal) {
        List<GeneratedMediaPlanItem> items = generated.getPublications();
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Employe createur = principal.getEmployeId() == null
                ? null
                : employeRepository.findById(principal.getEmployeId()).orElse(null);
        if (createur == null) {
            throw new ResourceNotFoundException("Employe", principal.getEmployeId());
        }

        List<MediaPlan> plans = new ArrayList<>();
        for (GeneratedMediaPlanItem item : items) {
            plans.add(MediaPlan.builder()
                    .client(client)
                    .createur(createur)
                    .datePublication(resolveDate(item.getDate(), month))
                    .heure(blankToNull(item.getHeure()))
                    .titre(item.getTitre() == null || item.getTitre().isBlank()
                            ? "Publication " + month
                            : item.getTitre())
                    .texteSurVisuel(blankToNull(item.getTexteSurVisuel()))
                    .inspiration(blankToNull(item.getInspiration()))
                    .autresElements(blankToNull(item.getAutresElements()))
                    .platforme(blankToNull(item.getPlatforme()))
                    .format(blankToNull(item.getFormat()))
                    .type(blankToNull(item.getType()))
                    .remarques(blankToNull(item.getJustification()))
                    // Drive indisponible : la ligne est quand meme creee, avec un
                    // marqueur que la reprise ciblee saura retrouver.
                    .lienDrive(driveLink == null ? DriveProvisioningService.LIEN_DRIVE_PENDING : driveLink)
                    .etatPublication(EtatPublication.PAS_ENCORE)
                    .statut(StatutMediaPlan.EN_ATTENTE)
                    .build());
        }
        return mediaPlanRepository.saveAll(plans);
    }

    /**
     * Une date hors du mois demande est ramenee dans le mois plutot que rejetee :
     * mieux vaut une ligne recalee qu'une generation entiere perdue pour un jour
     * d'ecart.
     */
    private LocalDate resolveDate(String raw, YearMonth month) {
        if (raw != null && !raw.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(raw.trim());
                if (YearMonth.from(date).equals(month)) {
                    return date;
                }
                log.debug("Date {} hors du mois {} : recalage sur le mois demande", date, month);
                int day = Math.min(date.getDayOfMonth(), month.lengthOfMonth());
                return month.atDay(day);
            } catch (DateTimeParseException e) {
                log.debug("Date generee illisible ({}) : repli sur le 1er du mois", raw);
            }
        }
        return month.atDay(1);
    }

    private void reindexQuietly(MediaPlan plan) {
        try {
            indexService.indexMediaPlan(plan);
        } catch (Exception e) {
            log.debug("Indexation differee de la publication {} : {}", plan.getId(), e.getMessage());
        }
    }

    // ---- Construction du contexte ------------------------------------------

    private String renderBrand(Client client) {
        StringBuilder sb = new StringBuilder("=== IDENTITE DE LA MARQUE ===\n");
        sb.append("Nom : ").append(client.getNom()).append('\n');
        appendIfPresent(sb, "Identite", client.getIdentite());
        appendIfPresent(sb, "Activite", client.getActivite());
        appendIfPresent(sb, "Positionnement", client.getPositionnement());
        appendIfPresent(sb, "Objectifs", client.getObjectifs());
        appendIfPresent(sb, "Notes", client.getNotes());
        appendIfPresent(sb, "Description", client.getDescription());
        return sb.toString();
    }

    private String renderProjects(List<Projet> projets, YearMonth month) {
        StringBuilder sb = new StringBuilder("=== PROJETS ET ACTIONS ===\n");
        if (projets.isEmpty()) {
            return sb.append("Aucun projet enregistre pour cette marque.\n").toString();
        }
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        for (Projet projet : projets) {
            boolean overlapsMonth = (projet.getDateDebut() == null || !projet.getDateDebut().isAfter(monthEnd))
                    && (projet.getDateFin() == null || !projet.getDateFin().isBefore(monthStart));
            sb.append("- ").append(projet.getNom());
            if (projet.getStatut() != null) {
                sb.append(" [").append(projet.getStatut().name()).append(']');
            }
            if (overlapsMonth) {
                sb.append(" (actif sur le mois demande)");
            }
            if (projet.getDescription() != null && !projet.getDescription().isBlank()) {
                sb.append(" : ").append(projet.getDescription().trim());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private List<MediaPlan> previousMonths(Long clientId, YearMonth month) {
        LocalDate from = month.minusMonths(HISTORY_MONTHS).atDay(1);
        LocalDate to = month.atDay(1);
        return mediaPlanRepository.findByClientId(clientId).stream()
                .filter(plan -> plan.getDatePublication() != null)
                .filter(plan -> !plan.getDatePublication().isBefore(from)
                        && plan.getDatePublication().isBefore(to))
                .sorted(Comparator.comparing(MediaPlan::getDatePublication))
                .toList();
    }

    private String renderHistory(List<MediaPlan> history, YearMonth month) {
        StringBuilder sb = new StringBuilder("=== HISTORIQUE DES " + HISTORY_MONTHS
                + " MOIS PRECEDANT " + month + " ===\n");
        if (history.isEmpty()) {
            return sb.append("Aucun historique : c'est la premiere generation pour cette marque. "
                    + "Appuie-toi uniquement sur l'identite, les objectifs et les projets.\n").toString();
        }
        for (MediaPlan plan : history) {
            sb.append("- ").append(plan.getDatePublication())
                    .append(" | ").append(nullSafe(plan.getPlatforme()))
                    .append(" | ").append(nullSafe(plan.getFormat()))
                    .append(" | ").append(nullSafe(plan.getType()))
                    .append(" | ").append(nullSafe(plan.getTitre()));
            if (plan.getTexteSurVisuel() != null && !plan.getTexteSurVisuel().isBlank()) {
                sb.append(" | texte : ").append(truncate(plan.getTexteSurVisuel(), 160));
            }
            sb.append('\n');
        }
        sb.append("\nCes thematiques, angles et accroches sont DEJA UTILISES : ne les reprends pas.\n");
        return sb.toString();
    }

    /**
     * Complement semantique a l'historique chronologique : la recherche hybride
     * remonte aussi des publications plus anciennes proches du positionnement, que
     * la fenetre de trois mois laisserait passer.
     */
    private String renderSemanticHistory(Client client, Long clientId) {
        String query = String.join(" ",
                nullSafe(client.getNom()),
                nullSafe(client.getPositionnement()),
                nullSafe(client.getObjectifs()));
        List<RagHit> hits = hybridRetriever.search(new RagQuery(
                query.isBlank() ? nullSafe(client.getNom()) : query,
                AiAccessScope.ClientScope.of(List.of(clientId)),
                Set.of(AiSourceType.MEDIA_PLAN, AiSourceType.CONTENT),
                6));
        if (hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("=== PUBLICATIONS PROCHES (recherche hybride) ===\n");
        for (RagHit hit : hits) {
            sb.append("[").append(hit.provenance()).append("] ")
                    .append(truncate(hit.chunk().getContent().replace('\n', ' '), 300)).append('\n');
        }
        return sb.toString();
    }

    private String renderPublishedContent(List<MediaPlan> history) {
        List<MediaPlan> published = history.stream()
                .filter(plan -> plan.getEtatPublication() == EtatPublication.PUBLIEE)
                .toList();
        StringBuilder sb = new StringBuilder("=== CONTENUS EFFECTIVEMENT PUBLIES ===\n");
        if (published.isEmpty()) {
            return sb.append("Aucune publication confirmee sur la periode.\n").toString();
        }
        sb.append("Formats deja exploites : ")
                .append(distinct(published, MediaPlan::getFormat)).append('\n');
        sb.append("Plateformes deja exploitees : ")
                .append(distinct(published, MediaPlan::getPlatforme)).append('\n');
        sb.append("Types deja exploites : ")
                .append(distinct(published, MediaPlan::getType)).append('\n');
        return sb.toString();
    }

    private String renderReferentiels() {
        StringBuilder sb = new StringBuilder("=== VALEURS AUTORISEES ===\n");
        sb.append("Plateformes : ").append(referentielValues(TypeReferentiel.PLATFORME_MEDIA_PLAN)).append('\n');
        sb.append("Formats : ").append(referentielValues(TypeReferentiel.FORMAT_MEDIA_PLAN)).append('\n');
        sb.append("Types : ").append(referentielValues(TypeReferentiel.TYPE_MEDIA_PLAN)).append('\n');
        return sb.toString();
    }

    private String renderInstructions(Client client, YearMonth month, boolean noHistory) {
        StringBuilder sb = new StringBuilder("=== DEMANDE ===\n");
        sb.append("Construis le media plan de la marque « ").append(client.getNom())
                .append(" » pour le mois ").append(month).append(".\n");
        sb.append("Le mois compte ").append(month.lengthOfMonth()).append(" jours : les dates doivent ")
                .append("toutes appartenir a ").append(month).append(".\n");
        if (noHistory) {
            sb.append("Cette marque n'a aucun historique. Pose une ligne editoriale fondatrice ")
                    .append("a partir de son identite et de ses objectifs, et signale-le dans ")
                    .append("syntheseEditoriale.\n");
        } else {
            sb.append("Evite explicitement les thematiques, angles et formats de l'historique ")
                    .append("fourni, et liste ce que tu as ecarte dans thematiquesEvitees.\n");
        }
        return sb.toString();
    }

    // ---- Helpers -----------------------------------------------------------

    private String referentielValues(TypeReferentiel type) {
        List<String> values = referentielRepository.findByTypeReferentielAndActifTrue(type).stream()
                .map(Referentiel::getLibelle)
                .filter(libelle -> libelle != null && !libelle.isBlank())
                .toList();
        return values.isEmpty() ? "(referentiel vide, choisis des valeurs usuelles)" : String.join(", ", values);
    }

    private String distinct(List<MediaPlan> plans, java.util.function.Function<MediaPlan, String> extractor) {
        List<String> values = plans.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return values.isEmpty() ? "-" : String.join(", ", values);
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(" : ").append(value.trim()).append('\n');
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
