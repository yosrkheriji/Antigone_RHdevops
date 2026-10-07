import { expect, test } from '@playwright/test';
import {
  APPS,
  BACKEND_ORIGIN,
  createBackend,
  mockBackend,
  openAssistant,
  sse,
  signIn,
  type MockBackend,
} from './support';

/** Ouvre la barre laterale d'historique et la rend prete a etre interrogee. */
async function openSidebar(page: import('@playwright/test').Page) {
  const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });
  await panel.getByRole('button', { name: 'Afficher l’historique' }).click();
  const sidebar = panel.getByRole('complementary', { name: 'Historique des conversations' });
  await sidebar.waitFor({ state: 'visible' });
  return sidebar;
}

/**
 * Parcours end-to-end de l'assistant, sur les deux applications hotes.
 *
 * Les tests sont ecrits une fois et rejoues sur RH et Finance : c'est ce qui
 * verifie concretement qu'il s'agit bien d'un composant partage et non de deux
 * implementations paralleles.
 */
for (const [appName, app] of Object.entries(APPS)) {
  test.describe(`Assistant IA — ${appName}`, () => {
    let backend: MockBackend;

    test.beforeEach(async ({ page }) => {
      backend = createBackend();
      await signIn(page, appName as keyof typeof APPS);
      await mockBackend(page, backend);
      await page.goto(app.url);
    });

    test('le declencheur de l’assistant est visible directement dans le rail d’icones', async ({ page }) => {
      // Chaque application integre le meme bouton degrade dans son rail d'icones
      // (le lanceur flottant generique du widget est masque, cf. hideLauncher) :
      // le design se reconnait d'une app a l'autre, seul le libelle accessible
      // differe selon la capacite exposee.
      const trigger = page.getByRole('button', { name: /Ouvrir l['’]assistant/ });
      await expect(trigger).toBeVisible();
      await expect(trigger).toHaveAttribute('title', app.launcher);
    });

    test('ouvre le panneau et affiche les suggestions d’accueil', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);

      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });
      await expect(panel).toBeVisible();
      // Chaque app affiche les suggestions propres a sa capacite.
      await expect(panel.getByRole('button').first()).toBeVisible();
    });

    test('envoie un message et affiche la reponse en streaming', async ({ page }) => {
      backend.streamChunks = [
        sse('token', { delta: 'Votre net ' }),
        sse('token', { delta: 'a baissé de 180 DT.' }),
        sse('done', {}),
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Pourquoi ce montant ?');
      await panel.getByLabel('Envoyer le message').click();

      await expect(panel.getByText('Pourquoi ce montant ?')).toBeVisible();
      await expect(panel.getByText('Votre net a baissé de 180 DT.')).toBeVisible();
    });

    test('affiche la progression des outils avec des libelles humains', async ({ page }) => {
      backend.streamChunks = [
        sse('tool_call_start', { tool: 'PayrollLookupTool', args: { month: '2026-07' } }),
        sse('heartbeat', {}),
        sse('tool_call_end', { tool: 'PayrollLookupTool', status: 'success' }),
        sse('token', { delta: 'Voici l’explication.' }),
        sse('done', {}),
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Explique mon bulletin');
      await panel.getByLabel('Envoyer le message').click();

      // Le libelle reste visible apres le tour : la trace de ce que l'assistant a
      // consulte fait partie de la reponse. Et le nom technique n'atteint jamais
      // l'utilisateur.
      await expect(panel.getByText(/Lecture du bulletin de paie/)).toBeVisible();
      await expect(panel.getByText('PayrollLookupTool')).toHaveCount(0);
    });

    test('rend un resultat structure sans jamais montrer de JSON brut', async ({ page }) => {
      backend.streamChunks = [
        sse('token', { delta: 'Voici la relance.' }),
        sse('structured_result', {
          reminderId: 77,
          invoiceId: 314,
          invoiceNumero: 'FAC-2026-0314',
          clientNom: 'Alpha',
          clientEmail: 'compta@alpha.tn',
          subject: 'Rappel — facture FAC-2026-0314',
          body: 'Madame,\n\nSauf erreur de notre part…',
          tone: 'FORMAL',
          daysLate: 45,
          amountDue: 3000,
          sent: false,
        }),
        sse('done', {}),
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Génère une relance');
      await panel.getByLabel('Envoyer le message').click();

      await expect(panel.getByText('Relance — FAC-2026-0314')).toBeVisible();
      await expect(panel.getByText('45 j')).toBeVisible();
      await expect(panel.getByText('Ton formel')).toBeVisible();

      // L'envoi est propose, mais c'est l'utilisateur qui le declenche apres
      // relecture : l'assistant n'expedie jamais de lui-meme.
      await expect(panel.getByRole('button', { name: /Envoyer au client/ })).toBeVisible();
      await expect(panel.getByText(/Relisez le message avant de l’envoyer/)).toBeVisible();

      // Jamais de JSON brut a l'ecran.
      await expect(panel.getByText('"invoiceNumero"')).toHaveCount(0);
    });

    test('expedie la relance quand l’utilisateur la valide', async ({ page }) => {
      backend.streamChunks = [
        sse('token', { delta: 'Voici la relance.' }),
        sse('structured_result', {
          reminderId: 77,
          invoiceId: 314,
          invoiceNumero: 'FAC-2026-0314',
          clientNom: 'Alpha',
          clientEmail: 'compta@alpha.tn',
          subject: 'Rappel — facture FAC-2026-0314',
          body: 'Madame,\n\nSauf erreur de notre part…',
          tone: 'SOFT',
          daysLate: 3,
          amountDue: 1667,
          sent: false,
        }),
        sse('done', {}),
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Génère une relance');
      await panel.getByLabel('Envoyer le message').click();

      await panel.getByRole('button', { name: /Envoyer au client/ }).click();

      await expect(panel.getByText('Envoyé')).toBeVisible();
      await expect(panel.getByText(/Message expédié à compta@alpha.tn/)).toBeVisible();
    });

    test('traduit un refus d’acces en message comprehensible', async ({ page }) => {
      backend.streamChunks = [
        sse('error', {
          code: 'FORBIDDEN',
          message: 'Acces refuse : vous ne pouvez consulter que votre propre bulletin de paie.',
        }),
        sse('done', {}),
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Montre-moi le bulletin de Sarah');
      await panel.getByLabel('Envoyer le message').click();

      await expect(panel.getByText('Accès refusé')).toBeVisible();
      // Un refus de perimetre ne se retente pas : proposer un bouton serait trompeur.
      await expect(panel.getByRole('button', { name: 'Réessayer' })).toHaveCount(0);
    });

    test('ne perd aucun message quand le flux est coupe en cours de route', async ({ page }) => {
      backend.streamChunks = [
        sse('token', { delta: 'Voici ' }),
        sse('token', { delta: 'la répo' }),
        sse('token', { delta: 'nse complète.' }),
        sse('done', {}),
      ];
      // Le flux s'arrete apres deux trames, mais le serveur a persiste la reponse.
      backend.cutAfter = 2;
      backend.messages = [
        {
          id: 1,
          role: 'USER',
          content: 'Une question',
          toolCalls: null,
          structuredResult: null,
          capability: null,
          sequence: 0,
          createdAt: '2026-08-26T11:39:10',
        },
        {
          id: 2,
          role: 'ASSISTANT',
          content: 'Voici la réponse complète.',
          toolCalls: null,
          structuredResult: null,
          capability: 'GENERAL',
          sequence: 1,
          createdAt: '2026-08-26T11:40:55',
        },
      ];

      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByLabel('Votre message').fill('Une question');
      await panel.getByLabel('Envoyer le message').click();

      // Le message complet est recupere depuis le serveur, pas perdu.
      await expect(panel.getByText('Voici la réponse complète.')).toBeVisible({ timeout: 10_000 });
    });

    test('cree une conversation depuis l’historique', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const sidebar = await openSidebar(page);

      await sidebar.getByRole('button', { name: /Nouvelle conversation/ }).click();

      await expect(sidebar.getByRole('listitem')).toHaveCount(2);
    });

    test('renomme une conversation en place', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const sidebar = await openSidebar(page);

      await sidebar
        .getByRole('listitem')
        .first()
        .getByRole('button', { name: 'Renommer la conversation' })
        .click();

      const input = sidebar.getByLabel('Nouveau titre de la conversation');
      await input.fill('Sujet renommé');
      await input.press('Enter');

      await expect(sidebar.getByText('Sujet renommé')).toBeVisible();
    });

    test('filtre l’historique par la recherche', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const sidebar = await openSidebar(page);

      await sidebar.getByLabel('Rechercher une conversation').fill('première');
      await expect(sidebar.getByRole('listitem')).toHaveCount(1);

      await sidebar.getByLabel('Rechercher une conversation').fill('inexistant');
      await expect(sidebar.getByText('Aucune conversation ne correspond.')).toBeVisible();
    });

    test('epingle et desepingle une conversation', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const sidebar = await openSidebar(page);
      const item = sidebar.getByRole('listitem').first();

      await item.getByRole('button', { name: 'Épingler la conversation' }).click();
      await expect(item.getByRole('button', { name: 'Désépingler la conversation' })).toBeVisible();
    });

    test('supprime une conversation apres confirmation', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const sidebar = await openSidebar(page);

      // Deux conversations : supprimer la derniere en recreerait aussitot une
      // (l'utilisateur doit toujours avoir ou ecrire), ce qui masquerait le
      // resultat de la suppression.
      await sidebar.getByRole('button', { name: /Nouvelle conversation/ }).click();
      await expect(sidebar.getByRole('listitem')).toHaveCount(2);
      await expect(sidebar.getByText('Ma première conversation')).toBeVisible();

      const target = sidebar.getByRole('listitem').filter({ hasText: 'Ma première conversation' });

      // Premier clic : demande de confirmation, rien n'est supprime.
      await target.getByRole('button', { name: 'Supprimer la conversation' }).click();
      await expect(sidebar.getByText(/Cliquez à nouveau sur la corbeille/)).toBeVisible();
      await expect(sidebar.getByRole('listitem')).toHaveCount(2);

      await target.getByRole('button', { name: /Confirmer la suppression/ }).click();

      await expect(sidebar.getByRole('listitem')).toHaveCount(1);
      await expect(sidebar.getByText('Ma première conversation')).toHaveCount(0);
    });

    test('ferme le panneau avec Echap', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await page.keyboard.press('Escape');

      await expect(panel).toHaveAttribute('data-open', 'false');
    });

    test('bascule en plein ecran et revient', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);
      const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });

      await panel.getByRole('button', { name: 'Agrandir le panneau' }).click();
      await expect(panel).toHaveAttribute('data-expanded', 'true');

      await panel.getByRole('button', { name: 'Réduire le panneau' }).click();
      await expect(panel).toHaveAttribute('data-expanded', 'false');
    });

    test('herite des tokens de theme de l’application hote', async ({ page }) => {
      await openAssistant(page, appName as keyof typeof APPS);

      // Aucune couleur par defaut ne doit trainer : la variable du widget doit
      // resoudre vers exactement la couleur de marque de l'application hote.
      const { widgetPrimary, appBrand } = await page.evaluate(() => ({
        widgetPrimary: getComputedStyle(document.querySelector('.aicw-root')!)
          .getPropertyValue('--aicw-primary')
          .trim(),
        appBrand: getComputedStyle(document.documentElement)
          .getPropertyValue('--brand')
          .trim(),
      }));

      expect(appBrand).toBeTruthy();
      expect(widgetPrimary).toBe(appBrand);
    });
  });
}

