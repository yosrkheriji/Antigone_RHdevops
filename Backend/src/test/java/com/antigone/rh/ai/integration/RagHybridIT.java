package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.entity.AiDocumentChunk;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.rag.AiSourceTypeConstraintCleanup;
import com.antigone.rh.ai.rag.EmbeddingIndexService;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.PgVectorSupport;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.repository.AiDocumentChunkRepository;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.ai.tools.AiCallContext;
import com.antigone.rh.ai.tools.AiToolFactory;
import com.antigone.rh.ai.tools.PolicyTools;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Scenario 11 : les deux branches du RAG hybride contribuent au contexte.
 *
 * <p>Postgres est reel, donc {@code to_tsvector} et {@code ts_rank_cd} le sont aussi :
 * la branche lexicale est reellement exercee, ce qu'aucune base embarquee ne
 * permettrait. Le modele d'embedding, lui, est deterministe et projette quelques
 * concepts sur des axes distincts — la pertinence semantique reelle depend d'OpenAI
 * et n'a pas a etre testee ici ; ce qui doit l'etre, c'est la fusion.
 */
class RagHybridIT extends AbstractAiIntegrationTest {

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private EmbeddingIndexService indexService;

    @Autowired
    private AiDocumentChunkRepository chunkRepository;

    @Autowired
    private PgVectorSupport pgVectorSupport;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private AiToolFactory toolFactory;

    @Autowired
    private AiSourceTypeConstraintCleanup constraintCleanup;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private Client client;
    private AiAccessScope.ClientScope scope;

    /**
     * Projection deterministe : axe 0 « lancement / collection », axe 1 « recrutement »,
     * axe 2 residuel. Deux textes parlant de lancement se ressemblent, meme sans
     * partager de mot.
     */
    private static float[] fakeVector(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        float launch = 0f;
        float hiring = 0f;
        for (String token : List.of("lancement", "collection", "nouveaute", "sortie", "teasing")) {
            if (lower.contains(token)) {
                launch += 1f;
            }
        }
        for (String token : List.of("recrutement", "poste", "carriere", "equipe")) {
            if (lower.contains(token)) {
                hiring += 1f;
            }
        }
        return new float[]{launch, hiring, 0.15f};
    }

    @BeforeEach
    void setUp() {
        when(embeddingModel.embed(anyString()))
                .thenAnswer(call -> Response.from(Embedding.from(fakeVector(call.getArgument(0)))));
        when(embeddingModel.embedAll(any()))
                .thenAnswer(call -> {
                    List<TextSegment> segments = call.getArgument(0);
                    return Response.from(segments.stream()
                            .map(segment -> Embedding.from(fakeVector(segment.text())))
                            .toList());
                });

        Employe createur = fixtures.employe("Indexeur", "Test");
        client = fixtures.client("Zenith" + System.nanoTime());
        scope = AiAccessScope.ClientScope.of(List.of(client.getId()));

        fixtures.publication(client, createur, LocalDate.of(2026, 4, 8),
                "Teasing de la nouvelle collection capsule", "Instagram", "Reel", "Produit");
        fixtures.publication(client, createur, LocalDate.of(2026, 5, 12),
                "On recrute un chef de projet", "LinkedIn", "Carrousel", "Marque employeur");
        fixtures.publication(client, createur, LocalDate.of(2026, 6, 3),
                "Sortie officielle de la gamme ete", "Facebook", "Video", "Produit");

        indexService.indexBrands();
        indexService.indexMediaPlans();
        indexService.embedPending();
    }

