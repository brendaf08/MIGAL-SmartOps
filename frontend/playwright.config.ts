import { defineConfig } from '@playwright/test';

const reportDir = process.env.E2E_REPORT_DIR ?? '../artifacts/e2e-local';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.pw.ts',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['junit', { outputFile: `${reportDir}/junit.xml`, includeProjectInTestName: false }],
    ['html', { outputFolder: `${reportDir}/html`, open: 'never', attachments: true }],
  ],
  outputDir: `${reportDir}/test-output`,
  use: {
    baseURL: process.env.E2E_FRONTEND_URL ?? 'http://127.0.0.1:5173',
    browserName: 'chromium',
    headless: true,
    actionTimeout: 12_000,
    navigationTimeout: 20_000,
    // Ante un fallo se conservan la traza y la captura para diagnosticarlo sin repetir la ejecución.
    trace: 'retain-on-failure',
    video: 'off',
    screenshot: 'only-on-failure',
  },
});
