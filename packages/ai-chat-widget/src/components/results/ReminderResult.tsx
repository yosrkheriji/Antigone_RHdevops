import React, { useState } from 'react';
import { AlertIcon, CheckIcon, CopyIcon, DocumentIcon, MailIcon, SendIcon } from '../Icons';
import { AiApiError, presentError } from '../../api/errors';
import { useAiClient } from '../../hooks/useAiClient';
import type { ReminderResultPayload, ReminderTone } from '../../api/types';

/**
 * Relance client generee.
 *
 * <p>L'assistant redige, l'utilisateur relit, puis <em>lui</em> declenche l'envoi
 * depuis ce composant. C'est la validation humaine exigee sur un courrier qui
 * engage la relation commerciale : elle vient de quelqu'un qui a lu le texte, pas
 * d'un indicateur de configuration.
 *
 * <p>Le message expedie est celui affiche ici, sans regeneration cote serveur : ce
 * que l'utilisateur valide est exactement ce que le client recevra.
 */

const TONE_LABEL: Record<ReminderTone, string> = {
  SOFT: 'Ton amical',
  FIRM: 'Ton ferme',
  FORMAL: 'Ton formel',
};

const TONE_HINT: Record<ReminderTone, string> = {
  SOFT: 'Retard récent — simple oubli supposé',
  FIRM: 'Échéance dépassée — une date de règlement est demandée',
  FORMAL: 'Retard important — les suites possibles sont évoquées',
};

const currency = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export const ReminderResult: React.FC<{ result: ReminderResultPayload }> = ({ result }) => {
  const { client } = useAiClient();
  const [copied, setCopied] = useState(false);
  // Etat derive plutot que copie : si le parent fournit un resultat deja marque
  // envoye (rechargement d'une conversation), l'affichage le reflete sans avoir a
  // synchroniser un etat local sur la prop.
  const [sentLocally, setSentLocally] = useState(false);
  const sent = result.sent || sentLocally;
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);

  const fullMessage = `${result.subject}\n\n${result.body}`;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(fullMessage);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // Presse-papiers refuse (contexte non securise, permission) : l'utilisateur
      // peut toujours selectionner le texte, inutile de l'alarmer.
    }
  };

  const send = async () => {
    if (sending || sent || !result.reminderId) return;
    setSending(true);
    setSendError(null);
    try {
      const updated = await client.sendReminder(result.reminderId);
      setSentLocally(updated.sent);
    } catch (cause) {
      setSendError(
        cause instanceof AiApiError
          ? presentError(cause).description
          : "L’envoi a échoué. Réessayez ou copiez le message dans votre client mail.",
      );
    } finally {
      setSending(false);
    }
  };

  const mailtoHref = result.clientEmail
    ? `mailto:${encodeURIComponent(result.clientEmail)}?subject=${encodeURIComponent(
        result.subject,
      )}&body=${encodeURIComponent(result.body)}`
    : null;

  const canSend = !!result.reminderId && !!result.clientEmail && !sent;

  return (
    <section className="aicw-result" aria-label={`Relance pour la facture ${result.invoiceNumero}`}>
      <header className="aicw-result-head">
        <DocumentIcon />
        <span className="aicw-result-title">Relance — {result.invoiceNumero}</span>
        {sent ? (
          <span className="aicw-badge" data-variant="sent">
            <CheckIcon size={11} /> Envoyé
          </span>
        ) : (
          <span className="aicw-badge" data-tone={result.tone} title={TONE_HINT[result.tone]}>
            {TONE_LABEL[result.tone]}
          </span>
        )}
      </header>

      <div className="aicw-result-body">
        <div className="aicw-stat-row">
          <div className="aicw-stat">
            <span className="aicw-stat-label">Retard</span>
            <span className="aicw-stat-value" data-trend={result.daysLate > 30 ? 'down' : undefined}>
              {result.daysLate} j
            </span>
          </div>
          <div className="aicw-stat">
            <span className="aicw-stat-label">Reste dû</span>
            <span className="aicw-stat-value">{currency.format(result.amountDue)} DT</span>
          </div>
          {result.clientNom && (
            <div className="aicw-stat">
              <span className="aicw-stat-label">Client</span>
              <span className="aicw-stat-value" style={{ fontSize: '0.8125rem' }}>
                {result.clientNom}
              </span>
            </div>
          )}
        </div>

        <div className="aicw-email-preview">
          <div className="aicw-email-field">
            <span className="aicw-email-field-label">À</span>
            <span className="aicw-email-field-value">
              {result.clientEmail ?? 'Aucune adresse enregistrée'}
            </span>
          </div>
          <div className="aicw-email-field">
            <span className="aicw-email-field-label">Objet</span>
            <span className="aicw-email-field-value">{result.subject}</span>
          </div>

          {/*
            Le bandeau reprend la charte de l'email reellement expedie : l'utilisateur
            valide une mise en page, pas seulement un texte brut.
          */}
          <div className="aicw-email-brandbar" data-tone={result.tone}>
            <span className="aicw-email-brandbar-name">Antigone</span>
            <span className="aicw-email-brandbar-sub">
              {currency.format(result.amountDue)} DT · facture {result.invoiceNumero}
            </span>
          </div>

          {result.clientNom && (
            <div className="aicw-email-personalization">
              {result.clientLogoUrl && (
                <img
                  className="aicw-email-personalization-logo"
                  src={result.clientLogoUrl}
                  alt={result.clientNom}
                />
              )}
              <span className="aicw-email-personalization-name">{result.clientNom}</span>
            </div>
          )}

          <div className="aicw-email-body">{result.body}</div>
        </div>

        <div className="aicw-result-actions">
          {canSend && (
            <button
              type="button"
              className="aicw-button"
              data-variant="primary"
              onClick={send}
              disabled={sending}
            >
              {sending ? <span className="aicw-spinner" aria-hidden /> : <SendIcon size={13} />}
              {sending ? 'Envoi en cours…' : 'Envoyer au client'}
            </button>
          )}
          <button type="button" className="aicw-button" onClick={copy}>
            {copied ? <CheckIcon size={13} /> : <CopyIcon />}
            {copied ? 'Copié' : 'Copier le message'}
          </button>
          {mailtoHref && (
            <a className="aicw-button" href={mailtoHref}>
              <MailIcon /> Ouvrir dans le client mail
            </a>
          )}
        </div>

        {sendError && (
          <p className="aicw-notice" style={{ color: 'var(--aicw-danger)' }} role="alert">
            <AlertIcon size={12} /> {sendError}
          </p>
        )}

        {sent && (
          <p className="aicw-notice">
            Message expédié à {result.clientEmail}. Il reste consultable depuis l’historique des
            relances de la facture.
          </p>
        )}

        {!sent && !result.clientEmail && (
          <p className="aicw-notice">
            Aucune adresse e-mail n’est enregistrée pour ce client — renseignez-la dans sa fiche
            pour pouvoir lui écrire directement, ou copiez le message ci-dessus.
          </p>
        )}

        {!sent && result.clientEmail && (
          <p className="aicw-notice">
            Relisez le message avant de l’envoyer : il partira tel quel, mis en page aux couleurs
            d’Antigone.
          </p>
        )}
      </div>
    </section>
  );
};
