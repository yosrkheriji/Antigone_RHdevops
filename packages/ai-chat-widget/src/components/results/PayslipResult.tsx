import React from 'react';
import { DocumentIcon } from '../Icons';
import type { PayslipResultPayload } from '../../api/types';

/**
 * Explication d'un bulletin de paie.
 *
 * Les montants affiches ici sont ceux de la base : le backend ecrase
 * `previousNet` / `currentNet` / `delta` apres la generation, precisement pour que
 * l'ecran ne puisse pas montrer un chiffre invente par le modele. Ce composant les
 * rend donc tels quels, sans recalcul.
 *
 * Quand `previousNet` est nul, aucun bulletin n'existe pour le mois precedent : on
 * l'annonce clairement plutot que d'afficher un comparatif a moitie vide, qui
 * laisserait croire a une donnee manquante ou a un bug.
 */

const currency = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export const PayslipResult: React.FC<{ result: PayslipResultPayload }> = ({ result }) => {
  const { comparison } = result;
  const hasComparison = comparison.previousNet !== null && comparison.delta !== null;
  const delta = comparison.delta ?? 0;

  return (
    <section className="aicw-result" aria-label="Explication du bulletin de paie">
      <header className="aicw-result-head">
        <DocumentIcon />
        <span className="aicw-result-title">Votre bulletin de paie</span>
      </header>

      <div className="aicw-result-body">
        {hasComparison ? (
          <div className="aicw-stat-row">
            <div className="aicw-stat">
              <span className="aicw-stat-label">Net du mois précédent</span>
              <span className="aicw-stat-value">
                {currency.format(comparison.previousNet as number)} DT
              </span>
            </div>
            <div className="aicw-stat">
              <span className="aicw-stat-label">Net à payer</span>
              <span className="aicw-stat-value">{currency.format(comparison.currentNet)} DT</span>
            </div>
            <div className="aicw-stat">
              <span className="aicw-stat-label">Écart</span>
              <span
                className="aicw-stat-value"
                data-trend={delta > 0 ? 'up' : delta < 0 ? 'down' : undefined}
              >
                {delta > 0 ? '+' : ''}
                {currency.format(delta)} DT
              </span>
            </div>
          </div>
        ) : (
          <>
            <div className="aicw-stat-row">
              <div className="aicw-stat">
                <span className="aicw-stat-label">Net à payer</span>
                <span className="aicw-stat-value">{currency.format(comparison.currentNet)} DT</span>
              </div>
            </div>
            <p className="aicw-notice">
              Aucune comparaison disponible : il n’existe pas de bulletin pour le mois précédent.
            </p>
          </>
        )}

        {comparison.deltaReasons.length > 0 && (
          <>
            <p className="aicw-stat-label" style={{ marginBottom: '0.375rem' }}>
              Ce qui explique l’écart
            </p>
            <ul className="aicw-reason-list">
              {comparison.deltaReasons.map((reason, index) => (
                <li key={`${index}-${reason.slice(0, 24)}`}>{reason}</li>
              ))}
            </ul>
          </>
        )}

        {result.explanation && (
          <p className="aicw-notice" style={{ whiteSpace: 'pre-wrap' }}>
            {result.explanation}
          </p>
        )}
      </div>
    </section>
  );
};
