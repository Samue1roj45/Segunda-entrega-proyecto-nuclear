import { defineConfig } from '@playwright/test';

/**
 * Pruebas de recorrido completo (login → programar → participar → calificar).
 * Corren contra la interfaz real con un servidor simulado (e2e/servidor-simulado.ts), así no necesitan base de datos.
 * Uso:  npm run e2e        (si el navegador no está instalado:  npx playwright install chromium)
 * Con un Chromium propio:   SOMA_CHROMIUM=/ruta/a/chrome npm run e2e
 */
export default defineConfig({
  testDir: 'e2e',
  timeout: 60_000,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4300',
    viewport: { width: 1440, height: 900 },
    launchOptions: process.env['SOMA_CHROMIUM'] ? { executablePath: process.env['SOMA_CHROMIUM'] } : {},
    trace: 'retain-on-failure',
  },
  webServer: { command: 'npx ng serve --port 4300', url: 'http://localhost:4300', reuseExistingServer: true, timeout: 180_000 },
});
