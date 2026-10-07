import React, { useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { StructuredResultView } from './results';
import { ToolCallIndicator } from './ToolCallIndicator';
import type { ToolCallState } from '../hooks/streamReducer';
import type { MessageRole, StructuredResult } from '../api/types';

/**
 * Une bulle de message.
 *
 * Le markdown n'est rendu que pour l'assistant : le texte utilisateur est affiche
 * tel quel. Le rendre en markdown transformerait des caracteres anodins (un
 * asterisque, un souligne) en mise en forme non voulue, et melangerait du contenu
 * saisi par l'utilisateur avec du contenu interprete.
 */
export interface MessageBubbleProps {
  role: MessageRole;
  content: string | null;
  toolCalls?: ToolCallState[];
  structuredResult?: StructuredResult | null;
  createdAt?: string;
  /** Affiche le curseur de frappe tant que le flux n'est pas clos. */
  streaming?: boolean;
}

const timeFormatter = new Intl.DateTimeFormat('fr-FR', {
  hour: '2-digit',
  minute: '2-digit',
});

function formatTime(value?: string): string | null {
  if (!value) return null;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : timeFormatter.format(parsed);
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({
  role,
  content,
  toolCalls = [],
  structuredResult,
  createdAt,
  streaming = false,
}) => {
  const time = useMemo(() => formatTime(createdAt), [createdAt]);
  const isAssistant = role === 'ASSISTANT';
  const hasText = !!content && content.trim().length > 0;

  return (
    <div className="aicw-message" data-role={role}>
      {isAssistant && toolCalls.length > 0 && <ToolCallIndicator calls={toolCalls} />}

      {(hasText || streaming) && (
        <div className="aicw-bubble" data-role={role}>
          {isAssistant ? (
            <div className="aicw-markdown">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                // `ignoreMissing` : un bloc de code sans langage, ou dans un langage
                // inconnu, ne doit pas faire echouer le rendu de toute la reponse.
                rehypePlugins={[[rehypeHighlight, { ignoreMissing: true, detect: true }]]}
                components={{
                  // Les liens produits par le modele partent vers l'exterieur :
                  // `noopener` evite qu'ils accedent a la fenetre d'origine.
                  a: ({ node: _node, ...props }) => (
                    <a {...props} target="_blank" rel="noopener noreferrer" />
                  ),
                }}
              >
                {content ?? ''}
              </ReactMarkdown>
              {streaming && <span className="aicw-caret" aria-hidden />}
            </div>
          ) : (
            content
          )}
        </div>
      )}

      {structuredResult && <StructuredResultView result={structuredResult} />}

      {time && !streaming && <span className="aicw-message-meta">{time}</span>}
    </div>
  );
};
