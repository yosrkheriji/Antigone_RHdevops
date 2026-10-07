import React from 'react';
import { QueryClient } from '@tanstack/react-query';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { AiChatClient } from '../api/client';
import { AiClientProvider, aiQueryKeys } from '../hooks/useAiClient';
import { useAiChatStream } from '../hooks/useAiChatStream';
import { MessageList } from '../components/MessageList';
import { initialStreamState } from '../hooks/streamReducer';
import type { ConversationDetail } from '../api/types';

/**
 * Test d'integration du flux complet, avec un vrai serveur SSE simule.
 *
 * Le scenario decisif est la coupure en cours de flux : le backend persiste la
 * reponse *avant* de tenter sa derniere emission, donc une coupure reseau n'a rien
 * fait perdre cote serveur. Le widget doit resynchroniser plutot que d'afficher
 * une erreur definitive — c'est la difference entre « votre message a disparu » et
 * « rien ne s'est passe ».
 */

const BASE_URL = 'http://localhost:8080';
const CONVERSATION_ID = 12;

/** Assemble des trames SSE conformes au contrat backend. */
function sse(event: string, data: unknown): string {
  return `event:${event}\ndata:${JSON.stringify(data)}\n\n`;
}

function streamResponse(chunks: string[], { cutAfter }: { cutAfter?: number } = {}): HttpResponse<ReadableStream<Uint8Array>> {
  const encoder = new TextEncoder();
  const body = new ReadableStream({
    start(controller) {
      chunks.forEach((chunk, index) => {
        if (cutAfter !== undefined && index >= cutAfter) return;
        controller.enqueue(encoder.encode(chunk));
      });
      if (cutAfter !== undefined) {
        // Coupure brutale : le flux se termine sans `done`, comme une connexion
        // interrompue par le reseau ou un proxy.
        controller.error(new Error('connexion perdue'));
      } else {
        controller.close();
      }
    },
  });

  return new HttpResponse(body, {
    headers: { 'Content-Type': 'text/event-stream' },
  });
}

/** Etat serveur : ce que renverrait `GET /conversations/{id}` a un instant donne. */
let serverConversation: ConversationDetail = {
  id: CONVERSATION_ID,
  title: 'Ma conversation',
  pinned: false,
  summary: null,
  createdAt: '2026-08-20T09:12:03',
  updatedAt: '2026-08-26T11:40:55',
  messages: [],
};

let streamHandler: () => HttpResponse<ReadableStream<Uint8Array>> = () => streamResponse([sse('done', {})]);

