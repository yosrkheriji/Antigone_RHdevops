package com.antigone.rh.ai.tools;

import com.antigone.rh.security.AuthPrincipal;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Identite de l'appelant, liee a l'instance des outils.
 *
 * <p>Ce contexte n'est volontairement pas un {@code ThreadLocal}. LangChain4j
 * execute les outils sur le thread de callback du client HTTP, pas sur celui qui a
 * lance la generation : un ThreadLocal pose cote appelant y serait invisible, et un
 * ThreadLocal pose cote callback resterait accroche a un thread mutualise si un
 * outil echouait avant son nettoyage — l'appel suivant, potentiellement d'un autre
 * utilisateur, en heriterait.
 *
 * <p>Les outils sont donc instancies par requete via {@code AiToolFactory}, chacun
 * portant le principal de son appelant. Le cloisonnement ne depend d'aucune
 * discipline de nettoyage.
 *
 * <p>{@code structuredResult} est le meme canal, applique a la sortie plutot qu'a
 * l'identite : un outil de conversation libre (ex. {@code EmailDraftTool}) n'a pas
 * de valeur de retour structuree — LangChain4j n'attend de lui qu'une chaine a lire
 * par le modele. Le deposer ici permet a {@code ChatOrchestratorService}, qui detient
 * la meme instance de contexte pour tout le tour, de le recuperer une fois le flux
 * termine et de l'emettre en {@code structured_result} — le seul evenement que le
 * widget sait rendre en composant riche (bouton d'envoi, apercu HTML, etc.).
 *
 * @param principal      identite issue du JWT
 * @param conversationId conversation courante, pour la piste d'audit ; null hors conversation
 * @param structuredResult resultat structure depose par un outil pour ce tour ; null si aucun
 */
public record AiCallContext(AuthPrincipal principal, Long conversationId,
                             AtomicReference<Object> structuredResult) {

    public AiCallContext {
        if (principal == null) {
            throw new IllegalArgumentException("Un contexte d'appel IA exige un principal authentifie.");
        }
    }

    public static AiCallContext of(AuthPrincipal principal, Long conversationId) {
        return new AiCallContext(principal, conversationId, new AtomicReference<>());
    }

    /** Contexte hors conversation : generation directe via un endpoint dedie. */
    public static AiCallContext of(AuthPrincipal principal) {
        return new AiCallContext(principal, null, new AtomicReference<>());
    }
}
