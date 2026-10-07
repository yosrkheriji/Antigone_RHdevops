package com.antigone.rh.ai.util;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Lecture d'un mois au format {@code YYYY-MM}.
 *
 * <p>Extrait ici plutot que duplique dans chaque service : la paie et le media plan
 * n'ont rien a voir l'un avec l'autre, et faire dependre l'un de l'autre pour une
 * simple conversion serait un couplage gratuit.
 *
 * <p>Le message d'erreur est destine a l'utilisateur comme au LLM : il rappelle le
 * format attendu avec un exemple, ce qui permet au modele de se corriger seul quand
 * il passe un mois mal forme a un outil.
 */
public final class MonthParser {

    private MonthParser() {
    }

    /**
     * @throws IllegalArgumentException si la valeur est vide ou mal formee
     */
    public static YearMonth parse(String mois) {
        if (mois == null || mois.isBlank()) {
            throw new IllegalArgumentException("Le mois est obligatoire (format YYYY-MM).");
        }
        try {
            return YearMonth.parse(mois.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Mois invalide : '" + mois + "'. Format attendu YYYY-MM, par exemple 2026-07.");
        }
    }

    /** Variante tolerante : retombe sur le mois courant plutot que d'echouer. */
    public static YearMonth parseOrCurrent(String mois) {
        if (mois == null || mois.isBlank()) {
            return YearMonth.now();
        }
        return parse(mois);
    }
}
