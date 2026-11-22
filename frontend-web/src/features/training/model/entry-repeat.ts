import type { EntryCommand } from "./session.types";

const copiedInputFields = [
  "climbId",
  "clientClimbId",
  "climbName",
  "climbType",
  "gradeLabel",
  "effortProfileCode",
  "ascentMode",
  "protectionMode",
  "totalMoves",
  "notes",
] as const satisfies readonly (keyof EntryCommand)[];

/** Repeat an input entry, keeping its climb identity and giving the new attempt its own identity. */
export function repeatEntry(
  entries: EntryCommand[],
  sourceClientEntryId: string,
): EntryCommand[] {
  const index = entries.findIndex(
    (entry) => entry.clientEntryId === sourceClientEntryId,
  );
  if (index < 0)
    throw new Error("The entry to repeat is no longer in this session.");
  const source = entries[index];
  const repeated: EntryCommand = {
    ...Object.fromEntries(
      copiedInputFields
        .filter((field) => Object.hasOwn(source, field))
        .map((field) => [field, source[field]]),
    ),
    clientEntryId: crypto.randomUUID(),
    entryOrder: index + 1,
    resultType: source.resultType,
    executedMoves: source.executedMoves,
  };
  if (source.resultType !== "WARMUP") repeated.historyConflictConfirmed = false;
  return [
    ...entries.slice(0, index + 1),
    repeated,
    ...entries.slice(index + 1),
  ].map((entry, entryOrder) => ({ ...entry, entryOrder }));
}
