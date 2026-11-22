import { test } from 'node:test';
import assert from 'node:assert/strict';
import { repeatSessionDraft, suggestedEndInput } from '../src/features/training/model/session-repeat.ts';
import { commandFromDraft, draftKey, parseDraft } from '../src/features/training/model/session-draft.ts';

const calculation = { profileIntensityIndex: 10, crux: null, moveIntensity: 9.25, classicLoad: 92.5, adjustedLoad: 37,
  styleMultiplier: 1, relativeEffortMultiplier: 1, currentLevelGradeLabel: '6B', currentLevelIndex: 9, relativeGradeDifference: 0, calculationModelVersion: 'v2' };
const source = {
  sessionId: 'source-session', clientSessionId: 'source-client-session', version: 7, sessionName: 'Similar climbing',
  sessionDate: '2025-10-15', timeZoneId: 'Europe/Warsaw', startedAt: '2025-10-15T16:00:00Z', endedAt: '2025-10-15T18:00:00Z',
  areaId: 'bronx', areaName: 'Bronx', durationMinutes: 120, totalMoves: 51, classicLoad: 300, adjustedLoad: 210,
  averageMoveIntensity: 6, maxBoulderGrade: '6B', maxRouteGrade: '7a', calculationModelVersion: 'v2',
  entries: [
    { entryId: 'stored-ascent', clientEntryId: 'source-ascent', entryOrder: 4, resultType: 'ASCENT', climbId: 'boulder-a',
      climbName: 'Green', climbType: 'BOULDER', gradeLabel: '6B', effortProfileCode: 'BOULDER', effortProfileName: 'Boulder',
      ascentMode: 'OS', totalMoves: 4, executedMoves: 4, notes: 'Original beta', historyConflictConfirmed: true,
      history: { conflictConfirmed: true, attemptOrdinalAllTime: 7, familiarityBand: 'HIGH' }, calculation },
    { entryId: 'stored-warmup', clientEntryId: 'source-warmup', entryOrder: 0, resultType: 'WARMUP', executedMoves: 40,
      notes: 'Easy traversing', climbName: null, effortProfileName: null, history: null, calculation },
    { entryId: 'stored-attempt', clientEntryId: 'source-attempt', entryOrder: 2, resultType: 'ATTEMPT', climbId: 'route-a',
      climbName: 'Green', climbType: 'ROUTE', gradeLabel: '7a', effortProfileCode: 'MEDIUM_CRUX', effortProfileName: 'Medium crux',
      ascentMode: 'FLASH', protectionMode: 'TOP_ROPE', totalMoves: 20, executedMoves: 7, notes: 'Keep this attempt',
      history: { conflictConfirmed: false, attemptOrdinalAllTime: 1, familiarityBand: 'LOW' }, calculation },
  ],
};

test('manual end suggestion follows the selected date and crosses midnight, month and year', () => {
  assert.equal(suggestedEndInput('2026-11-04T17:15', 'Europe/Warsaw'), '2026-11-04T18:15');
  assert.equal(suggestedEndInput('2026-11-30T23:30', 'Europe/Warsaw'), '2026-12-01T00:30');
  assert.equal(suggestedEndInput('2026-12-31T23:30', 'Europe/Warsaw'), '2027-01-01T00:30');
  assert.equal(suggestedEndInput('2028-02-29T23:30', 'Europe/Warsaw'), '2028-03-01T00:30');
  assert.equal(suggestedEndInput('2026-11-04T17:15', 'Pacific/Auckland'), '2026-11-04T18:15');
});

test('suggestion uses the session zone and rejects incomplete, impossible and nonexistent times', () => {
  assert.equal(suggestedEndInput('2026-03-29T01:30', 'Europe/Warsaw'), '2026-03-29T03:30');
  assert.equal(suggestedEndInput('2026-03-29T02:30', 'Europe/Warsaw'), null);
  assert.equal(suggestedEndInput('2026-11-04T17:15', 'Not/AZone'), null);
  for (const invalid of ['', '2026-11-04', '2026-11-04T17:', '2026-02-30T17:15', '2026-11-04T24:00', 'not-a-date']) {
    assert.equal(suggestedEndInput(invalid, 'Europe/Warsaw'), null, invalid);
  }
});

