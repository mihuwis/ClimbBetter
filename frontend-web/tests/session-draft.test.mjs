import { test } from 'node:test';
import assert from 'node:assert/strict';
import { newDraft, commandFromDraft, draftKey, parseDraft } from '../src/features/training/model/session-draft.ts';
import { groupEntries, toCommand, zonedInput, zonedInstant } from '../src/features/training/model/session.helpers.ts';
import { effortProfileIssue } from '../src/features/training/model/effort-profiles.ts';

test('session dates use the selected zone and reject nonexistent DST times', () => {
  assert.equal(zonedInput('2026-10-15T22:30:00Z', 'Europe/Warsaw'), '2026-10-16T00:30');
  assert.equal(zonedInstant('2026-10-16T00:30', 'Europe/Warsaw'), '2026-10-15T22:30:00.000Z');
  assert.equal(zonedInstant('2026-01-16T00:30', 'Europe/Warsaw'), '2026-01-15T23:30:00.000Z');
  assert.throws(() => zonedInstant('2026-03-29T02:30', 'Europe/Warsaw'), /does not exist/);
});
test('draft keys isolate users, modes and session edits; corrupt drafts stay explicit', () => {
  assert.notEqual(draftKey('mihu', 'live'), draftKey('local', 'live'));
  assert.notEqual(draftKey('mihu', 'manual'), draftKey('mihu', 'live'));
  assert.notEqual(draftKey('mihu', 'edit', 'a'), draftKey('mihu', 'edit', 'b'));
  assert.throws(() => parseDraft('{broken', 'mihu'));
  const draft = newDraft('mihu', 'manual');
  assert.deepEqual(parseDraft(JSON.stringify(draft), 'mihu'), draft);
  assert.throws(() => parseDraft(JSON.stringify(draft), 'local'));
});
test('pending save preserves its payload and running sessions cannot be finished', () => {
  const draft = newDraft('mihu', 'live');
  draft.running = true;
  assert.throws(() => commandFromDraft(draft), /Stop the timer/);
  draft.pendingRequest = { ...draft.command, entries: [{ clientEntryId: 'a', entryOrder: 0, resultType: 'WARMUP', executedMoves: 40 }] };
  assert.equal(commandFromDraft(draft), draft.pendingRequest);
});
test('climbs with equal names remain separate; returning to a climb keeps chronological order', () => {
  const entries = [
    { climbId: 'a', climbName: 'Green', clientEntryId: '3', entryOrder: 2 },
    { climbId: 'b', climbName: 'Green', clientEntryId: '2', entryOrder: 1 },
    { climbId: 'a', climbName: 'Green', clientEntryId: '1', entryOrder: 0 },
  ];
  const groups = groupEntries(entries);
  assert.equal(groups.length, 2);
  assert.deepEqual(groups[0].entries.map(e => e.clientEntryId), ['1', '3']);
  assert.deepEqual(entries.map(e => e.clientEntryId), ['3', '2', '1']);
});

test('retired profiles stay in old drafts until explicitly repaired; warm-ups need no profile', () => {
  const draft = newDraft('mihu', 'manual');
  draft.command.entries = [{ clientEntryId: 'old-entry', entryOrder: 0, resultType: 'ATTEMPT', climbType: 'ROUTE', effortProfileCode: 'SHORT_CONTINUOUS_ROUTE', executedMoves: 7 }];
  draft.entryDraft = { mode: 'new', entry: { ...draft.command.entries[0], notes: 'Keep these notes' } };
  const restored = parseDraft(JSON.stringify(draft), 'mihu');
  assert.equal(restored.schemaVersion, 1);
  assert.match(draftKey('mihu', 'manual'), /:v1:/);
  assert.deepEqual(restored.command.entries, draft.command.entries);
  assert.deepEqual(restored.entryDraft, draft.entryDraft);
  assert.match(effortProfileIssue(restored.command.entries[0], []), /no longer available/);
  assert.equal(effortProfileIssue({ resultType: 'WARMUP', executedMoves: 40 }, []), null);
  const profiles = [{ code: 'MEDIUM_CRUX' }, { code: 'CIRCUIT' }];
  assert.match(effortProfileIssue({ resultType: 'ASCENT', climbType: 'BOULDER', effortProfileCode: 'MEDIUM_CRUX' }, profiles), /route or circuit/);
  assert.equal(effortProfileIssue({ resultType: 'ASCENT', climbType: 'CIRCUIT', effortProfileCode: 'CIRCUIT' }, profiles), null);
});

test('editing sends only input fields, never saved intensity or crux calculations', () => {
  const input = { clientEntryId: 'entry-1', entryOrder: 0, resultType: 'ATTEMPT', climbId: 'route-1', climbName: 'Crux route', climbType: 'ROUTE', gradeLabel: '7a', effortProfileCode: 'LONG_CRUX', ascentMode: 'RP', protectionMode: 'LEAD', totalMoves: 50, executedMoves: 40, notes: 'Keep these notes' };
  const command = toCommand({ clientSessionId: 'session-1', sessionName: 'Test', sessionDate: '2026-10-15', timeZoneId: 'Europe/Warsaw', startedAt: '2026-10-15T16:00:00Z', endedAt: '2026-10-15T18:00:00Z', areaId: 'area-1', calculationModelVersion: 'v2', entries: [{ ...input, calculation: { profileIntensityIndex: 5, moveIntensity: 4.2, crux: { hardMoveCount: 6, easyMoveCount: 34 } }, history: { conflictConfirmed: true } }] });
  assert.deepEqual(command.entries, [{ ...input, historyConflictConfirmed: true }]);
  assert.equal('calculationModelVersion' in command, false);
  assert.equal('calculation' in command.entries[0], false);
});
