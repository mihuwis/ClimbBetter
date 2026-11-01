import { test, expect } from '@playwright/test';

// Explicit opt-in: creates one session in the local Mihu database and soft-deletes it.
test('React → real API → PostgreSQL: create, read, edit, calendar, delete', async ({ page }) => {
  const title = `[SMOKE] React CRUD ${Date.now()}`;
  let sessionId: string | null = null;
  try {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: "Mihu's dashboard" })).toBeVisible();
    await expect(page.locator('.session-card').first()).toBeVisible();
    await page.screenshot({ path: 'test-results/dashboard-desktop.png', fullPage: true });
    await page.getByRole('link', { name: 'Add session', exact: true }).click();
    await page.getByLabel('Session name (optional)').fill(title);
    await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption({ label: 'Bronx · public' });
    await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
    await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption({ label: 'niebieski01 · 6A' });
    await page.getByRole('button', { name: 'Add to session' }).click();
    await page.getByRole('button', { name: 'Finish & save session' }).click();
    await expect(page).toHaveURL(/sessions\/[\da-f-]+$/);
    sessionId = new URL(page.url()).pathname.split('/').pop()!;
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await page.screenshot({ path: 'test-results/session-details-desktop.png', fullPage: true });
    await page.getByRole('link', { name: 'Edit session', exact: true }).click();
    await page.locator('.draft-entry').getByRole('button', { name: 'Edit', exact: true }).click();
    await page.getByLabel('Executed moves').fill('12');
    await page.getByRole('button', { name: 'Apply entry changes' }).click();
    await page.getByLabel('Reason for correction').fill('Browser integration smoke check');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    const persisted = await page.request.get(`/api/v1/training/sessions/${sessionId}`);
    expect(persisted.ok()).toBeTruthy();
    expect(await persisted.json()).toMatchObject({ version: 1, totalMoves: 12 });
    await page.getByRole('link', { name: 'Calendar', exact: true }).click();
    await expect(page.locator('.calendar-day.has-activity')).not.toHaveCount(0);
    await page.screenshot({ path: 'test-results/calendar-desktop.png', fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/');
    await expect(page.getByRole('heading', { name: "Mihu's dashboard" })).toBeVisible();
    await page.screenshot({ path: 'test-results/dashboard-mobile.png', fullPage: true });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
    await page.goto(`/sessions/${sessionId}`);
    await page.getByRole('button', { name: 'Delete session', exact: true }).click();
    await page.getByRole('button', { name: 'Yes, delete session' }).click();
    await expect(page).toHaveURL('/');
    expect((await page.request.get(`/api/v1/training/sessions/${sessionId}`)).status()).toBe(404);
  } finally {
    if (sessionId) {
      const response = await page.request.get(`/api/v1/training/sessions/${sessionId}`);
      if (response.ok()) {
        const current = await response.json();
        await page.request.delete(`/api/v1/training/sessions/${sessionId}?expectedVersion=${current.version}`);
      }
    }
  }
});
