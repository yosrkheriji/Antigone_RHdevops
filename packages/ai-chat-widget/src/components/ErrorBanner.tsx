import React, { useEffect, useState } from 'react';
import { AlertIcon } from './Icons';
import { presentError, type AiApiError } from '../api/errors';

/**
 * Restitution d'une erreur, avec l'action qui a du sens pour ce code precis.
 *
 * Le compte a rebours du quota est decompte ici plutot que fige : afficher
 * « reessayez dans 34 secondes » sans que le nombre bouge laisse l'utilisateur
 * relancer trop tot et se heurter au meme refus.
 */
interface ErrorBannerProps {
  error: AiApiError;
  onRetry?: () => void;
  onDismiss?: () => void;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({ error, onRetry, onDismiss }) => {
  const presentation = presentError(error);
  const [remaining, setRemaining] = useState(error.retryAfterSeconds ?? 0);

  useEffect(() => {
    setRemaining(error.retryAfterSeconds ?? 0);
  }, [error]);

  useEffect(() => {
    if (remaining <= 0) return;
    const timer = window.setInterval(() => setRemaining((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [remaining]);

  const description =
    error.code === 'RATE_LIMITED' && remaining > 0
      ? `Vous avez atteint la limite d’appels à l’assistant. Réessayez dans ${remaining} seconde${remaining > 1 ? 's' : ''}.`
      : presentation.description;

  return (
    <div className="aicw-banner" data-severity={presentation.severity} role="alert">
      <AlertIcon size={15} />
      <div className="aicw-banner-content">
        <div className="aicw-banner-title">{presentation.title}</div>
        <div>{description}</div>
        {(presentation.retryable || onDismiss) && (
          <div className="aicw-result-actions">
            {presentation.retryable && onRetry && (
              <button type="button" className="aicw-button" onClick={onRetry}>
                Réessayer
              </button>
            )}
            {onDismiss && (
              <button type="button" className="aicw-button" onClick={onDismiss}>
                Fermer
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
