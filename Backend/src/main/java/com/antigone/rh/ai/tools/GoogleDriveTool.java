package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.util.MonthParser;
import com.antigone.rh.ai.service.DriveProvisioningService;
import com.antigone.rh.entity.Client;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.security.AuthPrincipal;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Acces Google Drive expose au LLM.
 *
 * <p>Un echec Drive est rendu comme un message exploitable et non comme une
 * exception : le modele doit pouvoir poursuivre la generation en signalant que les
 * liens seront approvisionnes plus tard, plutot que d'interrompre tout le media
 * plan pour une indisponibilite passagere.
 */
@Slf4j
@lombok.AllArgsConstructor
public class GoogleDriveTool {

    private final DriveProvisioningService driveProvisioning;
    private final ClientRepository clientRepository;
    private final AiAccessScope accessScope;
    private final ToolAuditService audit;
    /** Appelant de cette instance d'outil. Une instance = un utilisateur. */
    private final AiCallContext context;

    @Tool(name = "GoogleDriveTool", value = """
            Cree si necessaire le dossier Drive du mois pour une marque et retourne son lien \
            partageable. Ce lien alimente le champ lienDrive de chaque publication du media plan.""")
    public String getOrCreateMonthFolder(
            @P("Identifiant numerique de la marque (client)") Long clientId,
            @P("Mois vise au format YYYY-MM") String mois) {
        return audit.execute(context, "GoogleDriveTool", "clientId=" + clientId + " mois=" + mois, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireMediaPlanAssistant(principal);
            accessScope.requireClientAllowed(principal, clientId);

            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                return "Marque introuvable pour l'identifiant " + clientId + ".";
            }
            YearMonth month = MonthParser.parseOrCurrent(mois);
            Optional<String> link = driveProvisioning.resolveMonthFolderLink(client.getNom(), month);
            if (link.isPresent()) {
                return "Dossier Drive pret : " + link.get();
            }
            return "Google Drive est momentanement indisponible. Poursuis la generation : les liens "
                    + "seront marques PENDING et approvisionnes automatiquement lors d'une reprise.";
        });
    }

    @Tool(name = "GoogleDriveLinkTool", value = """
            Retourne le lien du dossier Drive racine d'une marque, sans rien creer.""")
    public String getClientFolderLink(@P("Identifiant numerique de la marque (client)") Long clientId) {
        return audit.execute(context, "GoogleDriveLinkTool", "clientId=" + clientId, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireMediaPlanAssistant(principal);
            accessScope.requireClientAllowed(principal, clientId);

            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                return "Marque introuvable pour l'identifiant " + clientId + ".";
            }
            String link = driveProvisioning.clientFolderLink(client.getNom());
            return link == null
                    ? "Aucun dossier Drive n'existe encore pour cette marque."
                    : "Dossier Drive de la marque : " + link;
        });
    }
}
