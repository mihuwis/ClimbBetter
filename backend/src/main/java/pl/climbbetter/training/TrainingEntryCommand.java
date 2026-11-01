package pl.climbbetter.training;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record TrainingEntryCommand(
        @NotNull UUID clientEntryId,
        @PositiveOrZero int entryOrder,
        @NotBlank String resultType,
        UUID climbId,
        UUID clientClimbId,
        String climbName,
        String climbType,
        String gradeLabel,
        String effortProfileCode,
        String ascentMode,
        String protectionMode,
        @Positive Integer totalMoves,
        @Positive int executedMoves,
        String notes,
        Boolean historyConflictConfirmed
) {
}
