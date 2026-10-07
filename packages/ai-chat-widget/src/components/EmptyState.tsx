import React from 'react';
import { SparkleIcon } from './Icons';
import type { Capability } from '../api/types';

/**
 * Ecran d'accueil d'une conversation vide.
 *
 * Les suggestions sont propres a la capacite de l'application hote : une page
 * blanche laisse l'utilisateur deviner ce que l'assistant sait faire, alors que
 * trois exemples concrets le lui montrent.
 */
const SUGGESTIONS: Record<Capability, string[]> = {
  PAYSLIP: [
    'Pourquoi mon salaire net a-t-il baissé ce mois-ci ?',
    "Explique-moi les retenues de mon dernier bulletin",
    "À quoi correspond la ligne CNSS sur ma fiche de paie ?",
  ],
  REMINDER: [
    'Quelles factures sont en retard de paiement ?',
    'Rédige une relance pour la facture la plus ancienne',
    'Prépare un rappel courtois pour un retard de 10 jours',
  ],
  MEDIA_PLAN: [
    'Génère le media plan du mois prochain',
    'Quelles thématiques ont déjà été publiées ce trimestre ?',
    'Propose des idées de contenu pour Instagram',
  ],
  GENERAL: [
    'Quelle est la politique de télétravail ?',
    'Combien de jours de congé me reste-t-il ?',
    'Pourquoi mon salaire net a-t-il baissé ce mois-ci ?',
    'Comment demander une autorisation d’absence ?',
  ],
};

const HEADLINE: Record<Capability, string> = {
  PAYSLIP: 'Assistant paie',
  REMINDER: 'Assistant relances',
  MEDIA_PLAN: 'Assistant media plan',
  GENERAL: 'Assistant Antigone',
};

interface EmptyStateProps {
  capability: Capability;
  onPick: (suggestion: string) => void;
  disabled?: boolean;
}

export const EmptyState: React.FC<EmptyStateProps> = ({ capability, onPick, disabled }) => (
  <div className="aicw-empty">
    <div className="aicw-empty-icon">
      <SparkleIcon size={22} />
    </div>
    <p className="aicw-empty-title">{HEADLINE[capability]}</p>
    <p className="aicw-empty-text">
      Posez votre question en langage naturel. L’assistant consulte vos données réelles
      et cite les chiffres tels qu’ils figurent dans l’application.
    </p>
    <div className="aicw-suggestions">
      {SUGGESTIONS[capability].map((suggestion) => (
        <button
          key={suggestion}
          type="button"
          className="aicw-suggestion"
          onClick={() => onPick(suggestion)}
          disabled={disabled}
        >
          {suggestion}
        </button>
      ))}
    </div>
  </div>
);
