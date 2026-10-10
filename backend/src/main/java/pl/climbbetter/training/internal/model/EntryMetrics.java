package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

record EntryMetrics(
        BigDecimal edlCount,
        BigDecimal moveIntensity,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad
) {

    EntryMetrics {
        Objects.requireNonNull(edlCount, "EDL count is required");
        Objects.requireNonNull(
                moveIntensity,
                "Move intensity is required"
        );
        Objects.requireNonNull(
                classicLoad,
                "Classic load is required"
        );
        Objects.requireNonNull(
                adjustedLoad,
                "Adjusted load is required"
        );
    }

    EntryContribution contribution(MoveCount moveCount) {
        Objects.requireNonNull(
                moveCount,
                "Move count is required"
        );

        return new EntryContribution(
                moveCount.executedMoves(),
                classicLoad,
                adjustedLoad
        );
        }
}