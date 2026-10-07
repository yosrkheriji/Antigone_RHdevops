package com.antigone.rh.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Render's postgresql:// URL format to the jdbc:postgresql:// format
 * required by JDBC/HikariCP. Runs before any Spring Boot auto-configuration.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) return;
        if (databaseUrl.startsWith("jdbc:")) return; // already in JDBC format

        try {
            // Parse the postgresql:// URI
            String normalized = databaseUrl
                    .replace("postgresql://", "http://")
                    .replace("postgres://", "http://");
            URI uri = URI.create(normalized);

            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath(); // e.g. /rhantigone
            String userInfo = uri.getUserInfo(); // e.g. user:password

            String jdbcUrl = "jdbc:postgresql://" + host + (port > 0 ? ":" + port : "") + path;

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);

            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                props.put("spring.datasource.username", parts[0]);
                props.put("spring.datasource.password", parts[1]);
            }

            environment.getPropertySources().addFirst(
                    new MapPropertySource("renderDatabaseUrlConverter", props));

        } catch (Exception e) {
            // Fail silently — Spring Boot will report the datasource error itself
        }
    }
}
