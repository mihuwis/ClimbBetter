import { defineConfig } from '@playwright/test';

const port = Number(process.env.CB_WEB_TEST_PORT ?? 5174);
const baseURL = `http://127.0.0.1:${port}`;
export default defineConfig({
  testDir: './tests/browser',
  testMatch: process.env.CB_REAL_API ? '**/live-api.spec.ts' : '**/sessions.spec.ts',
  fullyParallel: true,
  use: { baseURL, channel: 'msedge', headless: true, trace: 'retain-on-failure' },
  webServer: { command: `npm run dev -- --host 127.0.0.1 --port ${port} --strictPort`, url: baseURL, reuseExistingServer: false },
});
