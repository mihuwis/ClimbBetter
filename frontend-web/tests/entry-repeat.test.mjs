import { test } from 'node:test';
import assert from 'node:assert/strict';
import { repeatEntry } from '../src/features/training/model/entry-repeat.ts';

const attempt = {
  clientEntryId: 'original-attempt', entryOrder: 1, resultType: 'ATTEMPT',
  climbId: 'known-route', climbName: 'Yellow route', climbType: 'ROUTE', gradeLabel: '7a',
  effortProfileCode: 'MEDIUM_CRUX', ascentMode: 'RP', protectionMode: 'TOP_ROPE',
  totalMoves: 20, executedMoves: 7, notes: 'Repeat exactly these moves', historyConflictConfirmed: true,
};

test('repeat inserts directly after the source, keeps every input and reindexes without changing originals', () => {
  const entries = [
    { clientEntryId: 'warmup', entryOrder: 0, resultType: 'WARMUP', executedMoves: 40 },
    attempt,
    { ...attempt, clientEntryId: 'later-entry', entryOrder: 2, executedMoves: 12 },
  ];
  const before = structuredClone(entries);
  const result = repeatEntry(entries, attempt.clientEntryId);
  assert.deepEqual(entries, before);
  assert.equal(result.length, 4);
  assert.deepEqual(result.map(entry => entry.entryOrder), [0, 1, 2, 3]);
  assert.equal(result[1].clientEntryId, attempt.clientEntryId);
  assert.equal(result[3].clientEntryId, 'later-entry');
  const copy = result[2];
  assert.notEqual(copy.clientEntryId, attempt.clientEntryId);
  assert.deepEqual(copy, { ...attempt, clientEntryId: copy.clientEntryId, entryOrder: 2, historyConflictConfirmed: false });
  copy.notes = 'Only the copy changes';
  assert.equal(attempt.notes, 'Repeat exactly these moves');
});

test('completed OS or Flash entries keep their declared style and completed result', () => {
  for (const ascentMode of ['OS', 'FLASH', 'RP']) {
    const original = { ...attempt, resultType: 'ASCENT', executedMoves: 20, ascentMode };
    const copy = repeatEntry([original], original.clientEntryId)[1];
    assert.equal(copy.resultType, 'ASCENT');
    assert.equal(copy.ascentMode, ascentMode);
    assert.equal(copy.totalMoves, 20);
    assert.equal(copy.executedMoves, 20);
    assert.equal(copy.historyConflictConfirmed, false);
  }
});

test('new private climbs retain one shared clientClimbId across repeated attempts with fresh entry IDs', () => {
  const original = { ...attempt, climbId: null, clientClimbId: 'local-climb', resultType: 'ATTEMPT', executedMoves: 2 };
  const once = repeatEntry([original], original.clientEntryId);
  const twice = repeatEntry(once, once[1].clientEntryId);
  assert.equal(once.length, 2);
  assert.equal(twice.length, 3);
  assert.equal(new Set(twice.map(entry => entry.clientEntryId)).size, 3);
  assert.deepEqual(twice.map(entry => entry.clientClimbId), ['local-climb', 'local-climb', 'local-climb']);
  assert.deepEqual(twice.map(entry => entry.climbId), [null, null, null]);
  assert.deepEqual(twice.map(entry => entry.executedMoves), [2, 2, 2]);
  assert.deepEqual(twice.map(entry => entry.entryOrder), [0, 1, 2]);
});

test('repeat copies input fields only and never carries source snapshot, calculation or history evidence', () => {
  const snapshot = { ...attempt, entryId: 'saved-entry', effortProfileName: 'Medium crux',
    calculation: { moveIntensity: 6.3, adjustedLoad: 44.1 }, history: { conflictConfirmed: true, attemptOrdinalAllTime: 8 } };
  const copy = repeatEntry([snapshot], snapshot.clientEntryId)[1];
  for (const field of ['entryId', 'calculation', 'history', 'effortProfileName']) assert.equal(field in copy, false);
  assert.equal(copy.historyConflictConfirmed, false);
  assert.equal(copy.climbId, snapshot.climbId);
  assert.deepEqual(snapshot.calculation, { moveIntensity: 6.3, adjustedLoad: 44.1 });
});

test('warm-ups can repeat without artificial climbing fields, while missing source is an explicit error', () => {
  const original = { clientEntryId: 'warmup', entryOrder: 0, resultType: 'WARMUP', executedMoves: 30, notes: 'Easy traverses' };
  const copy = repeatEntry([original], original.clientEntryId)[1];
  assert.deepEqual(copy, { ...original, clientEntryId: copy.clientEntryId, entryOrder: 1 });
  assert.equal('climbId' in copy, false);
  assert.equal('historyConflictConfirmed' in copy, false);
  assert.throws(() => repeatEntry([original], 'missing'), /no longer in this session/);
  assert.throws(() => repeatEntry([], original.clientEntryId), /no longer in this session/);
});
