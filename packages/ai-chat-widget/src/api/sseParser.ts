import type { AiStreamEvent, AiErrorCode, StructuredResult } from './types';

/**
 * Analyseur de trames SSE.
 *
 * Isole en fonction pure, sans React ni `fetch` : c'est la piece la plus facile a
 * casser silencieusement (une trame coupee au milieu d'un chunk reseau produit du
 * JSON invalide), et la seule qu'on puisse tester exhaustivement sans monter de
 * composant ni simuler de serveur.
 *
 * `EventSource` n'est pas utilisable ici : le backend expose ces flux en POST, que
 * l'API native ne sait pas emettre.
 */

/** Une trame SSE brute, avant interpretation metier. */
export interface RawSseFrame {
  event: string;
  data: string;
}

/**
 * Accumule les octets recus et n'emet que les trames completes.
 *
 * Un chunk reseau ne coincide pas avec une frontiere de trame : le reste est
 * conserve dans le tampon jusqu'a l'arrivee du separateur.
 */
export class SseFrameBuffer {
  private buffer = '';

  /** @returns les trames devenues completes avec ce chunk */
  push(chunk: string): RawSseFrame[] {
    this.buffer += chunk;

    // Tolere les fins de ligne CRLF comme LF : certains proxys reecrivent l'un en
    // l'autre, et une trame non reconnue serait perdue sans aucun signal.
    const normalised = this.buffer.replace(/\r\n/g, '\n');
    const segments = normalised.split('\n\n');

    // Le dernier segment est incomplet tant que le separateur n'est pas arrive.
    this.buffer = segments.pop() ?? '';

    return segments
      .map((segment) => parseFrame(segment))
      .filter((frame): frame is RawSseFrame => frame !== null);
  }

  /** Vide le tampon en fin de flux, au cas ou la derniere trame n'ait pas de separateur final. */
  flush(): RawSseFrame[] {
    const remaining = this.buffer;
    this.buffer = '';
    const frame = parseFrame(remaining.replace(/\r\n/g, '\n'));
    return frame ? [frame] : [];
  }
}

function parseFrame(segment: string): RawSseFrame | null {
  let event = 'message';
  let data = '';

  for (const line of segment.split('\n')) {
    if (line.startsWith(':')) {
      // Commentaire SSE (keep-alive bas niveau) : ignore.
      continue;
    }
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim();
    } else if (line.startsWith('data:')) {
      // Une trame peut porter plusieurs lignes `data:`, concatenees telles quelles.
      data += line.slice('data:'.length).trim();
    }
  }

  return data ? { event, data } : null;
}

/**
 * Traduit une trame brute en evenement metier type.
 *
 * @returns `null` si la trame est inexploitable — un flux ne doit jamais s'arreter
 *          parce qu'une seule trame etait malformee.
 */
export function toStreamEvent(frame: RawSseFrame): AiStreamEvent | null {
  let payload: unknown;
  try {
    payload = JSON.parse(frame.data);
  } catch {
    return null;
  }

  const record = (payload ?? {}) as Record<string, unknown>;

  switch (frame.event) {
    case 'token': {
      const delta = record.delta;
      return typeof delta === 'string' ? { type: 'token', delta } : null;
    }

    case 'tool_call_start': {
      const tool = record.tool;
      if (typeof tool !== 'string') return null;
      return {
        type: 'tool_call_start',
        tool,
        args: (record.args as Record<string, unknown>) ?? {},
      };
    }

    case 'tool_call_end': {
      const tool = record.tool;
      if (typeof tool !== 'string') return null;
      return {
        type: 'tool_call_end',
        tool,
        status: record.status === 'error' ? 'error' : 'success',
        detail: typeof record.detail === 'string' ? record.detail : undefined,
      };
    }

    case 'heartbeat':
      return { type: 'heartbeat' };

    case 'structured_result':
      return { type: 'structured_result', payload: payload as StructuredResult };

    case 'error':
      return {
        type: 'error',
        code: (typeof record.code === 'string' ? record.code : 'GENERATION_FAILED') as AiErrorCode,
        message:
          typeof record.message === 'string'
            ? record.message
            : "La generation a echoue.",
      };

    case 'done':
      return { type: 'done' };

    default:
      return null;
  }
}

/**
 * Lit un corps de reponse en flux et emet les evenements au fil de l'eau.
 *
 * Interrompt la lecture des qu'un `done` est recu : le serveur a fini, inutile de
 * maintenir le lecteur ouvert.
 */
export async function* readSseStream(
  body: ReadableStream<Uint8Array>,
  signal?: AbortSignal,
): AsyncGenerator<AiStreamEvent, void, undefined> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  const frames = new SseFrameBuffer();

  try {
    while (true) {
      if (signal?.aborted) return;

      const { done, value } = await reader.read();
      if (done) break;

      for (const frame of frames.push(decoder.decode(value, { stream: true }))) {
        const event = toStreamEvent(frame);
        if (!event) continue;
        yield event;
        if (event.type === 'done') return;
      }
    }

    for (const frame of frames.flush()) {
      const event = toStreamEvent(frame);
      if (event) yield event;
    }
  } finally {
    // Le lecteur peut deja etre libere si le flux s'est termine seul.
    reader.releaseLock?.();
  }
}
