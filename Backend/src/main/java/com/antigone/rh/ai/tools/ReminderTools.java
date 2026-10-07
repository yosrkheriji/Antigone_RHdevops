package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.dto.ReminderResponse;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.InvoiceLateInfo;
import com.antigone.rh.ai.service.ReminderEmailTemplate;
import com.antigone.rh.ai.service.ReminderGenerationService;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.entity.RelanceClient;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.repository.FactureRepository;
import com.antigone.rh.repository.RelanceClientRepository;
import com.antigone.rh.security.AuthPrincipal;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Outils de relance client, reserves aux administrateurs.
 *
 * <p>{@code EmailDraftTool} n'envoie rien : il enregistre un brouillon et rend sa
 * reference. {@code ConfirmAndSendReminderTool} l'expedie, mais seulement quand le
 * modele l'appelle a la suite d'une confirmation explicite de l'utilisateur en
 * conversation — le clic du bouton cote widget passe par un chemin distinct
 * ({@code ReminderGenerationService.send} via son endpoint REST), les deux menant
 * a la meme methode pour que l'envoi reste identique quelle que soit la voie de
 * confirmation.
 */
@Slf4j
@lombok.AllArgsConstructor
public class ReminderTools {

    private final FactureRepository factureRepository;
    private final RelanceClientRepository relanceRepository;
    private final AiAccessScope accessScope;
    private final AiProperties properties;
    private final ReminderGenerationService reminderGenerationService;
    private final ReminderEmailTemplate emailTemplate;
    private final ToolAuditService audit;
    /** Appelant de cette instance d'outil. Une instance = un utilisateur. */
    private final AiCallContext context;

