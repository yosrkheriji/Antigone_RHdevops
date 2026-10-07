import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end sur les deux applications hotes.
 *
 * Les serveurs Vite des deux apps sont demarres par Playwright ; le backend, lui,
 * est simule par `page.route()` dans chaque test. Aucune base de donnees, aucune
 * cle OpenAI, aucun cout — mais la vraie application React est exercee.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // Un seul worker : les deux serveurs Vite se partagent la machine, et les tests
  // manipulent un etat de backend simule par page.
  workers: 1,
  reporter: process.env.CI ? 'list' : [['list'], ['html', { open: 'never' }]],

  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'fr-FR',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: [
    {
      command: 'npm run dev -w frontend-rh',
      url: 'http://localhost:3001',
      cwd: '../..',
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: 'npm run dev -w frontend-finance',
      url: 'http://localhost:3002',
      cwd: '../..',
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
  ],
});
