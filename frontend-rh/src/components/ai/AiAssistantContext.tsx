import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

/**
 * Ouverture pilotee de l'assistant unique (reglement interieur + paie).
 *
 * Sans ce point d'entree, la barre laterale ne pourrait pas declencher le meme
 * widget que celui utilise partout ailleurs dans l'app : le contexte se contente
 * de transporter l'intention d'ouverture, c'est le widget qui gere la conversation.
 */
interface AiAssistantContextValue {
  open: boolean;
  setOpen: (open: boolean) => void;
  /** Message pre-rempli, consomme une seule fois a l'ouverture. */
  pendingMessage: string | null;
  consumePendingMessage: () => void;
  /** Ouvre l'assistant avec un message deja saisi dans la zone de texte. */
  askAssistant: (message: string) => void;
}

const AiAssistantContext = createContext<AiAssistantContextValue | null>(null);

export const AiAssistantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [open, setOpen] = useState(false);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);

  const askAssistant = useCallback((message: string) => {
    setPendingMessage(message);
    setOpen(true);
  }, []);

  const consumePendingMessage = useCallback(() => setPendingMessage(null), []);

  const value = useMemo<AiAssistantContextValue>(
    () => ({ open, setOpen, pendingMessage, consumePendingMessage, askAssistant }),
    [open, pendingMessage, consumePendingMessage, askAssistant],
  );

  return <AiAssistantContext.Provider value={value}>{children}</AiAssistantContext.Provider>;
};

/**
 * Rend `null` hors du fournisseur plutot que de lever.
 *
 * Une page rendue en dehors du layout (test isole, route d'authentification) doit
 * continuer de fonctionner : l'absence d'assistant n'est pas une erreur, elle
 * signifie simplement qu'il n'y a pas de bouton a proposer.
 */
export function useAiAssistant(): AiAssistantContextValue | null {
  return useContext(AiAssistantContext);
}