    @Test
    @DisplayName("L'indexation produit des chunks embeddes et rattaches a la marque")
    void indexingProducesScopedEmbeddedChunks() {
        List<AiDocumentChunk> chunks = chunkRepository.findEmbeddedInScope(
                false, List.of(client.getId()), true, Set.of(AiSourceType.MEDIA_PLAN));

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getClientId()).isEqualTo(client.getId());
            assertThat(chunk.getEmbedding()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Une recherche par mots exacts est portee par la branche lexicale")
    void exactKeywordSearchIsCarriedByTheLexicalBranch() {
        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "chef de projet recrute", scope, Set.of(AiSourceType.MEDIA_PLAN)));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anySatisfy(hit -> {
            assertThat(hit.chunk().getContent()).contains("On recrute un chef de projet");
            assertThat(hit.fromSparse()).isTrue();
        });
    }

    @Test
    @DisplayName("Une requete semantique vague est portee par la branche dense")
    void vagueSemanticQueryIsCarriedByTheDenseBranch() {
        // Aucun de ces mots n'apparait tel quel dans « Sortie officielle de la gamme
        // ete » : seule la proximite vectorielle peut le remonter.
        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "lancement produit", scope, Set.of(AiSourceType.MEDIA_PLAN)));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anySatisfy(hit -> assertThat(hit.fromDense()).isTrue());
    }

    @Test
    @DisplayName("Une requete mixte fait contribuer les deux branches")
    void mixedQueryDrawsFromBothBranches() {
        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "collection lancement recrute", scope, Set.of(AiSourceType.MEDIA_PLAN)));

        assertThat(hits).isNotEmpty();
        assertThat(hits).as("la branche dense doit contribuer").anyMatch(RagHit::fromDense);
        assertThat(hits).as("la branche lexicale doit contribuer").anyMatch(RagHit::fromSparse);
    }

    @Test
    @DisplayName("Le perimetre client est applique en SQL, pas apres coup")
    void clientScopeIsEnforcedInSql() {
        Employe autreCreateur = fixtures.employe("Autre", "Createur");
        Client autreMarque = fixtures.client("Omega" + System.nanoTime());
        fixtures.publication(autreMarque, autreCreateur, LocalDate.of(2026, 6, 10),
                "Teasing de la nouvelle collection capsule", "Instagram", "Reel", "Produit");
        indexService.indexMediaPlans();
        indexService.embedPending();

        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "collection capsule", scope, Set.of(AiSourceType.MEDIA_PLAN)));

        assertThat(hits).isNotEmpty();
        assertThat(hits).allSatisfy(hit ->
                assertThat(hit.chunk().getClientId()).isEqualTo(client.getId()));
    }

    @Test
    @DisplayName("Un perimetre vide ne remonte rien, sans erreur SQL")
    void emptyScopeReturnsNothingWithoutSqlError() {
        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "collection", AiAccessScope.ClientScope.of(List.of()), null));

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("La reindexation est incrementale : un contenu inchange n'est pas reecrit")
    void reindexingIsIncremental() {
        EmbeddingIndexService.IndexReport report = indexService.indexMediaPlans();

        assertThat(report.unchanged).isGreaterThanOrEqualTo(3);
        assertThat(report.written).isZero();
    }

    @Test
    @DisplayName("L'image de test fournit pgvector : le chemin dense natif est bien exerce")
    void pgVectorIsActiveOnTheTestImage() {
        assertThat(pgVectorSupport.isAvailable())
                .as("l'image pgvector/pgvector:pg16 doit activer l'extension")
                .isTrue();
    }

    @Test
    @DisplayName("Le profil de marque est indexe et retrouvable")
    void brandProfileIsIndexedAndRetrievable() {
        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "positionnement fait-main tunisien", scope, Set.of(AiSourceType.BRAND)));

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).chunk().getSourceType()).isEqualTo(AiSourceType.BRAND);
    }

    // ---- Reglement interieur ------------------------------------------------
    //
    // Le PDF partage par l'agence est deja extrait en texte (reglement_interieur.txt,
    // meme fichier que consommait l'ancien chatbot Groq). L'indexation en fait un
    // chunk par article : ces tests verifient que le decoupage retombe juste sur le
    // vrai document, pas sur un texte de synthese invente pour l'occasion.

    @Test
    @DisplayName("Le reglement interieur est indexe par article, sans rattachement a un client")
    void policyIsIndexedPerArticleAsATransverseDocument() {
        EmbeddingIndexService.IndexReport report = indexService.indexPolicy();

        // Preambule (Dispositions generales) + articles 2 a 13.
        assertThat(report.written).isGreaterThanOrEqualTo(13);

        List<AiDocumentChunk> article7 = chunkRepository.findBySourceTypeAndSourceId(AiSourceType.POLICY, 7L);
        assertThat(article7).hasSize(1);
        assertThat(article7.get(0).getClientId()).isNull();
        assertThat(article7.get(0).getContent())
                .contains("Article 7")
                .containsIgnoringCase("congé");
    }

    @Test
    @DisplayName("Une recherche exacte sur le reglement retrouve l'article correspondant")
    void policySearchFindsTheRelevantArticle() {
        indexService.indexPolicy();
        indexService.embedPending();

        List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                "congé de maternité durée accouchement",
                AiAccessScope.ClientScope.all(), Set.of(AiSourceType.POLICY)));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anySatisfy(hit -> assertThat(hit.chunk().getContent()).contains("2 mois"));
    }

    @Test
    @DisplayName("L'outil cite l'article reel du reglement plutot que d'halluciner une regle")
    void policyToolCitesTheRealArticleInsteadOfInventingOne() {
        indexService.indexPolicy();
        indexService.embedPending();

        AiCallContext context = AiCallContext.of(
                fixtures.employeePrincipal(fixtures.employe("Question", "RH")), 1L);
        PolicyTools tools = toolFactory.policyTools(context);

        String result = tools.lookup("combien de jours de congé en cas de décès d'un parent");

        assertThat(result).contains("5 jours");
    }

    @Test
    @DisplayName("Sans correspondance, l'outil le dit plutot que d'improviser une regle")
    void policyToolIsHonestWhenNothingMatches() {
        indexService.indexPolicy();
        indexService.embedPending();

        // Isole la branche lexicale : le vecteur factice de ce fichier degenere vers
        // le meme residu pour tout texte hors du vocabulaire media plan, ce qui
        // simulerait a tort une correspondance dense parfaite sur chaque article.
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("indisponible"));

        AiCallContext context = AiCallContext.of(
                fixtures.employeePrincipal(fixtures.employe("Question", "Hors sujet")), 1L);
        PolicyTools tools = toolFactory.policyTools(context);

        String result = tools.lookup("zzz xylophone quantique inexistant");

        assertThat(result).containsIgnoringCase("aucun article");
    }

    /**
     * Reproduit exactement l'incident constate en local : Hibernate genere, a la
     * creation de la table, une contrainte CHECK verrouillant les valeurs de
     * {@code AiSourceType} connues a ce moment-la. {@code ddl-auto: update} ne la
     * met jamais a jour quand l'enum gagne une valeur — l'ajout de {@code POLICY}
     * en a fait la demonstration : chaque insertion echouait cote SQL, alors que le
     * code Java au-dessus etait correct. Le schema de test est cree a neuf a chaque
     * run (la contrainte y inclut donc deja POLICY), il faut la reposer
     * manuellement dans son etat legacy pour reproduire le probleme.
     */
    @Test
    @DisplayName("Le nettoyage de la contrainte CHECK repare une insertion POLICY qui echouait")
    void constraintCleanupUnblocksPolicyInserts() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            // Des tests precedents ont deja pu ecrire des chunks POLICY inchanges :
            // sans les retirer, l'upsert incremental ne retenterait aucune insertion
            // et ne passerait donc jamais par la contrainte remise en place ci-dessous.
            statement.execute("DELETE FROM ai_document_chunks WHERE source_type = 'POLICY'");
            statement.execute("ALTER TABLE ai_document_chunks DROP CONSTRAINT IF EXISTS "
                    + "ai_document_chunks_source_type_check");
            // NOT VALID : n'exige pas que les lignes deja presentes (d'autres types)
            // satisfassent la contrainte, seules les prochaines ecritures le sont —
            // suffisant pour reproduire l'echec sur les insertions POLICY a venir.
            statement.execute("ALTER TABLE ai_document_chunks ADD CONSTRAINT "
                    + "ai_document_chunks_source_type_check "
                    + "CHECK (source_type IN ('BRAND','PROJECT','MEDIA_PLAN','CONTENT')) NOT VALID");
        }

        // Appel direct (pas via reindexAll/mergeSafely) : l'exception SQL doit donc
        // remonter telle quelle, confirmant que c'est bien la contrainte qui bloque
        // et non un defaut du code d'indexation.
        assertThatThrownBy(() -> indexService.indexPolicy())
                .hasMessageContaining("ai_document_chunks_source_type_check");

        constraintCleanup.dropStaleCheckConstraint();

        EmbeddingIndexService.IndexReport fixed = indexService.indexPolicy();
        assertThat(fixed.errors).isZero();
        assertThat(fixed.written).isGreaterThanOrEqualTo(13);
    }
}
