import React from 'react';
import { AlertIcon, CalendarIcon, ClockIcon, DocumentIcon, ShareIcon } from '../Icons';
import { useMediaPlanActions } from '../../hooks/useMediaPlanActions';
import type { MediaPlanResultPayload } from '../../api/types';

/**
 * Media plan genere, presente comme une grille de cartes.
 *
 * Les champs suivent l'entite reelle (`texteSurVisuel`, `autresElements`,
 * `platforme`, `etatPublication`) et non les libelles generiques du cahier des
 * charges initial, conformement a `API_DOCUMENTATION.md`.
 *
 * Le bouton « Partager » n'apparait que si l'hote a fourni `onShareMediaPlan` a
 * `AiChatWidget` (cf. `useMediaPlanActions`) : RH et Finance, qui ne rendent pas
 * encore cette capacite, n'ont rien a faire pour continuer a ne pas l'afficher.
 */
export const MediaPlanResult: React.FC<{ result: MediaPlanResultPayload }> = ({ result }) => {
  const { onShare } = useMediaPlanActions();

  return (
    <section className="aicw-result aicw-mp-result" aria-label={`Media plan de ${result.clientNom} pour ${result.month}`}>
      <header className="aicw-result-head">
        <DocumentIcon />
        <span className="aicw-result-title">
          {result.clientNom} — {result.month}
        </span>
        <span className="aicw-badge" data-tone="SOFT">
          {result.items.length} publication{result.items.length > 1 ? 's' : ''}
        </span>
      </header>

      <div className="aicw-result-body">
        {result.syntheseEditoriale && (
          <p className="aicw-notice" style={{ marginTop: 0, marginBottom: '0.75rem' }}>
            {result.syntheseEditoriale}
          </p>
        )}

        {result.drivePending && (
          <p className="aicw-notice" style={{ marginTop: 0, marginBottom: '0.75rem' }}>
            <AlertIcon size={12} /> Google Drive était indisponible : les liens sont marqués
            « PENDING » et seront approvisionnés lors d’une reprise. Aucun contenu n’est perdu.
          </p>
        )}

        <div className="aicw-mp-grid">
          {result.items.map((item) => (
            <article key={item.id} className="aicw-mp-card">
              <div className="aicw-mp-card-head">
                <span className="aicw-mp-card-date">
                  <CalendarIcon size={12} /> {item.datePublication}
                  {item.heure && (
                    <>
                      <span className="aicw-mp-card-dot" aria-hidden>
                        ·
                      </span>
                      <ClockIcon size={12} /> {item.heure}
                    </>
                  )}
                </span>
                {item.platforme && <span className="aicw-mp-pill">{item.platforme}</span>}
              </div>

              <h3 className="aicw-mp-card-title">{item.titre}</h3>

              {(item.format || item.type) && (
                <div className="aicw-mp-card-tags">
                  {item.format && <span className="aicw-mp-tag">{item.format}</span>}
                  {item.type && <span className="aicw-mp-tag">{item.type}</span>}
                </div>
              )}

              {item.texteSurVisuel && (
                <p className="aicw-mp-card-field">
                  <span className="aicw-mp-card-field-label">Texte sur visuel</span>
                  {item.texteSurVisuel}
                </p>
              )}

              {item.inspiration && (
                <p className="aicw-mp-card-field">
                  <span className="aicw-mp-card-field-label">Inspiration</span>
                  {item.inspiration}
                </p>
              )}

              {item.autresElements && (
                <p className="aicw-mp-card-field">
                  <span className="aicw-mp-card-field-label">Autres éléments</span>
                  {item.autresElements}
                </p>
              )}
            </article>
          ))}
        </div>

        {result.thematiquesEvitees.length > 0 && (
          <>
            <p className="aicw-stat-label" style={{ margin: '0.875rem 0 0.375rem' }}>
              Thématiques écartées pour éviter les répétitions
            </p>
            <ul className="aicw-reason-list">
              {result.thematiquesEvitees.map((theme) => (
                <li key={theme}>{theme}</li>
              ))}
            </ul>
          </>
        )}

        {onShare && (
          <div className="aicw-result-actions">
            <button type="button" className="aicw-button" data-variant="primary" onClick={() => onShare(result)}>
              <ShareIcon size={13} /> Partager vers le Media Plan
            </button>
          </div>
        )}
      </div>
    </section>
  );
};
