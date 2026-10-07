import type { Page, Route } from '@playwright/test';

/**
 * Outillage commun aux tests end-to-end.
 *
 * Le backend est entierement simule par `page.route()`, flux SSE compris : les
 * tests tournent donc sans base de donnees, sans cle OpenAI et sans cout, tout en
 * exercant la vraie application React servie par Vite.
 */

export const APPS = {
  rh: { url: 'http://localhost:3001', launcher: 'Assistant RH' },
  finance: { url: 'http://localhost:3002', launcher: 'Assistant relances' },
} as const;

/**
 * Origine du backend en developpement (`VITE_API_URL`).
 *
 * Les routes sont interceptees sur **cette origine uniquement**, jamais par un
 * motif du type `**\/api\/**` : les applications ont un dossier `src/api/`, et un
 * tel motif capterait aussi les modules servis par Vite (`/src/api/axios.ts`),
 * qui recevraient alors du JSON a la place de JavaScript — l'application ne
 * demarrerait tout simplement pas.
 */
export const BACKEND_ORIGIN = 'http://localhost:8080';

export type AppName = keyof typeof APPS;

/** Assemble une trame SSE conforme au contrat backend. */
export function sse(event: string, data: unknown): string {
  return `event:${event}\ndata:${JSON.stringify(data)}\n\n`;
}

interface Conversation {
  id: number;
  title: string;
  pinned: boolean;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface MockBackend {
  conversations: Conversation[];
  messages: Array<Record<string, unknown>>;
  /** Trames emises par le prochain envoi de message. */
  streamChunks: string[];
  /** Delai entre deux trames, pour observer le streaming a l'ecran. */
  chunkDelayMs: number;
  /** Coupe le flux apres N trames, sans `done`. */
  cutAfter?: number;
}

export function createBackend(): MockBackend {
  return {
    conversations: [
      {
        id: 1,
        title: 'Ma première conversation',
        pinned: false,
        messageCount: 0,
        createdAt: '2026-08-20T09:12:03',
        updatedAt: '2026-08-26T11:40:55',
      },
    ],
    messages: [],
    streamChunks: [sse('token', { delta: 'Bonjour.' }), sse('done', {})],
    chunkDelayMs: 20,
  };
}

/**
 * Installe l'authentification attendue par l'application.
 *
 * Le widget delegue a `getAccessToken`, qui lit ce meme instantane : on injecte
 * donc l'etat reel de l'app plutot que de contourner sa chaine d'auth.
 */
export async function signIn(page: Page, app: AppName): Promise<void> {
  await page.addInitScript(() => {
    const user = {
      compteId: 1,
      employeId: 1,
      username: 'test.user',
      nom: 'Test',
      prenom: 'Utilisateur',
      email: 'test@antigone.tn',
      roles: ['ADMIN'],
      // VIEW_MON_CALENDRIER/VIEW_MES_DEMANDES ouvrent le panneau « Mon Espace » cote
      // RH, sous lequel vit l'entree de l'assistant (VIEW_ASSISTANT_RH) : sans elles,
      // le declencheur reste inatteignable dans la barre laterale simulee.
      permissions: [
        'VIEW_FINANCE',
        'VIEW_MEDIA_PLAN',
        'VIEW_DASHBOARD',
        'VIEW_MON_CALENDRIER',
        'VIEW_MES_DEMANDES',
        'VIEW_ASSISTANT_RH',
      ],
      mustChangePassword: false,
      genre: null,
      message: 'ok',
      imageUrl: null,
      token: 'jeton-e2e',
      tokenExpiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    };
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('loginTime', String(Date.now()));
  });
  void app;
}

/** Branche le backend simule sur toutes les routes `/api/**`. */
export async function mockBackend(page: Page, backend: MockBackend): Promise<void> {
  // Les appels metier de l'application (factures, dashboard...) recoivent une
  // reponse vide : le test porte sur l'assistant, pas sur ces ecrans.
  await page.route(`${BACKEND_ORIGIN}/**`, async (route: Route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    const path = url.pathname;

    if (path === '/api/v1/conversations' && method === 'GET') {
      return route.fulfill({
        json: {
          content: backend.conversations,
          totalElements: backend.conversations.length,
          totalPages: 1,
          number: 0,
          size: 50,
        },
      });
    }

    if (path === '/api/v1/conversations' && method === 'POST') {
      const created: Conversation = {
        id: backend.conversations.length + 1,
        title: 'Nouvelle conversation',
        pinned: false,
        messageCount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      backend.conversations = [created, ...backend.conversations];
      return route.fulfill({ json: created });
    }

    const detailMatch = /^\/api\/v1\/conversations\/(\d+)$/.exec(path);
    if (detailMatch) {
      const id = Number(detailMatch[1]);

      if (method === 'GET') {
        const conversation = backend.conversations.find((item) => item.id === id);
        return route.fulfill({
          json: {
            id,
            title: conversation?.title ?? 'Conversation',
            pinned: conversation?.pinned ?? false,
            summary: null,
            createdAt: '2026-08-20T09:12:03',
            updatedAt: new Date().toISOString(),
            messages: backend.messages,
          },
        });
      }

      if (method === 'PATCH') {
        const changes = route.request().postDataJSON() as Partial<Conversation>;
        backend.conversations = backend.conversations.map((item) =>
          item.id === id ? { ...item, ...changes } : item,
        );
        return route.fulfill({ json: backend.conversations.find((item) => item.id === id) });
      }

      if (method === 'DELETE') {
        backend.conversations = backend.conversations.filter((item) => item.id !== id);
        return route.fulfill({ status: 204, body: '' });
      }
    }

    // Envoi d'un brouillon de relance valide par l'utilisateur.
    if (/^\/api\/v1\/reminders\/\d+\/send$/.test(path) && method === 'POST') {
      return route.fulfill({
        json: {
          reminderId: 77,
          invoiceId: 314,
          invoiceNumero: 'FAC-2026-0314',
          clientNom: 'Alpha',
          clientEmail: 'compta@alpha.tn',
          subject: 'Rappel — facture FAC-2026-0314',
          body: 'Madame,',
          tone: 'SOFT',
          daysLate: 3,
          amountDue: 1667,
          sent: true,
        },
      });
    }

    if (/^\/api\/v1\/conversations\/\d+\/messages$/.test(path) && method === 'POST') {
      const chunks =
        backend.cutAfter !== undefined
          ? backend.streamChunks.slice(0, backend.cutAfter)
          : backend.streamChunks;

      // Le vrai backend persiste la reponse *avant* d'emettre sa derniere trame.
      // Le simuler est indispensable : sans cela, la resynchronisation de fin de
      // tour rechargerait une conversation vide et le message disparaitrait de
      // l'ecran — un artefact du bouchon, pas un defaut du widget.
      if (backend.cutAfter === undefined) {
        const content = route.request().postDataJSON() as { content: string };
        const structured = extractStructuredResult(chunks);
        backend.messages = [
          ...backend.messages,
          {
            id: backend.messages.length + 1,
            role: 'USER',
            content: content.content,
            toolCalls: null,
            structuredResult: null,
            capability: null,
            sequence: backend.messages.length,
            createdAt: new Date().toISOString(),
          },
          {
            id: backend.messages.length + 2,
            role: 'ASSISTANT',
            content: extractText(chunks),
            // Le backend enregistre les appels d'outils en JSON sur le message :
            // c'est ce qui permet de rejouer la trace apres rechargement.
            toolCalls: extractToolCalls(chunks),
            structuredResult: structured ? JSON.stringify(structured) : null,
            capability: 'GENERAL',
            sequence: backend.messages.length + 1,
            createdAt: new Date().toISOString(),
          },
        ];
      }

      // Playwright ne diffuse pas reellement : le corps est assemble puis rendu
      // en une fois. Le parseur du widget lit le flux de la meme facon, la
      // difference ne porte que sur le rythme d'arrivee.
      return route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
        body: chunks.join(''),
      });
    }

    // Tout le reste de l'application : reponse neutre.
    return route.fulfill({ json: { success: true, data: [] } });
  });
}

