import { describe, expect, it } from 'vitest';
import {
  AiApiError,
  parseRetryAfterSeconds,
  presentError,
  toAiApiError,
  toNetworkError,
} from '../api/errors';

/**
 * Chaque code d'erreur du backend doit avoir un traitement explicite : c'est la
 * difference entre une interface qui explique et une interface qui echoue en
 * silence. Le cas le plus subtil est le 403, ambigu cote backend.
 */
function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('toAiApiError', () => {
  it('reconnait un refus de perimetre a son corps JSON', async () => {
    const error = await toAiApiError(
      jsonResponse(403, { error: "la marque 42 n'est pas dans votre perimetre", code: 'FORBIDDEN' }),
    );

    expect(error.code).toBe('FORBIDDEN');
    expect(error.message).toContain('perimetre');
  });

  it('reconnait un jeton expire a l’absence de corps', async () => {
    // Meme statut 403, mais sans corps : c'est Spring Security, pas le metier.
    // Confondre les deux afficherait « accès refusé » au lieu de reconnecter.
    const error = await toAiApiError(new Response(null, { status: 403 }));

    expect(error.code).toBe('UNAUTHENTICATED');
  });

  it('traite un 401 comme une session expiree', async () => {
    const error = await toAiApiError(new Response(null, { status: 401 }));

    expect(error.code).toBe('UNAUTHENTICATED');
  });

  it('extrait le delai d’attente d’un quota depasse', async () => {
    const error = await toAiApiError(
      jsonResponse(429, {
        error: "Quota d'appels a l'assistant atteint. Reessayez dans 34 secondes.",
        code: 'RATE_LIMITED',
      }),
    );

    expect(error.code).toBe('RATE_LIMITED');
    expect(error.retryAfterSeconds).toBe(34);
  });

  it('deduit le code du statut quand le corps n’en fournit pas', async () => {
    const error = await toAiApiError(jsonResponse(422, { error: 'month invalide' }));

    expect(error.code).toBe('VALIDATION_ERROR');
  });

  it('traite un 503 comme assistant indisponible', async () => {
    const error = await toAiApiError(
      jsonResponse(503, { error: 'non configuré', code: 'AI_UNAVAILABLE' }),
    );

    expect(error.code).toBe('AI_UNAVAILABLE');
  });
});

describe('parseRetryAfterSeconds', () => {
  it('lit un delai en secondes', () => {
    expect(parseRetryAfterSeconds('Reessayez dans 12 secondes.')).toBe(12);
    expect(parseRetryAfterSeconds('Reessayez dans 1 seconde.')).toBe(1);
  });

  it('rend undefined en l’absence de delai', () => {
    expect(parseRetryAfterSeconds('Quota atteint.')).toBeUndefined();
  });
});

describe('toNetworkError', () => {
  it('distingue un arret volontaire d’une coupure reseau', () => {
    // Un arret demande par l'utilisateur ne doit pas s'afficher comme une panne.
    const aborted = toNetworkError(new DOMException('aborted', 'AbortError'));
    expect(aborted.code).toBe('ABORTED');

    const network = toNetworkError(new TypeError('Failed to fetch'));
    expect(network.code).toBe('NETWORK');
  });
});

describe('presentError', () => {
  it('ne propose pas de reessai sur un refus d’acces', () => {
    const presentation = presentError(new AiApiError('FORBIDDEN', 'Accès refusé'));

    expect(presentation.retryable).toBe(false);
    expect(presentation.blocksInput).toBe(false);
  });

  it('reprend le message du backend sur un refus de perimetre', () => {
    // « la marque 42 n'est pas dans votre périmètre » est plus utile que le
    // libellé générique.
    const presentation = presentError(
      new AiApiError('FORBIDDEN', "La marque 42 n'est pas dans votre périmètre."),
    );

    expect(presentation.description).toContain('marque 42');
  });

  it('bloque la saisie et annonce le delai sur un quota depasse', () => {
    const presentation = presentError(
      new AiApiError('RATE_LIMITED', 'Quota atteint', 429, 42),
    );

    expect(presentation.blocksInput).toBe(true);
    expect(presentation.description).toContain('42 secondes');
  });

  it('bloque la saisie quand l’assistant est indisponible', () => {
    const presentation = presentError(new AiApiError('AI_UNAVAILABLE', 'non configuré'));

    expect(presentation.blocksInput).toBe(true);
    expect(presentation.severity).toBe('error');
  });

  it('propose un reessai sur saturation', () => {
    expect(presentError(new AiApiError('AI_BUSY', 'saturé')).retryable).toBe(true);
  });

  it('couvre tous les codes du contrat', () => {
    // Un code sans presentation retomberait sur un libelle generique, ce qui est
    // exactement le defaut que ce test doit empecher.
    const codes = [
      'BAD_REQUEST',
      'FORBIDDEN',
      'NOT_FOUND',
      'VALIDATION_ERROR',
      'RATE_LIMITED',
      'AI_UNAVAILABLE',
      'AI_BUSY',
      'GENERATION_FAILED',
      'INTERNAL_ERROR',
      'UNAUTHENTICATED',
      'NETWORK',
      'ABORTED',
    ] as const;

    for (const code of codes) {
      const presentation = presentError(new AiApiError(code, 'message'));
      expect(presentation.title, `code ${code}`).toBeTruthy();
      expect(presentation.description, `code ${code}`).toBeTruthy();
    }
  });
});
