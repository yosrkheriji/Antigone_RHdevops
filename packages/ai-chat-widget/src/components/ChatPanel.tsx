import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Composer, type ComposerHandle } from './Composer';
import { ConversationSidebar } from './ConversationSidebar';
import { ErrorBanner } from './ErrorBanner';
import { MessageList } from './MessageList';
import { CloseIcon, CollapseIcon, ExpandIcon, HistoryIcon, SparkleIcon } from './Icons';
import { AiApiError, presentError } from '../api/errors';
import { useAiClient } from '../hooks/useAiClient';
import { useAiChatStream } from '../hooks/useAiChatStream';
import {
  useConversation,
  useConversations,
  useCreateConversation,
  useDeleteConversation,
  useUpdateConversation,
} from '../hooks/useConversations';
import type { Capability } from '../api/types';

const CAPABILITY_SUBTITLE: Record<Capability, string> = {
  PAYSLIP: 'Paie et bulletins',
  REMINDER: 'Relances clients',
  MEDIA_PLAN: 'Media plan',
  GENERAL: 'Assistance générale',
};

export interface ChatPanelProps {
  open: boolean;
  onClose: () => void;
  /** Message pre-rempli, ex. depuis une fiche facture. Envoye une seule fois. */
  initialMessage?: string | null;
  onInitialMessageConsumed?: () => void;
}

