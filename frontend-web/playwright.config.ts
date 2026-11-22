import { defineConfig } from '@playwright/test';

const port = Number(process.env.CB_WEB_TEST_PORT ?? 5174);
const baseURL = `http://127.0.0.1:${port}`;
export default defineConfig({
  testDir: './tests/browser',
  testMatch: process.env.CB_REAL_AUTH_API ? '**/auth-live-api.spec.ts' : process.env.CB_AUTH_E2E ? '**/auth.spec.ts' : process.env.CB_REAL_PERIOD_SUMMARY_API ? '**/period-summary-live-api.spec.ts'
    : process.env.CB_REAL_STATISTICS_API ? '**/statistics-live-api.spec.ts'
    : process.env.CB_REAL_API ? '**/live-api.spec.ts' : ['**/dashboard-feed.spec.ts', '**/sessions.spec.ts', '**/session-repeat.spec.ts', '**/entry-repeat.spec.ts', '**/statistics.spec.ts', '**/period-summary.spec.ts'],
  fullyParallel: true,
  use: { baseURL, channel: 'msedge', headless: true, trace: 'retain-on-failure' },
  webServer: { command: `npm run dev -- --host 127.0.0.1 --port ${port} --strictPort`, url: baseURL, reuseExistingServer: false,
    env: { VITE_AUTH_MODE: process.env.CB_AUTH_E2E ? 'emulator' : 'stub',
      ...(process.env.CB_AUTH_E2E ? { VITE_FIREBASE_PROJECT_ID: 'demo-climbbetter',
        VITE_FIREBASE_AUTH_DOMAIN: 'localhost', VITE_FIREBASE_API_KEY: 'local-emulator',
        VITE_FIREBASE_APP_ID: 'local-climbbetter', VITE_AUTH_ACCOUNT_MODE: 'register' } : {}) } },
});
