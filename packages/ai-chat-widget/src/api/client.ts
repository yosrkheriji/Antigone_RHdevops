import { AiApiError, toAiApiError, toNetworkError } from './errors';
import { readSseStream } from './sseParser';
import type {
  AiStreamEvent,
  ConversationDetail,
  ConversationListParams,
  ConversationSummary,
  Page,
  ReminderResultPayload,
} from './types';

/**
 * Client HTTP du widget.
 *
 * Ne gere aucune authentification : le jeton est demande a l'application hote a
 * chaque appel via `getAuthToken`. Le widget ne lit donc jamais `localStorage`,
 * ni cookie, ni `window.name` — une seule chaine d'auth existe par application, et
 * c'est la sienne.
 */
export interface AiClientOptions {
  /** Racine de l'API, ex. `http://localhost:8080`. Le prefixe `/api/v1` est ajoute ici. */
  baseUrl: string;
  getAuthToken: () => string | null | undefined;
  /** Appele des qu'un 403 sans corps indique un jeton absent ou expire. */
  onUnauthorized?: () => void;
}

export class AiChatClient {
  constructor(private readonly options: AiClientOptions) {}

  private url(path: string): string {
    return `${this.options.baseUrl.replace(/\/+$/, '')}/api/v1${path}`;
  }

  private headers(extra?: HeadersInit): Headers {
    const headers = new Headers(extra);
    headers.set('Content-Type', 'application/json');
    const token = this.options.getAuthToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  /**
   * Route unique de sortie en erreur : c'est ici, et nulle part ailleurs, que la
   * session expiree remonte a l'application hote.
   */
  private async fail(response: Response): Promise<never> {
    const error = await toAiApiError(response);
    if (error.code === 'UNAUTHENTICATED') {
      this.options.onUnauthorized?.();
    }
    throw error;
  }

  private async request<T>(path: string, init?: RequestInit): Promise<T> {
    let response: Response;
    try {
      response = await fetch(this.url(path), { ...init, headers: this.headers(init?.headers) });
    } catch (cause) {
      throw toNetworkError(cause);
    }
    if (!response.ok) {
      return this.fail(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  // ── CRUD conversations ────────────────────────────────────────────────────

  listConversations(params: ConversationListParams = {}): Promise<Page<ConversationSummary>> {
    const query = new URLSearchParams({
      page: String(params.page ?? 0),
      size: String(params.size ?? 30),
      sort: params.sort ?? 'updatedAt',
      direction: params.direction ?? 'DESC',
    });
    return this.request<Page<ConversationSummary>>(`/conversations?${query}`);
  }

  getConversation(id: number): Promise<ConversationDetail> {
    return this.request<ConversationDetail>(`/conversations/${id}`);
  }

  createConversation(title?: string): Promise<ConversationSummary> {
    return this.request<ConversationSummary>('/conversations', {
      method: 'POST',
      body: JSON.stringify(title ? { title } : {}),
    });
  }

  updateConversation(
    id: number,
    changes: { title?: string; pinned?: boolean },
  ): Promise<ConversationSummary> {
    return this.request<ConversationSummary>(`/conversations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(changes),
    });
  }

  deleteConversation(id: number): Promise<void> {
    return this.request<void>(`/conversations/${id}`, { method: 'DELETE' });
  }

  // ── Streaming ─────────────────────────────────────────────────────────────

  /**
   * Envoie un message et rend le flux d'evenements.
   *
   * `Accept: text/event-stream` explicitement : la variante JSON bloquante existe
   * cote backend mais attendrait jusqu'a 90 s sans aucun retour visible, ce qui la
   * rend inutilisable dans une interface.
   *
   * Les erreurs survenues *avant* l'ouverture du flux (403, 429, 503) arrivent en
   * code HTTP et sont levees ici ; celles survenues *pendant* arrivent en
   * evenement `error` dans le flux lui-meme.
   */
  async *streamMessage(
    conversationId: number,
    content: string,
    signal: AbortSignal,
  ): AsyncGenerator<AiStreamEvent, void, undefined> {
    let response: Response;
    try {
      response = await fetch(this.url(`/conversations/${conversationId}/messages`), {
        method: 'POST',
        headers: this.headers({ Accept: 'text/event-stream' }),
        body: JSON.stringify({ content }),
        signal,
      });
    } catch (cause) {
      throw toNetworkError(cause);
    }

    if (!response.ok) {
      return this.fail(response);
    }
    if (!response.body) {
      throw new AiApiError('NETWORK', "Le flux de réponse est vide.");
    }

    try {
      yield* readSseStream(response.body, signal);
    } catch (cause) {
      throw toNetworkError(cause);
    }
  }

  // ── Relances ──────────────────────────────────────────────────────────────

  /**
   * Expedie un brouillon de relance valide par l'utilisateur.
   *
   * Le serveur envoie le texte enregistre, sans regeneration : le client recoit
   * exactement ce que l'utilisateur a relu.
   */
  sendReminder(reminderId: number): Promise<ReminderResultPayload> {
    return this.request<ReminderResultPayload>(`/reminders/${reminderId}/send`, { method: 'POST' });
  }

  // ── Administration ────────────────────────────────────────────────────────

  /** Etat de l'assistant. Reserve aux administrateurs : 403 sinon. */
  getStatus(): Promise<Record<string, unknown>> {
    return this.request<Record<string, unknown>>('/ai/admin/status');
  }
}
