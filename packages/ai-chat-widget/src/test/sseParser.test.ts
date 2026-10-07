import { describe, expect, it } from 'vitest';
import { SseFrameBuffer, readSseStream, toStreamEvent } from '../api/sseParser';
import type { AiStreamEvent } from '../api/types';

/**
 * Le parseur SSE est la piece la plus exposee du widget : elle recoit des octets
 * decoupes arbitrairement par le reseau, et une trame mal reassemblee produit du
 * JSON invalide. Elle est testee ici sans React ni serveur, exhaustivement.
 */
describe('SseFrameBuffer', () => {
  it('extrait une trame complete', () => {
    const buffer = new SseFrameBuffer();

    const frames = buffer.push('event:token\ndata:{"delta":"Bonjour"}\n\n');

    expect(frames).toEqual([{ event: 'token', data: '{"delta":"Bonjour"}' }]);
  });

  it('reassemble une trame coupee entre deux chunks reseau', () => {
    const buffer = new SseFrameBuffer();

    // Une frontiere de chunk ne coincide jamais avec une frontiere de trame.
    expect(buffer.push('event:token\ndata:{"del')).toEqual([]);
    expect(buffer.push('ta":"salut"}\n\n')).toEqual([
      { event: 'token', data: '{"delta":"salut"}' },
    ]);
  });

  it('extrait plusieurs trames arrivees dans le meme chunk', () => {
    const buffer = new SseFrameBuffer();

    const frames = buffer.push(
      'event:token\ndata:{"delta":"a"}\n\nevent:token\ndata:{"delta":"b"}\n\n',
    );

    expect(frames).toHaveLength(2);
    expect(frames[1].data).toBe('{"delta":"b"}');
  });

  it('tolere les fins de ligne CRLF', () => {
    const buffer = new SseFrameBuffer();

    // Certains proxys reecrivent LF en CRLF : la trame doit rester reconnue.
    const frames = buffer.push('event:done\r\ndata:{}\r\n\r\n');

    expect(frames).toEqual([{ event: 'done', data: '{}' }]);
  });

  it('ignore les commentaires SSE de maintien de connexion', () => {
    const buffer = new SseFrameBuffer();

    expect(buffer.push(':keep-alive\n\n')).toEqual([]);
  });

  it('rend la derniere trame sans separateur final au flush', () => {
    const buffer = new SseFrameBuffer();
    buffer.push('event:done\ndata:{}');

    expect(buffer.flush()).toEqual([{ event: 'done', data: '{}' }]);
  });

  it('concatene plusieurs lignes data d’une meme trame', () => {
    const buffer = new SseFrameBuffer();

    const frames = buffer.push('event:token\ndata:{"delta":\ndata:"coupe"}\n\n');

    expect(frames[0].data).toBe('{"delta":"coupe"}');
  });
});

describe('toStreamEvent', () => {
  it('type un fragment de texte', () => {
    expect(toStreamEvent({ event: 'token', data: '{"delta":"Bonjour"}' })).toEqual({
      type: 'token',
      delta: 'Bonjour',
    });
  });

  it('type un demarrage d’outil avec ses arguments', () => {
    expect(
      toStreamEvent({
        event: 'tool_call_start',
        data: '{"tool":"InvoiceLookupTool","args":{"invoiceId":314}}',
      }),
    ).toEqual({
      type: 'tool_call_start',
      tool: 'InvoiceLookupTool',
      args: { invoiceId: 314 },
    });
  });

  it('type une fin d’outil en echec', () => {
    expect(
      toStreamEvent({
        event: 'tool_call_end',
        data: '{"tool":"GoogleDriveTool","status":"error","detail":"indisponible"}',
      }),
    ).toEqual({
      type: 'tool_call_end',
      tool: 'GoogleDriveTool',
      status: 'error',
      detail: 'indisponible',
    });
  });

  it('type un heartbeat', () => {
    expect(toStreamEvent({ event: 'heartbeat', data: '{}' })).toEqual({ type: 'heartbeat' });
  });

  it('type un resultat structure sans en alterer le contenu', () => {
    const event = toStreamEvent({
      event: 'structured_result',
      data: '{"invoiceNumero":"FAC-2026-0314","tone":"FORMAL"}',
    });

    expect(event).toEqual({
      type: 'structured_result',
      payload: { invoiceNumero: 'FAC-2026-0314', tone: 'FORMAL' },
    });
  });

  it('type une erreur avec son code', () => {
    expect(
      toStreamEvent({ event: 'error', data: '{"code":"FORBIDDEN","message":"Accès refusé"}' }),
    ).toEqual({ type: 'error', code: 'FORBIDDEN', message: 'Accès refusé' });
  });

  it('retombe sur GENERATION_FAILED quand le code manque', () => {
    const event = toStreamEvent({ event: 'error', data: '{"message":"boum"}' });

    expect(event).toMatchObject({ type: 'error', code: 'GENERATION_FAILED' });
  });

  it('ignore une trame au JSON invalide sans lever', () => {
    // Une seule trame malformee ne doit pas interrompre tout le flux.
    expect(toStreamEvent({ event: 'token', data: '{ pas du json' })).toBeNull();
  });

  it('ignore un evenement inconnu', () => {
    expect(toStreamEvent({ event: 'inconnu', data: '{}' })).toBeNull();
  });

  it('ignore un token sans champ delta', () => {
    expect(toStreamEvent({ event: 'token', data: '{"autre":1}' })).toBeNull();
  });
});

// ── Lecture d'un flux complet ────────────────────────────────────────────────

function streamFrom(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
      controller.close();
    },
  });
}

async function collect(stream: ReadableStream<Uint8Array>, signal?: AbortSignal) {
  const events: AiStreamEvent[] = [];
  for await (const event of readSseStream(stream, signal)) {
    events.push(event);
  }
  return events;
}

describe('readSseStream', () => {
  it('rend les evenements dans l’ordre d’arrivee', async () => {
    const events = await collect(
      streamFrom([
        'event:tool_call_start\ndata:{"tool":"InvoiceLookupTool","args":{}}\n\n',
        'event:tool_call_end\ndata:{"tool":"InvoiceLookupTool","status":"success"}\n\n',
        'event:token\ndata:{"delta":"Bon"}\n\n',
        'event:token\ndata:{"delta":"jour"}\n\n',
        'event:done\ndata:{}\n\n',
      ]),
    );

    expect(events.map((event) => event.type)).toEqual([
      'tool_call_start',
      'tool_call_end',
      'token',
      'token',
      'done',
    ]);
  });

  it('s’arrete des reception de done', async () => {
    // Le serveur a fini : maintenir le lecteur ouvert n'apporterait rien.
    const events = await collect(
      streamFrom([
        'event:done\ndata:{}\n\n',
        'event:token\ndata:{"delta":"apres la fin"}\n\n',
      ]),
    );

    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('done');
  });

  it('reassemble un flux decoupe caractere par caractere', async () => {
    const raw = 'event:token\ndata:{"delta":"ok"}\n\nevent:done\ndata:{}\n\n';

    const events = await collect(streamFrom(raw.split('')));

    expect(events).toEqual([{ type: 'token', delta: 'ok' }, { type: 'done' }]);
  });

  it('cesse de lire quand le signal est annule', async () => {
    const controller = new AbortController();
    controller.abort();

    const events = await collect(streamFrom(['event:token\ndata:{"delta":"x"}\n\n']), controller.signal);

    expect(events).toEqual([]);
  });
});
