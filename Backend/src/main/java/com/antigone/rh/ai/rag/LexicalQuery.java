package com.antigone.rh.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Traduit une question en langage naturel en expression pour
 * {@code websearch_to_tsquery}.
 *
 * <p>Le probleme resolu ici est que {@code websearch_to_tsquery} combine les mots
 * par ET. « collection lancement recrute » exige donc un document contenant les
 * trois termes — ce qui, sur une question ecrite en langage naturel, ne renvoie
 * presque jamais rien. La branche lexicale devient alors muette, et la fusion
 * hybride se reduit silencieusement a sa moitie dense.
 *
 * <p>Les termes sont donc combines par OU, et c'est {@code ts_rank_cd} qui fait le
 * tri : un document couvrant plusieurs termes remonte devant un document n'en
 * couvrant qu'un. C'est le comportement attendu d'une recherche de type BM25.
 *
 * <p>Une phrase entre guillemets est en revanche laissee intacte : l'utilisateur
 * demande explicitement une correspondance exacte, et c'est precisement ce que la
 * branche lexicale sait faire mieux que la branche dense.
 */
public final class LexicalQuery {

    /** Borne le cout de la requete sur une question tres longue. */
    private static final int MAX_TERMS = 30;

    /** Tout ce qui n'est ni lettre ni chiffre separe deux termes. */
    private static final String SEPARATORS = "[^\\p{L}\\p{N}]+";

    private LexicalQuery() {
    }

    public static String from(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Guillemets presents : intention de correspondance exacte, on ne touche a rien.
        if (text.indexOf('"') >= 0) {
            return text.trim();
        }

        List<String> terms = new ArrayList<>();
        List<String> shortTerms = new ArrayList<>();

        for (String raw : text.split(SEPARATORS)) {
            if (raw.isEmpty() || terms.size() >= MAX_TERMS) {
                continue;
            }
            String term = raw.toLowerCase(Locale.ROOT);
            if (raw.length() < 2) {
                if (!shortTerms.contains(term)) {
                    shortTerms.add(term);
                }
                continue;
            }
            if (!terms.contains(term)) {
                terms.add(term);
            }
        }

        // Un fragment d'un caractere est du bruit au milieu d'une phrase. Mais s'il
        // constitue toute la requete, l'ecarter rendrait la branche lexicale muette
        // sans que rien ne le signale : mieux vaut chercher que ne rien renvoyer.
        List<String> retained = terms.isEmpty() ? shortTerms : terms;

        // Les mots vides sont elimines par la configuration textuelle de Postgres,
        // inutile d'en maintenir une liste ici.
        return String.join(" OR ", retained);
    }
}
