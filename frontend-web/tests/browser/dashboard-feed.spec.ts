import { test, expect, type Page } from '@playwright/test';

async function feedApi(page: Page, count = 45) {
  const ids = Array.from({ length: count }, (_, i) => `session-${i + 1}`);
  const requests: string[] = [];
  let fail = false;
  const summary = (id: string) => ({ sessionId: id, sessionName: `Training ${id}`,
    sessionDate: '2026-11-08', areaName: 'Model gym', durationMinutes: 60, totalMoves: 30,
    classicLoad: 20, adjustedLoad: 25, averageMoveIntensity: 5, maxBoulderGrade: null,
    maxRouteGrade: null, calculationModelVersion: 'v3' });
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/api/v1', '');
    if (path === '/identity/me') return route.fulfill({ json: { userId: 'feed-user', displayName: 'Mihu' } });
    if (path === '/dashboard/personal-bests') {
      const empty = { overall: null, os: null, flash: null, rp: null };
      return route.fulfill({ json: { boulder: empty, route: empty } });
    }
    if (path === '/dashboard/sessions/page') {
      const cursor = url.searchParams.get('cursor') ?? '';
      requests.push(cursor);
      if (fail) { fail = false; return route.fulfill({ status: 503, json: { detail: 'Please retry loading sessions.' } }); }
      const start = Number(cursor || 0), limit = Number(url.searchParams.get('limit'));
      return route.fulfill({ json: { items: ids.slice(start, start + limit).map(summary),
        nextCursor: start + limit < ids.length ? String(start + limit) : null } });
    }
    if (path.startsWith('/training/sessions/')) {
      const id = path.split('/').at(-1)!;
      return route.fulfill({ json: { ...summary(id), entries: [], version: 0,
        clientSessionId: id, timeZoneId: 'Europe/Warsaw', startedAt: '2026-11-08T10:00:00Z',
        endedAt: '2026-11-08T11:00:00Z' } });
    }
    return route.fulfill({ json: [] });
  });
  return { ids, requests, failNext: () => { fail = true; } };
}

async function bottom(page: Page) {
  await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
}

test('scroll loads all history and Dashboard return restores pages and scroll position', async ({ page }) => {
  const api = await feedApi(page);
  await page.goto('/');
  await expect(page.locator('.session-card')).toHaveCount(20);
  expect(api.requests).toEqual(['']);
  await bottom(page);
  await expect(page.locator('.session-card')).toHaveCount(40);
  const card = page.locator('.session-card').nth(35);
  await card.scrollIntoViewIfNeeded();
  const scroll = await page.evaluate(() => window.scrollY);
  await card.getByRole('link', { name: 'View session' }).click();
  await expect(page.getByRole('heading', { name: 'Training session-36', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Dashboard', exact: true }).last().click();
  await expect(page.locator('.session-card')).toHaveCount(40);
  await expect.poll(async () => Math.abs(await page.evaluate(() => window.scrollY) - scroll)).toBeLessThan(100);
  await expect(page.getByText('Loading sessions…', { exact: true })).toHaveCount(0);
  api.failNext();
  await bottom(page);
  await expect(page.getByRole('alert')).toContainText('Please retry loading sessions.');
  await expect(page.locator('.session-card')).toHaveCount(40);
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.locator('.session-card')).toHaveCount(45);
  expect(api.requests.slice(-2)).toEqual(['40', '40']);
  await expect(page.getByText('You have reached the beginning of your journal.')).toBeVisible();
  expect(await page.locator('.session-card h2').allTextContents()).toHaveLength(45);
  expect(new Set(await page.locator('.session-card h2').allTextContents()).size).toBe(45);
});

test('failed next page retains loaded sessions and retries the same cursor once', async ({ page }) => {
  const api = await feedApi(page, 40);
  await page.goto('/');
  await expect(page.locator('.session-card')).toHaveCount(20);
  api.failNext();
  await bottom(page);
  await expect(page.getByRole('alert')).toContainText('Please retry loading sessions.');
  await expect(page.locator('.session-card')).toHaveCount(20);
  expect(api.requests).toEqual(['', '20']);
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.locator('.session-card')).toHaveCount(40);
  expect(api.requests).toEqual(['', '20', '20']);
  await expect(page.getByText('You have reached the beginning of your journal.')).toBeVisible();
});

test('browser back revalidates all restored pages after a session is removed', async ({ page }) => {
  const api = await feedApi(page, 40);
  await page.goto('/');
  await expect(page.locator('.session-card')).toHaveCount(20);
  await bottom(page);
  await expect(page.locator('.session-card')).toHaveCount(40);
  await page.locator('.session-card').nth(30).getByRole('link', { name: 'View session' }).click();
  await expect(page).toHaveURL(/sessions\/session-31$/);
  api.ids.splice(0, 1);
  await page.goBack();
  await expect(page.locator('.session-card')).toHaveCount(39);
  await expect(page.locator('.session-card h2').filter({ hasText: /^Training session-1$/ })).toHaveCount(0);
  expect(new Set(await page.locator('.session-card h2').allTextContents()).size).toBe(39);
});

test('empty history and an initial load failure can recover', async ({ page }) => {
  const api = await feedApi(page, 0);
  api.failNext();
  await page.goto('/');
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByText('Your first session starts here')).toHaveCount(0);
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.getByText('Your first session starts here')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Load more sessions' })).toHaveCount(0);
});
