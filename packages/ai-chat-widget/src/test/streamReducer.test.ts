import { describe, expect, it } from 'vitest';
import { AiApiError } from '../api/errors';
import {
  initialStreamState,
  isStreamActive,
  streamReducer,
  type StreamAction,
  type StreamState,
} from '../hooks/streamReducer';

/**
 * Reducer du flux : etat le plus mouvant du widget, et celui dont une incoherence
 * se voit immediatement a l'ecran (spinner perpetuel, curseur qui reste apres la
 * fin, texte qui continue de s'ecrire apres un arret).
 */
const apply = (actions: StreamAction[], from: StreamState = initialStreamState): StreamState =>
  actions.reduce(streamReducer, from);

describe('streamReducer', () => {
  it('demarre un tour en affichant le message utilisateur', () => {
    const state = apply([{ type: 'START', userMessage: 'Pourquoi mon net a baissé ?' }]);

    expect(state.status).toBe('streaming');
    expect(state.pendingUserMessage).toBe('Pourquoi mon net a baissé ?');
    expect(state.buffer).toBe('');
    expect(isStreamActive(state)).toBe(true);
  });

  it('accumule les fragments dans l’ordre', () => {
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'APPEND', text: 'Votre net ' },
      { type: 'APPEND', text: 'a baissé de 180 DT.' },
    ]);

    expect(state.buffer).toBe('Votre net a baissé de 180 DT.');
  });

  it('ignore un fragment arrive apres la cloture', () => {
    // Rouvrir un flux termine afficherait du texte apres le curseur final.
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'APPEND', text: 'fini' },
      { type: 'DONE' },
      { type: 'APPEND', text: ' en retard' },
    ]);

    expect(state.buffer).toBe('fini');
    expect(state.status).toBe('done');
  });

  it('suit le cycle de vie d’un outil', () => {
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'TOOL_START', tool: 'InvoiceLookupTool', label: 'Recherche…', args: { id: 1 } },
    ]);
    expect(state.toolCalls[0].status).toBe('running');

    const done = streamReducer(state, {
      type: 'TOOL_END',
      tool: 'InvoiceLookupTool',
      status: 'success',
    });
    expect(done.toolCalls[0].status).toBe('success');
  });

  it('distingue deux appels du meme outil dans un tour', () => {
    // Cloturer par nom seul marquerait la mauvaise occurrence.
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'TOOL_START', tool: 'GoogleDriveTool', label: 'a', args: {} },
      { type: 'TOOL_END', tool: 'GoogleDriveTool', status: 'success' },
      { type: 'TOOL_START', tool: 'GoogleDriveTool', label: 'b', args: {} },
    ]);

    expect(state.toolCalls).toHaveLength(2);
    expect(state.toolCalls[0].status).toBe('success');
    expect(state.toolCalls[1].status).toBe('running');

    const closed = streamReducer(state, {
      type: 'TOOL_END',
      tool: 'GoogleDriveTool',
      status: 'error',
      detail: 'quota',
    });
    expect(closed.toolCalls[0].status).toBe('success');
    expect(closed.toolCalls[1].status).toBe('error');
    expect(closed.toolCalls[1].detail).toBe('quota');
  });

  it('ignore la cloture d’un outil jamais demarre', () => {
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'TOOL_END', tool: 'Inconnu', status: 'success' },
    ]);

    expect(state.toolCalls).toHaveLength(0);
  });

  it('conserve le resultat structure', () => {
    const payload = { invoiceNumero: 'FAC-2026-0314' };
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'STRUCTURED', payload },
    ]);

    expect(state.structured).toEqual(payload);
  });

  it('cloture les outils encore en cours sur echec', () => {
    // Les laisser tourner afficherait des spinners perpetuels.
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'TOOL_START', tool: 'PayrollLookupTool', label: 'Lecture…', args: {} },
      { type: 'FAIL', error: new AiApiError('GENERATION_FAILED', 'boum') },
    ]);

    expect(state.status).toBe('error');
    expect(state.error?.code).toBe('GENERATION_FAILED');
    expect(state.toolCalls[0].status).toBe('error');
    expect(isStreamActive(state)).toBe(false);
  });

  it('cloture les outils en cours sur arret volontaire', () => {
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'TOOL_START', tool: 'PayrollLookupTool', label: 'Lecture…', args: {} },
      { type: 'APPEND', text: 'texte partiel' },
      { type: 'ABORT' },
    ]);

    expect(state.status).toBe('aborted');
    expect(state.toolCalls[0].status).toBe('error');
    // Le texte deja produit est conserve : l'utilisateur voit ce qu'il a interrompu.
    expect(state.buffer).toBe('texte partiel');
    expect(isStreamActive(state)).toBe(false);
  });

  it('remet tout a zero au changement de tour', () => {
    const state = apply([
      { type: 'START', userMessage: 'q' },
      { type: 'APPEND', text: 'ancien' },
      { type: 'TOOL_START', tool: 'X', label: 'x', args: {} },
      { type: 'DONE' },
      { type: 'RESET' },
    ]);

    expect(state).toEqual(initialStreamState);
  });

  it('un nouveau START efface l’etat du tour precedent', () => {
    const state = apply([
      { type: 'START', userMessage: 'premier' },
      { type: 'APPEND', text: 'reponse 1' },
      { type: 'DONE' },
      { type: 'START', userMessage: 'second' },
    ]);

    expect(state.buffer).toBe('');
    expect(state.toolCalls).toHaveLength(0);
    expect(state.pendingUserMessage).toBe('second');
  });
});
