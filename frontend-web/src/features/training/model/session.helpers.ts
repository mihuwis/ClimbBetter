import type { EntryCommand, SessionCommand, SessionDetails, SessionEntry } from './session.types';

export function groupEntries(entries: SessionEntry[]) {
  const groups = new Map<string, { key: string; name: string; entries: SessionEntry[] }>();
  for (const entry of [...entries].sort((a, b) => a.entryOrder - b.entryOrder)) {
    const key = entry.resultType === 'WARMUP' ? 'warmup' : entry.climbId ?? entry.clientEntryId;
    if (!groups.has(key)) groups.set(key, { key, name: entry.resultType === 'WARMUP' ? 'Warm-up' : entry.climbName ?? 'Unnamed climb', entries: [] });
    groups.get(key)!.entries.push(entry);
  }
  return [...groups.values()];
}

export function toCommand(details: SessionDetails): SessionCommand {
  return {
    clientSessionId: details.clientSessionId, sessionName: details.sessionName,
    sessionDate: details.sessionDate, timeZoneId: details.timeZoneId,
    startedAt: details.startedAt, endedAt: details.endedAt, areaId: details.areaId,
    entries: details.entries.map(entry => entry.resultType === 'WARMUP'
      ? { clientEntryId: entry.clientEntryId, entryOrder: entry.entryOrder, resultType: 'WARMUP', executedMoves: entry.executedMoves, notes: entry.notes }
      : { clientEntryId: entry.clientEntryId, entryOrder: entry.entryOrder, resultType: entry.resultType,
          climbId: entry.climbId, climbName: entry.climbName, climbType: entry.climbType, gradeLabel: entry.gradeLabel,
          effortProfileCode: entry.effortProfileCode, ascentMode: entry.ascentMode,
          protectionMode: entry.protectionMode, totalMoves: entry.totalMoves,
          executedMoves: entry.executedMoves, notes: entry.notes,
          historyConflictConfirmed: entry.history?.conflictConfirmed ?? false }),
  };
}

export function reorderEntries(entries: EntryCommand[]): EntryCommand[] {
  return entries.map((entry, entryOrder) => ({ ...entry, entryOrder }));
}

export function zonedInput(instant: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('sv-SE', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date(instant));
  const get = (type: string) => parts.find(part => part.type === type)?.value ?? '';
  return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`;
}

// Convert a wall-clock input in the session's zone, not the browser's zone.
// Reject nonexistent DST times instead of silently moving the user's session.
export function zonedInstant(value: string, timeZone: string): string {
  const target = Date.parse(`${value}:00Z`);
  if (!Number.isFinite(target)) throw new Error('Enter a valid date and time.');
  let guess = target;
  for (let i = 0; i < 4; i++) {
    const wall = Date.parse(`${zonedInput(new Date(guess).toISOString(), timeZone)}:00Z`);
    if (wall === target) return new Date(guess).toISOString();
    guess += target - wall;
  }
  throw new Error('This local time does not exist in the selected time zone. Choose another time.');
}
