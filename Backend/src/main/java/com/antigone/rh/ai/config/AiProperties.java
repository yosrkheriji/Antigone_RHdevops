package com.antigone.rh.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration de l'assistant IA (préfixe {@code app.ai}).
 *
 * <p>Toute la chaîne est dimensionnée pour des générations longues (~60-90 s) :
 * une génération de media plan enchaîne retrieval hybride, plusieurs appels LLM
 * et des appels Google Drive. Les timeouts par défaut des clients HTTP (10-30 s)
 * couperaient la connexion en plein milieu.
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

    /**
     * Duree substituee quand aucune limite n'est configuree.
     *
     * Une absence totale de timeout au niveau du socket laisserait une connexion
     * morte immobiliser un thread indefiniment. Cette borne est assez haute pour
     * n'interrompre aucune generation legitime, tout en garantissant que rien ne
     * reste bloque pour toujours.
     */
    public static final Duration NO_LIMIT = Duration.ofHours(2);

    /** Traduit une duree configuree, {@code 0} valant « aucune limite ». */
    public static Duration effective(Duration configured) {
        return configured == null || configured.isZero() || configured.isNegative()
                ? NO_LIMIT
                : configured;
    }

    /** Vrai si aucune limite n'a ete fixee. */
    public static boolean isUnlimited(Duration configured) {
        return configured == null || configured.isZero() || configured.isNegative();
    }

    /** Désactive complètement l'assistant (aucun bean LLM créé) quand false. */
    private boolean enabled = true;

    private final Chat chat = new Chat();
    private final Embedding embedding = new Embedding();
    private final Rag rag = new Rag();
    private final Memory memory = new Memory();
    private final Sse sse = new Sse();
    private final RateLimit rateLimit = new RateLimit();
    private final Reminder reminder = new Reminder();

    @Getter
    @Setter
    public static class Chat {
        private String apiKey = "";
        /** Surchargeable pour pointer vers un endpoint compatible OpenAI. */
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o";
        private Double temperature = 0.7;
        /** Large volontairement : une génération de media plan peut durer ~60 s. */
        private Duration timeout = Duration.ofSeconds(120);
        private Integer maxRetries = 2;
        private boolean logRequests = false;
        private boolean logResponses = false;
        /** Garde-fou anti-boucle sur l'orchestration d'outils. */
        private Integer maxToolCallingRoundTrips = 10;
    }

    @Getter
    @Setter
    public static class Embedding {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "text-embedding-3-small";
        /** Doit correspondre au modèle : 1536 pour text-embedding-3-small. */
        private int dimensions = 1536;
        /** {@code 0s} = aucune limite. */
        private Duration timeout = Duration.ZERO;
        private Integer maxRetries = 3;
    }

    @Getter
    @Setter
    public static class Rag {
        /** Nombre de chunks remontés par chaque branche avant fusion. */
        private int denseTopK = 12;
        private int sparseTopK = 12;
        /** Nombre de chunks conservés après fusion RRF. */
        private int finalTopK = 8;
        /** Poids de la branche dense dans la fusion (le sparse reçoit 1 - poids). */
        private double denseWeight = 0.6;
        /** Constante k de la Reciprocal Rank Fusion. */
        private int rrfK = 60;
        /** Configuration textuelle Postgres utilisée par la recherche lexicale. */
        private String textSearchConfig = "french";
        /** Réindexation périodique des chunks (cron Spring). */
        private String reindexCron = "0 0 3 * * *";
        private boolean reindexOnStartup = false;
    }

    @Getter
    @Setter
    public static class Memory {
        /** Messages conservés en clair dans la fenêtre de contexte. */
        private int maxMessages = 20;
        /** Résume les messages évincés dès que ce nombre est atteint. */
        private int summarizeEvery = 6;
        private boolean summarizationEnabled = true;
    }

    @Getter
    @Setter
    public static class Sse {
        /** Intervalle des heartbeats pendant les phases sans token généré. */
        private Duration heartbeatInterval = Duration.ofSeconds(15);
        /**
         * Duree de vie maximale d'un flux SSE. {@code 0s} = aucune limite.
         *
         * Le heartbeat maintient la connexion ouverte ; c'est lui, et non un
         * minuteur, qui garantit qu'un proxy ne la ferme pas.
         */
        private Duration timeout = Duration.ZERO;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        /** Requêtes IA autorisées par utilisateur et par fenêtre. */
        private int requestsPerWindow = 20;
        private Duration window = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class Reminder {
        /** Borne haute (incluse) du palier « ton amical », en jours de retard. */
        private int softMaxDays = 7;
        /** Borne haute (incluse) du palier « ferme mais courtois ». */
        private int firmMaxDays = 30;

        // Aucun reglage d'envoi : la generation produit toujours un brouillon, et
        // l'expedition passe par une confirmation explicite de l'utilisateur (clic sur
        // le bouton du widget, ou confirmation en conversation). Un indicateur de
        // configuration ne saurait pas distinguer un texte relu d'un texte simplement
        // genere.
    }
}
