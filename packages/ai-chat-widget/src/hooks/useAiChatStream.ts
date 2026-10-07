import { useCallback, useEffect, useReducer, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AiApiError, toNetworkError } from '../api/errors';
import { toolLabel } from '../api/toolLabels';
import { aiQueryKeys, useAiClient } from './useAiClient';
import {
  initialStreamState,
  streamReducer,
  type StreamState,
} from './streamReducer';
import type { ConversationDetail } from '../api/types';

/**
 * Consommation du flux SSE d'un tour de conversation.
 *
 * Deux exigences dictent la forme de ce hook :
 *
 * 1. **Ne pas rendre a chaque token.** Les fragments arrivent par dizaines par
 *    seconde ; les ecrire directement dans l'etat React declencherait autant de
 *    rendus. Ils sont donc accumules dans une ref et vides une fois par frame.
 *
 * 2. **Ne jamais perdre un message.** Le backend persiste la reponse avant de
 *    tenter la derniere emission : une coupure reseau en cours de flux n'a donc
 *    pas perdu la reponse cote serveur. On resynchronise plutot que d'afficher
 *    une erreur definitive.
 */
export interface UseAiChatStreamResult {
  state: StreamState;
  send: (content: string) => Promise<void>;
  stop: () => void;
  reset: () => void;
  isStreaming: boolean;
}

export function useAiChatStream(conversationId: number | null): UseAiChatStreamResult {
  const { client } = useAiClient();
  const queryClient = useQueryClient();
  const [state, dispatch] = useReducer(streamReducer, initialStreamState);

  const abortRef = useRef<AbortController | null>(null);
  const pendingTokensRef = useRef<string>('');
  const frameRef = useRef<number | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      abortRef.current?.abort();
      if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
    };
  }, []);

  /** Vide les fragments accumules en un seul rendu. */
  const flushTokens = useCallback(() => {
    frameRef.current = null;
    const pending = pendingTokensRef.current;
    if (!pending) return;
    pendingTokensRef.current = '';
    dispatch({ type: 'APPEND', text: pending });
  }, []);

  const scheduleFlush = useCallback(() => {
    if (frameRef.current !== null) return;
    frameRef.current = requestAnimationFrame(flushTokens);
  }, [flushTokens]);

  /**
   * Recharge la conversation depuis le serveur.
   *
   * Seule source de verite apres une coupure : le message assistant y est complet,
   * meme si le flux s'est interrompu en plein milieu.
   */
  const resynchronise = useCallback(
    async (id: number) => {
      try {
        const fresh = await client.getConversation(id);
        queryClient.setQueryData<ConversationDetail>(aiQueryKeys.conversation(id), fresh);
        queryClient.invalidateQueries({ queryKey: aiQueryKeys.conversations });
        return fresh;
      } catch {
        // La resynchronisation est un filet de securite : son echec ne doit pas
        // ecraser l'erreur d'origine, plus informative pour l'utilisateur.
        return null;
      }
    },
    [client, queryClient],
  );

  const stop = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    if (frameRef.current !== null) {
      cancelAnimationFrame(frameRef.current);
      frameRef.current = null;
    }
    flushTokens();
    dispatch({ type: 'ABORT' });
  }, [flushTokens]);

  const reset = useCallback(() => {
    pendingTokensRef.current = '';
    dispatch({ type: 'RESET' });
  }, []);

  const send = useCallback(
    async (content: string) => {
      if (conversationId === null || !content.trim()) return;

      const controller = new AbortController();
      abortRef.current = controller;
      pendingTokensRef.current = '';
      dispatch({ type: 'START', userMessage: content });

      // Le backend fait suivre un `error` d'un `done` : sans ce drapeau, la
      // consolidation de fin de tour effacerait l'erreur avant que l'utilisateur
      // n'ait pu la lire.
      let failed = false;

      try {
        for await (const event of client.streamMessage(conversationId, content, controller.signal)) {
          if (!mountedRef.current) return;

          switch (event.type) {
            case 'token':
              pendingTokensRef.current += event.delta;
              scheduleFlush();
              break;

            case 'tool_call_start':
              flushTokens();
              dispatch({
                type: 'TOOL_START',
                tool: event.tool,
                label: toolLabel(event.tool),
                args: event.args,
              });
              break;

            case 'tool_call_end':
              dispatch({
                type: 'TOOL_END',
                tool: event.tool,
                status: event.status,
                detail: event.detail,
              });
              break;

            case 'structured_result':
              flushTokens();
              dispatch({ type: 'STRUCTURED', payload: event.payload });
              break;

            case 'error':
              flushTokens();
              failed = true;
              dispatch({
                type: 'FAIL',
                error: new AiApiError(event.code, event.message),
              });
              break;

            case 'heartbeat':
              // Maintien de connexion : aucun effet visible, par conception.
              break;

            case 'done':
              flushTokens();
              dispatch({ type: 'DONE' });
              break;
          }
        }

        // Le serveur a fini d'ecrire : le message consolide rejoint le cache, la
        // conversation redevient rechargeable comme n'importe quelle autre.
        if (mountedRef.current) {
          await resynchronise(conversationId);
          // Sur echec, l'etat est conserve : c'est lui qui porte la banniere.
          if (!failed) {
            dispatch({ type: 'RESET' });
          }
        }
      } catch (cause) {
        if (!mountedRef.current) return;
        flushTokens();

        const error = cause instanceof AiApiError ? cause : toNetworkError(cause);

        if (error.code === 'ABORTED') {
          // L'arret volontaire a deja ete traite par stop(). Le serveur, lui, a
          // poursuivi jusqu'a persister : on recupere ce qui a ete produit.
          await resynchronise(conversationId);
          return;
        }

        if (error.code === 'NETWORK') {
          dispatch({ type: 'FAIL', error });
          const fresh = await resynchronise(conversationId);
          // Le message etait bien enregistre : la coupure n'a rien coute a
          // l'utilisateur, inutile de lui laisser une banniere d'erreur.
          if (fresh && mountedRef.current) {
            dispatch({ type: 'RESET' });
          }
          return;
        }

        dispatch({ type: 'FAIL', error });
      } finally {
        abortRef.current = null;
      }
    },
    [client, conversationId, flushTokens, resynchronise, scheduleFlush],
  );

  // Changer de conversation pendant un flux laisserait un buffer orphelin
  // s'afficher sous la nouvelle conversation.
  useEffect(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    pendingTokensRef.current = '';
    dispatch({ type: 'RESET' });
  }, [conversationId]);

  return {
    state,
    send,
    stop,
    reset,
    isStreaming: state.status === 'streaming',
  };
}
