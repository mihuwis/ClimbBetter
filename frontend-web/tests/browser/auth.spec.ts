import { test, expect, type Page } from '@playwright/test';

type Profile = { userId: string; displayName: string; profileComplete: boolean };
const good = 'Test_dobry_wspinacz_1';
const beginner = 'Test_poczatkujacy_1';

async function authenticatedApi(page: Page) {
  const accounts = new Map<string, Profile>();
  const requests: { path: string; uid: string; body: string | null }[] = [];
  let rejectNextProfile = 0;
  let profileUnavailable = false;
  let delayDashboard = false;
  let release: (() => void) | undefined;
  const delayed = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/api/v1', '');
    const token = request.headers().authorization?.replace(/^Bearer /, '');
    if (!token) { await route.fulfill({ status: 401, json: { code: 'AUTH_REQUIRED' } }); return; }
    const identity = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());
    const uid = identity.sub as string;
    requests.push({ path, uid, body: request.postData() });
    if (path === '/identity/bootstrap') {
      if (!accounts.has(uid)) accounts.set(uid, { userId: crypto.randomUUID(), displayName: identity.name, profileComplete: false });
      await route.fulfill({ json: accounts.get(uid) }); return;
    }
    const profile = accounts.get(uid);
    if (!profile) { await route.fulfill({ status: 409, json: { code: 'ACCOUNT_NOT_PROVISIONED' } }); return; }
    if (path === '/identity/me') {
      if (profileUnavailable) { await route.fulfill({ status: 503, json: { code: 'AUTH_UNAVAILABLE' } }); return; }
      if (rejectNextProfile > 0) {
        rejectNextProfile -= 1;
        await route.fulfill({ status: 401, json: { code: 'INVALID_TOKEN' } }); return;
      }
      if (request.method() === 'PUT') {
        profile.displayName = request.postDataJSON().displayName;
        profile.profileComplete = true;
      }
      await route.fulfill({ json: profile }); return;
    }
    if (path === '/dashboard/sessions/page' && delayDashboard) {
      delayDashboard = false;
      await delayed;
      await route.fulfill({ json: { nextCursor: null, items: [{ sessionId: 'private-old', sessionName: 'Previous account private data',
        sessionDate: '2026-11-08', areaName: 'Private', durationMinutes: 60, totalMoves: 20, classicLoad: 0, adjustedLoad: 0 }] } }).catch(() => {});
      return;
    }
    const emptyBests = { overall: null, os: null, flash: null, rp: null };
    await route.fulfill({ json: path === '/dashboard/sessions/page' ? { items: [], nextCursor: null } : path === '/dashboard/personal-bests' ? { boulder: emptyBests, route: emptyBests } : [] });
  });
  return { accounts, requests, rejectProfiles: (count: number) => { rejectNextProfile = count; },
    unavailable: (value: boolean) => { profileUnavailable = value; },
    delay: () => { delayDashboard = true; }, release: () => release?.() };
}

async function login(page: Page, account: string) {
  await page.getByRole('button', { name: account, exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Make this journal yours' })).toBeVisible();
  await expect(page.getByLabel('Your name')).toHaveValue(account);
  await page.getByRole('button', { name: 'Open my journal', exact: true }).click();
  await expect(page.getByRole('heading', { name: `${account}'s dashboard` })).toBeVisible();
}

test('real Firebase SDK and emulator support profile setup, reload and sign out without an actual mailbox', async ({ page }) => {
  const api = await authenticatedApi(page);
  await page.goto('/sessions/new');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText('Local test accounts', { exact: true })).toBeVisible();
  await login(page, good);
  const original = [...api.accounts.values()][0];
  await page.reload();
  await expect(page.getByRole('heading', { name: `${good}'s dashboard` })).toBeVisible();
  expect(api.accounts.size).toBe(1);
  expect([...api.accounts.values()][0].userId).toBe(original.userId);
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Continue with Google' })).toBeVisible();
  await page.goto('/statistics');
  await expect(page).toHaveURL(/\/login$/);
});

test('switching accounts keeps drafts separated and restores the first users own draft', async ({ page }) => {
  await authenticatedApi(page);
  await page.goto('/login');
  await login(page, good);
  await page.goto('/sessions/new');
  await page.getByLabel('Session name').fill('Owned by the first account');
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await login(page, beginner);
  await page.goto('/sessions/new');
  await expect(page.getByLabel('Session name')).toHaveValue('');
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await page.getByRole('button', { name: good, exact: true }).click();
  // Returning users resume the protected page they were on before signing out.
  await expect(page).toHaveURL(/\/sessions\/new$/);
  await expect(page.getByLabel('Session name')).toHaveValue('Owned by the first account');
});

test('an old accounts in-flight response cannot populate the next accounts UI', async ({ page }) => {
  const api = await authenticatedApi(page);
  api.delay();
  await page.goto('/login');
  await login(page, good);
  await expect.poll(() => api.requests.filter(request => request.path === '/dashboard/sessions/page').length).toBe(1);
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await login(page, beginner);
  api.release();
  await expect(page.getByRole('heading', { name: `${beginner}'s dashboard` })).toBeVisible();
  await expect(page.getByText('Previous account private data')).toHaveCount(0);
  expect(api.accounts.size).toBe(2);
});

test('401 refreshes exactly once, a 503 keeps the user signed in, and repeated 401 signs out', async ({ page }) => {
  const api = await authenticatedApi(page);
  await page.goto('/login');
  await login(page, good);
  const before = api.requests.filter(request => request.path === '/identity/me').length;
  api.rejectProfiles(1);
  const profile = await page.evaluate(async () => {
    const { request } = await import('/src/shared/api/http.ts');
    return request('/identity/me');
  });
  expect(profile).toMatchObject({ displayName: good });
  expect(api.requests.filter(request => request.path === '/identity/me')).toHaveLength(before + 2);
  api.unavailable(true);
  expect(await page.evaluate(async () => {
    const { request } = await import('/src/shared/api/http.ts');
    try { await request('/identity/me'); } catch (error) { return (error as { status: number }).status; }
  })).toBe(503);
  await expect(page.getByRole('heading', { name: `${good}'s dashboard` })).toBeVisible();
  api.unavailable(false);
  api.rejectProfiles(2);
  await page.evaluate(async () => {
    const { request } = await import('/src/shared/api/http.ts');
    try { await request('/identity/me'); } catch { /* the auth boundary handles sign-out */ }
  });
  await expect(page.getByRole('button', { name: 'Continue with Google' })).toBeVisible();
});
