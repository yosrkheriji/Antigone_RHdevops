import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Virtuoso, type VirtuosoHandle } from 'react-virtuoso';
import { MessageBubble } from './MessageBubble';
import { EmptyState } from './EmptyState';
import { ArrowDownIcon } from './Icons';
import { type StreamState, type ToolCallState } from '../hooks/streamReducer';
import { toolLabel } from '../api/toolLabels';
import type { Capability, ConversationMessage, StructuredResult } from '../api/types';

/**
 * Historique de la conversation.
 *
 * Deux modes de rendu : liste simple en dessous du seuil, virtualisee au-dela.
 * La virtualisation coute un conteneur de defilement supplementaire et complique
 * l'ancrage en bas ; elle n'est donc activee que quand elle rapporte reellement,
 * c'est-a-dire sur les historiques longs.
 *
 * L'auto-defilement suit la generation, sauf si l'utilisateur est remonte lire
 * plus haut — lui arracher le defilement pendant qu'il lit est le defaut le plus
 * agacant de ce type d'interface.
 */
const VIRTUALISATION_THRESHOLD = 50;
/** Marge sous laquelle on considere l'utilisateur « colle » au bas de la liste. */
const STICKY_BOTTOM_PX = 80;

interface DisplayMessage {
  key: string;
  role: ConversationMessage['role'];
  content: string | null;
  structuredResult: StructuredResult | null;
  toolCalls: ToolCallState[];
  createdAt?: string;
}

export interface MessageListProps {
  messages: ConversationMessage[];
  stream: StreamState;
  capability: Capability;
  loading?: boolean;
  onPickSuggestion: (text: string) => void;
  composerDisabled?: boolean;
}

/** Le resultat structure est persiste en JSON : il est relu au rechargement. */
function parseStructured(raw: string | null): StructuredResult | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StructuredResult;
  } catch {
    return null;
  }
}

/**
 * Rejoue les appels d'outils persistes.
 *
 * Les faire disparaitre a la fin du tour effacerait la trace de ce que
 * l'assistant a reellement consulte — precisement l'information qui rend une
 * reponse verifiable. Le backend les conserve, l'interface les conserve aussi.
 */
