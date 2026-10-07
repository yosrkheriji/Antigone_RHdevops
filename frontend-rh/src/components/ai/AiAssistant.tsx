import React, { useCallback, useMemo } from 'react';
import { AiChatWidget } from '@antigone/ai-chat-widget';
import { API_BASE } from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../hooks/useTheme';
import { clearAuthSnapshot, getAccessToken } from '../../utils/authStorage';
import { useAiAssistant } from './AiAssistantContext';

/**
 * Montage du widget de chat IA dans l'application.
 *
 * Assistant unique : il remplace a la fois l'ancien « Assistant RH » (chatbot
 * Groq/Llama distinct, base sur le reglement interieur) et le widget « Assistant
 * paie ». La capacite GENERAL couvre deja les questions RH generales (conges,
 * reglement interieur) et peut mobiliser les outils de paie a la demande : le
 * classement d'intention du backend route chaque message independamment de cette
 * valeur, qui ne sert ici qu'a orienter les suggestions affichees a l'ouverture.
 *
 * Le widget ne gere aucune authentification : il recoit le jeton via
 * `getAccessToken`, la meme source que l'intercepteur axios de l'application. Une
 * seule chaine d'auth existe donc, et le widget n'en cree pas une seconde.
 *
 * Les tokens de theme clairs pointent vers les variables CSS de l'application : si
 * la charte evolue, le chat suit sans modification. Les tokens sombres sont en
 * revanche explicites — l'audit a montre que les variables `:root` d'Antigone ne
 * basculent pas en mode sombre (seuls les utilitaires Tailwind `dark:` le font),
 * les consommer telles quelles donnerait un chat clair sur une application sombre.
 */
const AiAssistant: React.FC = () => {
  const { isAuthenticated, user } = useAuth();
  const { theme } = useTheme();
  const assistant = useAiAssistant();

  const isAdmin = useMemo(
    () => !!user?.roles?.includes('ADMIN') || !!user?.permissions?.includes('VIEW_FINANCE'),
    [user],
  );

  /**
   * Jeton absent ou expire : on purge et on renvoie vers la connexion, exactement
   * comme l'intercepteur axios. Le backend repondant 403 (et non 401) dans ce cas,
   * cet intercepteur ne se declenche pas : le widget comble ce trou pour son
   * propre perimetre.
   */
  const handleUnauthorized = useCallback(() => {
    clearAuthSnapshot();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
  }, []);

  // Rien a afficher tant que l'utilisateur n'est pas connecte.
  if (!isAuthenticated) return null;

  return (
    <AiChatWidget
      apiBaseUrl={API_BASE}
      getAuthToken={getAccessToken}
      onUnauthorized={handleUnauthorized}
      capability="GENERAL"
      isAdmin={isAdmin}
      open={assistant?.open}
      onOpenChange={assistant?.setOpen}
      initialMessage={assistant?.pendingMessage}
      onInitialMessageConsumed={assistant?.consumePendingMessage}
      launcherLabel="Assistant RH"
      // Le declencheur vit dans la barre laterale (« Assistant RH ») : le lanceur
      // flottant par defaut aurait duplique cette entree.
      hideLauncher
      theme={{
        mode: theme,
        light: {
          colorPrimary: 'var(--brand)',
          colorPrimaryText: '#ffffff',
          colorPrimarySoft: 'var(--brand-light)',
          colorBackground: 'var(--bg)',
          colorSurface: 'var(--surface)',
          colorBorder: 'var(--border)',
          colorTextPrimary: 'var(--text-1)',
          colorTextSecondary: 'var(--text-2)',
          colorTextMuted: 'var(--text-3)',
          fontFamily: 'var(--app-font)',
          radiusMd: 'var(--radius-md)',
          radiusLg: 'var(--radius-lg)',
          shadow: 'var(--shadow-md)',
        },
        dark: {
          colorPrimary: 'var(--brand-mid)',
          colorPrimaryText: '#150b1a',
          colorPrimarySoft: 'rgba(171, 120, 195, 0.16)',
          fontFamily: 'var(--app-font)',
          radiusMd: 'var(--radius-md)',
          radiusLg: 'var(--radius-lg)',
        },
      }}
    />
  );
};

export default AiAssistant;
