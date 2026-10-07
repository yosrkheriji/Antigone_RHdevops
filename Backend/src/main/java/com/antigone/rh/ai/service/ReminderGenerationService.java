package com.antigone.rh.ai.service;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.dto.ReminderDraft;
import com.antigone.rh.ai.exception.AiUnavailableException;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.entity.RelanceClient;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.repository.FactureRepository;
import com.antigone.rh.repository.RelanceClientRepository;
import com.antigone.rh.security.AuthPrincipal;
import com.antigone.rh.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Redaction et envoi des relances clients.
 *
 * <p>Le palier de ton (SOFT / FIRM / FORMAL) est derive des jours de retard reels
 * par {@link InvoiceLateInfo} et impose au modele. Laisser le LLM apprecier lui-meme
 * la fermete d'un courrier commercial rendrait le ton non reproductible d'un appel
 * a l'autre.
 *
 * <p><b>Deux temps, deliberement.</b> {@link #generate} redige et persiste un
 * brouillon ; {@link #send} l'expedie. Le modele n'envoie jamais rien de lui-meme :
 * la validation humaine est le clic explicite de l'utilisateur sur le brouillon
 * qu'il vient de lire, ce qui est plus sur qu'un indicateur de configuration — et,
 * contrairement a lui, cela permet reellement d'envoyer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderGenerationService {

    public record Result(Long relanceId,
                         Long factureId,
                         String numero,
                         String clientNom,
                         String clientEmail,
                         String subject,
                         String body,
                         /** Corps mis en page, tel qu'il sera recu par le client. */
                         String htmlPreview,
                         /** Logo du client destinataire, deja affiche dans htmlPreview ; null si absent. */
                         String clientLogoUrl,
                         InvoiceLateInfo.Tone tone,
                         long joursDeRetard,
                         double resteDu,
                         boolean sent) {
    }

    private final FactureRepository factureRepository;
    private final RelanceClientRepository relanceRepository;
    private final EmailService emailService;
    private final AiAccessScope accessScope;
    private final AiProperties properties;
    private final ReminderEmailTemplate emailTemplate;
    private final ObjectProvider<StructuredAgents.ReminderStructuredAgent> agentProvider;

    /**
     * Redige un brouillon de relance et le persiste.
     *
     * <p>N'envoie rien : l'identifiant rendu sert a l'expedier ensuite, une fois
     * l'utilisateur d'accord avec le texte.
     */
    @Transactional
    public Result generate(AuthPrincipal principal, Long factureId) {
        accessScope.requireReminderAssistant(principal);

        StructuredAgents.ReminderStructuredAgent agent = agentProvider.getIfAvailable();
        if (agent == null) {
            throw new AiUnavailableException("L'assistant IA n'est pas configure (cle API manquante).");
        }

        Facture facture = requireUnpaidInvoice(factureId);
        InvoiceLateInfo info = InvoiceLateInfo.from(facture, properties.getReminder(), LocalDate.now());
        ReminderDraft draft = agent.draft(info.toPromptBlock());

        RelanceClient relance = relanceRepository.save(RelanceClient.builder()
                .facture(facture)
                .dateRelance(LocalDate.now())
                .envoyee(false)
                .objet(draft.getSubject())
                .corps(draft.getBody())
                .ton(info.tone().name())
                .destinataireEmail(info.clientEmail())
                .destinataireNom(info.destinataireNom() == null ? info.clientNom() : info.destinataireNom())
                // Conserve dans le champ metier historique, pour rester lisible depuis
                // l'ecran Finance sans passer par l'assistant.
                .note("[" + info.tone().name() + " / " + info.joursDeRetard() + " j] "
                        + draft.getSubject() + "\n\n" + draft.getBody())
                .build());

        return toResult(relance, info, draft.getSubject(), draft.getBody(), false);
    }

    /**
     * Expedie un brouillon precedemment valide par l'utilisateur.
     *
     * <p>Le texte envoye est celui enregistre, jamais une regeneration : l'utilisateur
     * doit recevoir exactement ce qu'il a relu. Une relance deja partie n'est pas
     * renvoyee — le client recevrait deux fois le meme rappel.
     */
    @Transactional
    public Result send(AuthPrincipal principal, Long relanceId) {
        accessScope.requireReminderAssistant(principal);

        RelanceClient relance = relanceRepository.findById(relanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Relance", relanceId));

        Facture facture = relance.getFacture();
        InvoiceLateInfo info = InvoiceLateInfo.from(facture, properties.getReminder(), LocalDate.now());

        if (Boolean.TRUE.equals(relance.getEnvoyee())) {
            log.info("Relance {} deja envoyee le {} : nouvel envoi ignore",
                    relanceId, relance.getDateEnvoi());
            return toResult(relance, info, relance.getObjet(), relance.getCorps(), true);
        }
        if (relance.getDestinataireEmail() == null || relance.getDestinataireEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Aucune adresse e-mail n'est enregistree pour " + info.clientNom()
                            + " : renseignez-la dans sa fiche client avant d'envoyer.");
        }

        emailService.sendHtml(
                relance.getDestinataireEmail(),
                relance.getObjet(),
                emailTemplate.render(info, relance.getCorps()),
                relance.getDestinataireNom());

        relance.setEnvoyee(true);
        relance.setDateEnvoi(LocalDateTime.now());
        relanceRepository.save(relance);

        log.info("Relance {} envoyee pour la facture {} a {}",
                relanceId, info.numero(), relance.getDestinataireEmail());

        return toResult(relance, info, relance.getObjet(), relance.getCorps(), true);
    }

    private Facture requireUnpaidInvoice(Long factureId) {
        Facture facture = factureRepository.findByIdWithClient(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));
        if (facture.getType() != TypeDocument.FACTURE) {
            throw new IllegalArgumentException(
                    "Le document " + facture.getNumero() + " est un devis : aucune relance de paiement.");
        }
        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new IllegalArgumentException(
                    "La facture " + facture.getNumero() + " est deja soldee : aucune relance a rediger.");
        }
        return facture;
    }

    private Result toResult(RelanceClient relance, InvoiceLateInfo info,
                            String subject, String body, boolean sent) {
        return new Result(
                relance.getId(),
                info.factureId(),
                info.numero(),
                info.clientNom(),
                relance.getDestinataireEmail(),
                subject,
                body,
                emailTemplate.render(info, body),
                emailTemplate.clientLogoUrl(info),
                info.tone(),
                info.joursDeRetard(),
                info.resteDu(),
                sent);
    }
}
