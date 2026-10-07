import React, { useCallback, useMemo } from 'react';
import { AiChatWidget, type MediaPlanResultPayload } from '@antigone/ai-chat-widget';
import '@antigone/ai-chat-widget/styles.css';
import { API_BASE } from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../hooks/useTheme';
import { clearAuthSnapshot, getAccessToken } from '../../utils/authStorage';

/**
 * Montage du widget de chat IA, cantonne a la page Media Plan.
 *
 * Contrairement a RH/Finance ou l'assistant est global, il n'est pertinent ici
 * que dans le contexte d'un media plan precis (capacite `MEDIA_PLAN`) : il vit
 * donc dans `MediaPlanPage`, pas dans le layout applicatif.
 *
 * Le widget ne gere aucune authentification : il recoit le jeton via
 * `getAccessToken`, la meme source que l'intercepteur axios de l'application.
 */
export interface MediaPlanAiAssistantProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onShareMediaPlan: (result: MediaPlanResultPayload) => void;
}

const MediaPlanAiAssistant: React.FC<MediaPlanAiAssistantProps> = ({
  open,
  onOpenChange,
  onShareMediaPlan,
}) => {
  const { isAuthenticated, user } = useAuth();
  const { theme } = useTheme();

  const isAdmin = useMemo(() => !!user?.roles?.includes('ADMIN'), [user]);

  const handleUnauthorized = useCallback(() => {
    clearAuthSnapshot();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
  }, []);

  if (!isAuthenticated) return null;

  return (
    <AiChatWidget
      apiBaseUrl={API_BASE}
      getAuthToken={getAccessToken}
      onUnauthorized={handleUnauthorized}
      capability="MEDIA_PLAN"
      isAdmin={isAdmin}
      open={open}
      onOpenChange={onOpenChange}
      onShareMediaPlan={onShareMediaPlan}
      launcherLabel="Assistant Media Plan"
      // Le declencheur vit dans l'entete de la page : pas de lanceur flottant.
      hideLauncher
      theme={{
        mode: theme,
        light: {
          colorPrimary: 'var(--color-brand-500)',
          colorPrimaryText: '#ffffff',
          colorPrimarySoft: 'var(--color-brand-50)',
          fontFamily: 'var(--app-font)',
        },
        dark: {
          colorPrimary: 'var(--color-brand-400)',
          colorPrimaryText: '#1a1814',
          colorPrimarySoft: 'rgba(255, 133, 51, 0.16)',
          fontFamily: 'var(--app-font)',
        },
      }}
    />
  );
};

export default MediaPlanAiAssistant;
