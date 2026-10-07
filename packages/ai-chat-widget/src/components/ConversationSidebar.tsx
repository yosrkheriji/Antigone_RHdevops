import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { PencilIcon, PinIcon, PlusIcon, TrashIcon } from './Icons';
import type { ConversationSummary } from '../api/types';

/**
 * Liste des conversations : recherche, selection, renommage en place, epinglage,
 * suppression.
 *
 * Les actions sont des freres du bouton de selection, jamais ses enfants : un
 * element interactif imbrique dans un `<button>` est du HTML invalide, et le
 * rend ambigu pour la navigation clavier comme pour les lecteurs d'ecran.
 *
 * La suppression demande confirmation en place, sans boite de dialogue : une
 * modale au-dessus d'un panneau lateral empile deux couches de superposition, et
 * l'action reste reversible cote serveur (suppression logique).
 */
export interface ConversationSidebarProps {
  conversations: ConversationSummary[];
  activeId: number | null;
  loading?: boolean;
  onSelect: (id: number) => void;
  onCreate: () => void;
  onRename: (id: number, title: string) => void;
  onTogglePin: (id: number, pinned: boolean) => void;
  onDelete: (id: number) => void;
  /** Bloque les actions destructrices pendant une generation. */
  busy?: boolean;
}

export const ConversationSidebar: React.FC<ConversationSidebarProps> = ({
  conversations,
  activeId,
  loading = false,
  onSelect,
  onCreate,
  onRename,
  onTogglePin,
  onDelete,
  busy = false,
}) => {
  const [search, setSearch] = useState('');
  const [renamingId, setRenamingId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const itemRefs = useRef<Map<number, HTMLButtonElement>>(new Map());
  const renameInputRef = useRef<HTMLInputElement | null>(null);

  /**
   * Focus pose apres le montage plutot que par `autoFocus`.
   *
   * Le champ apparait au milieu d'un re-rendu de la liste : `autoFocus` peut
   * perdre la main au profit d'un autre element, et le `blur` qui suit refermerait
   * aussitot le champ — le renommage devient alors impossible a declencher de
   * maniere fiable.
   */
  useEffect(() => {
    if (renamingId === null) return;
    const input = renameInputRef.current;
    if (!input) return;
    input.focus();
    input.select();
  }, [renamingId]);

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase();
    if (!needle) return conversations;
    return conversations.filter((item) => item.title.toLowerCase().includes(needle));
  }, [conversations, search]);

  const startRename = useCallback((conversation: ConversationSummary) => {
    setRenamingId(conversation.id);
    setRenameValue(conversation.title);
    setConfirmingId(null);
  }, []);

  const commitRename = useCallback(() => {
    if (renamingId === null) return;
    const trimmed = renameValue.trim();
    // Un titre vide effacerait le repere de l'utilisateur : on annule plutot.
    if (trimmed) {
      onRename(renamingId, trimmed);
    }
    setRenamingId(null);
  }, [renamingId, renameValue, onRename]);

  /** Fleches haut/bas dans la liste, pour une navigation clavier complete. */
  const handleItemKeyDown = useCallback(
    (event: React.KeyboardEvent, index: number) => {
      if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
      event.preventDefault();
      const nextIndex = event.key === 'ArrowDown' ? index + 1 : index - 1;
      const target = filtered[nextIndex];
      if (!target) return;
      itemRefs.current.get(target.id)?.focus();
    },
    [filtered],
  );

  return (
    <aside className="aicw-sidebar" aria-label="Historique des conversations">
      <div className="aicw-sidebar-head">
        <button type="button" className="aicw-new-button" onClick={onCreate} disabled={busy}>
          <PlusIcon /> Nouvelle conversation
        </button>
        <input
          type="search"
          className="aicw-search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Rechercher…"
          aria-label="Rechercher une conversation"
        />
      </div>

      <div className="aicw-conversation-list" role="list">
        {loading &&
          [0, 1, 2].map((index) => (
            <div
              key={`skeleton-${index}`}
              className="aicw-skeleton"
              style={{ height: '2.25rem', margin: '0.125rem 0' }}
              aria-hidden
            />
          ))}

        {!loading && filtered.length === 0 && (
          <p className="aicw-empty-text" style={{ padding: '1rem 0.5rem', textAlign: 'center' }}>
            {search ? 'Aucune conversation ne correspond.' : 'Aucune conversation pour le moment.'}
          </p>
        )}

        {filtered.map((conversation, index) => {
          const isRenaming = renamingId === conversation.id;
          const isConfirming = confirmingId === conversation.id;

          return (
            <div key={conversation.id} role="listitem">
              <div className="aicw-conversation-item" data-active={conversation.id === activeId}>
                {conversation.pinned && !isRenaming && (
                  <PinIcon className="aicw-pin-indicator" size={12} />
                )}

                {isRenaming ? (
                  <input
                    ref={renameInputRef}
                    className="aicw-conversation-rename"
                    value={renameValue}
                    onChange={(event) => setRenameValue(event.target.value)}
                    // Ne valide que si le focus a reellement quitte le champ.
                    // Un re-rendu du panneau hote peut provoquer un blur transitoire
                    // suivi d'un retour immediat du focus ; valider dessus fermerait
                    // le champ en pleine saisie. Le controle est donc reporte apres
                    // le cycle de rendu en cours.
                    onBlur={() => {
                      window.setTimeout(() => {
                        if (document.activeElement === renameInputRef.current) return;
                        commitRename();
                      }, 0);
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault();
                        commitRename();
                      }
                      if (event.key === 'Escape') {
                        event.preventDefault();
                        setRenamingId(null);
                      }
                    }}
                    aria-label="Nouveau titre de la conversation"
                  />
                ) : (
                  <button
                    type="button"
                    ref={(node) => {
                      if (node) itemRefs.current.set(conversation.id, node);
                      else itemRefs.current.delete(conversation.id);
                    }}
                    className="aicw-conversation-select"
                    onClick={() => onSelect(conversation.id)}
                    onDoubleClick={() => startRename(conversation)}
                    onKeyDown={(event) => handleItemKeyDown(event, index)}
                    aria-current={conversation.id === activeId ? 'true' : undefined}
                  >
                    {conversation.title}
                  </button>
                )}

                {!isRenaming && (
                  <span className="aicw-conversation-actions">
                    <button
                      type="button"
                      className="aicw-mini-button"
                      data-active={conversation.pinned}
                      title={conversation.pinned ? 'Désépingler' : 'Épingler'}
                      aria-label={
                        conversation.pinned
                          ? 'Désépingler la conversation'
                          : 'Épingler la conversation'
                      }
                      onClick={() => onTogglePin(conversation.id, !conversation.pinned)}
                    >
                      <PinIcon />
                    </button>

                    <button
                      type="button"
                      className="aicw-mini-button"
                      title="Renommer"
                      aria-label="Renommer la conversation"
                      onClick={() => startRename(conversation)}
                    >
                      <PencilIcon />
                    </button>

                    <button
                      type="button"
                      className="aicw-mini-button"
                      data-danger="true"
                      title={isConfirming ? 'Confirmer la suppression' : 'Supprimer'}
                      aria-label={
                        isConfirming
                          ? 'Confirmer la suppression de la conversation'
                          : 'Supprimer la conversation'
                      }
                      onClick={() => {
                        if (isConfirming) {
                          onDelete(conversation.id);
                          setConfirmingId(null);
                        } else {
                          setConfirmingId(conversation.id);
                        }
                      }}
                    >
                      <TrashIcon />
                    </button>
                  </span>
                )}
              </div>

              {isConfirming && (
                <div
                  className="aicw-notice"
                  style={{ margin: '0.25rem 0.25rem 0.5rem' }}
                  role="alert"
                >
                  Supprimer « {conversation.title} » ? Cliquez à nouveau sur la corbeille pour
                  confirmer.
                  <button
                    type="button"
                    className="aicw-button"
                    style={{ marginTop: '0.375rem' }}
                    onClick={() => setConfirmingId(null)}
                  >
                    Annuler
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
};
