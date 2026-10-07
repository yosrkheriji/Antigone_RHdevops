package com.antigone.rh.ai.service;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.PayslipExplanation;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.exception.AiUnavailableException;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.util.MonthParser;
import com.antigone.rh.entity.BulletinPaie;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;

/**
 * Explication d'un bulletin de paie.
 *
 * <p>Deux garde-fous. D'abord l'acces : {@code employeId} passe systematiquement par
 * {@link AiAccessScope#resolvePayslipEmployeId}, qui ramene tout compte non
 * administrateur a son propre identifiant. Aucun bulletin d'un tiers n'entre donc
 * dans le contexte, quelle que soit la formulation de la demande.
 *
 * <p>Ensuite l'exactitude : {@code previousNet} et {@code currentNet} de la reponse
 * sont ecrases par les valeurs de la base apres la generation. Meme si le modele se
 * trompait dans sa prose, les chiffres exposes par l'API restent ceux du bulletin.
 *
 * <p>Le contexte est construit par {@link PayslipContextBuilder}, partage avec
 * l'outil conversationnel : les deux chemins expliquent donc un meme bulletin de
 * la meme facon.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipExplanationService {

    private final PayslipContextBuilder contextBuilder;
    private final AiAccessScope accessScope;
    private final ObjectProvider<StructuredAgents.PayslipStructuredAgent> agentProvider;

    @Transactional(readOnly = true)
    public PayslipExplanation explain(AuthPrincipal principal, Long requestedEmployeId, String mois) {
        if (!accessScope.canUsePayslipAssistant(principal)) {
            throw new AiForbiddenException("Acces refuse : compte employe ou administrateur requis.");
        }
        Long employeId = accessScope.resolvePayslipEmployeId(principal, requestedEmployeId);

        StructuredAgents.PayslipStructuredAgent agent = agentProvider.getIfAvailable();
        if (agent == null) {
            throw new AiUnavailableException("L'assistant IA n'est pas configure (cle API manquante).");
        }

        YearMonth month = MonthParser.parse(mois);
        PayslipContextBuilder.Resolved resolved = contextBuilder.resolve(employeId, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun bulletin de paie disponible pour cet employe"));

        PayslipExplanation explanation = agent.explain(contextBuilder.build(employeId, resolved));

        // Les montants exposes proviennent de la base, jamais de la generation.
        overrideWithGroundTruth(explanation, resolved.current(), resolved.previous());
        return explanation;
    }

    private void overrideWithGroundTruth(PayslipExplanation explanation, BulletinPaie current,
                                         BulletinPaie previous) {
        PayslipExplanation.Comparison comparison = explanation.getComparison();
        if (comparison == null) {
            comparison = new PayslipExplanation.Comparison();
            explanation.setComparison(comparison);
        }
        double currentNet = value(current.getNetAPayer());
        Double previousNet = previous == null ? null : value(previous.getNetAPayer());

        comparison.setCurrentNet(currentNet);
        comparison.setPreviousNet(previousNet);
        comparison.setDelta(previousNet == null ? null : currentNet - previousNet);
        if (comparison.getDeltaReasons() == null) {
            comparison.setDeltaReasons(new ArrayList<>());
        }
    }

    private double value(Double amount) {
        return amount == null ? 0.0 : amount;
    }
}
