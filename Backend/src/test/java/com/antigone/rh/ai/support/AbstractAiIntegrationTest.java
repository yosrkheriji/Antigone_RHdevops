package com.antigone.rh.ai.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Socle des tests d'integration : PostgreSQL reel via Testcontainers.
 *
 * <p>L'image {@code pgvector/pgvector:pg16} est retenue plutot qu'un Postgres nu :
 * elle exerce le chemin dense natif (operateur {@code <=>}) plutot que son repli en
 * Java, et la recherche lexicale s'appuie de toute facon sur {@code tsvector}, que
 * seul un vrai Postgres sait executer. Une base embarquee testerait un comportement
 * different de la production.
 *
 * <p>Ces classes sont nommees {@code *IT} : failsafe les execute sur `mvn verify`,
 * surefire ne les voit pas sur `mvn test`. Un poste sans Docker peut donc lancer la
 * suite unitaire, et {@link DockerAvailableCondition} les saute proprement si
 * `mvn verify` y est tout de meme lance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(DockerAvailableCondition.class)
public abstract class AbstractAiIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("antigone_rh_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
