import type { EntryCommand } from "./session.types";

export function completedMoveIssue(
  entry: Pick<EntryCommand, "resultType" | "totalMoves" | "executedMoves">,
): string | null {
  if (
    entry.resultType !== "ASCENT" ||
    !entry.totalMoves ||
    entry.totalMoves <= 0 ||
    entry.executedMoves === entry.totalMoves
  )
    return null;
  return `Completed climbs require exactly ${entry.totalMoves} executed moves. Use Attempt for a partial climb or repeated moves.`;
}
