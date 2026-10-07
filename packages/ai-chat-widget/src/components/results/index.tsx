import React from 'react';
import { MediaPlanResult } from './MediaPlanResult';
import { PayslipResult } from './PayslipResult';
import { ReminderResult } from './ReminderResult';
import {
  isMediaPlanResult,
  isPayslipResult,
  isReminderResult,
  type StructuredResult,
} from '../../api/types';

export { MediaPlanResult, PayslipResult, ReminderResult };

/**
 * Registre des rendus de resultats structures.
 *
 * Le backend n'etiquette pas le type de son `structured_result` : il est deduit de
 * la forme du payload. Concentrer cette deduction ici evite de la disperser, et
 * rend l'ajout d'une capacite mecanique — une garde de type, une entree dans ce
 * tableau, rien d'autre a toucher.
 */
type ResultEntry = {
  matches: (value: unknown) => boolean;
  render: (value: never) => React.ReactElement;
};

const REGISTRY: ResultEntry[] = [
  {
    matches: isReminderResult,
    render: ((value: Parameters<typeof ReminderResult>[0]['result']) => (
      <ReminderResult result={value} />
    )) as ResultEntry['render'],
  },
  {
    matches: isPayslipResult,
    render: ((value: Parameters<typeof PayslipResult>[0]['result']) => (
      <PayslipResult result={value} />
    )) as ResultEntry['render'],
  },
  {
    matches: isMediaPlanResult,
    render: ((value: Parameters<typeof MediaPlanResult>[0]['result']) => (
      <MediaPlanResult result={value} />
    )) as ResultEntry['render'],
  },
];

/**
 * Rend un resultat structure.
 *
 * Retourne `null` sur une forme inconnue : afficher du JSON brut a l'utilisateur
 * serait pire que de ne rien afficher — la reponse en prose de l'assistant reste
 * visible et porte deja l'information.
 */
export const StructuredResultView: React.FC<{ result: StructuredResult }> = ({ result }) => {
  const entry = REGISTRY.find((candidate) => candidate.matches(result));
  if (!entry) return null;
  return entry.render(result as never);
};