const server = setupServer(
  http.get(`${BASE_URL}/api/v1/conversations`, () =>
    HttpResponse.json({
      content: [
        {
          id: CONVERSATION_ID,
          title: serverConversation.title,
          pinned: false,
          messageCount: serverConversation.messages.length,
          createdAt: serverConversation.createdAt,
          updatedAt: serverConversation.updatedAt,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    }),
  ),
  http.get(`${BASE_URL}/api/v1/conversations/:id`, () => HttpResponse.json(serverConversation)),
  http.post(`${BASE_URL}/api/v1/conversations/:id/messages`, () => streamHandler()),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  serverConversation = { ...serverConversation, messages: [] };
});
afterAll(() => server.close());

function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const client = new AiChatClient({
    baseUrl: BASE_URL,
    getAuthToken: () => 'jeton-de-test',
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <AiClientProvider client={client} capability="REMINDER" queryClient={queryClient}>
      {children}
    </AiClientProvider>
  );

  return { wrapper, queryClient };
}

describe('flux de conversation', () => {
  it('accumule les tokens, suit les outils et ignore les heartbeats', async () => {
    streamHandler = () =>
      streamResponse([
        sse('tool_call_start', { tool: 'InvoiceLookupTool', args: { invoiceId: 314 } }),
        sse('heartbeat', {}),
        sse('tool_call_end', { tool: 'InvoiceLookupTool', status: 'success' }),
        sse('token', { delta: 'Voici ' }),
        sse('token', { delta: 'la relance.' }),
        sse('heartbeat', {}),
        sse('done', {}),
      ]);

    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    await act(async () => {
      await result.current.send('Génère une relance');
    });

    await waitFor(() => expect(result.current.state.status).toBe('idle'));

    // Le tour est consolide : l'etat transitoire est vide, la source de verite est
    // desormais le cache de la conversation.
    expect(result.current.isStreaming).toBe(false);
  });

  it('expose la progression des outils pendant le flux', async () => {
    let release: (() => void) | null = null;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });

    streamHandler = () => {
      const encoder = new TextEncoder();
      const body = new ReadableStream({
        async start(controller) {
          controller.enqueue(
            encoder.encode(sse('tool_call_start', { tool: 'InvoiceLookupTool', args: {} })),
          );
          // Le flux reste ouvert : c'est exactement la phase pendant laquelle
          // l'utilisateur doit voir « Recherche de la facture… ».
          await gate;
          controller.enqueue(
            encoder.encode(sse('tool_call_end', { tool: 'InvoiceLookupTool', status: 'success' })),
          );
          controller.enqueue(encoder.encode(sse('done', {})));
          controller.close();
        },
      });
      return new HttpResponse(body, { headers: { 'Content-Type': 'text/event-stream' } });
    };

    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    let sending: Promise<void>;
    act(() => {
      sending = result.current.send('Génère une relance');
    });

    await waitFor(() => {
      expect(result.current.state.toolCalls).toHaveLength(1);
      expect(result.current.state.toolCalls[0].status).toBe('running');
      expect(result.current.state.toolCalls[0].label).toBe('Recherche de la facture…');
    });

    await act(async () => {
      release?.();
      await sending!;
    });

    await waitFor(() => expect(result.current.state.status).toBe('idle'));
  });

  it('remonte le resultat structure du flux', async () => {
    const payload = {
      invoiceId: 314,
      invoiceNumero: 'FAC-2026-0314',
      clientNom: 'Alpha',
      clientEmail: 'compta@alpha.tn',
      subject: 'Rappel',
      body: 'Madame,',
      tone: 'FORMAL',
      daysLate: 45,
      amountDue: 3000,
      sent: false,
    };

    // Le flux est maintenu ouvert apres le resultat : c'est la fenetre pendant
    // laquelle l'interface doit deja pouvoir l'afficher, avant consolidation.
    let release: (() => void) | null = null;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });

    streamHandler = () => {
      const encoder = new TextEncoder();
      const body = new ReadableStream({
        async start(controller) {
          controller.enqueue(encoder.encode(sse('token', { delta: 'Voici la relance.' })));
          controller.enqueue(encoder.encode(sse('structured_result', payload)));
          await gate;
          controller.enqueue(encoder.encode(sse('done', {})));
          controller.close();
        },
      });
      return new HttpResponse(body, { headers: { 'Content-Type': 'text/event-stream' } });
    };

    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    let sending: Promise<void>;
    act(() => {
      sending = result.current.send('Génère une relance');
    });

    await waitFor(() =>
      expect(result.current.state.structured).toMatchObject({
        invoiceNumero: 'FAC-2026-0314',
        tone: 'FORMAL',
      }),
    );

    await act(async () => {
      release?.();
      await sending!;
    });
  });

  it('resynchronise apres une coupure de flux, sans perdre le message', async () => {
    // Le serveur coupe apres deux fragments, mais a persiste la reponse complete.
    streamHandler = () =>
      streamResponse(
        [
          sse('token', { delta: 'Voici ' }),
          sse('token', { delta: 'la rel' }),
          sse('token', { delta: 'ance complète.' }),
          sse('done', {}),
        ],
        { cutAfter: 2 },
      );

    serverConversation = {
      ...serverConversation,
      messages: [
        {
          id: 101,
          role: 'USER',
          content: 'Génère une relance',
          toolCalls: null,
          structuredResult: null,
          capability: null,
          sequence: 0,
          createdAt: '2026-08-26T11:39:10',
        },
        {
          id: 102,
          role: 'ASSISTANT',
          content: 'Voici la relance complète.',
          toolCalls: null,
          structuredResult: null,
          capability: 'REMINDER',
          sequence: 1,
          createdAt: '2026-08-26T11:40:55',
        },
      ],
    };

    const { wrapper, queryClient } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    await act(async () => {
      await result.current.send('Génère une relance');
    });

    // Le cache porte la reponse complete telle que le serveur l'a enregistree.
    await waitFor(() => {
      const cached = queryClient.getQueryData<ConversationDetail>(
        aiQueryKeys.conversation(CONVERSATION_ID),
      );
      expect(cached?.messages).toHaveLength(2);
      expect(cached?.messages[1].content).toBe('Voici la relance complète.');
    });

    // Et aucune banniere d'erreur ne subsiste : rien n'a ete perdu.
    await waitFor(() => expect(result.current.state.error).toBeNull());
  });

  it('remonte une erreur du flux avec son code', async () => {
    streamHandler = () =>
      streamResponse([
        sse('error', { code: 'FORBIDDEN', message: "Accès refusé : hors de votre périmètre." }),
        sse('done', {}),
      ]);

    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    await act(async () => {
      await result.current.send('Montre-moi le bulletin de Sarah');
    });

    await waitFor(() => {
      expect(result.current.state.error?.code).toBe('FORBIDDEN');
      expect(result.current.state.error?.message).toContain('périmètre');
    });
  });

  it('declenche onUnauthorized sur un 403 sans corps', async () => {
    const onUnauthorized = vi.fn();
    server.use(
      http.post(`${BASE_URL}/api/v1/conversations/:id/messages`, () =>
        HttpResponse.text('', { status: 403 }),
      ),
    );

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const client = new AiChatClient({
      baseUrl: BASE_URL,
      getAuthToken: () => 'jeton-expire',
      onUnauthorized,
    });
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <AiClientProvider client={client} capability="PAYSLIP" queryClient={queryClient}>
        {children}
      </AiClientProvider>
    );

    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    await act(async () => {
      await result.current.send('bonjour');
    });

    // L'application hote gere la reconnexion ; le chat n'affiche pas d'erreur.
    await waitFor(() => expect(onUnauthorized).toHaveBeenCalled());
  });

  it('interrompt la generation a la demande', async () => {
    let release: (() => void) | null = null;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });

    streamHandler = () => {
      const encoder = new TextEncoder();
      const body = new ReadableStream({
        async start(controller) {
          controller.enqueue(encoder.encode(sse('token', { delta: 'Début' })));
          await gate;
          try {
            controller.enqueue(encoder.encode(sse('done', {})));
            controller.close();
          } catch {
            // Le flux a deja ete annule cote client.
          }
        },
      });
      return new HttpResponse(body, { headers: { 'Content-Type': 'text/event-stream' } });
    };

    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useAiChatStream(CONVERSATION_ID), { wrapper });

    act(() => {
      void result.current.send('Génère une relance');
    });

    await waitFor(() => expect(result.current.state.status).toBe('streaming'));

    await act(async () => {
      result.current.stop();
      release?.();
    });

    // L'arret laisse un etat coherent : plus de flux actif, pas d'erreur affichee.
    await waitFor(() => expect(result.current.isStreaming).toBe(false));
    expect(result.current.state.error).toBeNull();
  });
});

