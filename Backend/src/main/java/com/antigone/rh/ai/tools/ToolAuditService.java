package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.entity.AiToolAuditLog;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.repository.AiToolAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Enveloppe d'execution commune a tous les outils : controle d'acces, journal
 * d'audit, et normalisation des erreurs.
 *
 * <p>Les erreurs renvoyees au LLM sont des phrases courtes et explicites, jamais
 * une stack trace : le modele doit pouvoir raisonner dessus (« la marque demandee
 * n'est pas dans votre perimetre ») sans qu'aucun detail d'implementation ne
 * transite par le prompt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolAuditService {

    private final AiToolAuditRepository auditRepository;

    /**
     * Execute un outil sous audit.
     *
     * @param context   identite de l'appelant, portee par l'instance de l'outil
     * @param toolName  nom expose au LLM
     * @param arguments arguments, pour la piste d'audit
     * @param action    corps de l'outil ; sa valeur de retour est rendue au LLM
     */
    public String execute(AiCallContext context, String toolName, String arguments,
                          Supplier<String> action) {
        long start = System.currentTimeMillis();
        try {
            String result = action.get();
            record(context, toolName, arguments, "GRANTED", null, System.currentTimeMillis() - start);
            return result;
        } catch (AiForbiddenException e) {
            record(context, toolName, arguments, "DENIED", e.getMessage(), System.currentTimeMillis() - start);
            log.warn("Outil {} refuse pour le compte {} : {}", toolName, context.principal().getAccountId(),
                    e.getMessage());
            return "REFUSE : " + e.getMessage();
        } catch (Exception e) {
            record(context, toolName, arguments, "ERROR", e.getMessage(), System.currentTimeMillis() - start);
            log.error("Outil {} en echec : {}", toolName, e.getMessage(), e);
            return "ERREUR : l'outil " + toolName + " n'a pas pu aboutir (" + safeMessage(e) + "). "
                    + "Poursuis sans cette information et signale-le dans ta reponse.";
        }
    }

    /**
     * Ecrit la trace dans sa propre transaction : l'audit doit survivre au rollback
     * de la transaction metier qui l'a declenche, sinon un acces refuse disparait
     * du journal precisement quand il compte.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiCallContext context, String toolName, String arguments, String outcome,
                       String detail, long durationMs) {
        try {
            auditRepository.save(AiToolAuditLog.builder()
                    .compteId(context.principal().getAccountId())
                    .conversationId(context.conversationId())
                    .toolName(toolName)
                    .arguments(truncate(arguments, 4000))
                    .outcome(outcome)
                    .detail(truncate(detail, 2000))
                    .durationMs(durationMs)
                    .build());
        } catch (Exception e) {
            log.warn("Ecriture du journal d'audit en echec pour {} : {}", toolName, e.getMessage());
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return truncate(message, 200);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