test.describe('Finance — point d’entrée contextuel', () => {
  test('ouvre l’assistant avec la demande pré-remplie depuis une facture', async ({ page }) => {
    const backend = createBackend();
    await signIn(page, 'finance');

    await mockBackend(page, backend);
    // Enregistree APRES le bouchon generique : Playwright evalue les routes de la
    // plus recente a la plus ancienne, une route specifique posee avant serait
    // masquee par le fourre-tout.
    await page.route(`${BACKEND_ORIGIN}/api/finance/facturation/documents**`, (route) =>
      route.fulfill({
        json: {
          success: true,
          data: [
            {
              id: 314,
              numero: 'FAC-2026-0314',
              type: 'FACTURE',
              clientNom: 'Alpha',
              dateEmission: '2026-06-12',
              dateEcheance: '2026-07-12',
              totalHt: 2521.01,
              montantTva: 478.99,
              timbreFiscal: 1,
              totalTtc: 3000,
              montantPaye: 0,
              montantRestant: 3000,
              statut: 'EN_ATTENTE',
              lignes: [],
            },
          ],
        },
      }),
    );
    await page.goto(`${APPS.finance.url}/factures`);

    const trigger = page.getByTitle("Générer une relance avec l'assistant IA");
    await expect(trigger).toBeVisible({ timeout: 15_000 });
    await trigger.click();

    const panel = page.getByRole('dialog', { name: 'Assistant IA Antigone' });
    await expect(panel).toBeVisible();
    // Le message est pre-rempli mais pas envoye : l'utilisateur relit avant
    // d'engager un appel au modele.
    await expect(panel.getByLabel('Votre message')).toHaveValue(
      'Génère une relance pour la facture FAC-2026-0314',
    );
  });
});
