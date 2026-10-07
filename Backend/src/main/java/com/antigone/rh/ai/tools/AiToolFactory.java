package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.agent.Capability;
import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.DriveProvisioningService;
import com.antigone.rh.ai.service.MediaPlanGenerationService;
import com.antigone.rh.ai.service.PayslipContextBuilder;
import com.antigone.rh.ai.service.ReminderEmailTemplate;
import com.antigone.rh.ai.service.ReminderGenerationService;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.FactureRepository;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.repository.ProjetRepository;
import com.antigone.rh.repository.RelanceClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fabrique les outils lies a un appelant.
 *
 * <p>Les dependances (repositories, services) sont des singletons Spring ; seules
 * les instances d'outils sont creees par requete, avec le {@link AiCallContext} de
 * leur appelant. Une instance d'outil ne peut donc servir qu'un utilisateur, ce qui
 * rend le cloisonnement independant de la gestion des threads par LangChain4j.
 */
@Component
@RequiredArgsConstructor
public class AiToolFactory {

    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final MediaPlanRepository mediaPlanRepository;
    private final FactureRepository factureRepository;
    private final EmployeRepository employeRepository;
    private final HybridRetriever hybridRetriever;
    private final MediaPlanGenerationService mediaPlanGenerationService;
    private final PayslipContextBuilder payslipContextBuilder;
    private final DriveProvisioningService driveProvisioning;
    private final RelanceClientRepository relanceRepository;
    private final ReminderGenerationService reminderGenerationService;
    private final ReminderEmailTemplate reminderEmailTemplate;
    private final AiAccessScope accessScope;
    private final AiProperties properties;
    private final ToolAuditService audit;

    /** Outils correspondant a une capacite, lies a l'appelant. */
    public List<Object> toolsFor(Capability capability, AiCallContext context) {
        return switch (capability) {
            case MEDIA_PLAN -> List.of(mediaPlanTools(context), googleDriveTool(context));
            case REMINDER -> List.of(reminderTools(context));
            case PAYSLIP -> List.of(payrollTools(context));
            // L'agent generaliste garde tout : c'est le point d'entree par defaut, et
            // le controle d'acces est porte par les outils eux-memes.
            case GENERAL -> List.of(mediaPlanTools(context), googleDriveTool(context),
                    reminderTools(context), payrollTools(context), policyTools(context));
        };
    }

    public MediaPlanTools mediaPlanTools(AiCallContext context) {
        return new MediaPlanTools(clientRepository, projetRepository, mediaPlanRepository,
                hybridRetriever, mediaPlanGenerationService, accessScope, audit, context);
    }

    public GoogleDriveTool googleDriveTool(AiCallContext context) {
        return new GoogleDriveTool(driveProvisioning, clientRepository, accessScope, audit, context);
    }

    public ReminderTools reminderTools(AiCallContext context) {
        return new ReminderTools(factureRepository, relanceRepository, accessScope, properties,
                reminderGenerationService, reminderEmailTemplate, audit, context);
    }

    public PayrollTools payrollTools(AiCallContext context) {
        return new PayrollTools(payslipContextBuilder, employeRepository, accessScope, audit, context);
    }

    public PolicyTools policyTools(AiCallContext context) {
        return new PolicyTools(hybridRetriever, audit, context);
    }
}
