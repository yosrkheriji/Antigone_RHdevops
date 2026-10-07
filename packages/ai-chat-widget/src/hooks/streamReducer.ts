import type { AiApiError } from '../api/errors';
import type { StructuredResult } from '../api/types';

/**
 * Etat du flux en cours, isole du reste de l'application.
 *
 * Volontairement hors de TanStack Query : le buffer de tokens change plusieurs
 * dizaines de fois par seconde, ce qu'un cache de requetes n'est pas concu pour
 * absorber. A la fin du flux, le message consolide rejoint le cache — l'etat
 * transitoire ne survit pas au tour.
 */

export interface ToolCallState {
  /** Identite stable, un meme outil pouvant etre appele plusieurs fois. */
  id: string;
  tool: string;
  label: string;
  args: Record<string, unknown>;
  status: 'running' | 'success' | 'error';
  detail?: string;
}

export type StreamStatus = 'idle' | 'streaming' | 'done' | 'error' | 'aborted';

export interface StreamState {
  status: StreamStatus;
  /** Texte accumule du message assistant en cours. */
  buffer: string;
  toolCalls: ToolCallState[];
  structured: StructuredResult | null;
  error: AiApiError | null;
  /** Message utilisateur du tour, affiche immediatement sans attendre le serveur. */
  pendingUserMessage: string | null;
}

export const initialStreamState: StreamState = {
  status: 'idle',
  buffer: '',
  toolCalls: [],
  structured: null,
  error: null,
  pendingUserMessage: null,
};

export type StreamAction =
  | { type: 'START'; userMessage: string }
  /** Lot de fragments : le regroupement evite un rendu par caractere. */
  | { type: 'APPEND'; text: string }
  | { type: 'TOOL_START'; tool: string; label: string; args: Record<string, unknown> }
  | { type: 'TOOL_END'; tool: string; status: 'success' | 'error'; detail?: string }
  | { type: 'STRUCTURED'; payload: StructuredResult }
  | { type: 'FAIL'; error: AiApiError }
  | { type: 'DONE' }
  | { type: 'ABORT' }
  | { type: 'RESET' };

export function streamReducer(state: StreamState, action: StreamAction): StreamState {
  switch (action.type) {
    case 'START':
      return {
        ...initialStreamState,
        status: 'streaming',
        pendingUserMessage: action.userMessage,
      };

    case 'APPEND':
      // Un fragment arrive apres la cloture serait une incoherence : on l'ignore
      // plutot que de rouvrir un flux termine.
      if (state.status !== 'streaming') return state;
      return { ...state, buffer: state.buffer + action.text };

    case 'TOOL_START':
      return {
        ...state,
        toolCalls: [
          ...state.toolCalls,
          {
            id: `${action.tool}-${state.toolCalls.length}`,
            tool: action.tool,
            label: action.label,
            args: action.args,
            status: 'running',
          },
        ],
      };

    case 'TOOL_END': {
      // Cloture le dernier appel encore en cours de cet outil : le meme outil peut
      // etre invoque plusieurs fois dans un tour, et une cloture par nom seul
      // marquerait la mauvaise occurrence.
      const index = findLastRunningIndex(state.toolCalls, action.tool);
      if (index === -1) return state;

      const toolCalls = state.toolCalls.slice();
      toolCalls[index] = {
        ...toolCalls[index],
        status: action.status,
        detail: action.detail,
      };
      return { ...state, toolCalls };
    }

    case 'STRUCTURED':
      return { ...state, structured: action.payload };

    case 'FAIL':
      return {
        ...state,
        status: 'error',
        error: action.error,
        // Les outils restes en cours n'aboutiront pas : les laisser tourner
        // afficherait des spinners perpetuels.
        toolCalls: settleRunning(state.toolCalls, 'error'),
      };

    case 'ABORT':
      return {
        ...state,
        status: 'aborted',
        toolCalls: settleRunning(state.toolCalls, 'error'),
      };

    case 'DONE':
      return { ...state, status: 'done' };

    case 'RESET':
      return initialStreamState;

    default:
      return state;
  }
}

function findLastRunningIndex(toolCalls: ToolCallState[], tool: string): number {
  for (let index = toolCalls.length - 1; index >= 0; index--) {
    if (toolCalls[index].tool === tool && toolCalls[index].status === 'running') {
      return index;
    }
  }
  return -1;
}

function settleRunning(
  toolCalls: ToolCallState[],
  status: 'success' | 'error',
): ToolCallState[] {
  if (!toolCalls.some((call) => call.status === 'running')) return toolCalls;
  return toolCalls.map((call) => (call.status === 'running' ? { ...call, status } : call));
}

/** Vrai tant que le flux occupe la conversation : le Composer reste desactive. */
export function isStreamActive(state: StreamState): boolean {
  return state.status === 'streaming';
}
