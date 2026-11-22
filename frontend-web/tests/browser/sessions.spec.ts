import { test, expect, type Page } from '@playwright/test';
import type { EntryCommand, SessionDetails, SessionEntry, SessionSummary } from '../../src/features/training/model/session.types';
import type { EffortProfile } from '../../src/features/catalog/model/catalog.types';
import { draftKey, newDraft, type SessionDraft } from '../../src/features/training/model/session-draft';
import { toCommand } from '../../src/features/training/model/session.helpers';

const summary: SessionSummary = { sessionId: 'session-1', sessionName: 'Bronx evening', sessionDate: '2026-10-15', areaName: 'Bronx', durationMinutes: 120, totalMoves: 48, classicLoad: 85, adjustedLoad: 68, averageMoveIntensity: 8.5, maxBoulderGrade: '6A', maxRouteGrade: null, calculationModelVersion: 'v2' };
const calculation: SessionEntry['calculation'] = { adjustedLoad: 68, classicLoad: 85, profileIntensityIndex: 10, crux: null, moveIntensity: 8.5, styleMultiplier: 1, relativeEffortMultiplier: 1, currentLevelGradeLabel: '6A', currentLevelIndex: 4, relativeGradeDifference: 0, calculationModelVersion: 'v2' };
const warmup: SessionEntry = { entryId: 'warmup-id', clientEntryId: 'warmup-1', entryOrder: 0, resultType: 'WARMUP', climbName: null, effortProfileName: null, executedMoves: 40, calculation: { ...calculation, adjustedLoad: 0, classicLoad: 0, profileIntensityIndex: null, moveIntensity: null, styleMultiplier: null, relativeEffortMultiplier: null, currentLevelGradeLabel: null, currentLevelIndex: null, relativeGradeDifference: null }, history: null };
const climb = { id: 'climb-1', areaId: 'area-1', name: 'Niebieski01', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', suggestedMoveCount: 8, isPublic: false };
const entry: SessionEntry = { entryId: 'entry-id', clientEntryId: 'entry-1', entryOrder: 1, resultType: 'ASCENT', climbId: climb.id, climbName: climb.name, climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', effortProfileName: 'Boulder', ascentMode: 'RP', totalMoves: 8, executedMoves: 8, calculation, history: { conflictConfirmed: false, attemptOrdinalAllTime: 1, familiarityBand: 'LOW' } };
const details: SessionDetails = { ...summary, clientSessionId: 'client-session-1', version: 0, areaId: 'area-1', timeZoneId: 'Europe/Warsaw', startedAt: '2026-10-15T16:00:00Z', endedAt: '2026-10-15T18:00:00Z', entries: [warmup, entry] };
const user = { userId: 'mihu-1', displayName: 'Mihu' };
const emptyBests = { overall: null, os: null, flash: null, rp: null, rpVsOsGap: null };
const profiles: EffortProfile[] = [
  { code: 'BOULDER', displayName: 'Boulder', description: 'Usually 4–10 moves; may be shorter or longer.', profileIntensityIndex: 10 },
  { code: 'SHORT_BOULDERY_ROUTE', displayName: 'Short bouldery route', description: 'A bouldery route with 4–12 moves.', profileIntensityIndex: 10 },
  { code: 'MEDIUM_CONTINUOUS', displayName: 'Medium continuous', description: 'Sustained climbing, usually 15–30 moves.', profileIntensityIndex: 7 },
  { code: 'MEDIUM_CRUX', displayName: 'Medium crux', description: 'One or two cruxes with easier sections and rests.', profileIntensityIndex: 7 },
  { code: 'LONG_CONTINUOUS', displayName: 'Long continuous', description: 'Long sustained climbing.', profileIntensityIndex: 5 },
  { code: 'LONG_CRUX', displayName: 'Long crux', description: 'A long route with large easier sections around its cruxes.', profileIntensityIndex: 5 },
  { code: 'CIRCUIT', displayName: 'Circuit', description: 'An indoor climbing circuit.', profileIntensityIndex: 7 },
];
type ApiOptions = { session?: SessionDetails | (() => SessionDetails); profiles?: EffortProfile[]; climbs?: unknown[] };

async function api(page: Page, mutate?: (method: string, body: Record<string, unknown> | null, url: URL) => unknown, options: ApiOptions = {}) {
  await page.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname.replace('/api/v1', '');
    if (req.method() !== 'GET') {
      const result = mutate?.(req.method(), req.method() === 'DELETE' ? null : req.postDataJSON(), url);
      if (result === 'abort') { await route.abort('failed'); return; }
      await route.fulfill({ status: req.method() === 'DELETE' ? 204 : 200, json: req.method() === 'DELETE' ? undefined : result ?? { sessionId: summary.sessionId } }); return;
    }
    const data = path === '/identity/me' ? user
      : path === '/dashboard/sessions/page' ? { items: [summary], nextCursor: null }
      : path === '/dashboard/sessions' || path === '/training/sessions' ? [summary]
      : path === '/dashboard/personal-bests' ? { boulder: { ...emptyBests, overall: { gradeLabel: '6A', achievedOn: summary.sessionDate, sessionId: summary.sessionId } }, route: emptyBests }
      : path.startsWith('/training/sessions/') ? (typeof options.session === 'function' ? options.session() : options.session ?? details)
      : path === '/catalog/areas' ? [{ id: 'area-1', name: 'Bronx', areaType: 'GYM', facilityType: 'BOULDER_GYM', isPublic: true }]
      : path.endsWith('/sectors') ? []
      : path.endsWith('/climbs') ? options.climbs ?? [climb]
      : path === '/catalog/grades' ? [{ id: 'grade-1', label: '6A' }, { id: 'grade-2', label: '6B' }, { id: 'grade-3', label: '7a' }]
      : path === '/catalog/effort-profiles' ? options.profiles ?? profiles : [];
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
for (const mode of ['create', 'edit'] as const) {
  test(`${mode}: a time conflict keeps the draft and allows correcting the times`, async ({ page }) => {
    const commands: Record<string, unknown>[] = [];
    const message = 'You already have a session during this time. Change the start or end time.';
    await api(page);
    await page.route(mode === 'edit' ? '**/api/v1/training/sessions/session-1' : '**/api/v1/training/sessions', async route => {
      if (route.request().method() === 'GET') return route.fallback();
      const body = route.request().postDataJSON();
      commands.push(mode === 'edit' ? body.session : body);
      if (commands.length === 1) {
        await route.fulfill({ status: 409, json: { code: 'SESSION_TIME_CONFLICT', detail: message } });
      } else {
        await route.fulfill({ status: mode === 'edit' ? 200 : 201, json: { sessionId: summary.sessionId } });
      }
    });
    await page.goto(mode === 'edit' ? '/sessions/session-1/edit' : '/sessions/new');
    if (mode === 'create') {
      await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
      await addWarmup(page);
    } else {
      await page.getByLabel('Reason for correction').fill('Correct session time');
    }
    await page.getByLabel('Time zone', { exact: true }).fill('UTC');
    await page.getByLabel('Start', { exact: true }).fill('2026-10-15T18:00');
    await page.getByLabel('End', { exact: true }).fill('2026-10-15T20:00');
    const saveLabel = mode === 'edit' ? 'Save changes' : 'Finish & save session';
    await page.getByRole('button', { name: saveLabel, exact: true }).click();
    await expect(page.getByRole('alert')).toContainText(message);
    await expect(page.getByRole('button', { name: saveLabel, exact: true })).toBeEnabled();
    await expect(page.getByRole('button', { name: 'Retry save', exact: true })).toHaveCount(0);

    await page.reload();
    await expect(page.locator('.draft-entry')).toHaveCount(mode === 'edit' ? 2 : 1);
    await page.getByLabel('Start', { exact: true }).fill('2026-10-15T20:00');
    await page.getByLabel('End', { exact: true }).fill('2026-10-15T21:00');
    await page.getByRole('button', { name: saveLabel, exact: true }).click();
    await expect(page).toHaveURL(/sessions\/session-1$/);
    expect(commands).toHaveLength(2);
    expect(commands[1]).toMatchObject({
      clientSessionId: commands[0].clientSessionId, entries: commands[0].entries,
      startedAt: '2026-10-15T20:00:00.000Z', endedAt: '2026-10-15T21:00:00.000Z',
    });
  });
}

test('edit removes a climb, adds an entry and sends the complete versioned aggregate', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  await api(page, (method, body) => { if (method === 'PUT') saved = body; });
  await page.goto('/sessions/session-1/edit');
  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: "Remove climb's entries" }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(1);
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption('climb-1');
  await page.getByLabel('Total moves').fill('10');
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

const cruxEntry: SessionEntry = {
  ...entry, entryOrder: 0, climbId: 'route-1', climbName: 'Crux project', climbType: 'ROUTE',
  gradeLabel: '7a', effortProfileCode: 'MEDIUM_CRUX', effortProfileName: 'Medium crux',
  resultType: 'ATTEMPT', protectionMode: 'LEAD', totalMoves: 50, executedMoves: 40,
  calculation: { ...calculation, classicLoad: 90, profileIntensityIndex: 7, moveIntensity: 5.9169, adjustedLoad: 236.6757,
    currentLevelGradeLabel: '7a', crux: { hardMoveCount: 6, easyMoveCount: 34, hardMoveIntensity: 6.6405, easyMoveIntensity: 5.7892, baseMoveGradeLabel: '6B', hardMoveGradeLabel: '6C', easyMoveGradeLabel: '6A' } },
};

function draftWith(entries: EntryCommand[]): SessionDraft {
  const draft = newDraft(user.userId, 'manual', new Date('2026-10-15T18:00:00Z'));
  return { ...draft, command: { ...draft.command, areaId: 'area-1', entries } };
}

async function seedDraft(page: Page, draft: SessionDraft) {
  await page.addInitScript(({ key, draft }) => {
    if (!localStorage.getItem(key)) localStorage.setItem(key, JSON.stringify(draft));
  }, { key: draftKey(user.userId, draft.mode, draft.sessionId), draft });
}

test('v2 details show backend intensity and crux splits for completed climbs and attempts on mobile', async ({ page }) => {
  const session: SessionDetails = { ...details, totalMoves: 284, classicLoad: 542.5, adjustedLoad: 1306.6081, averageMoveIntensity: 5.355, maxBoulderGrade: '6B', maxRouteGrade: '7a', entries: [
    warmup,
    { ...entry, gradeLabel: '6B', totalMoves: 4, executedMoves: 4, calculation: { ...calculation, classicLoad: 92.5, moveIntensity: 9.25, adjustedLoad: 37 } },
    { ...cruxEntry, clientEntryId: 'continuous', entryOrder: 2, effortProfileCode: 'MEDIUM_CONTINUOUS', effortProfileName: 'Medium continuous', resultType: 'ASCENT', totalMoves: 20, executedMoves: 20, calculation: { ...calculation, classicLoad: 90, profileIntensityIndex: 7, moveIntensity: 6.3, adjustedLoad: 126 } },
    { ...cruxEntry, clientEntryId: 'crux-completed', entryOrder: 3, resultType: 'ASCENT', totalMoves: 30, executedMoves: 30, calculation: { ...cruxEntry.calculation, moveIntensity: 5.9311, adjustedLoad: 177.9324, crux: { ...cruxEntry.calculation.crux!, hardMoveCount: 5, easyMoveCount: 25 } } },
    { ...cruxEntry, clientEntryId: 'crux-attempt', entryOrder: 4 },
    { ...entry, clientEntryId: 'circuit', entryOrder: 5, climbType: 'CIRCUIT', gradeLabel: '7a', effortProfileCode: 'CIRCUIT', effortProfileName: 'Circuit', totalMoves: 30, executedMoves: 30, calculation: { ...calculation, classicLoad: 90, profileIntensityIndex: 7, moveIntensity: 6.3, adjustedLoad: 189 } },
    { ...cruxEntry, clientEntryId: 'long-continuous', entryOrder: 6, effortProfileCode: 'LONG_CONTINUOUS', effortProfileName: 'Long continuous', resultType: 'ASCENT', totalMoves: 120, executedMoves: 120, calculation: { ...calculation, classicLoad: 90, profileIntensityIndex: 5, moveIntensity: 4.5, adjustedLoad: 540 } },
  ] };
  await api(page, undefined, { session });
  await page.goto('/sessions/session-1');
  await page.getByRole('button', { name: 'Show chronology' }).click();
  const entries = page.locator('.entry-detail');
  await expect(entries).toHaveCount(7);
  await expect(entries.nth(0).getByText('Average move intensity')).toHaveCount(0);
  await expect(entries.nth(0).locator('details')).toHaveCount(0);
  await expect(entries.nth(1).locator('.stat-row')).toHaveText(['Adjusted Load37', 'Average move intensity9.25']);
  await expect(entries.nth(2).locator('.stat-row')).toHaveText(['Adjusted Load126', 'Average move intensity6.3']);
  await expect(entries.nth(6).locator('.stat-row')).toHaveText(['Adjusted Load540', 'Average move intensity4.5']);
  await expect(page.getByRole('region', { name: 'Estimated difficulty split' })).toHaveCount(2);
  await expect(entries.nth(3).locator('tbody tr').nth(0)).toHaveText('Harder5Font 6C6.64');
  await expect(entries.nth(3).locator('tbody tr').nth(1)).toHaveText('Easier25Font 6A5.79');
  await expect(entries.nth(4).locator('tbody tr').nth(0)).toHaveText('Harder6Font 6C6.64');
  await expect(entries.nth(4).locator('tbody tr').nth(1)).toHaveText('Easier34Font 6A5.79');
  await expect(entries.nth(4)).toContainText('40 / 50 moves');
  await entries.nth(5).getByText('Calculation details', { exact: true }).click();
  await expect(entries.nth(5).locator('dt', { hasText: /^Intensity index$/ }).locator('+ dd')).toHaveText('7');
  await expect(entries.nth(5).locator('dt', { hasText: /^Model version$/ }).locator('+ dd')).toHaveText('v2');
  await expect(page.getByText('EDL', { exact: true })).toHaveCount(0);
  await page.screenshot({ path: 'test-results/v2-details-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(entries.nth(4).getByRole('table')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.screenshot({ path: 'test-results/v2-details-mobile.png', fullPage: true });
});

test('crux editing sends input fields and displays the newly returned backend split', async ({ page }) => {
  let session: SessionDetails = { ...details, entries: [cruxEntry] };
  let saved: Record<string, unknown> | null = null;
  await api(page, (method, body) => {
    if (method === 'PUT') {
      saved = body;
      session = { ...session, version: 1, entries: [{ ...cruxEntry, executedMoves: 41, calculation: { ...cruxEntry.calculation, crux: { ...cruxEntry.calculation.crux!, hardMoveCount: 7, easyMoveCount: 34 } } }] };
    }
  }, { session: () => session });
  await page.goto('/sessions/session-1/edit?entry=entry-1');
  await expect(page.getByRole('combobox', { name: 'Effort profile', exact: true })).toHaveValue('MEDIUM_CRUX');
  await expect(page.getByText(/The model estimates a harder section/)).toBeVisible();
  await page.getByLabel('Executed moves').fill('41');
  await page.getByRole('button', { name: 'Apply entry changes' }).click();
  await page.getByLabel('Reason for correction').fill('Correct the attempt to 41 moves');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  await expect(page.locator('tbody tr').nth(0)).toHaveText('Harder7Font 6C6.64');
  const inputs = (saved!.session as { entries: EntryCommand[] }).entries;
  expect(inputs[0].executedMoves).toBe(41);
  expect(inputs[0]).not.toHaveProperty('calculation');
  expect(inputs[0]).not.toHaveProperty('profileIntensityIndex');
});

test('restored entries with retired profiles require an explicit repair and retain their data', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  const oldEntry: EntryCommand = { ...toCommand(details).entries[1], resultType: 'ATTEMPT', climbType: 'ROUTE', gradeLabel: '7a', effortProfileCode: 'SHORT_CONTINUOUS_ROUTE', totalMoves: 30, executedMoves: 23, notes: 'Keep this note' };
  await api(page, (_method, body) => { saved = body; });
  await seedDraft(page, draftWith([oldEntry]));
  await page.goto('/sessions/new');
  await expect(page.locator('.draft-entry')).toContainText('no longer available');
  await page.getByRole('button', { name: 'Finish & save session' }).click();
  await expect(page.getByRole('alert')).toContainText('Entry #1');
  expect(saved).toBeNull();
  await page.locator('.draft-entry').getByRole('button', { name: 'Edit', exact: true }).click();
  const profile = page.getByRole('combobox', { name: 'Effort profile', exact: true });
  await expect(profile).toHaveValue('SHORT_CONTINUOUS_ROUTE');
  await expect(profile.locator('option:checked')).toHaveText('Unavailable profile: SHORT_CONTINUOUS_ROUTE');
  await expect(profile.locator('option:checked')).toHaveJSProperty('disabled', true);
  await expect(page.getByLabel('Executed moves')).toHaveValue('23');
  await expect(page.getByLabel('Entry notes')).toHaveValue('Keep this note');
  await profile.selectOption('MEDIUM_CONTINUOUS');
  await expect(page.getByText('Sustained climbing, usually 15–30 moves.')).toBeVisible();
  await page.getByRole('button', { name: 'Apply entry changes' }).click();
  await page.getByRole('button', { name: 'Finish & save session' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  expect(saved).toMatchObject({ entries: [{ clientEntryId: oldEntry.clientEntryId, effortProfileCode: 'MEDIUM_CONTINUOUS', executedMoves: 23, totalMoves: 30, notes: 'Keep this note' }] });
});

test('unfinished and repeated draft entries retain unavailable profiles; boulders exclude crux choices', async ({ page }) => {
  const oldEntry: EntryCommand = { ...toCommand(details).entries[1], climbId: null, clientClimbId: 'draft-climb', effortProfileCode: 'MEDIUM_CRUX', notes: 'Unfinished note' };
  const draft = draftWith([oldEntry]);
  draft.editingEntryId = 'new';
  draft.entryDraft = { mode: 'new', entry: { ...oldEntry, clientEntryId: 'unfinished-entry' } };
  await api(page);
  await seedDraft(page, draft);
  await page.goto('/sessions/new');
  await expect(page.getByLabel('Entry notes')).toHaveValue('Unfinished note');
  const profile = page.getByRole('combobox', { name: 'Effort profile', exact: true });
  await expect(profile).toHaveValue('MEDIUM_CRUX');
  await expect(profile.locator('option[value="LONG_CRUX"]')).toHaveCount(0);
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('route or circuit');
  await page.getByLabel('Entry source').selectOption('draft');
  await page.getByLabel('Climb in this draft').selectOption('draft-climb');
  await expect(profile).toHaveValue('MEDIUM_CRUX');
  await profile.selectOption('BOULDER');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(2);
});

test('known climbs with a retired profile cannot silently become a different profile', async ({ page }) => {
  await api(page, undefined, { climbs: [{ ...climb, effortProfileCode: 'SHORT_CONTINUOUS_ROUTE' }] });
  await page.goto('/sessions/new');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption('climb-1');
  await expect(page.getByRole('combobox', { name: 'Effort profile', exact: true })).toHaveValue('SHORT_CONTINUOUS_ROUTE');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('no longer available');
  await expect(page.locator('.draft-entry')).toHaveCount(0);
});

test('pending saves retry the unchanged request even when the effort catalog is unavailable', async ({ page }) => {
  let saved: Record<string, unknown> | null = null;
  const draft = draftWith([{ ...toCommand(details).entries[1], effortProfileCode: 'SHORT_CONTINUOUS_ROUTE' }]);
  draft.pendingRequest = structuredClone(draft.command);
  await api(page, (_method, body) => { saved = body; });
  await page.route('**/api/v1/catalog/effort-profiles', route => route.fulfill({ status: 503, json: { detail: 'Catalog unavailable' } }));
  await seedDraft(page, draft);
  await page.goto('/sessions/new');
  await page.getByRole('button', { name: 'Retry save' }).click();
  await expect(page).toHaveURL(/sessions\/session-1$/);
  expect(saved).toEqual(draft.pendingRequest);
});

test('legacy results remain readable without an intensity index or crux split', async ({ page }) => {
  await api(page, undefined, { session: { ...details, calculationModelVersion: 'v1', entries: [{ ...entry, calculation: { ...calculation, profileIntensityIndex: null, crux: null, calculationModelVersion: 'v1' } }] } });
  await page.goto('/sessions/session-1');
  await expect(page.getByText('This session uses an earlier calculation model.')).toBeVisible();
  await page.getByText('Calculation details', { exact: true }).click();
  await expect(page.locator('dt', { hasText: /^Intensity index$/ }).locator('+ dd')).toHaveText('—');
  await expect(page.getByRole('region', { name: 'Estimated difficulty split' })).toHaveCount(0);
});