function parseToolCalls(raw: string | null): ToolCallState[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as Array<{ tool?: string; status?: string }>;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((entry) => typeof entry.tool === 'string')
      .map((entry, index) => ({
        id: `${entry.tool}-${index}`,
        tool: entry.tool as string,
        label: toolLabel(entry.tool as string),
        args: {},
        status: entry.status === 'error' ? ('error' as const) : ('success' as const),
      }));
  } catch {
    return [];
  }
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  stream,
  capability,
  loading = false,
  onPickSuggestion,
  composerDisabled,
}) => {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const virtuosoRef = useRef<VirtuosoHandle | null>(null);
  const [atBottom, setAtBottom] = useState(true);

  /**
   * Historique persiste + tour en cours.
   *
   * Le message utilisateur du tour est ajoute localement sans attendre le serveur :
   * le voir apparaitre immediatement est ce qui rend l'envoi credible.
   */
  const displayed = useMemo<DisplayMessage[]>(() => {
    const history: DisplayMessage[] = messages
      // Les messages d'outil et systeme appartiennent au journal d'audit, pas a
      // la conversation lue par l'utilisateur.
      .filter((message) => message.role === 'USER' || message.role === 'ASSISTANT')
      .map((message) => ({
        key: `msg-${message.id}`,
        role: message.role,
        content: message.content,
        structuredResult: parseStructured(message.structuredResult),
        toolCalls: parseToolCalls(message.toolCalls),
        createdAt: message.createdAt,
      }));

    if (stream.pendingUserMessage) {
      history.push({
        key: 'pending-user',
        role: 'USER',
        content: stream.pendingUserMessage,
        structuredResult: null,
        toolCalls: [],
      });
    }

    const hasLiveAssistant =
      stream.status === 'streaming' ||
      stream.buffer.length > 0 ||
      stream.structured !== null ||
      stream.toolCalls.length > 0;

    if (hasLiveAssistant) {
      history.push({
        key: 'pending-assistant',
        role: 'ASSISTANT',
        content: stream.buffer,
        structuredResult: stream.structured,
        toolCalls: stream.toolCalls,
      });
    }

    return history;
  }, [messages, stream]);

  const isVirtualised = displayed.length > VIRTUALISATION_THRESHOLD;

  // `ScrollBehavior` du DOM inclut 'instant', que react-virtuoso n'accepte pas :
  // on restreint au sous-ensemble commun aux deux modes de rendu.
  const scrollToBottom = useCallback(
    (behavior: 'auto' | 'smooth' = 'smooth') => {
      if (isVirtualised) {
        virtuosoRef.current?.scrollToIndex({ index: 'LAST', behavior, align: 'end' });
      } else if (scrollRef.current) {
        scrollRef.current.scrollTo({ top: scrollRef.current.scrollHeight, behavior });
      }
      setAtBottom(true);
    },
    [isVirtualised],
  );

  const handleScroll = useCallback(() => {
    const node = scrollRef.current;
    if (!node) return;
    const distance = node.scrollHeight - node.scrollTop - node.clientHeight;
    setAtBottom(distance < STICKY_BOTTOM_PX);
  }, []);

  // Suit la generation, mais seulement si l'utilisateur n'a pas remonte.
  useEffect(() => {
    if (!atBottom || isVirtualised) return;
    const node = scrollRef.current;
    if (node) {
      node.scrollTop = node.scrollHeight;
    }
  }, [displayed, stream.buffer, atBottom, isVirtualised]);

  if (loading) {
    return (
      <div className="aicw-messages">
        <div className="aicw-skeleton-list" aria-hidden>
          <div className="aicw-skeleton" style={{ height: '2.5rem', width: '45%', alignSelf: 'flex-end' }} />
          <div className="aicw-skeleton" style={{ height: '5rem', width: '85%' }} />
          <div className="aicw-skeleton" style={{ height: '2.5rem', width: '35%', alignSelf: 'flex-end' }} />
          <div className="aicw-skeleton" style={{ height: '7rem', width: '90%' }} />
        </div>
        <span className="aicw-visually-hidden" role="status">
          Chargement de la conversation…
        </span>
      </div>
    );
  }

  if (displayed.length === 0) {
    return (
      <div className="aicw-messages">
        <EmptyState
          capability={capability}
          onPick={onPickSuggestion}
          disabled={composerDisabled}
        />
      </div>
    );
  }

  const renderMessage = (message: DisplayMessage) => (
    <MessageBubble
      role={message.role}
      content={message.content}
      toolCalls={message.toolCalls}
      structuredResult={message.structuredResult}
      createdAt={message.createdAt}
      streaming={message.key === 'pending-assistant' && stream.status === 'streaming'}
    />
  );

  return (
    <div className="aicw-messages">
      {isVirtualised ? (
        <Virtuoso
          ref={virtuosoRef}
          data={displayed}
          style={{ height: '100%' }}
          className="aicw-messages-scroll"
          followOutput={(isAtBottom) => (isAtBottom ? 'smooth' : false)}
          atBottomStateChange={setAtBottom}
          atBottomThreshold={STICKY_BOTTOM_PX}
          computeItemKey={(_index, message) => message.key}
          itemContent={(_index, message) => renderMessage(message)}
        />
      ) : (
        <div className="aicw-messages-scroll" ref={scrollRef} onScroll={handleScroll}>
          {displayed.map((message) => (
            <React.Fragment key={message.key}>{renderMessage(message)}</React.Fragment>
          ))}
        </div>
      )}

      {/*
        Zone lue par les lecteurs d'ecran. `polite` et non `assertive` : annoncer
        chaque fragment interromprait la lecture en cours a chaque token.
      */}
      <div className="aicw-visually-hidden" aria-live="polite" aria-atomic="false">
        {stream.status === 'streaming' ? stream.buffer : ''}
      </div>

      {!atBottom && (
        <button
          type="button"
          className="aicw-scroll-bottom"
          onClick={() => scrollToBottom()}
        >
          <ArrowDownIcon /> Nouveaux messages
        </button>
      )}
    </div>
  );
};
