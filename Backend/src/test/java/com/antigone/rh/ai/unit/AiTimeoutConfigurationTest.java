package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une generation de media plan peut durer ~60-90 s.
 *
 * <p>Ce test lit la configuration reellement livree — pas les valeurs par defaut du
 * code — et verifie qu'aucun maillon ne coupera avant. Un timeout HTTP ramene a 30 s
 * lors d'un reglage ulterieur casserait la fonctionnalite d'une maniere qu'aucun
 * test fonctionnel ne detecterait : il echouerait par intermittence, seulement sur
 * les generations les plus longues.
 */
class AiTimeoutConfigurationTest {

    /** Duree de reference d'une generation complexe, servant de repere. */
    private static final Duration LONG_GENERATION = Duration.ofSeconds(90);

    /**
     * Lit le YAML livre plutot que les valeurs par defaut du code : c'est bien le
     * fichier deploye qui doit porter les garanties, pas la classe de configuration.
     * Les placeholders {@code ${VAR:defaut}} sont resolus par l'environnement.
     */
    private AiProperties bind(String yamlFile) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(yamlFile, new ClassPathResource(yamlFile));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));

        return Binder.get(environment).bindOrCreate("app.ai", AiProperties.class);
    }

    @Test
    @DisplayName("Aucune limite de duree n'est imposee au modele")
    void chatHasNoTimeLimit() throws IOException {
        AiProperties properties = bind("application.yml");

        // Une generation de media plan enchaine retrieval, plusieurs appels LLM et
        // des appels Drive. Une borne, meme large, finirait par couper une reponse
        // valide en plein milieu.
        assertThat(AiProperties.isUnlimited(properties.getChat().getTimeout()))
                .as("app.ai.chat.timeout doit valoir 0s (aucune limite)")
                .isTrue();
    }

    @Test
    @DisplayName("Le flux SSE ne s'interrompt jamais de lui-meme")
    void sseStreamNeverExpires() throws IOException {
        AiProperties properties = bind("application.yml");

        assertThat(AiProperties.isUnlimited(properties.getSse().getTimeout()))
                .as("app.ai.sse.timeout doit valoir 0s (aucune limite)")
                .isTrue();
    }

    @Test
    @DisplayName("La borne de securite absorbe largement une generation longue")
    void safetyBoundOutlivesAnyGeneration() {
        // Une absence totale de timeout au niveau du socket laisserait une connexion
        // morte immobiliser un thread indefiniment : cette borne existe pour ce cas
        // seulement, et doit rester tres au-dessus de toute generation legitime.
        assertThat(AiProperties.effective(java.time.Duration.ZERO))
                .isGreaterThan(LONG_GENERATION.multipliedBy(10));
    }

    @Test
    @DisplayName("Le heartbeat est plus frequent que les fermetures de proxy usuelles")
    void heartbeatIsFrequentEnoughForProxies() throws IOException {
        AiProperties properties = bind("application.yml");

        // Sans limite de duree, c'est le heartbeat — et lui seul — qui empeche un
        // proxy de fermer la connexion. Nginx coupe a 60 s d'inactivite.
        assertThat(properties.getSse().getHeartbeatInterval())
                .as("un heartbeat trop espace laisse un proxy fermer la connexion")
                .isLessThanOrEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("La configuration de production tient les memes garanties")
    void productionProfileKeepsTheSameGuarantees() throws IOException {
        AiProperties properties = bind("application-prod.yml");

        assertThat(AiProperties.isUnlimited(properties.getChat().getTimeout())).isTrue();
        assertThat(AiProperties.isUnlimited(properties.getSse().getTimeout())).isTrue();
        assertThat(properties.getSse().getHeartbeatInterval()).isLessThanOrEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Les bornes de palier livrees correspondent aux regles metier")
    void reminderThresholdsMatchBusinessRules() throws IOException {
        AiProperties properties = bind("application.yml");

        assertThat(properties.getReminder().getSoftMaxDays()).isEqualTo(7);
        assertThat(properties.getReminder().getFirmMaxDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("La dimension d'embedding correspond au modele configure")
    void embeddingDimensionsMatchTheModel() throws IOException {
        AiProperties properties = bind("application.yml");

        // Une dimension desalignee ne se voit qu'a l'execution, sur la colonne
        // pgvector : autant l'attraper ici.
        if ("text-embedding-3-small".equals(properties.getEmbedding().getModel())) {
            assertThat(properties.getEmbedding().getDimensions()).isEqualTo(1536);
        }
    }
}
