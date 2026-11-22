import { test, expect, type Page } from '@playwright/test';
import type { SessionCommand, SessionDetails, SessionEntry } from '../../src/features/training/model/session.types';
import { draftKey, newDraft, type SessionDraft } from '../../src/features/training/model/session-draft';

const userId = 'entry-repeat-user';
const key = draftKey(userId, 'manual');
const calculation: SessionEntry['calculation'] = {
  adjustedLoad: 20, classicLoad: 40, profileIntensityIndex: 10, crux: null, moveIntensity: 8.5,
  styleMultiplier: 1, relativeEffortMultiplier: 1, currentLevelGradeLabel: '6A', currentLevelIndex: 4,
  relativeGradeDifference: 0, calculationModelVersion: 'v2',
};
const climbs = [
  { id: 'blue', areaId: 'area-1', name: 'Blue problem', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', suggestedMoveCount: 8, isPublic: false },
  { id: 'orange', areaId: 'area-1', name: 'Orange problem', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', suggestedMoveCount: 8, isPublic: false },
];

async function api(page: Page, abortFirstSave = false) {
  const writes: SessionCommand[] = [];
  await page.route('**/api/v1/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname.replace('/api/v1', '');
    if (request.method() !== 'GET') {
      expect(request.method()).toBe('POST');
      writes.push(request.postDataJSON());
      if (abortFirstSave && writes.length === 1) return route.abort('failed');
      return route.fulfill({ json: { sessionId: 'saved-session' } });
    }
    const command = writes.at(-1);
    const details: SessionDetails | null = command ? {
      ...command, areaId: 'area-1', areaName: 'Bronx', sessionId: 'saved-session', sessionName: command.sessionName || 'Repeated entries',
      version: 0, durationMinutes: 60, totalMoves: command.entries.reduce((sum, entry) => sum + entry.executedMoves, 0),
      classicLoad: 40, adjustedLoad: 20, averageMoveIntensity: 8.5, maxBoulderGrade: '6A', maxRouteGrade: null,
      calculationModelVersion: 'v2', entries: command.entries.map(entry => ({ ...entry, entryId: entry.clientEntryId,
        climbName: entry.climbName ?? null, effortProfileName: 'Boulder', calculation, history: null })),
    } : null;
    const data = path === '/identity/me' ? { userId, displayName: 'Mihu' }
      : path === '/training/sessions/saved-session' ? details
      : path === '/catalog/areas' ? [{ id: 'area-1', name: 'Bronx', areaType: 'GYM', isPublic: true }]
      : path.endsWith('/sectors') ? []
      : path.endsWith('/climbs') ? climbs
      : path === '/catalog/grades' ? [{ id: 'grade-1', label: '6A' }]
      : path === '/catalog/effort-profiles' ? [{ code: 'BOULDER', displayName: 'Boulder', description: 'Boulder', profileIntensityIndex: 10 }] : [];
    await route.fulfill({ json: data });
  });
  return writes;
}

async function addKnown(page: Page, climbId: string, attempt = false) {
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption(climbId);
  if (attempt) {
    await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ATTEMPT');
    await page.getByLabel('Executed moves', { exact: true }).fill('2');
    await page.getByRole('textbox', { name: 'Entry notes', exact: true }).fill('Keep the original beta');
  }
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
}

async function savedDraft(page: Page): Promise<SessionDraft> {
  return page.evaluate(draftStorageKey => JSON.parse(localStorage.getItem(draftStorageKey)!), key);
}

test('Repeat climb inserts an independent known-climb attempt which survives refresh and saves in order', async ({ page }) => {
  const writes = await api(page);
  await page.goto('/sessions/new');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await addKnown(page, 'blue', true);
  await addKnown(page, 'orange');
  const before = await savedDraft(page);
  await page.locator('.draft-entry').first().getByRole('button', { name: 'Repeat climb', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(3);
  const repeated = await savedDraft(page);
  const [source, copy, tail] = repeated.command.entries;
  expect(source).toEqual(before.command.entries[0]);
  expect(copy).toEqual({ ...source, clientEntryId: copy.clientEntryId, entryOrder: 1, historyConflictConfirmed: false });
  expect(copy.clientEntryId).not.toBe(source.clientEntryId);
  expect(tail.climbId).toBe('orange');
  expect(tail.entryOrder).toBe(2);
  await page.locator('.draft-entry').nth(1).getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Executed moves', { exact: true }).fill('5');
  await page.getByRole('textbox', { name: 'Entry notes', exact: true }).fill('Adjusted copy');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await page.reload();
  await expect(page.locator('.draft-entry')).toHaveCount(3);
  await expect(page.locator('.draft-entry').first()).toContainText('2 / 8 moves');
  await expect(page.locator('.draft-entry').nth(1)).toContainText('5 / 8 moves');
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/saved-session$/);
  expect(writes).toHaveLength(1);
  expect(writes[0].entries.map(entry => entry.climbId)).toEqual(['blue', 'blue', 'orange']);
  expect(writes[0].entries.map(entry => entry.executedMoves)).toEqual([2, 5, 8]);
  expect(writes[0].entries.map(entry => entry.entryOrder)).toEqual([0, 1, 2]);
  expect(writes[0].entries.map(entry => entry.notes)).toEqual(['Keep the original beta', 'Adjusted copy', undefined]);
  expect(new Set(writes[0].entries.map(entry => entry.clientEntryId)).size).toBe(3);
});

test('repeating a new private climb reuses its catalog identity while entry copies can be removed independently', async ({ page }) => {
  const writes = await api(page);
  await page.goto('/sessions/new');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Entry source', exact: true }).selectOption('new');
  await page.getByLabel('Climb name', { exact: true }).fill('New white problem');
  await page.getByRole('combobox', { name: 'Grade (canonical)', exact: true }).selectOption('6A');
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ATTEMPT');
  await page.getByRole('combobox', { name: 'Ascent mode', exact: true }).selectOption('RP');
  await page.getByLabel('Executed moves', { exact: true }).fill('2');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await page.locator('.draft-entry').first().getByRole('button', { name: 'Repeat climb', exact: true }).click();
  await page.locator('.draft-entry').last().getByRole('button', { name: 'Repeat climb', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(3);
  const copied = await savedDraft(page);
  expect(copied.command.entries.every(entry => entry.clientClimbId === copied.command.entries[0].clientClimbId)).toBe(true);
  expect(copied.command.entries[0].clientClimbId).toBeTruthy();
  await page.locator('.draft-entry').nth(1).getByRole('button', { name: 'Remove entry', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(2);
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/saved-session$/);
  expect(writes[0].entries).toHaveLength(2);
  expect(writes[0].entries.every(entry => !entry.climbId && entry.clientClimbId === copied.command.entries[0].clientClimbId)).toBe(true);
  expect(writes[0].entries.map(entry => entry.clientEntryId)).toEqual([copied.command.entries[0].clientEntryId, copied.command.entries[2].clientEntryId]);
  expect(writes[0].entries.map(entry => entry.entryOrder)).toEqual([0, 1]);
});

test('repeated entries keep exact styles and preserve one pending request through retry', async ({ page }) => {
  const writes = await api(page, true);
  const draft = newDraft(userId, 'manual');
  draft.command.areaId = 'area-1';
  draft.command.entries = [{ clientEntryId: 'original-entry', entryOrder: 0, resultType: 'ASCENT', climbId: 'blue',
    climbName: 'Blue problem', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', ascentMode: 'OS',
    totalMoves: 8, executedMoves: 8, notes: 'Exact copied ascent', historyConflictConfirmed: true }];
  await page.addInitScript(({ storageKey, value }) => {
    if (!localStorage.getItem(storageKey)) localStorage.setItem(storageKey, value);
  }, { storageKey: key, value: JSON.stringify(draft) });
  await page.goto('/sessions/new');
  await page.locator('.draft-entry').first().getByRole('button', { name: 'Repeat climb', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(2);
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Retry save', exact: true })).toBeVisible();
  await expect(page.locator('.draft-entry').last().getByRole('button', { name: 'Repeat climb', exact: true })).toBeDisabled();
  await page.reload();
  await page.getByRole('button', { name: 'Retry save', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/saved-session$/);
  expect(writes).toHaveLength(2);
  expect(writes[0]).toEqual(writes[1]);
  expect(writes[0].entries.map(entry => entry.ascentMode)).toEqual(['OS', 'OS']);
  expect(writes[0].entries.map(entry => entry.historyConflictConfirmed)).toEqual([true, false]);
  expect(writes[0].entries[1].clientEntryId).not.toBe('original-entry');
});

test('Completed requires exactly the full climb while partial attempts remain editable', async ({ page }) => {
  const writes = await api(page);
  await page.goto('/sessions/new');
  await page.getByRole('combobox', { name: 'Place', exact: true }).selectOption('area-1');
  await page.getByRole('button', { name: '+ Add entry', exact: true }).click();
  await page.getByRole('combobox', { name: 'Climb', exact: true }).selectOption('blue');
  await page.getByLabel('Executed moves', { exact: true }).fill('2');
  await expect(page.getByLabel('Executed moves', { exact: true })).toHaveAttribute('aria-invalid', 'true');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Completed climbs require exactly 8 executed moves');
  await expect(page.locator('.draft-entry')).toHaveCount(0);
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ATTEMPT');
  await page.getByRole('button', { name: 'Add to session', exact: true }).click();
  await expect(page.locator('.draft-entry')).toHaveCount(1);
  await expect(page.locator('.draft-entry')).toContainText('ATTEMPT');
  await expect(page.locator('.draft-entry')).toContainText('2 / 8 moves');
  await page.locator('.draft-entry').getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ASCENT');
  await expect(page.getByLabel('Executed moves', { exact: true })).toHaveValue('8');
  await page.getByLabel('Total moves', { exact: true }).fill('12');
  await expect(page.getByLabel('Executed moves', { exact: true })).toHaveValue('12');
  await page.getByLabel('Total moves', { exact: true }).fill('8');
  await expect(page.getByLabel('Executed moves', { exact: true })).toHaveValue('8');
  await page.getByLabel('Executed moves', { exact: true }).fill('10');
  await expect(page.getByLabel('Executed moves', { exact: true })).toHaveAttribute('aria-invalid', 'true');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Completed climbs require exactly 8 executed moves');
  await expect(page.locator('.draft-entry')).toContainText('ATTEMPT');
  await expect(page.locator('.draft-entry')).toContainText('2 / 8 moves');
  await page.getByLabel('Executed moves', { exact: true }).fill('8');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await expect(page.locator('.draft-entry')).toContainText('ASCENT');
  await expect(page.locator('.draft-entry')).toContainText('8 / 8 moves');
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/saved-session$/);
  expect(writes).toHaveLength(1);
  expect(writes[0].entries[0]).toMatchObject({ resultType: 'ASCENT', totalMoves: 8, executedMoves: 8 });
});

test('an old incomplete Completed draft cannot be saved until the entry is corrected to Attempt', async ({ page }) => {
  const writes = await api(page);
  const draft = newDraft(userId, 'manual');
  draft.command.areaId = 'area-1';
  draft.command.entries = [{ clientEntryId: 'old-incomplete-entry', entryOrder: 0, resultType: 'ASCENT', climbId: 'blue',
    climbName: 'Blue problem', climbType: 'BOULDER', gradeLabel: '6A', effortProfileCode: 'BOULDER', ascentMode: 'RP',
    totalMoves: 8, executedMoves: 2, historyConflictConfirmed: false }];
  await page.addInitScript(({ storageKey, value }) => {
    if (!localStorage.getItem(storageKey)) localStorage.setItem(storageKey, value);
  }, { storageKey: key, value: JSON.stringify(draft) });
  await page.goto('/sessions/new');
  await expect(page.locator('.draft-entry')).toContainText('Completed climbs require exactly 8 executed moves');
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Entry #1: Completed climbs require exactly 8 executed moves');
  expect(writes).toHaveLength(0);
  await page.locator('.draft-entry').getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByRole('combobox', { name: 'Result', exact: true }).selectOption('ATTEMPT');
  await page.getByRole('button', { name: 'Apply entry changes', exact: true }).click();
  await page.getByRole('button', { name: 'Finish & save session', exact: true }).click();
  await expect(page).toHaveURL(/\/sessions\/saved-session$/);
  expect(writes).toHaveLength(1);
  expect(writes[0].entries[0]).toMatchObject({ resultType: 'ATTEMPT', totalMoves: 8, executedMoves: 2 });
});
