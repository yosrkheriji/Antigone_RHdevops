package com.antigone.rh.bi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pilote le rechargement de l'entrepot decisionnel.
 *
 * <p>Toute la logique de transformation est dans {@code dwh.refresh_all()}
 * (voir 02_refresh_function.sql) : le traitement reste au plus pres des donnees,
 * sans transiter par la JVM, et reste rejouable a la main depuis psql ou pgAdmin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DwhEtlService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Deux rechargements simultanes se videraient mutuellement leurs tables
     * (TRUNCATE puis INSERT) : le second appel est refuse plutot que mis en file.
     */
    private final AtomicBoolean enCours = new AtomicBoolean(false);

    /** Rechargement nocturne, hors heures de saisie. */
    @Scheduled(cron = "${app.bi.etl-cron:0 30 2 * * *}")
    public void rafraichissementPlanifie() {
        try {
            long lignes = rafraichir();
            log.info("[BI] Rechargement planifie termine : {} lignes de faits.", lignes);
        } catch (Exception e) {
            log.error("[BI] Rechargement planifie en echec : {}", e.getMessage(), e);
        }
    }

    /**
     * Recharge l'entrepot.
     *
     * @return le nombre de lignes de faits chargees
     * @throws IllegalStateException si un rechargement est deja en cours
     */
    public long rafraichir() {
        if (!enCours.compareAndSet(false, true)) {
            throw new IllegalStateException("Un rechargement de l'entrepot est deja en cours.");
        }
        try {
            Long lignes = jdbcTemplate.queryForObject("SELECT dwh.refresh_all()", Long.class);
            return lignes == null ? 0L : lignes;
        } finally {
            enCours.set(false);
        }
    }
}
