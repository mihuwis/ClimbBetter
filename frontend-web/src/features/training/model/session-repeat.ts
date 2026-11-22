import { newDraft } from "./session-draft.ts";
import type { SessionDraft } from "./session-draft";
import { toCommand, zonedInput, zonedInstant } from "./session.helpers.ts";
import type { EntryCommand, SessionDetails } from "./session.types";

const oneHour = 60 * 60 * 1000;

/** Suggest one elapsed hour in the session's zone without normalizing invalid inputs. */
export function suggestedEndInput(
  startInput: string,
  timeZoneId: string,
): string | null {
  try {
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(startInput)) return null;
    const wallTime = new Date(`${startInput}:00.000Z`);
    if (wallTime.toISOString().slice(0, 16) !== startInput) return null;
    const start = zonedInstant(startInput, timeZoneId);
    return zonedInput(
      new Date(Date.parse(start) + oneHour).toISOString(),
      timeZoneId,
    );
  } catch {
    // Keep the user's previous end while they finish typing or correct a DST gap/zone.
    return null;
  }
}

/** A repeat is a new manual session, retaining catalog identity but no old save/audit identity. */
export function repeatSessionDraft(
  userId: string,
  source: SessionDetails,
  now = new Date(),
): SessionDraft {
  const draft = newDraft(userId, "manual", now);
  const template = toCommand(source);
  const startedAt = new Date(now.getTime() - oneHour).toISOString();
  const endedAt = now.toISOString();
  const startInput = zonedInput(startedAt, source.timeZoneId);
  const entries: EntryCommand[] = [...template.entries]
    .sort((a, b) => a.entryOrder - b.entryOrder)
    .map((entry, entryOrder) => {
      const copy = { ...entry, clientEntryId: crypto.randomUUID(), entryOrder };
      return copy.resultType === "WARMUP"
        ? copy
        : {
            ...copy,
            ascentMode:
              copy.ascentMode === "OS" || copy.ascentMode === "FLASH"
                ? "RP"
                : copy.ascentMode,
            historyConflictConfirmed: false,
          };
    });
  return {
    ...draft,
    repeatSourceSessionId: source.sessionId,
    repeatSetupComplete: false,
    placeName: source.areaName,
    startInput,
    endInput: zonedInput(endedAt, source.timeZoneId),
    command: {
      ...draft.command,
      sessionName: template.sessionName,
      sessionDate: startInput.slice(0, 10),
      timeZoneId: source.timeZoneId,
      startedAt,
      endedAt,
      areaId: source.areaId,
      entries,
    },
  };
}
