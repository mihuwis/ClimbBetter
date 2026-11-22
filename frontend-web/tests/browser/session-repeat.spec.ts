import { test, expect, type Page } from '@playwright/test';
import type { SessionCommand, SessionDetails, SessionEntry } from '../../src/features/training/model/session.types';
import { draftKey, newDraft } from '../../src/features/training/model/session-draft';

const calculation: SessionEntry['calculation'] = {
  adjustedLoad: 68, classicLoad: 85, profileIntensityIndex: 10, crux: null, moveIntensity: 8.5,
  styleMultiplier: 1, relativeEffortMultiplier: 1, currentLevelGradeLabel: '6A', currentLevelIndex: 4,
  relativeGradeDifference: 0, calculationModelVersion: 'v2',
};
const boulder: SessionEntry = {
  entryId: 'source-boulder-entry', clientEntryId: 'source-boulder-client', entryOrder: 2,
  resultType: 'ASCENT', climbId: 'climb-1', climbName: 'Blue problem', climbType: 'BOULDER', gradeLabel: '6A',
  effortProfileCode: 'BOULDER', effortProfileName: 'Boulder', ascentMode: 'OS', totalMoves: 8, executedMoves: 8,
  notes: 'Keep the boulder notes', calculation, history: { conflictConfirmed: true, attemptOrdinalAllTime: 2, familiarityBand: 'LOW' },
};
const source: SessionDetails = {
  sessionId: 'source-session', clientSessionId: 'source-client-session', version: 7,
  sessionName: 'Bronx training', sessionDate: '2024-03-14', timeZoneId: 'Europe/Warsaw',
  startedAt: '2024-03-14T17:00:00Z', endedAt: '2024-03-14T19:00:00Z',
  areaId: 'area-1', areaName: 'Bronx', durationMinutes: 120, totalMoves: 72,
  classicLoad: 100, adjustedLoad: 160, averageMoveIntensity: 8, maxBoulderGrade: '6A', maxRouteGrade: null,
  calculationModelVersion: 'v2', entries: [
    { entryId: 'source-warmup-entry', clientEntryId: 'source-warmup-client', entryOrder: 0,
      resultType: 'WARMUP', executedMoves: 40, climbName: null, effortProfileName: null,
      calculation: { ...calculation, profileIntensityIndex: null, moveIntensity: null, adjustedLoad: 0, classicLoad: 0 }, history: null },
    { ...boulder, entryId: 'source-attempt-entry', clientEntryId: 'source-attempt-client', entryOrder: 1,
      resultType: 'ATTEMPT', ascentMode: 'RP', executedMoves: 4 },
    boulder,
    { ...boulder, entryId: 'source-route-entry', clientEntryId: 'source-route-client', entryOrder: 3,
      climbId: 'route-1', climbName: 'Yellow route', climbType: 'ROUTE', gradeLabel: '7a',
      effortProfileCode: 'MEDIUM_CONTINUOUS', effortProfileName: 'Medium continuous', ascentMode: 'FLASH',
      protectionMode: 'TOP_ROPE', totalMoves: 20, executedMoves: 20 },
  ],
};
const userId = 'repeat-user';
type Write = { method: string; body: SessionCommand };

