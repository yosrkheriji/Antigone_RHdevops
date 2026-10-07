package com.antigone.rh.ai.rag;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.entity.AiDocumentChunk;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.repository.AiDocumentChunkRepository;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.entity.Projet;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.repository.ProjetRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alimente l'index du RAG a partir des tables metier reelles : {@code clients},
 * {@code projets}, {@code media_plans}. Aucune donnee n'est dupliquee comme source
 * de verite, l'index n'est qu'une projection reconstructible a tout moment.
 *
 * <p>L'indexation est incrementale : le contenu textuel de chaque source est hache,
 * et seuls les chunks dont le hash a change sont reecrits puis reembeddes. Une
 * reindexation complete coute donc un embedding par ligne modifiee, pas par ligne
 * existante.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingIndexService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    /** Le modele OpenAI accepte de gros lots ; 64 borne l'empreinte memoire. */
    private static final int EMBED_BATCH_SIZE = 64;

    /**
     * Extraction texte du PDF partage par l'agence, deja produite pour l'ancien
     * chatbot RH (Groq). Un chunk par article : c'est l'unite de citation naturelle
     * d'un reglement, et une recherche sur « conges maladie » doit remonter tout
     * l'article 7, pas une phrase isolee qui perdrait son contexte.
     */
    private static final String POLICY_RESOURCE = "reglement_interieur.txt";
    private static final Pattern POLICY_ARTICLE_HEADING =
            Pattern.compile("(?m)^\\s*Article\\s+(\\d+)\\s*[:\\-]\\s*(.+)$");
    /** En-tete/pied de page repetes a chaque page du PDF source : bruit pur pour le RAG. */
    private static final Set<String> POLICY_BOILERPLATE_LINES = Set.of(
            "ANTIGONE CONSULTING",
            "Contact@antigoneagency.com",
            "012 , RUE HABIB THAMEUR , RADES 2040",
            "MF: 1761176/Q/A/M/000");

    private final AiDocumentChunkRepository chunkRepository;
    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final MediaPlanRepository mediaPlanRepository;
    private final AiProperties properties;
    private final PgVectorSupport pgVectorSupport;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    /** N'avertit qu'une fois : le desaccord vaudrait pour chaque chunk. */
    private final java.util.concurrent.atomic.AtomicBoolean dimensionMismatchLogged =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // ---- Declencheurs ------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    @Async(com.antigone.rh.ai.config.AiAsyncConfig.AI_EXECUTOR)
    public void indexOnStartup() {
        if (!properties.getRag().isReindexOnStartup()) {
            return;
        }
        reindexAll();
    }

    @Scheduled(cron = "${app.ai.rag.reindex-cron:0 0 3 * * *}")
    @Async(com.antigone.rh.ai.config.AiAsyncConfig.AI_EXECUTOR)
    public void scheduledReindex() {
        reindexAll();
    }

    /**
     * Reindexation complete (incrementale sur le contenu).
     *
     * <p>Chaque source est isolee dans son propre passage : un incident sur l'une
     * (ex. une donnee metier inattendue dans les projets) ne doit pas empecher les
     * autres de s'indexer. Un seul {@code try/catch} englobant les cinq etapes a
     * deja provoque exactement ca en pratique — un echec precoce rendait le
     * reglement interieur introuvable sans qu'aucune erreur ne le signale
     * explicitement dans les logs.
     */
    public IndexReport reindexAll() {
        long start = System.currentTimeMillis();
        IndexReport report = new IndexReport();
        mergeSafely(report, "marques", this::indexBrands);
        mergeSafely(report, "projets", this::indexProjects);
        mergeSafely(report, "media plans", this::indexMediaPlans);
        mergeSafely(report, "reglement interieur", this::indexPolicy);
        mergeSafely(report, "embeddings", this::embedPending);
        report.durationMs = System.currentTimeMillis() - start;
        log.info("Reindexation RAG : {} chunks ecrits, {} inchanges, {} embeddings, {} erreurs en {} ms",
                report.written, report.unchanged, report.embedded, report.errors, report.durationMs);
        return report;
    }

    private void mergeSafely(IndexReport report, String label, java.util.function.Supplier<IndexReport> step) {
        try {
            report.merge(step.get());
        } catch (Exception e) {
            log.error("Reindexation de {} interrompue : {}", label, e.getMessage(), e);
            report.errors++;
            report.errorMessages.add(label + " : " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    // ---- Indexation par source --------------------------------------------

    @Transactional
    public IndexReport indexBrands() {
        IndexReport report = new IndexReport();
        for (Client client : clientRepository.findAll()) {
            String content = renderBrand(client);
            upsert(report, AiSourceType.BRAND, client.getId(), client.getId(), null, content,
                    "{\"nom\":\"" + escape(client.getNom()) + "\"}");
        }
        return report;
    }

    @Transactional
    public IndexReport indexProjects() {
        IndexReport report = new IndexReport();
        for (Projet projet : projetRepository.findAll()) {
            Long clientId = projet.getClient() != null ? projet.getClient().getId() : null;
            String content = renderProject(projet);
            String month = projet.getDateDebut() != null ? projet.getDateDebut().format(MONTH) : null;
            upsert(report, AiSourceType.PROJECT, projet.getId(), clientId, month, content,
                    "{\"nom\":\"" + escape(projet.getNom()) + "\"}");
        }
        return report;
    }

    @Transactional
    public IndexReport indexMediaPlans() {
        IndexReport report = new IndexReport();
        for (MediaPlan plan : mediaPlanRepository.findAll()) {
            Long clientId = plan.getClient() != null ? plan.getClient().getId() : null;
            String month = plan.getDatePublication() != null ? plan.getDatePublication().format(MONTH) : null;
            String content = renderMediaPlan(plan);
            upsert(report, AiSourceType.MEDIA_PLAN, plan.getId(), clientId, month, content,
                    "{\"titre\":\"" + escape(plan.getTitre()) + "\",\"mois\":\"" + (month == null ? "" : month)
                            + "\"}");
        }
        return report;
    }

    /**
     * Indexe le reglement interieur, un chunk par article.
     *
     * <p>{@code clientId} reste null : c'est un document transverse, lisible par
     * tout employe quelle que soit son affectation a une marque — {@link
     * com.antigone.rh.ai.tools.PolicyTools} l'interroge d'ailleurs avec un
     * perimetre non restreint, precisement pour cette raison.
     */
    @Transactional
    public IndexReport indexPolicy() {
        IndexReport report = new IndexReport();
        String raw = loadPolicyResource();
        if (raw == null) {
            return report;
        }
        for (PolicyArticle article : splitPolicyArticles(cleanPolicyText(raw))) {
            upsert(report, AiSourceType.POLICY, (long) article.number(), null, null, article.content(),
                    "{\"titre\":\"" + escape(article.title()) + "\"}");
        }
        return report;
    }

    private String loadPolicyResource() {
        try {
            ClassPathResource resource = new ClassPathResource(POLICY_RESOURCE);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Reglement interieur introuvable ({}) : l'assistant ne pourra pas y repondre.",
                    e.getMessage());
            return null;
        }
    }

    /**
     * Retire l'en-tete/pied de page repetes a chaque page du PDF source, et
     * normalise les doubles espaces que laisse l'extraction d'un texte justifie
     * (« de  maternite  de  2  mois ») : sans ca, une citation exacte comme
     * {@code contains("2 mois")} echoue pour une raison purement cosmetique.
     */
    private String cleanPolicyText(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean previousBlank = false;
        for (String line : raw.split("\n")) {
            String trimmed = line.strip().replaceAll(" {2,}", " ");
            if (POLICY_BOILERPLATE_LINES.contains(trimmed)) {
                continue;
            }
            boolean blank = trimmed.isEmpty();
            if (blank && previousBlank) {
                continue;
            }
            sb.append(trimmed).append('\n');
            previousBlank = blank;
        }
        return sb.toString();
    }

    private record PolicyArticle(int number, String title, String content) {
    }

    /**
     * Un chunk par article : le premier (« Dispositions generales ») regroupe tout
     * ce qui precede la premiere ligne « Article N », qui n'est jamais numerotee
     * dans le document source.
     */
    private List<PolicyArticle> splitPolicyArticles(String text) {
        List<Integer> starts = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        Matcher matcher = POLICY_ARTICLE_HEADING.matcher(text);
        while (matcher.find()) {
            starts.add(matcher.start());
            numbers.add(Integer.parseInt(matcher.group(1)));
            titles.add(matcher.group(2).trim());
        }
        if (starts.isEmpty()) {
            return List.of();
        }

        List<PolicyArticle> articles = new ArrayList<>();
        String preamble = text.substring(0, starts.get(0)).trim();
        if (!preamble.isBlank()) {
            articles.add(new PolicyArticle(1, "Dispositions generales", preamble));
        }
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
            String body = text.substring(starts.get(i), end).trim();
            if (!body.isBlank()) {
                articles.add(new PolicyArticle(numbers.get(i), titles.get(i), body));
            }
        }
        return articles;
    }

    /** Reindexe une seule ligne de media plan, apres creation ou modification. */
    @Transactional
    public void indexMediaPlan(MediaPlan plan) {
        IndexReport report = new IndexReport();
        Long clientId = plan.getClient() != null ? plan.getClient().getId() : null;
        String month = plan.getDatePublication() != null ? plan.getDatePublication().format(MONTH) : null;
        upsert(report, AiSourceType.MEDIA_PLAN, plan.getId(), clientId, month, renderMediaPlan(plan),
                "{\"titre\":\"" + escape(plan.getTitre()) + "\"}");
        if (report.written > 0) {
            embedPending();
        }
    }

    private void upsert(IndexReport report, AiSourceType sourceType, Long sourceId, Long clientId,
                        String periodMonth, String content, String metadata) {
        if (content == null || content.isBlank()) {
            return;
        }
        String hash = sha256(content);
        Optional<AiDocumentChunk> existing =
                chunkRepository.findFirstBySourceTypeAndSourceIdOrderByIdAsc(sourceType, sourceId);

        if (existing.isPresent()) {
            AiDocumentChunk chunk = existing.get();
            if (hash.equals(chunk.getContentHash())
                    && chunk.getEmbedding() != null
                    && java.util.Objects.equals(chunk.getClientId(), clientId)) {
                report.unchanged++;
                return;
            }
            chunk.setContent(content);
            chunk.setContentHash(hash);
            chunk.setClientId(clientId);
            chunk.setPeriodMonth(periodMonth);
            chunk.setMetadata(metadata);
            // Le contenu a change : l'embedding precedent ne le represente plus.
            chunk.setEmbedding(null);
            chunkRepository.save(chunk);
        } else {
            chunkRepository.save(AiDocumentChunk.builder()
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .clientId(clientId)
                    .periodMonth(periodMonth)
                    .content(content)
                    .contentHash(hash)
                    .metadata(metadata)
                    .build());
        }
        report.written++;
    }

    // ---- Embeddings --------------------------------------------------------

    /**
     * Calcule les embeddings manquants par lots. Un echec reseau laisse simplement
     * les chunks concernes sans embedding : ils restent trouvables par la branche
     * lexicale et seront repris au passage suivant.
     */
    @Transactional
    public IndexReport embedPending() {
        IndexReport report = new IndexReport();
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            return report;
        }
        List<AiDocumentChunk> pending = chunkRepository.findTop200ByEmbeddingIsNull();
        for (int from = 0; from < pending.size(); from += EMBED_BATCH_SIZE) {
            List<AiDocumentChunk> batch = pending.subList(from, Math.min(from + EMBED_BATCH_SIZE, pending.size()));
            try {
                List<TextSegment> segments = batch.stream()
                        .map(chunk -> TextSegment.from(chunk.getContent()))
                        .toList();
                List<Embedding> embeddings = model.embedAll(segments).content();
                for (int i = 0; i < batch.size() && i < embeddings.size(); i++) {
                    AiDocumentChunk chunk = batch.get(i);
                    String encoded = EmbeddingCodec.encode(embeddings.get(i).vector());
                    chunk.setEmbedding(encoded);
                    chunkRepository.save(chunk);
                    writeVectorColumn(chunk.getId(), encoded, embeddings.get(i).dimension());
                    report.embedded++;
                }
            } catch (Exception e) {
                log.warn("Lot d'embeddings en echec ({}) - reprise au prochain passage", e.getMessage());
                report.errors++;
            }
        }
        return report;
    }

    /**
     * Alimente la colonne pgvector, si et seulement si la dimension correspond.
     *
     * <p>Postgres refuse un vecteur de taille differente de celle declaree sur la
     * colonne, et cette erreur marque la transaction courante comme rollback-only :
     * tout le lot d'indexation serait perdu, y compris la colonne TEXT portable deja
     * ecrite. On verifie donc avant d'ecrire. En cas de desaccord, la recherche dense
     * bascule simplement sur le calcul en Java.
     */
    private void writeVectorColumn(Long chunkId, String encoded, int dimensions) {
        if (encoded == null || !pgVectorSupport.isAvailable()) {
            return;
        }
        int expected = pgVectorSupport.columnDimensions();
        if (expected > 0 && dimensions != expected) {
            if (dimensionMismatchLogged.compareAndSet(false, true)) {
                log.warn("Le modele d'embedding renvoie {} dimensions alors que la colonne pgvector en "
                                + "declare {} : verifiez app.ai.embedding.dimensions. La recherche dense "
                                + "utilisera le calcul en Java.",
                        dimensions, expected);
            }
            return;
        }
        chunkRepository.updateEmbeddingVector(chunkId, encoded);
    }

    // ---- Rendu textuel -----------------------------------------------------

    /**
     * Le rendu est volontairement etiquete ligne a ligne : la branche lexicale
     * indexe ce texte tel quel, donc les libelles ("Positionnement :") deviennent
     * des points d'ancrage exploitables par une recherche par mots-cles.
     */
    private String renderBrand(Client client) {
        StringBuilder sb = new StringBuilder();
        sb.append("Marque : ").append(nullSafe(client.getNom())).append('\n');
        appendIfPresent(sb, "Identite", client.getIdentite());
        appendIfPresent(sb, "Activite", client.getActivite());
        appendIfPresent(sb, "Positionnement", client.getPositionnement());
        appendIfPresent(sb, "Objectifs", client.getObjectifs());
        appendIfPresent(sb, "Notes", client.getNotes());
        appendIfPresent(sb, "Description", client.getDescription());
        return sb.toString().trim();
    }

    private String renderProject(Projet projet) {
        StringBuilder sb = new StringBuilder();
        sb.append("Projet : ").append(nullSafe(projet.getNom())).append('\n');
        if (projet.getClient() != null) {
            sb.append("Marque : ").append(nullSafe(projet.getClient().getNom())).append('\n');
        }
        if (projet.getStatut() != null) {
            sb.append("Statut : ").append(projet.getStatut().name()).append('\n');
        }
        if (projet.getDateDebut() != null) {
            sb.append("Debut : ").append(projet.getDateDebut()).append('\n');
        }
        if (projet.getDateFin() != null) {
            sb.append("Fin : ").append(projet.getDateFin()).append('\n');
        }
        appendIfPresent(sb, "Description", projet.getDescription());
        return sb.toString().trim();
    }

    private String renderMediaPlan(MediaPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Publication media plan\n");
        if (plan.getClient() != null) {
            sb.append("Marque : ").append(nullSafe(plan.getClient().getNom())).append('\n');
        }
        if (plan.getDatePublication() != null) {
            sb.append("Date : ").append(plan.getDatePublication()).append('\n');
        }
        appendIfPresent(sb, "Titre", plan.getTitre());
        appendIfPresent(sb, "Format", plan.getFormat());
        appendIfPresent(sb, "Type", plan.getType());
        appendIfPresent(sb, "Plateforme", plan.getPlatforme());
        appendIfPresent(sb, "Texte sur visuel", plan.getTexteSurVisuel());
        appendIfPresent(sb, "Inspiration", plan.getInspiration());
        appendIfPresent(sb, "Autres elements", plan.getAutresElements());
        if (plan.getEtatPublication() != null) {
            sb.append("Etat de publication : ").append(plan.getEtatPublication().name()).append('\n');
        }
        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(" : ").append(value.trim()).append('\n');
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Compte-rendu d'une passe d'indexation, remonte par l'endpoint d'admin. */
    public static class IndexReport {
        public int written;
        public int unchanged;
        public int embedded;
        public int errors;
        public long durationMs;
        /** Un message par etape en echec ("reglement interieur : ..."), pour diagnostiquer sans les logs serveur. */
        public final List<String> errorMessages = new ArrayList<>();

        void merge(IndexReport other) {
            this.written += other.written;
            this.unchanged += other.unchanged;
            this.embedded += other.embedded;
            this.errors += other.errors;
            this.errorMessages.addAll(other.errorMessages);
        }

        public List<String> summary() {
            List<String> lines = new ArrayList<>();
            lines.add(written + " chunks ecrits");
            lines.add(unchanged + " inchanges");
            lines.add(embedded + " embeddings calcules");
            lines.add(errors + " erreurs");
            return lines;
        }
    }
}
