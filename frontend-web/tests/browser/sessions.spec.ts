import { test, expect, type Page } from '@playwright/test';

const summary = { sessionId: 'session-1', sessionName: 'Bronx evening', sessionDate: '2026-10-15', areaName: 'Bronx', durationMinutes: 120, totalMoves: 48, classicLoad: 400, adjustedLoad: 450, averageMoveIntensity: 12, maxBoulderGrade: '6A', maxRouteGrade: null };
const calculation = { adjustedLoad: 100, classicLoad: 90, edlCount: 1, moveIntensity: 12, styleMultiplier: 1, relativeEffortMultiplier: 1, currentLevelGradeLabel: '6A', currentLevelIndex: 4, relativeGradeDifference: 0, calculationModelVersion: 'v1' };
const warmup = { clientEntryId: 'warmup-1', entryOrder: 0, resultType: 'WARMUP', executedMoves: 40, calculation };
const climb = { id: 'climb-1', areaId: 'area-1', name: 'Niebieski01', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', suggestedMoveCount: 8, isPublic: false };
const entry = { clientEntryId: 'entry-1', entryOrder: 1, resultType: 'ASCENT', climbId: climb.id, climbName: climb.name, climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', ascentMode: 'RP', totalMoves: 8, executedMoves: 8, calculation, history: { conflictConfirmed: false } };
const details = { ...summary, clientSessionId: 'client-session-1', version: 0, areaId: 'area-1', timeZoneId: 'Europe/Warsaw', startedAt: '2026-10-15T16:00:00Z', endedAt: '2026-10-15T18:00:00Z', entries: [warmup, entry] };
const user = { userId: 'mihu-1', displayName: 'Mihu' };
const emptyBests = { overall: null, os: null, flash: null, rp: null, rpVsOsGap: null };

async function api(page: Page, mutate?: (method: string, body: Record<string, unknown> | null, url: URL) => unknown) {
  await page.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname.replace('/api/v1', '');
    if (req.method() !== 'GET') {
      const result = mutate?.(req.method(), req.method() === 'DELETE' ? null : req.postDataJSON(), url);
      if (result === 'abort') { await route.abort('failed'); return; }
      await route.fulfill({ status: req.method() === 'DELETE' ? 204 : 200, json: req.method() === 'DELETE' ? undefined : result ?? { sessionId: summary.sessionId } }); return;
    }
    const data = path === '/identity/me' ? user
      : path === '/dashboard/sessions' || path === '/training/sessions' ? [summary]
      : path === '/dashboard/personal-bests' ? { boulder: { ...emptyBests, overall: { gradeLabel: '6A', achievedOn: summary.sessionDate, sessionId: summary.sessionId } }, route: emptyBests }
      : path.startsWith('/training/sessions/') ? details
      : path === '/catalog/areas' ? [{ id: 'area-1', name: 'Bronx', areaType: 'GYM', facilityType: 'BOULDER_GYM', isPublic: true }]
      : path.endsWith('/sectors') ? []
      : path.endsWith('/climbs') ? [climb]
      : path === '/catalog/grades' ? [{ id: 'grade-1', label: '6A' }, { id: 'grade-2', label: '6B' }]
      : path === '/catalog/effort-profiles' ? [{ code: 'BOULDER', displayName: 'Boulder', baseEdl: 8 }] : [];
    await route.fulfill({ json: data });
  });
}
async function addWarmup(page: Page) {
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByLabel('Entry source').selectOption('warmup');
  await page.getByLabel('Executed moves').fill('40');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
}
test('dashboard, grouped details, chronology and calendar use API data', async ({ page }) => {
  await api(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: "Mihu's dashboard" })).toBeVisible();
  await page.getByRole('link', { name: 'Open latest session' }).click();
  await expect(page.getByRole('heading', { name: 'Bronx evening' })).toBeVisible();
  await page.getByRole('button', { name: /Niebieski01/ }).click();
  await expect(page.getByRole('heading', { name: 'Niebieski01' })).toBeVisible();
  await page.getByRole('button', { name: 'Show chronology' }).click();
  await expect(page.locator('.entry-detail')).toHaveCount(2);
  await page.getByRole('link', { name: 'Calendar', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Calendar', exact: true })).toBeVisible();
  const previous = page.getByRole('button', { name: 'Previous month' });
  await previous.click();
  await expect(page).toHaveURL(/calendar/);
  await expect(page.locator('.calendar-day')).toHaveCount(35);
});
test('manual session recovers a draft and retries the identical unconfirmed request', async ({ page }) => {
  const requests: unknown[] = [];
  await api(page, (method, body) => { if (method === 'POST') { requests.push(body); return requests.length === 1 ? 'abort' : { sessionId: 'session-1' }; } });
  await page.goto('/sessions/new');
  await page.getByLabel('Session name (optional)').fill('My saved draft');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await addWarmup(page);
  await page.reload();
  await expect(page.getByText('Your saved draft was recovered.', { exact: false })).toBeVisible();
  await expect(page.getByLabel('Session name (optional)')).toHaveValue('My saved draft');
  await expect(page.locator('.draft-entry')).toHaveCount(1);
  await page.getByRole('button', { name: 'Finish & save session' }).click();
  await expect(page.getByRole('button', { name: 'Retry save' })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Retry save' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  expect(requests).toHaveLength(2); expect(requests[0]).toEqual(requests[1]);
  expect(await page.evaluate(() => Object.keys(localStorage).filter(key => key.includes('session-draft')))).toHaveLength(0);
});
test('edit removes a climb, adds an entry and sends the complete versioned aggregate', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  await api(page, (method, body) => { if (method === 'PUT') saved = body; });
  await page.goto('/sessions/session-1/edit');
  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: "Remove climb's entries" }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(1);
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption('climb-1');
  await page.getByLabel('Executed moves').fill('10');
  await page.getByRole('button', { name: 'Add to session' }).click();
  await page.getByLabel('Reason for correction').fill('Correct the climb and moves');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  expect(saved).toMatchObject({ expectedVersion: 0, changeReason: 'Correct the climb and moves', session: { clientSessionId: details.clientSessionId, areaId: 'area-1', entries: [{ clientEntryId: 'warmup-1', entryOrder: 0, resultType: 'WARMUP' }, { climbId: 'climb-1', entryOrder: 1, executedMoves: 10 }] } });
});
test('start now keeps the running timer and entries through refresh, then saves fixed times', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  await api(page, (_method, body) => { saved = body; });
  await page.goto('/sessions/record');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await page.getByRole('button', { name: 'Start climbing' }).click();
  await addWarmup(page);
  await page.reload();
  await expect(page.getByRole('button', { name: 'Stop session' })).toBeVisible();
  await expect(page.locator('.draft-entry')).toHaveCount(1);
  await page.getByRole('button', { name: 'Stop session' }).click();
  await page.getByRole('button', { name: 'Finish & save session' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  expect(saved).toMatchObject({ areaId: 'area-1', entries: [{ resultType: 'WARMUP', executedMoves: 40 }] });
});
test('delete requires confirmation and includes the server version', async ({ page }) => {
  let removed = '';
  await api(page, (method, _body, url) => { if (method === 'DELETE') removed = url.search; });
  await page.goto('/sessions/session-1');
  await page.getByRole('button', { name: 'Delete session', exact: true }).click();
  expect(removed).toBe('');
  await page.getByRole('button', { name: 'Yes, delete session' }).click();
  await expect(page).toHaveURL('/'); expect(removed).toBe('?expectedVersion=0');
});

test('new climb entry survives refresh; its attempt and ascent keep one client climb ID', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  await api(page, (_method, body) => { saved = body; });
  await page.goto('/sessions/new');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Entry source', exact: true }).selectOption('new');
  await page.getByLabel('Climb name').fill('Orange project');
  await page.getByRole('combobox', { name: 'Grade (canonical)', exact: true }).selectOption('6B');
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ATTEMPT');
  await page.getByLabel('Executed moves').fill('4');
  await page.reload();
  await expect(page.getByLabel('Climb name')).toHaveValue('Orange project');
  await expect(page.getByLabel('Executed moves')).toHaveValue('4');
  await page.getByRole('button', { name: 'Add to session' }).click();
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Entry source', exact: true }).selectOption('draft');
  await page.getByRole('combobox', { name: 'Climb in this draft', exact: true }).selectOption({ label: 'Orange project' });
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ASCENT');
  await page.getByLabel('Executed moves').fill('8');
  await page.getByRole('button', { name: 'Add to session' }).click();
  await page.getByRole('button', { name: 'Finish & save session' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  const entries = saved!.entries as { clientEntryId: string; clientClimbId: string; resultType: string }[];
  expect(entries).toHaveLength(2);
  expect(entries[0].clientClimbId).toBe(entries[1].clientClimbId);
  expect(entries[0].clientEntryId).not.toBe(entries[1].clientEntryId);
  expect(entries.map(e => e.resultType)).toEqual(['ATTEMPT', 'ASCENT']);
});