/** Reconstitue le texte final a partir des trames `token`, comme le ferait le serveur. */
function extractText(chunks: string[]): string {
  return chunks
    .filter((chunk) => chunk.startsWith('event:token'))
    .map((chunk) => {
      const match = /data:(.*)/.exec(chunk);
      return match ? (JSON.parse(match[1].trim()) as { delta: string }).delta : '';
    })
    .join('');
}

/** Reconstitue le journal des appels d'outils, comme le persiste le backend. */
function extractToolCalls(chunks: string[]): string | null {
  const calls = chunks
    .filter((chunk) => chunk.startsWith('event:tool_call_end'))
    .map((chunk) => {
      const match = /data:(.*)/.exec(chunk);
      if (!match) return null;
      const payload = JSON.parse(match[1].trim()) as { tool: string; status: string };
      return { tool: payload.tool, args: '{}', status: payload.status };
    })
    .filter(Boolean);

  return calls.length > 0 ? JSON.stringify(calls) : null;
}

/** Extrait le resultat structure eventuel, pour le rejouer au rechargement. */
function extractStructuredResult(chunks: string[]): unknown | null {
  const frame = chunks.find((chunk) => chunk.startsWith('event:structured_result'));
  if (!frame) return null;
  const match = /data:(.*)/.exec(frame);
  return match ? JSON.parse(match[1].trim()) : null;
}

/** Libelle accessible du declencheur du widget dans le rail d'icones de chaque app. */
const RAIL_TRIGGER_LABEL: Record<AppName, string> = {
  rh: "Ouvrir l'assistant RH",
  finance: "Ouvrir l'assistant relances",
};

/**
 * Ouvre le panneau et attend qu'il soit visible (chunk charge a la demande).
 *
 * Chaque application declenche le widget partage depuis un bouton degrade integre
 * a son rail d'icones (meme design des deux cotes), plutot que le lanceur flottant
 * generique du widget (`hideLauncher`). Le panneau ouvert ensuite est en revanche
 * strictement le meme composant partage, ce que le reste des tests verifie.
 */
export async function openAssistant(page: Page, app: AppName): Promise<void> {
  await page.getByRole('button', { name: RAIL_TRIGGER_LABEL[app] }).click();
  await page.getByRole('dialog', { name: 'Assistant IA Antigone' }).waitFor({ state: 'visible' });
}
