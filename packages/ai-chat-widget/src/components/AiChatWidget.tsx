import React, { Suspense, useCallback, useEffect, useMemo, useState } from 'react';

import { SparkleIcon } from './Icons';
import { AiChatClient } from '../api/client';
import { AiClientProvider } from '../hooks/useAiClient';
import { MediaPlanActionsProvider } from '../hooks/useMediaPlanActions';
import { ChatThemeProvider } from '../theme/ChatThemeProvider';
import { mergeTheme, type ChatTheme, type ChatThemeTokens } from '../theme/tokens';
import type { Capability, MediaPlanResultPayload } from '../api/types';
import '../styles/widget.css';

/**
 * Le panneau est charge a la demande.
 *
 * Il tire le rendu markdown, la coloration syntaxique et la virtualisation, soit
 * l'essentiel du poids du widget. Les charger au demarrage ferait payer ce cout a
 * tous les utilisateurs, y compris a ceux qui n'ouvrent jamais l'assistant. Le
 * lanceur, lui, ne pese que quelques kilo-octets.
 */
const ChatPanel = React.lazy(() =>
  import('./ChatPanel').then((module) => ({ default: module.ChatPanel })),
);

/**
 * Point d'entree unique du widget.
 *
 * Monte le fournisseur de theme, le client HTTP, le lanceur et le panneau. Une
 * application hote n'a qu'a poser ce composant dans son layout et lui passer sa
 * charte, sa capacite et son mecanisme d'authentification.
 */
export interface AiChatWidgetProps {
  /** Racine de l'API, ex. `http://localhost:8080`. */
  apiBaseUrl: string;
  /** Delegue a l'authentification existante de l'application. */
  getAuthToken: () => string | null | undefined;
  /** Appele sur un 403 sans corps : jeton absent ou expire. */
  onUnauthorized?: () => void;

  /** Capacite metier de l'application hote. Sert l'affichage, pas le routage. */
  capability: Capability;

  /**
   * Surcharge de theme. Partielle : ne redefinir que ce qui differe de la charte
   * Antigone par defaut.
   */
  theme?: {
    light?: Partial<ChatThemeTokens>;
    dark?: Partial<ChatThemeTokens>;
    mode?: ChatTheme['mode'];
  };

  /** Vrai si le compte connecte est administrateur. Defense en profondeur uniquement. */
  isAdmin?: boolean;

  /** Masque le lanceur flottant quand l'hote fournit son propre declencheur. */
  hideLauncher?: boolean;
  launcherLabel?: string;

  /** Ouverture pilotee par l'hote (bouton « Générer une relance » sur une fiche facture). */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  initialMessage?: string | null;
  onInitialMessageConsumed?: () => void;

  /**
   * Bouton « Partager » sur un media plan genere. Absent = bouton masque : les
   * hotes qui ne rendent pas encore cette capacite (RH, Finance) n'ont rien a
   * faire pour continuer a ne pas l'afficher.
   */
  onShareMediaPlan?: (result: MediaPlanResultPayload) => void;
}

export const AiChatWidget: React.FC<AiChatWidgetProps> = ({
  apiBaseUrl,
  getAuthToken,
  onUnauthorized,
  capability,
  theme,
  isAdmin = false,
  hideLauncher = false,
  launcherLabel = 'Assistant IA',
  open: controlledOpen,
  onOpenChange,
  initialMessage,
  onInitialMessageConsumed,
  onShareMediaPlan,
}) => {
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
  /** Passe a vrai a la premiere ouverture et n'en revient pas : cf. commentaire du panneau. */
  const [hasOpened, setHasOpened] = useState(false);

  // Composant controle ou non selon que l'hote pilote l'ouverture.
  const isControlled = controlledOpen !== undefined;
  const open = isControlled ? controlledOpen : uncontrolledOpen;

  const setOpen = useCallback(
    (next: boolean) => {
      if (!isControlled) setUncontrolledOpen(next);
      onOpenChange?.(next);
    },
    [isControlled, onOpenChange],
  );

  useEffect(() => {
    if (open) setHasOpened(true);
  }, [open]);

  const client = useMemo(
    () => new AiChatClient({ baseUrl: apiBaseUrl, getAuthToken, onUnauthorized }),
    [apiBaseUrl, getAuthToken, onUnauthorized],
  );

  const resolvedTheme = useMemo(() => mergeTheme(theme), [theme]);

  return (
    <ChatThemeProvider theme={resolvedTheme}>
      <AiClientProvider client={client} capability={capability} isAdmin={isAdmin}>
        <MediaPlanActionsProvider onShare={onShareMediaPlan}>
          {!hideLauncher && !open && (
            <button
              type="button"
              className="aicw-launcher"
              onClick={() => setOpen(true)}
              aria-label="Ouvrir l’assistant IA"
            >
              <span className="aicw-launcher-icon" aria-hidden>
                <SparkleIcon size={14} />
              </span>
              {launcherLabel}
            </button>
          )}

          {/*
            Une fois ouvert, le panneau reste monte : le demonter a la fermeture
            interromprait une generation en cours, qui peut durer une minute et a
            deja ete payee au modele. Il n'est donc monte qu'a la premiere ouverture,
            et plus jamais demonte ensuite.
          */}
          {hasOpened && (
            <Suspense fallback={null}>
              <ChatPanel
                open={open}
                onClose={() => setOpen(false)}
                initialMessage={initialMessage}
                onInitialMessageConsumed={onInitialMessageConsumed}
              />
            </Suspense>
          )}
        </MediaPlanActionsProvider>
      </AiClientProvider>
    </ChatThemeProvider>
  );
};
