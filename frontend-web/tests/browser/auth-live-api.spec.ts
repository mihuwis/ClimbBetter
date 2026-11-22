import { test, expect } from '@playwright/test';

test('model accounts use real Firebase Admin, own data and private-session access checks', async ({ page }, testInfo) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Test_dobry_wspinacz_1', exact: true }).click();
  await expect(page.getByRole('heading', { name: "Test_dobry_wspinacz_1's dashboard" })).toBeVisible();
  await expect(page.getByText('Model boulder progression 2026-11-01', { exact: true })).toBeVisible();
  await expect(page.getByText('Model boulder progression 2026-11-08', { exact: true })).toBeVisible();
  await expect(page.getByText('Test data', { exact: true })).toBeVisible();
  const sessions = await page.evaluate(async () => {
    const { request } = await import('/src/shared/api/http.ts');
    return request<{ sessionId: string }[]>('/dashboard/sessions');
  });
  expect(sessions).toHaveLength(2);
  await page.screenshot({ path: testInfo.outputPath('model-account-dashboard.png'), fullPage: true });
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await page.getByRole('button', { name: 'Test_poczatkujacy_1', exact: true }).click();
  await expect(page.getByRole('heading', { name: "Test_poczatkujacy_1's dashboard" })).toBeVisible();
  await expect(page.getByText('Model boulder progression 2026-11-01', { exact: true })).toHaveCount(0);
  await page.goto('/sessions/' + sessions[0].sessionId);
  await expect(page.getByRole('heading', { name: 'Session not found', exact: true })).toBeVisible();
});
