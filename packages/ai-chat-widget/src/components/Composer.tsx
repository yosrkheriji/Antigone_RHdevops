import React, { forwardRef, useCallback, useImperativeHandle, useLayoutEffect, useRef, useState } from 'react';
import { SendIcon, StopIcon } from './Icons';

/**
 * Zone de saisie.
 *
 * Le texte est en etat local et non remonte au parent : le composer se re-rend a
 * chaque frappe, et faire remonter la valeur ferait re-rendre tout le panneau,
 * historique compris, pour chaque caractere tape.
 */
export interface ComposerHandle {
  focus: () => void;
  setValue: (value: string) => void;
}

export interface ComposerProps {
  onSend: (content: string) => void;
  onStop: () => void;
  isStreaming: boolean;
  disabled?: boolean;
  placeholder?: string;
  /** Raison du blocage, annoncee a la place de l'aide clavier. */
  disabledReason?: string;
}

/** Environ six lignes, puis defilement interne. */
const MAX_HEIGHT_REM = 9;

export const Composer = forwardRef<ComposerHandle, ComposerProps>(function Composer(
  { onSend, onStop, isStreaming, disabled = false, placeholder, disabledReason },
  ref,
) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  useImperativeHandle(ref, () => ({
    focus: () => textareaRef.current?.focus(),
    setValue: (next: string) => {
      setValue(next);
      // Laisse le curseur en fin de texte : l'utilisateur enchaine souvent en
      // completant une suggestion plutot qu'en la remplacant.
      requestAnimationFrame(() => {
        const node = textareaRef.current;
        if (!node) return;
        node.focus();
        node.setSelectionRange(next.length, next.length);
      });
    },
  }));

  // Auto-dimensionnement, avant peinture pour eviter un saut visible.
  useLayoutEffect(() => {
    const node = textareaRef.current;
    if (!node) return;
    node.style.height = 'auto';
    const maxHeight = MAX_HEIGHT_REM * parseFloat(getComputedStyle(document.documentElement).fontSize);
    node.style.height = `${Math.min(node.scrollHeight, maxHeight)}px`;
  }, [value]);

  const submit = useCallback(() => {
    const trimmed = value.trim();
    if (!trimmed || disabled || isStreaming) return;
    onSend(trimmed);
    setValue('');
  }, [value, disabled, isStreaming, onSend]);

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
      // Shift+Enter insere un saut de ligne ; Enter seul envoie.
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        submit();
      }
    },
    [submit],
  );

  const canSend = value.trim().length > 0 && !disabled && !isStreaming;

  return (
    <div className="aicw-composer">
      {isStreaming && (
        <button type="button" className="aicw-stop-button" onClick={onStop}>
          <StopIcon /> Arrêter la génération
        </button>
      )}

      <div className="aicw-composer-box">
        <textarea
          ref={textareaRef}
          className="aicw-composer-input"
          rows={1}
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled || isStreaming}
          placeholder={
            disabled
              ? (disabledReason ?? 'Assistant indisponible')
              : (placeholder ?? 'Posez votre question…')
          }
          aria-label="Votre message"
        />
        <button
          type="button"
          className="aicw-send-button"
          onClick={submit}
          disabled={!canSend}
          aria-label="Envoyer le message"
        >
          <SendIcon />
        </button>
      </div>

      <p className="aicw-composer-hint">
        {disabled && disabledReason
          ? disabledReason
          : 'Entrée pour envoyer · Maj + Entrée pour un saut de ligne'}
      </p>
    </div>
  );
});
