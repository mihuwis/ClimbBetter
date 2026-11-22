package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

record EntryMetrics(
        BigDecimal moveIntensity,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad,
        BigDecimal profileIntensityIndex,
        CruxMetrics crux
) {

    EntryMetrics {
        Objects.requireNonNull(
                profileIntensityIndex,
                "Profile intensity index is required"
        );
        if (profileIntensityIndex.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Profile intensity index must be positive"
            );
        }
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
