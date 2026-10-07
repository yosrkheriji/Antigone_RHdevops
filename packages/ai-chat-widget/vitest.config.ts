import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Les specs Playwright vivent dans e2e/ et ont leurs propres globales :
    // les laisser collecter par Vitest les ferait echouer sans rien tester.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['e2e/**', 'node_modules/**'],
    css: false,
    // Le widget n'a pas de dependance sur un moteur de rendu concurrent :
    // l'isolation par fichier suffit et garde la suite rapide.
    isolate: true,
  },
});