async function api(page: Page, options: { abortFirstSave?: boolean } = {}) {
  const writes: Write[] = [];
  await page.route('**/api/v1/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname.replace('/api/v1', '');
    if (request.method() !== 'GET') {
      writes.push({ method: request.method(), body: request.postDataJSON() });
      if (options.abortFirstSave && writes.length === 1) return route.abort('failed');
      return route.fulfill({ json: { sessionId: 'new-session' } });
    }
    const created = writes.at(-1)?.body;
    const newDetails = { ...source, ...created, sessionId: 'new-session', version: 0,
      entries: created?.entries.map(entry => ({ ...entry, entryId: entry.clientEntryId, effortProfileName: 'Boulder', calculation, history: null })) ?? source.entries };
    const data = path === '/identity/me' ? { userId, displayName: 'Mihu' }
      : path === '/training/sessions/source-session' ? source
      : path === '/training/sessions/new-session' ? newDetails
      : path === '/catalog/areas' ? [{ id: 'area-1', name: 'Bronx', areaType: 'GYM', isPublic: true }]
      : path.endsWith('/sectors') ? []
      : path.endsWith('/climbs') ? [{ id: 'climb-1', areaId: 'area-1', name: 'Blue problem', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', suggestedMoveCount: 8, isPublic: false }]
      : path === '/catalog/grades' ? [{ id: 'grade-1', label: '6A' }, { id: 'grade-2', label: '7a' }]
      : path === '/catalog/effort-profiles' ? [
        { code: 'BOULDER', displayName: 'Boulder', description: 'Boulder', profileIntensityIndex: 10 },
        { code: 'MEDIUM_CONTINUOUS', displayName: 'Medium continuous', description: 'Sustained route', profileIntensityIndex: 7 },
      ] : [];
    await route.fulfill({ json: data });
  });
  return writes;
}

async function chooseDate(page: Page, start = '2026-11-04T18:30') {
  await expect(page.getByRole('heading', { name: 'Choose session date', exact: true })).toBeVisible();
  await page.getByLabel('Start', { exact: true }).fill(start);
  await page.getByRole('button', { name: 'Continue to entries', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(4);
}

test('manual start sets end one hour later across midnight while end remains editable and session edits preserve it', async ({ page }) => {
  await api(page);
  await page.goto('/sessions/new');
  await page.getByLabel('Time zone', { exact: true }).fill('Europe/Warsaw');
  await page.getByLabel('Start', { exact: true }).fill('2026-11-04T18:20');
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2026-11-04T19:20');
  await page.getByLabel('End', { exact: true }).fill('2026-11-04T22:00');
  await page.getByLabel('Start', { exact: true }).fill('2026-12-31T23:30');
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2027-01-01T00:30');
  await page.getByLabel('End', { exact: true }).fill('2027-01-01T01:45');
  await page.getByLabel('Session name (optional)').fill('My custom duration');
  await page.reload();
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2027-01-01T01:45');
  await page.goto('/sessions/source-session/edit');
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2024-03-14T20:00');
  await page.getByLabel('Start', { exact: true }).fill('2024-03-14T17:30');
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2024-03-14T20:00');
});

test('repeat chooses dates first then edits copied entries and creates a fresh session retaining climb identities', async ({ page }) => {
  const writes = await api(page);
  await page.goto('/sessions/source-session');
  await page.getByRole('link', { name: 'Repeat session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/source-session\/repeat$/);
  await expect(page.getByRole('heading', { name: 'New session', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Choose session date', exact: true })).toBeVisible();
  await expect(page.locator('.draft-entry')).toHaveCount(0);
  await expect(page.getByLabel('Start', { exact: true })).not.toHaveValue(/2024-03-14/);
  await chooseDate(page);
  await expect(page.getByRole('combobox', { name: 'Place', exact: true })).toHaveCount(0);
  await expect(page.getByText(/OS.*Flash.*RP|OS\/Flash.*RP/).first()).toBeVisible();
  await page.locator('.draft-entry').nth(1).getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Executed moves', { exact: true }).fill('6');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/new-session$/);
  expect(writes).toHaveLength(1);
  expect(writes[0].method).toBe('POST');
  const command = writes[0].body;
  expect(command.clientSessionId).not.toBe(source.clientSessionId);
  expect(command).toMatchObject({ areaId: source.areaId, sessionDate: '2026-11-04', startedAt: '2026-11-04T17:30:00.000Z', endedAt: '2026-11-04T18:30:00.000Z' });
  expect(command.entries.map(entry => entry.entryOrder)).toEqual([0, 1, 2, 3]);
  expect(new Set(command.entries.map(entry => entry.clientEntryId)).size).toBe(4);
  expect(command.entries.every(entry => !source.entries.some(old => old.clientEntryId === entry.clientEntryId))).toBe(true);
  expect(command.entries).toMatchObject([
    { resultType: 'WARMUP', executedMoves: 40 },
    { resultType: 'ATTEMPT', climbId: 'climb-1', executedMoves: 6, ascentMode: 'RP' },
    { resultType: 'ASCENT', climbId: 'climb-1', executedMoves: 8, ascentMode: 'RP', historyConflictConfirmed: false },
    { resultType: 'ASCENT', climbId: 'route-1', executedMoves: 20, ascentMode: 'RP', protectionMode: 'TOP_ROPE', historyConflictConfirmed: false },
  ]);
  expect(command.entries.every(entry => !('calculation' in entry) && !('history' in entry) && !('entryId' in entry))).toBe(true);
  expect('expectedVersion' in command).toBe(false);
  expect(source.entries[1].executedMoves).toBe(4);
});

test('repeated draft keeps its dates, edited entries and setup step across refresh without replacing a normal manual draft', async ({ page }) => {
  await api(page);
  const manual = newDraft(userId, 'manual', new Date('2026-10-10T12:00:00Z'));
  manual.command.sessionName = 'Keep this ordinary draft';
  const manualKey = draftKey(userId, 'manual'), rawManual = JSON.stringify(manual);
  await page.addInitScript(({ key, value }) => {
    if (!localStorage.getItem(key)) localStorage.setItem(key, value);
  }, { key: manualKey, value: rawManual });
  await page.goto('/sessions/source-session/repeat');
  await chooseDate(page, '2026-11-05T21:15');
  await page.locator('.draft-entry').first().getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Executed moves', { exact: true }).fill('55');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await page.getByRole('button', { name: 'Change date', exact: true }).click();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Choose session date', exact: true })).toBeVisible();
  await expect(page.getByLabel('Start', { exact: true })).toHaveValue('2026-11-05T21:15');
  await expect(page.getByLabel('End', { exact: true })).toHaveValue('2026-11-05T22:15');
  await page.getByRole('button', { name: 'Continue to entries', exact: true }).click();
  await expect(page.locator('.draft-entry').first()).toContainText('55');
  await page.reload();
  await expect(page.locator('.draft-entry')).toHaveCount(4);
  await expect(page.locator('.draft-entry').first()).toContainText('55');
  expect(await page.evaluate(key => localStorage.getItem(key), manualKey)).toBe(rawManual);
  await page.goto('/sessions/new');
  await expect(page.getByLabel('Session name (optional)')).toHaveValue('Keep this ordinary draft');
});

test('repeated session retries the identical pending create request after a refresh', async ({ page }) => {
  const writes = await api(page, { abortFirstSave: true });
  await page.goto('/sessions/source-session/repeat');
  await chooseDate(page);
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Retry save', exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Retry save', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/new-session$/);
  expect(writes).toHaveLength(2);
  expect(writes[0]).toEqual(writes[1]);
  expect(await page.evaluate(() => Object.keys(localStorage).filter(key => key.includes('session-draft')))).toHaveLength(0);
});
