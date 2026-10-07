import type { AiErrorCode, AiErrorBody } from './types';

/**
 * Traduction des erreurs backend en retours utilisateur.
 *
 * Un seul endroit decide de ce que l'utilisateur lit et de ce qu'il peut faire :
 * disperser ces choix dans les composants garantirait des formulations
 * incoherentes d'un ecran a l'autre.
 */

export class AiApiError extends Error {
  readonly code: AiErrorCode;
  readonly status: number;
  /** Delai avant nouvelle tentative, en secondes, quand le backend le precise. */
  readonly retryAfterSeconds?: number;

  constructor(code: AiErrorCode, message: string, status = 0, retryAfterSeconds?: number) {
    super(message);
    this.name = 'AiApiError';
    this.code = code;
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export interface ErrorPresentation {
  title: string;
  description: string;
  /** Un retry manuel a-t-il une chance d'aboutir ? */
  retryable: boolean;
  /** Le Composer doit-il etre desactive tant que l'erreur dure ? */
  blocksInput: boolean;
  severity: 'error' | 'warning';
}

const PRESENTATIONS: Record<AiErrorCode, ErrorPresentation> = {
  FORBIDDEN: {
    title: "Accès refusé",
    description:
      "Vous n'avez pas accès à cette fonctionnalité. Rapprochez-vous d'un administrateur si vous pensez que c'est une erreur.",
    // Reproposer un retry serait trompeur : le refus vient du périmètre, pas d'un aléa.
    retryable: false,
    blocksInput: false,
    severity: 'error',
  },
  RATE_LIMITED: {
    title: "Trop de demandes",
    description: "Vous avez atteint la limite d'appels à l'assistant.",
    retryable: false,
    blocksInput: true,
    severity: 'warning',
  },
  AI_UNAVAILABLE: {
    title: "Assistant indisponible",
    description:
      "L'assistant IA n'est pas configuré ou est momentanément hors service. Le reste de l'application fonctionne normalement.",
    retryable: false,
    blocksInput: true,
    severity: 'error',
  },
  AI_BUSY: {
    title: "Assistant saturé",
    description: "L'assistant traite trop de demandes en ce moment.",
    retryable: true,
    blocksInput: false,
    severity: 'warning',
  },
  GENERATION_FAILED: {
    title: "La génération a échoué",
    description: "Le modèle n'a pas pu terminer sa réponse.",
    retryable: true,
    blocksInput: false,
    severity: 'error',
  },
  NOT_FOUND: {
    title: "Conversation introuvable",
    description: "Cette conversation n'existe plus ou ne vous appartient pas.",
    retryable: false,
    blocksInput: false,
    severity: 'error',
  },
  VALIDATION_ERROR: {
    title: "Demande invalide",
    description: "Le message envoyé n'a pas été accepté.",
    retryable: false,
    blocksInput: false,
    severity: 'warning',
  },
  BAD_REQUEST: {
    title: "Demande invalide",
    description: "Un paramètre de la demande n'est pas valide.",
    retryable: false,
    blocksInput: false,
    severity: 'warning',
  },
  INTERNAL_ERROR: {
    title: "Erreur interne",
    description: "Une erreur inattendue est survenue côté serveur.",
    retryable: true,
    blocksInput: false,
    severity: 'error',
  },
  NETWORK: {
    title: "Connexion perdue",
    description: "La connexion a été interrompue. Récupération du message en cours…",
    retryable: true,
    blocksInput: false,
    severity: 'warning',
  },
  UNAUTHENTICATED: {
    // Jamais affiché : intercepté en amont pour déclencher onUnauthorized().
    title: "Session expirée",
    description: "Votre session a expiré, reconnexion nécessaire.",
    retryable: false,
    blocksInput: true,
    severity: 'error',
  },
  ABORTED: {
    title: "Génération interrompue",
    description: "Vous avez arrêté la génération.",
    retryable: true,
    blocksInput: false,
    severity: 'warning',
  },
};

export function presentError(error: AiApiError): ErrorPresentation {
  const base = PRESENTATIONS[error.code] ?? PRESENTATIONS.INTERNAL_ERROR;

  if (error.code === 'RATE_LIMITED' && error.retryAfterSeconds) {
    return {
      ...base,
      description: `${base.description} Réessayez dans ${error.retryAfterSeconds} secondes.`,
    };
  }
  // Le backend produit des messages exploitables (« la marque 42 n'est pas dans
  // votre périmètre ») : plus utiles que le libellé générique quand ils existent.
  if (error.message && error.code === 'FORBIDDEN') {
    return { ...base, description: error.message };
  }
  return base;
}

/** Extrait le délai d'attente d'un message de quota, le backend ne l'exposant pas en en-tête. */
export function parseRetryAfterSeconds(message: string): number | undefined {
  const match = /dans\s+(\d+)\s*secondes?/i.exec(message);
  return match ? Number(match[1]) : undefined;
}

/**
 * Convertit une réponse HTTP en échec en {@link AiApiError}.
 *
 * Le 403 est ambigu côté backend : refus de périmètre (avec corps JSON) ou jeton
 * absent/expiré (sans corps). C'est la présence du corps qui les départage — et
 * c'est ce qui permet de déclencher une reconnexion plutôt que d'afficher une
 * erreur d'accès trompeuse.
 */
export async function toAiApiError(response: Response): Promise<AiApiError> {
  let body: Partial<AiErrorBody> | null = null;
  try {
    body = (await response.json()) as Partial<AiErrorBody>;
  } catch {
    body = null;
  }

  if (response.status === 401 || (response.status === 403 && !body?.code)) {
    return new AiApiError('UNAUTHENTICATED', 'Session expirée.', response.status);
  }

  const code = (body?.code as AiErrorCode) ?? inferCodeFromStatus(response.status);
  const message = body?.error ?? "L'assistant n'a pas pu traiter la demande.";

  return new AiApiError(
    code,
    message,
    response.status,
    code === 'RATE_LIMITED' ? parseRetryAfterSeconds(message) : undefined,
  );
}

function inferCodeFromStatus(status: number): AiErrorCode {
  switch (status) {
    case 400:
      return 'BAD_REQUEST';
    case 403:
      return 'FORBIDDEN';
    case 404:
      return 'NOT_FOUND';
    case 422:
      return 'VALIDATION_ERROR';
    case 429:
      return 'RATE_LIMITED';
    case 503:
      return 'AI_UNAVAILABLE';
    default:
      return 'INTERNAL_ERROR';
  }
}

/** Un échec réseau (hors ligne, coupure) ne doit pas être présenté comme une erreur serveur. */
export function toNetworkError(cause: unknown): AiApiError {
  if (cause instanceof DOMException && cause.name === 'AbortError') {
    return new AiApiError('ABORTED', "Génération interrompue.");
  }
  return new AiApiError('NETWORK', "Connexion perdue pendant la génération.");
}
