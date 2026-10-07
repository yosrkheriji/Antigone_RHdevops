/**
 * Types du contrat backend.
 *
 * Derives de `API_DOCUMENTATION.md` (le comportement reellement implemente), et
 * non de la nomenclature du cahier des charges initial : les deux divergent sur
 * plusieurs noms de champs, et c'est la doc qui fait foi.
 */

/** Capacite ayant produit un message. Renvoyee par le backend, jamais devinee ici. */
export type Capability = 'GENERAL' | 'MEDIA_PLAN' | 'REMINDER' | 'PAYSLIP';

export type MessageRole = 'USER' | 'ASSISTANT' | 'TOOL' | 'SYSTEM';

// ── Conversations ───────────────────────────────────────────────────────────

export interface ConversationSummary {
  id: number;
  title: string;
  pinned: boolean;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationMessage {
  id: number;
  role: MessageRole;
  content: string | null;
  /** JSON serialise des appels d'outils du tour. */
  toolCalls: string | null;
  /** JSON serialise du resultat structure, rejoue au rechargement. */
  structuredResult: string | null;
  capability: Capability | null;
  sequence: number;
  createdAt: string;
}

export interface ConversationDetail {
  id: number;
  title: string;
  pinned: boolean;
  summary: string | null;
  createdAt: string;
  updatedAt: string;
  messages: ConversationMessage[];
}

/** Enveloppe de pagination Spring Data. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ConversationListParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

// ── Evenements SSE ──────────────────────────────────────────────────────────

/** Fragment de texte genere. */
export interface TokenEvent {
  type: 'token';
  delta: string;
}

export interface ToolCallStartEvent {
  type: 'tool_call_start';
  tool: string;
  args: Record<string, unknown>;
}

export interface ToolCallEndEvent {
  type: 'tool_call_end';
  tool: string;
  status: 'success' | 'error';
  detail?: string;
}

/** Maintien de connexion. Aucun rendu cote UI. */
export interface HeartbeatEvent {
  type: 'heartbeat';
}

export interface StructuredResultEvent {
  type: 'structured_result';
  payload: StructuredResult;
}

export interface StreamErrorEvent {
  type: 'error';
  code: AiErrorCode;
  message: string;
}

export interface DoneEvent {
  type: 'done';
}

/** Union discriminee : le `switch` du consommateur est exhaustif par construction. */
export type AiStreamEvent =
  | TokenEvent
  | ToolCallStartEvent
  | ToolCallEndEvent
  | HeartbeatEvent
  | StructuredResultEvent
  | StreamErrorEvent
  | DoneEvent;

// ── Codes d'erreur ──────────────────────────────────────────────────────────

export type AiErrorCode =
  | 'BAD_REQUEST'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'RATE_LIMITED'
  | 'AI_UNAVAILABLE'
  | 'AI_BUSY'
  | 'GENERATION_FAILED'
  | 'INTERNAL_ERROR'
  /** Jeton absent ou expire : 403 sans corps. Traite hors du chat. */
  | 'UNAUTHENTICATED'
  /** Coupure reseau pendant le flux. Declenche une resynchronisation. */
  | 'NETWORK'
  /** Flux interrompu volontairement par l'utilisateur. */
  | 'ABORTED';

export interface AiErrorBody {
  error: string;
  code: AiErrorCode;
}

// ── Resultats structures ────────────────────────────────────────────────────

export type ReminderTone = 'SOFT' | 'FIRM' | 'FORMAL';

export interface ReminderResultPayload {
  /** Reference du brouillon persiste, a passer a l'endpoint d'envoi. */
  reminderId: number;
  invoiceId: number;
  invoiceNumero: string;
  clientNom: string | null;
  clientEmail: string | null;
  subject: string;
  body: string;
  /** Corps mis en page, tel que le client le recevra. */
  htmlPreview?: string | null;
  /** Logo du client destinataire, deja integre a htmlPreview ; absent ou null s'il n'en a pas. */
  clientLogoUrl?: string | null;
  tone: ReminderTone;
  daysLate: number;
  amountDue: number;
  /** Vrai uniquement si le backend a reellement expedie l'email. */
  sent: boolean;
}

export interface PayslipComparison {
  /** Null quand aucun bulletin n'existe pour le mois precedent. */
  previousNet: number | null;
  currentNet: number;
  delta: number | null;
  deltaReasons: string[];
}

export interface PayslipResultPayload {
  explanation: string;
  comparison: PayslipComparison;
}

/**
 * Publication de media plan.
 *
 * Champs nommes comme l'entite reelle (`texteSurVisuel`, `autresElements`,
 * `platforme`, `etatPublication`), pas comme le cahier des charges.
 * Non rendu par les apps RH et Finance, mais type des maintenant pour qu'un
 * futur montage `capability="MEDIA_PLAN"` soit une addition, pas une reecriture.
 */
export interface MediaPlanItem {
  id: number;
  datePublication: string;
  heure: string | null;
  titre: string;
  texteSurVisuel: string | null;
  inspiration: string | null;
  autresElements: string | null;
  platforme: string | null;
  format: string | null;
  type: string | null;
  /** Lien Drive, ou la chaine "PENDING" si Drive etait indisponible. */
  lienDrive: string | null;
  etatPublication: string | null;
  statut: string | null;
  remarques: string | null;
}

export interface MediaPlanResultPayload {
  clientId: number;
  clientNom: string;
  month: string;
  status: string;
  drivePending: boolean;
  syntheseEditoriale: string | null;
  thematiquesEvitees: string[];
  items: MediaPlanItem[];
}

export type StructuredResult =
  | ReminderResultPayload
  | PayslipResultPayload
  | MediaPlanResultPayload
  | Record<string, unknown>;

// ── Gardes de type ──────────────────────────────────────────────────────────
// Le backend n'etiquette pas le type de resultat : il est deduit de sa forme.
// Ces gardes sont le seul endroit ou cette deduction a lieu.

export function isReminderResult(value: unknown): value is ReminderResultPayload {
  const candidate = value as ReminderResultPayload;
  return (
    !!candidate &&
    typeof candidate === 'object' &&
    typeof candidate.invoiceNumero === 'string' &&
    typeof candidate.subject === 'string' &&
    typeof candidate.tone === 'string'
  );
}

export function isPayslipResult(value: unknown): value is PayslipResultPayload {
  const candidate = value as PayslipResultPayload;
  return (
    !!candidate &&
    typeof candidate === 'object' &&
    typeof candidate.explanation === 'string' &&
    !!candidate.comparison &&
    typeof candidate.comparison === 'object'
  );
}

export function isMediaPlanResult(value: unknown): value is MediaPlanResultPayload {
  const candidate = value as MediaPlanResultPayload;
  return (
    !!candidate &&
    typeof candidate === 'object' &&
    typeof candidate.month === 'string' &&
    Array.isArray(candidate.items)
  );
}