export const ChatPanel: React.FC<ChatPanelProps> = ({
  open,
  onClose,
  initialMessage,
  onInitialMessageConsumed,
}) => {
  const { capability } = useAiClient();
  const [expanded, setExpanded] = useState(false);
  const [showSidebar, setShowSidebar] = useState(false);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [dismissedError, setDismissedError] = useState(false);
  const composerRef = useRef<ComposerHandle | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);

  const conversations = useConversations();
  const conversation = useConversation(activeId);
  const createConversation = useCreateConversation();
  const updateConversation = useUpdateConversation();
  const deleteConversation = useDeleteConversation();

  const { state: stream, send, stop } = useAiChatStream(activeId);

  const listError = conversations.error instanceof AiApiError ? conversations.error : null;
  const activeError = stream.error ?? listError;
  const presentation = activeError ? presentError(activeError) : null;
  const inputBlocked = presentation?.blocksInput ?? false;

  // Selectionne la conversation la plus recente a l'ouverture, ou en cree une :
  // arriver sur un panneau sans conversation active obligerait a un clic de plus
  // avant de pouvoir ecrire.
  useEffect(() => {
    if (!open || activeId !== null || conversations.isLoading) return;
    const existing = conversations.data?.[0];
    if (existing) {
      setActiveId(existing.id);
    } else if (conversations.data && !createConversation.isPending) {
      createConversation.mutate(undefined, {
        onSuccess: (created) => setActiveId(created.id),
      });
    }
  }, [open, activeId, conversations.isLoading, conversations.data, createConversation]);

  useEffect(() => {
    if (open) {
      // Le focus part sur le composer : c'est la seule action attendue a l'ouverture.
      // Sauf si l'utilisateur a deja agi dans le panneau pendant le delai (ex. champ
      // de renommage de l'historique) : lui voler le focus validerait ce champ au blur.
      const timer = window.setTimeout(() => {
        const active = document.activeElement;
        if (active && active !== document.body && panelRef.current?.contains(active)) return;
        composerRef.current?.focus();
      }, 220);
      return () => window.clearTimeout(timer);
    }
    return undefined;
  }, [open]);

  // Echap ferme le panneau, sauf si une generation tourne — la couper par
  // inadvertance couterait un appel LLM deja engage.
  useEffect(() => {
    if (!open) return undefined;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      if (stream.status === 'streaming') return;
      onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose, stream.status]);

  // Message pre-rempli (point d'entree contextuel depuis une fiche facture).
  useEffect(() => {
    if (!open || !initialMessage || activeId === null) return;
    composerRef.current?.setValue(initialMessage);
    onInitialMessageConsumed?.();
  }, [open, initialMessage, activeId, onInitialMessageConsumed]);

  const handleSend = useCallback(
    (content: string) => {
      setDismissedError(false);
      void send(content);
      composerRef.current?.focus();
    },
    [send],
  );

  const handleCreate = useCallback(() => {
    createConversation.mutate(undefined, {
      onSuccess: (created) => {
        setActiveId(created.id);
        composerRef.current?.focus();
      },
    });
  }, [createConversation]);

  const handleDelete = useCallback(
    (id: number) => {
      deleteConversation.mutate(id, {
        onSuccess: () => {
          if (id !== activeId) return;
          // La conversation courante vient de disparaitre : bascule sur une autre
          // plutot que de laisser un panneau vide sans explication.
          const fallback = conversations.data?.find((item) => item.id !== id);
          setActiveId(fallback?.id ?? null);
        },
      });
    },
    [deleteConversation, activeId, conversations.data],
  );

  const sidebarVisible = expanded || showSidebar;

  return (
    <>
      <div
        className="aicw-overlay"
        data-open={open}
        onClick={onClose}
        aria-hidden
      />

      <div
        ref={panelRef}
        className="aicw-panel"
        data-open={open}
        data-expanded={expanded}
        role="dialog"
        aria-modal="false"
        aria-label="Assistant IA Antigone"
        // Retire du parcours de tabulation quand ferme : le panneau reste monte
        // pour ne pas interrompre une generation, il ne doit pas pour autant
        // capter le focus depuis l'application.
        {...(open ? {} : { inert: '' as unknown as boolean })}
      >
        <header className="aicw-header">
          <span className="aicw-launcher-icon" aria-hidden>
            <SparkleIcon size={14} />
          </span>
          <span className="aicw-header-title">
            <span className="aicw-header-name">Assistant Antigone</span>
            <span className="aicw-header-subtitle">{CAPABILITY_SUBTITLE[capability]}</span>
          </span>

          {!expanded && (
            <button
              type="button"
              className="aicw-icon-button"
              onClick={() => setShowSidebar((value) => !value)}
              aria-label="Afficher l’historique"
              aria-pressed={showSidebar}
              title="Historique"
            >
              <HistoryIcon />
            </button>
          )}
          <button
            type="button"
            className="aicw-icon-button"
            onClick={() => setExpanded((value) => !value)}
            aria-label={expanded ? 'Réduire le panneau' : 'Agrandir le panneau'}
            title={expanded ? 'Réduire' : 'Plein écran'}
          >
            {expanded ? <CollapseIcon /> : <ExpandIcon />}
          </button>
          <button
            type="button"
            className="aicw-icon-button"
            onClick={onClose}
            aria-label="Fermer l’assistant"
            title="Fermer"
          >
            <CloseIcon />
          </button>
        </header>

        <div className="aicw-panel-body">
          {sidebarVisible && (
            <ConversationSidebar
              conversations={conversations.data ?? []}
              activeId={activeId}
              loading={conversations.isLoading}
              onSelect={(id) => {
                setActiveId(id);
                if (!expanded) setShowSidebar(false);
              }}
              onCreate={handleCreate}
              onRename={(id, title) => updateConversation.mutate({ id, changes: { title } })}
              onTogglePin={(id, pinned) => updateConversation.mutate({ id, changes: { pinned } })}
              onDelete={handleDelete}
              busy={stream.status === 'streaming'}
            />
          )}

          <div className="aicw-conversation-column">
            {activeError && !dismissedError && (
              <ErrorBanner
                error={activeError}
                onRetry={
                  presentation?.retryable
                    ? () => {
                        setDismissedError(true);
                        void conversations.refetch();
                      }
                    : undefined
                }
                onDismiss={() => setDismissedError(true)}
              />
            )}

            <MessageList
              messages={conversation.data?.messages ?? []}
              stream={stream}
              capability={capability}
              loading={activeId !== null && conversation.isLoading}
              onPickSuggestion={(text) => composerRef.current?.setValue(text)}
              composerDisabled={inputBlocked}
            />

            <Composer
              ref={composerRef}
              onSend={handleSend}
              onStop={stop}
              isStreaming={stream.status === 'streaming'}
              disabled={inputBlocked || activeId === null}
              disabledReason={inputBlocked ? presentation?.title : undefined}
            />
          </div>
        </div>
      </div>
    </>
  );
};
