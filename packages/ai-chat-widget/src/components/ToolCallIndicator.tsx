import React from 'react';
import { AlertIcon, CheckIcon } from './Icons';
import { toolLabelCompleted } from '../api/toolLabels';
import type { ToolCallState } from '../hooks/streamReducer';

/**
 * Progression des outils pendant une generation.
 *
 * C'est ce qui distingue une attente d'une minute vecue comme une panne d'une
 * attente vecue comme un traitement : l'utilisateur voit ce que fait l'assistant,
 * etape par etape, plutot qu'un spinner immobile.
 *
 * Un outil en echec est signale sans interrompre l'affichage du reste : le
 * backend rend les erreurs d'outil au modele, qui poursuit generalement sa
 * reponse en le mentionnant.
 */
export const ToolCallIndicator: React.FC<{ calls: ToolCallState[] }> = ({ calls }) => {
  if (calls.length === 0) return null;

  return (
    <div className="aicw-toolcalls">
      {calls.map((call) => (
        <span key={call.id} className="aicw-toolcall" data-status={call.status}>
          {call.status === 'running' && <span className="aicw-spinner" aria-hidden />}
          {call.status === 'success' && <CheckIcon className="aicw-toolcall-check" />}
          {call.status === 'error' && <AlertIcon className="aicw-toolcall-warn" />}
          <span>{call.status === 'running' ? call.label : toolLabelCompleted(call.tool)}</span>
        </span>
      ))}
    </div>
  );
};
