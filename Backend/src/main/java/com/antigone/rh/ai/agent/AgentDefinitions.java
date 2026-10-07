package com.antigone.rh.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interfaces {@code AiServices} de l'assistant.
 *
 * <p>Regroupees ici parce qu'elles ne portent aucune logique : ce sont des contrats
 * declaratifs que LangChain4j implemente par proxy. Les prompts systeme complets
 * vivent dans {@link AgentPrompts} et sont injectes a la construction, ce qui
 * permet d'y interpoler le contexte (perimetre, resume de conversation, date).
 */
public final class AgentDefinitions {

    private AgentDefinitions() {
    }

    /**
     * Agent conversationnel en streaming. Le prompt systeme est fourni a la
     * construction via {@code systemMessageProvider}, afin d'y injecter le resume
     * de la conversation courante.
     */
    public interface ConversationalAgent {
        TokenStream chat(@MemoryId Long conversationId, @UserMessage String message);
    }

    /**
     * Classification d'intention. Retourne une valeur d'enumeration, forme la plus
     * fiable de sortie structuree : aucun parsing de texte libre a faire.
     */
    public interface IntentClassifier {

        @SystemMessage("""
                Tu classes la demande d'un collaborateur d'Antigone, une agence de communication, \
                dans exactement une categorie.

                MEDIA_PLAN : generer, proposer, completer ou ajuster un media plan / calendrier \
                editorial / planning de publications pour une marque et un mois.
                REMINDER : rediger une relance, un rappel de paiement ou une mise en demeure \
                concernant une facture impayee.
                PAYSLIP : expliquer un bulletin de paie, un salaire net, une retenue, l'IRPP, \
                la CNSS ou un ecart de paie entre deux mois.
                GENERAL : tout le reste, notamment les questions RH, conges, reglement interieur.

                En cas de doute, reponds GENERAL.
                """)
        @UserMessage("Demande : {{message}}")
        Capability classify(@V("message") String message);
    }
}
