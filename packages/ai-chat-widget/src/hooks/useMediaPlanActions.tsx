import React, { createContext, useContext } from 'react';
import type { MediaPlanResultPayload } from '../api/types';

/**
 * Transporte l'action « Partager » d'un media plan genere jusqu'a
 * `MediaPlanResult`, sans forcer les composants generiques du fil de messages
 * (`MessageList`, `MessageBubble`, `StructuredResultView`) a connaitre cette
 * capacite : seule l'application hote qui fournit `onShareMediaPlan` a
 * `AiChatWidget` sait ce que « partager » signifie chez elle.
 */
interface MediaPlanActionsValue {
  onShare?: (result: MediaPlanResultPayload) => void;
}

const MediaPlanActionsContext = createContext<MediaPlanActionsValue>({});

export const MediaPlanActionsProvider: React.FC<{
  onShare?: (result: MediaPlanResultPayload) => void;
  children: React.ReactNode;
}> = ({ onShare, children }) => (
  <MediaPlanActionsContext.Provider value={{ onShare }}>{children}</MediaPlanActionsContext.Provider>
);

export function useMediaPlanActions(): MediaPlanActionsValue {
  return useContext(MediaPlanActionsContext);
}
