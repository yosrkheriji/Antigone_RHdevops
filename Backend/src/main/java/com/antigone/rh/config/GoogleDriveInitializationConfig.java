package com.antigone.rh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Initializes Google Drive service account credentials from base64-encoded
 * environment variable.
 * 
 * In production (Render), set:
 *   GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64 → BOT.json  (for employees/internal use)
 *   GOOGLE_DRIVE_VIEWER_KEY_B64          → cleint.json (for client read-only portal)
 * Locally, use the JSON files directly in the classpath.
 */
@Configuration
@Slf4j
public class GoogleDriveInitializationConfig {

    private static final String KEY_B64_ENV = "GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64";
    private static final String KEY_FILE = "service-account.json";

    private static final String VIEWER_KEY_B64_ENV = "GOOGLE_DRIVE_VIEWER_KEY_B64";
    private static final String VIEWER_KEY_FILE = "client-viewer-service-account.json";

    @PostConstruct
    public void initializeServiceAccountKey() {
        decodeAndWrite(KEY_B64_ENV, KEY_FILE);
        decodeAndWrite(VIEWER_KEY_B64_ENV, VIEWER_KEY_FILE);
    }

    private void decodeAndWrite(String envVarName, String targetFile) {
        String b64Key = System.getenv(envVarName);

        if (b64Key == null || b64Key.trim().isEmpty()) {
            log.info("No base64 key found for {}. Using local file (if exists): {}", envVarName, targetFile);
            return;
        }

        try {
            log.info("Decoding base64 key from env var: {} → {}", envVarName, targetFile);
            byte[] decodedBytes = Base64.getDecoder().decode(b64Key.trim());
            String jsonContent = new String(decodedBytes);
            Files.write(Paths.get(targetFile), jsonContent.getBytes());
            log.info("Key written to: {} ({} bytes)", targetFile, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 encoding for {}: {}", envVarName, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to initialize key for {}: {}", envVarName, e.getMessage(), e);
        }
    }
}
