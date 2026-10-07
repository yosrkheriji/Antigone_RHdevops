package com.antigone.rh.bi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Cree le schema decisionnel {@code dwh} au demarrage.
 *
 * <p>S'execute en {@link ApplicationRunner} : Hibernate a deja cree les tables
 * OLTP et {@code data.sql} a deja tourne, les tables sources existent donc.
 *
 * <p>Le schema ne contient que des donnees derivees : quand
 * {@link #VERSION_MODELE} change, on le supprime et on le recree au lieu
 * d'ecrire une migration. Aucune donnee metier n'est perdue.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class DwhSchemaInitializer implements ApplicationRunner {

    /** A incrementer des que 01_schema.sql change de structure. */
    static final String VERSION_MODELE = "2";

    private static final String SCRIPT_SCHEMA = "dwh/01_schema.sql";
    private static final String SCRIPT_FONCTION = "dwh/02_refresh_function.sql";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final DwhEtlService etlService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!versionAJour()) {
                log.info("[BI] Construction du schema decisionnel dwh (version {})...", VERSION_MODELE);
                jdbcTemplate.execute("DROP SCHEMA IF EXISTS dwh CASCADE");
                executerScriptDdl(new ClassPathResource(SCRIPT_SCHEMA));
            }

            // La fonction est rejouee a chaque demarrage (CREATE OR REPLACE) :
            // une correction de l'ETL est ainsi prise en compte sans toucher au schema.
            executerInstructionUnique(new ClassPathResource(SCRIPT_FONCTION));

            if (entrepotVide()) {
                log.info("[BI] Entrepot vide — chargement initial en cours...");
                long lignes = etlService.rafraichir();
                log.info("[BI] Chargement initial termine : {} lignes de faits.", lignes);
            }
        } catch (Exception e) {
            // Un entrepot indisponible ne doit pas empecher l'application de demarrer :
            // les pages Analytique renverront une erreur explicite, le reste fonctionne.
            log.error("[BI] Initialisation de l'entrepot impossible — les tableaux de bord "
                    + "decisionnels seront indisponibles. Cause : {}", e.getMessage(), e);
        }
    }

    private boolean versionAJour() {
        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT valeur FROM dwh.meta WHERE cle = 'schema_version'", String.class);
            return VERSION_MODELE.equals(version);
        } catch (Exception e) {
            // Schema ou table absents : c'est une premiere installation.
            return false;
        }
    }

    private boolean entrepotVide() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dwh.dim_date", Long.class);
        return total == null || total == 0;
    }

    /** Script multi-instructions separees par des points-virgules. */
    private void executerScriptDdl(Resource resource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.setSeparator(";");
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    /**
     * Script constituant une seule instruction. Le corps PL/pgSQL contient des
     * points-virgules : le decouper comme un script ordinaire le casserait, on
     * l'envoie donc tel quel au pilote.
     */
    private void executerInstructionUnique(Resource resource) throws Exception {
        try (var flux = resource.getInputStream()) {
            jdbcTemplate.execute(new String(flux.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