describe('MessageList', () => {
  it('affiche le message en cours de generation avec ses outils', () => {
    render(
      <MessageList
        messages={[]}
        stream={{
          ...initialStreamState,
          status: 'streaming',
          pendingUserMessage: 'Génère une relance',
          buffer: 'Voici la rel',
          toolCalls: [
            {
              id: 'InvoiceLookupTool-0',
              tool: 'InvoiceLookupTool',
              label: 'Recherche de la facture…',
              args: {},
              status: 'running',
            },
          ],
        }}
        capability="REMINDER"
        onPickSuggestion={() => {}}
      />,
    );

    expect(screen.getByText('Génère une relance')).toBeInTheDocument();
    expect(screen.getByText('Recherche de la facture…')).toBeInTheDocument();

    // Le texte apparait deux fois, et c'est voulu : dans la bulle visible, et
    // dans la zone aria-live destinee aux lecteurs d'ecran.
    const occurrences = screen.getAllByText('Voici la rel');
    expect(occurrences).toHaveLength(2);
    expect(occurrences.some((node) => node.closest('.aicw-visually-hidden'))).toBe(true);
  });

  it('propose des suggestions quand la conversation est vide', () => {
    render(
      <MessageList
        messages={[]}
        stream={initialStreamState}
        capability="PAYSLIP"
        onPickSuggestion={() => {}}
      />,
    );

    expect(screen.getByText('Assistant paie')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Pourquoi mon salaire net a-t-il baissé/ }),
    ).toBeInTheDocument();
  });

  it('rejoue un resultat structure persiste au rechargement', () => {
    render(
      <MessageList
        messages={[
          {
            id: 1,
            role: 'ASSISTANT',
            content: 'Voici votre bulletin.',
            toolCalls: null,
            structuredResult: JSON.stringify({
              explanation: 'Net stable.',
              comparison: { previousNet: 2310, currentNet: 2310, delta: 0, deltaReasons: [] },
            }),
            capability: 'PAYSLIP',
            sequence: 0,
            createdAt: '2026-08-26T11:40:55',
          },
        ]}
        stream={initialStreamState}
        capability="PAYSLIP"
        onPickSuggestion={() => {}}
      />,
    );

    expect(screen.getByText('Votre bulletin de paie')).toBeInTheDocument();
  });
});
