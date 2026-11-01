import { test } from 'node:test';
import assert from 'node:assert/strict';
import { newDraft, commandFromDraft, draftKey, parseDraft } from '../src/features/training/model/session-draft.ts';
import { groupEntries, zonedInput, zonedInstant } from '../src/features/training/model/session.helpers.ts';

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
