package com.antigone.rh.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Retire la contrainte CHECK que Hibernate genere pour {@code AiSourceType} (champ
 * {@code @Enumerated(STRING)}) au moment ou la table est creee — verrouillant les
 * valeurs alors connues de l'enum.
 *
 * <p>{@code ddl-auto: update} ne met jamais cette contrainte a jour quand l'enum
 * gagne une valeur : l'ajout de {@code POLICY} en a fait la douloureuse
 * demonstration — chaque insertion echouait cote SQL avec un message qui n'apparait
 * que dans les logs serveur, alors que le code Java au-dessus etait parfaitement
 * correct. L'enum garantit deja lui seul l'ensemble des valeurs valides (rien
 * d'autre n'ecrit dans cette table) : cette contrainte SQL est une redondance qui
 * n'a jamais protege personne, au prix d'un piege a chaque evolution future.
 *
 * <p>{@code DROP CONSTRAINT IF EXISTS} est idempotent : ce passage est sans risque
 * a chaque demarrage, meme une fois la contrainte deja retiree.
 */
@Component
@Slf4j
public class AiSourceTypeConstraintCleanup {

    private final DataSource dataSource;

    public AiSourceTypeConstraintCleanup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Synchrone et ordonnee avant {@code EmbeddingIndexService#indexOnStartup}
     * (@Order 100, lui-meme @Async) : le multicaster d'evenements execute les
     * listeners synchrones dans l'ordre avant de lancer les asynchrones, la
     * contrainte est donc deja retiree quand la reindexation demarre.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(50)
    public void dropStaleCheckConstraint() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE ai_document_chunks DROP CONSTRAINT IF EXISTS ai_document_chunks_source_type_check");
        } catch (Exception e) {
            log.warn("Retrait de la contrainte source_type en echec ({}) - non bloquant", e.getMessage());
        }
    }
}
