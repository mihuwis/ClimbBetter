import type { EntryCommand, SessionCommand, SessionDetails } from './session.types';
import { toCommand, zonedInput, zonedInstant } from './session.helpers.ts';

export type EntryDraft = { entry: EntryCommand; mode: string };
export type SessionDraft = {
  schemaVersion: 1; userId: string; mode: 'manual' | 'live' | 'edit'; sessionId?: string;
  expectedVersion?: number; command: SessionCommand; startInput: string; endInput: string;
  running: boolean; pendingRequest: SessionCommand | null; revision: string;
  liveStarted?: boolean; placeName?: string;
  changeReason?: string;
  editingEntryId?: string | null;
  entryDraft?: EntryDraft;
};
export function draftKey(userId: string, mode: SessionDraft['mode'], id?: string) {
  return `climbbetter:session-draft:v1:${userId}:${mode}${id ? `:${id}` : ''}`;
}
export function newDraft(userId: string, mode: 'manual' | 'live', now = new Date()): SessionDraft {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const start = mode === 'manual' ? new Date(now.getTime() - 60 * 60 * 1000) : now;
  const startInput = zonedInput(start.toISOString(), zone);
  return { schemaVersion: 1, userId, mode, running: false, pendingRequest: null, revision: crypto.randomUUID(),
    startInput, endInput: zonedInput(now.toISOString(), zone),
    command: { clientSessionId: crypto.randomUUID(), sessionName: '', sessionDate: startInput.slice(0, 10),
      timeZoneId: zone, startedAt: start.toISOString(), endedAt: now.toISOString(), entries: [] } };
}
export function editDraft(userId: string, details: SessionDetails): SessionDraft {
  return { schemaVersion: 1, userId, mode: 'edit', sessionId: details.sessionId, expectedVersion: details.version,
    command: toCommand(details), startInput: zonedInput(details.startedAt, details.timeZoneId),
    endInput: zonedInput(details.endedAt, details.timeZoneId), running: false, pendingRequest: null, revision: crypto.randomUUID() };
}
export function parseDraft(raw: string | null, userId: string): SessionDraft | null {
  if (!raw) return null;
  const value = JSON.parse(raw) as SessionDraft;
  if (value.schemaVersion !== 1 || value.userId !== userId || !value.command?.clientSessionId
      || !Array.isArray(value.command.entries) || !value.startInput || !value.endInput
      || !['manual', 'live', 'edit'].includes(value.mode)) throw new Error('This saved draft cannot be read. Discard it explicitly to start again.');
  return value;
}
export function commandFromDraft(draft: SessionDraft): SessionCommand {
  if (draft.pendingRequest) return draft.pendingRequest;
  if (draft.running) throw new Error('Stop the timer before saving your session.');
  const startedAt = draft.mode === 'live' ? draft.command.startedAt : zonedInstant(draft.startInput, draft.command.timeZoneId);
  const endedAt = draft.mode === 'live' ? draft.command.endedAt : zonedInstant(draft.endInput, draft.command.timeZoneId);
  if (Date.parse(endedAt) <= Date.parse(startedAt)) throw new Error('End time must be after start time.');
  if (!draft.command.areaId && !draft.command.areaName?.trim()) throw new Error('Choose a place or enter a new place name.');
  if (!draft.command.entries.length) throw new Error('Add at least one entry.');
  return { ...draft.command, startedAt, endedAt, sessionDate: draft.startInput.slice(0, 10),
    entries: draft.command.entries.map((entry, entryOrder) => ({ ...entry, entryOrder })) };
}
