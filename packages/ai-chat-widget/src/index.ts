/**
 * API publique de @antigone/ai-chat-widget.
 *
 * Volontairement restreinte : plus la surface exportee est large, plus une
 * evolution interne risque de casser une application hote.
 */

// Point d'entree principal
export { AiChatWidget } from './components/AiChatWidget';
export type { AiChatWidgetProps } from './components/AiChatWidget';

// Theming
export { ChatThemeProvider, useChatTheme } from './theme/ChatThemeProvider';
export {
  ANTIGONE_DARK,
  ANTIGONE_LIGHT,
  DEFAULT_THEME,
  mergeTheme,
  toCssVariables,
} from './theme/tokens';
export type { ChatTheme, ChatThemeTokens } from './theme/tokens';

// Types du contrat backend
export type {
  AiErrorBody,
  AiErrorCode,
  AiStreamEvent,
  Capability,
  ConversationDetail,
  ConversationMessage,
  ConversationSummary,
  MediaPlanItem,
  MediaPlanResultPayload,
  MessageRole,
  Page,
  PayslipComparison,
  PayslipResultPayload,
  ReminderResultPayload,
  ReminderTone,
  StructuredResult,
} from './api/types';

// Client et erreurs, exposes pour un hote qui voudrait appeler l'API hors du chat
export { AiChatClient } from './api/client';
export type { AiClientOptions } from './api/client';
export { AiApiError, presentError } from './api/errors';
export type { ErrorPresentation } from './api/errors';

// Composants de resultats, reutilisables hors conversation
// (par exemple pour rejouer une relance depuis une fiche facture)
export {
  MediaPlanResult,
  PayslipResult,
  ReminderResult,
  StructuredResultView,
} from './components/results';

// Briques internes utiles a un hote qui composerait sa propre disposition
export { ChatPanel } from './components/ChatPanel';
export { AiClientProvider, useAiClient, aiQueryKeys } from './hooks/useAiClient';
export { useAiChatStream } from './hooks/useAiChatStream';
export {
  useConversation,
  useConversations,
  useCreateConversation,
  useDeleteConversation,
  useUpdateConversation,
} from './hooks/useConversations';