test('repeat creates fresh session and entry identities while preserving climbs, input and order', () => {
  const before = structuredClone(source);
  const draft = repeatSessionDraft('mihu', source, new Date('2026-11-04T18:15:00Z'));
  assert.deepEqual(source, before);
  assert.equal(draft.mode, 'manual');
  assert.equal(draft.repeatSourceSessionId, source.sessionId);
  assert.equal(draft.repeatSetupComplete, false);
  assert.equal(draft.placeName, source.areaName);
  assert.equal(draft.command.areaId, source.areaId);
  assert.equal(draft.command.sessionName, source.sessionName);
  assert.notEqual(draft.command.clientSessionId, source.clientSessionId);
  assert.equal(new Set(draft.command.entries.map(entry => entry.clientEntryId)).size, 3);
  assert.equal(draft.command.entries.some(entry => source.entries.some(old => old.clientEntryId === entry.clientEntryId)), false);
  assert.deepEqual(draft.command.entries.map(entry => entry.entryOrder), [0, 1, 2]);
  assert.deepEqual(draft.command.entries.map(entry => entry.resultType), ['WARMUP', 'ATTEMPT', 'ASCENT']);
  assert.deepEqual(draft.command.entries.map(entry => entry.climbId ?? null), [null, 'route-a', 'boulder-a']);
  assert.deepEqual(draft.command.entries.map(entry => entry.executedMoves), [40, 7, 4]);
  assert.deepEqual(draft.command.entries.map(entry => entry.notes), ['Easy traversing', 'Keep this attempt', 'Original beta']);
  assert.equal(draft.command.entries[1].protectionMode, 'TOP_ROPE');
  assert.equal(draft.command.entries[1].effortProfileCode, 'MEDIUM_CRUX');
});

test('repeat does not copy old calculations, confirmations, versions or save state', () => {
  const draft = repeatSessionDraft('mihu', source);
  assert.equal(draft.sessionId, undefined);
  assert.equal(draft.expectedVersion, undefined);
  assert.equal(draft.pendingRequest, null);
  assert.equal(draft.running, false);
  assert.equal(draft.entryDraft, undefined);
  assert.equal(draft.editingEntryId, undefined);
  for (const entry of draft.command.entries) {
    assert.equal('entryId' in entry, false);
    assert.equal('history' in entry, false);
    assert.equal('calculation' in entry, false);
    assert.equal('clientClimbId' in entry, false);
    if (entry.resultType !== 'WARMUP') {
      assert.equal(entry.ascentMode, 'RP');
      assert.equal(entry.historyConflictConfirmed, false);
    }
  }
  assert.equal('calculationModelVersion' in draft.command, false);
  assert.equal('version' in draft.command, false);
});

test('repeat dates start a fresh one-hour window in the source zone and survive recovery/retry', () => {
  const draft = repeatSessionDraft('mihu', { ...source, timeZoneId: 'Pacific/Auckland' }, new Date('2026-11-04T11:30:00Z'));
  assert.equal(draft.command.timeZoneId, 'Pacific/Auckland');
  assert.equal(draft.startInput, '2026-11-04T23:30');
  assert.equal(draft.endInput, '2026-11-05T00:30');
  assert.equal(draft.command.sessionDate, '2026-11-04');
  assert.equal(Date.parse(draft.command.endedAt) - Date.parse(draft.command.startedAt), 3600000);
  const recovered = parseDraft(JSON.stringify(draft), 'mihu');
  assert.equal(JSON.stringify(recovered), JSON.stringify(draft));
  const command = commandFromDraft(recovered);
  assert.equal(command.clientSessionId, draft.command.clientSessionId);
  assert.equal(command.startedAt, '2026-11-04T10:30:00.000Z');
  assert.equal(command.endedAt, '2026-11-04T11:30:00.000Z');
  recovered.pendingRequest = command;
  assert.equal(commandFromDraft(recovered), command);
  assert.notEqual(draftKey('mihu', 'manual', source.sessionId), draftKey('mihu', 'manual'));
  assert.notEqual(draftKey('mihu', 'manual', source.sessionId), draftKey('mihu', 'edit', source.sessionId));
  const second = repeatSessionDraft('mihu', source);
  assert.notEqual(second.command.clientSessionId, draft.command.clientSessionId);
});