    @Tool(name = "InvoiceLookupTool", value = """
            Recupere une facture impayee et calcule ses jours de retard, le reste du et le \
            palier de ton a appliquer. Retourne des montants reels a citer tels quels.""")
    public String invoiceLookup(@P("Identifiant numerique de la facture") Long factureId) {
        return audit.execute(context, "InvoiceLookupTool", "factureId=" + factureId, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireReminderAssistant(principal);

            Facture facture = factureRepository.findByIdWithClient(factureId).orElse(null);
            if (facture == null) {
                return "Aucune facture trouvee pour l'identifiant " + factureId + ".";
            }
            if (facture.getType() != TypeDocument.FACTURE) {
                return "Le document " + facture.getNumero() + " est un devis, pas une facture : "
                        + "aucune relance de paiement ne s'applique.";
            }
            if (facture.getStatut() == StatutFacture.PAYEE) {
                return "La facture " + facture.getNumero() + " est deja soldee. Aucune relance a rediger.";
            }
            InvoiceLateInfo info = InvoiceLateInfo.from(facture, properties.getReminder(), LocalDate.now());
            return info.toPromptBlock();
        });
    }

    @Tool(name = "ListUnpaidInvoicesTool", value = """
            Liste les factures non soldees, de la plus en retard a la plus recente, avec leur \
            identifiant. A utiliser quand l'utilisateur ne precise pas de numero de facture.""")
    public String listUnpaidInvoices() {
        return audit.execute(context, "ListUnpaidInvoicesTool", "", () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireReminderAssistant(principal);

            LocalDate today = LocalDate.now();
            List<InvoiceLateInfo> unpaid = factureRepository
                    .findUnpaidWithClient(TypeDocument.FACTURE, StatutFacture.PAYEE).stream()
                    .map(facture -> InvoiceLateInfo.from(facture, properties.getReminder(), today))
                    .sorted(Comparator.comparingLong(InvoiceLateInfo::joursDeRetard).reversed())
                    .toList();

            if (unpaid.isEmpty()) {
                return "Aucune facture impayee en ce moment.";
            }
            StringBuilder sb = new StringBuilder("Factures non soldees :\n");
            for (InvoiceLateInfo info : unpaid) {
                sb.append("- ").append(info.numero())
                        .append(" (id ").append(info.factureId()).append(")")
                        .append(" | ").append(info.clientNom() == null ? "-" : info.clientNom())
                        .append(" | reste du ").append(String.format(java.util.Locale.FRANCE, "%.3f", info.resteDu()))
                        .append(" DT | ").append(info.joursDeRetard()).append(" jours de retard")
                        .append(" | palier ").append(info.tone().name())
                        .append('\n');
            }
            return sb.toString();
        });
    }

    @Tool(name = "EmailDraftTool", value = """
            Enregistre le brouillon de relance redige (objet + corps) pour validation par \
            l'utilisateur. L'outil n'envoie jamais l'email : l'utilisateur relit le message \
            puis declenche lui-meme l'envoi depuis l'interface.""")
    public String emailDraft(
            @P("Identifiant numerique de la facture concernee") Long factureId,
            @P("Objet de l'email") String objet,
            @P("Corps de l'email, en texte brut") String corps) {
        return audit.execute(context, "EmailDraftTool", "factureId=" + factureId, () -> {
            AuthPrincipal principal = context.principal();
            accessScope.requireReminderAssistant(principal);

            Facture facture = factureRepository.findByIdWithClient(factureId).orElse(null);
            if (facture == null) {
                return "Aucune facture trouvee pour l'identifiant " + factureId + ".";
            }
            InvoiceLateInfo info = InvoiceLateInfo.from(facture, properties.getReminder(), LocalDate.now());

            // Le brouillon est persiste ici : c'est son identifiant qui permettra a
            // l'interface de l'expedier tel quel une fois l'utilisateur d'accord.
            RelanceClient relance = relanceRepository.save(RelanceClient.builder()
                    .facture(facture)
                    .dateRelance(LocalDate.now())
                    .envoyee(false)
                    .objet(objet)
                    .corps(corps)
                    .ton(info.tone().name())
                    .destinataireEmail(info.clientEmail())
                    .destinataireNom(info.destinataireNom() == null ? info.clientNom() : info.destinataireNom())
                    .note("[" + info.tone().name() + " / " + info.joursDeRetard() + " j] "
                            + objet + "\n\n" + corps)
                    .build());

            // Depose pour ChatOrchestratorService : c'est ce qui fait apparaitre le
            // composant riche (aperçu mis en page + bouton d'envoi) sous la reponse du
            // modele, plutot qu'un texte qui se contente de decrire un bouton absent.
            context.structuredResult().set(ReminderResponse.builder()
                    .reminderId(relance.getId())
                    .invoiceId(info.factureId())
                    .invoiceNumero(info.numero())
                    .clientNom(info.clientNom())
                    .clientEmail(info.clientEmail())
                    .subject(objet)
                    .body(corps)
                    .htmlPreview(emailTemplate.render(info, corps))
                    .clientLogoUrl(emailTemplate.clientLogoUrl(info))
                    .tone(info.tone().name())
                    .daysLate(info.joursDeRetard())
                    .amountDue(info.resteDu())
                    .sent(false)
                    .build());

            StringBuilder response = new StringBuilder();
            response.append("Brouillon enregistre (reference ").append(relance.getId())
                    .append(") pour la facture ").append(info.numero()).append(".\n");
            if (info.clientEmail() == null) {
                response.append("Aucune adresse e-mail n'est enregistree pour ")
                        .append(info.clientNom())
                        .append(" : signale-le a l'utilisateur, l'envoi sera impossible ")
                        .append("tant que la fiche client n'est pas completee.\n");
            } else {
                response.append("Destinataire : ").append(info.clientEmail()).append(".\n");
            }
            response.append("Presente maintenant l'objet et le corps complets a l'utilisateur, ")
                    .append("puis indique-lui qu'il peut soit cliquer sur le bouton d'envoi ")
                    .append("sous le brouillon, soit confirmer directement ici pour que tu l'envoies. ")
                    .append("N'affirme jamais que l'email a ete expedie.");
            return response.toString();
        });
    }

    @Tool(name = "ConfirmAndSendReminderTool", value = """
            Expedie reellement par email le brouillon de relance identifie par sa reference \
            (reminderId rendu par EmailDraftTool). A n'appeler que lorsque l'utilisateur vient \
            d'exprimer explicitement son accord pour l'envoi, apres avoir vu le brouillon complet. \
            N'invente jamais de reference : utilise celle rendue par EmailDraftTool dans ce tour \
            ou un tour precedent de la conversation.""")
    public String confirmAndSend(
            @P("Reference numerique du brouillon a expedier (reminderId)") Long reminderId) {
        return audit.execute(context, "ConfirmAndSendReminderTool", "reminderId=" + reminderId, () -> {
            AuthPrincipal principal = context.principal();
            ReminderGenerationService.Result result = reminderGenerationService.send(principal, reminderId);

            // Meme depot que EmailDraftTool : si l'envoi a lieu dans le meme tour que la
            // redaction, le widget doit afficher le badge « Envoye », pas le bouton.
            context.structuredResult().set(ReminderResponse.from(result));

            return "Email envoye avec succes a " + result.clientEmail() + " pour la facture "
                    + result.numero() + ". Confirme-le brievement a l'utilisateur, sans repeter "
                    + "le contenu integral du message deja affiche.";
        });
    }
}
