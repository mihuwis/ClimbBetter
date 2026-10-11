package pl.climbbetter.training;

import java.util.UUID;

public record TrainingEntryCommand(
        UUID clientEntryId,
        int entryOrder,
        String resultType,
        UUID climbId,
        UUID clientClimbId,
        String climbName,
        String climbType,
        String gradeLabel,
        String effortProfileCode,
        String ascentMode,
        String protectionMode,
        Integer totalMoves,
        int executedMoves,
        String notes,
        Boolean historyConflictConfirmed
) {
}