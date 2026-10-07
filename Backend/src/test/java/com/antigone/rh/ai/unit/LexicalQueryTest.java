package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.rag.LexicalQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La branche lexicale doit rester utile sur une question en langage naturel.
 *
 * <p>{@code websearch_to_tsquery} combine les mots par ET : sans traduction, une
 * phrase entiere n'aurait quasiment jamais de correspondance, et la fusion hybride
 * se reduirait a sa seule branche dense sans que rien ne le signale.
 */
class LexicalQueryTest {

    @Test
    @DisplayName("Les termes d'une question sont combines par OU")
    void naturalLanguageTermsAreCombinedWithOr() {
        assertThat(LexicalQuery.from("collection lancement recrute"))
                .isEqualTo("collection OR lancement OR recrute");
    }

    @Test
    @DisplayName("La ponctuation et les accents ne cassent pas le decoupage")
    void punctuationDoesNotBreakTokenisation() {
        assertThat(LexicalQuery.from("Media plan : juillet, marque Alpha ?"))
                .isEqualTo("media OR plan OR juillet OR marque OR alpha");
    }

    @Test
    @DisplayName("Une phrase entre guillemets est laissee intacte")
    void quotedPhraseIsPreserved() {
        String quoted = "\"lancement collection capsule\"";

        assertThat(LexicalQuery.from(quoted)).isEqualTo(quoted);
    }

    @Test
    @DisplayName("Les doublons sont supprimes")
    void duplicateTermsAreCollapsed() {
        assertThat(LexicalQuery.from("collection Collection COLLECTION")).isEqualTo("collection");
    }

    @Test
    @DisplayName("Les fragments d'un caractere sont ignores au milieu d'une phrase")
    void singleCharacterFragmentsAreDroppedWithinAPhrase() {
        assertThat(LexicalQuery.from("a b collection")).isEqualTo("collection");
    }

    @Test
    @DisplayName("Une requete faite uniquement de fragments courts reste cherchee")
    void queryMadeOnlyOfShortFragmentsIsStillSearched() {
        // Les ecarter rendrait la branche lexicale muette sur cette requete, et la
        // fusion se reduirait a sa moitie dense sans que rien ne le signale.
        assertThat(LexicalQuery.from("q")).isEqualTo("q");
        assertThat(LexicalQuery.from("a b")).isEqualTo("a OR b");
    }

    @Test
    @DisplayName("Un tiret initial ne devient pas une negation")
    void leadingHyphenDoesNotBecomeNegation() {
        // websearch_to_tsquery interprete -mot comme une exclusion : le decoupage
        // sur les non-alphanumeriques neutralise le probleme.
        assertThat(LexicalQuery.from("-collection lancement"))
                .isEqualTo("collection OR lancement");
    }

    @Test
    @DisplayName("Une requete vide ne produit aucune expression")
    void blankInputProducesNothing() {
        assertThat(LexicalQuery.from(null)).isEmpty();
        assertThat(LexicalQuery.from("   ")).isEmpty();
        assertThat(LexicalQuery.from("! ? ,")).isEmpty();
    }

    @Test
    @DisplayName("Le nombre de termes est borne")
    void termCountIsCapped() {
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longQuery.append("terme").append(i).append(' ');
        }

        long terms = LexicalQuery.from(longQuery.toString()).split(" OR ").length;

        assertThat(terms).isLessThanOrEqualTo(30);
    }
}
