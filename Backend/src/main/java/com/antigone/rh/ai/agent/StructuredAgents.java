package com.antigone.rh.ai.agent;

import com.antigone.rh.ai.dto.GeneratedMediaPlan;
import com.antigone.rh.ai.dto.PayslipExplanation;
import com.antigone.rh.ai.dto.ReminderDraft;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agents a sortie structuree.
 *
 * <p>Sans memoire ni outils : le contexte leur est fourni entierement construit par
 * le pipeline appelant, qui a deja fait le retrieval et applique le controle
 * d'acces. Un agent qui ne peut rien aller chercher de lui-meme ne peut pas non
 * plus sortir du perimetre.
 *
 * <p>Le type de retour est un POJO, donc LangChain4j exige du modele un JSON
 * conforme au schema — la donnee finale n'est jamais du texte libre a reparser.
 */
public final class StructuredAgents {

    private StructuredAgents() {
    }

    public interface MediaPlanStructuredAgent {

        @SystemMessage(AgentPrompts.MEDIA_PLAN_STRUCTURED)
        @UserMessage("{{contexte}}")
        GeneratedMediaPlan generate(@V("contexte") String contexte);
    }

    public interface ReminderStructuredAgent {

        @SystemMessage(AgentPrompts.REMINDER_STRUCTURED)
        @UserMessage("{{contexte}}")
        ReminderDraft draft(@V("contexte") String contexte);
    }

    public interface PayslipStructuredAgent {

        @SystemMessage(AgentPrompts.PAYSLIP_STRUCTURED)
        @UserMessage("{{contexte}}")
        PayslipExplanation explain(@V("contexte") String contexte);
    }

    /** Titrage automatique d'une conversation depuis son premier message. */
    public interface ConversationTitler {

        @SystemMessage("""
                Tu produis un titre court pour une conversation, a partir de son premier \
                message. Cinq mots maximum, en francais, sans guillemets, sans ponctuation \
                finale, sans prefixe. Reponds uniquement par le titre.
                """)
        @UserMessage("{{message}}")
        String title(@V("message") String message);
    }
}
