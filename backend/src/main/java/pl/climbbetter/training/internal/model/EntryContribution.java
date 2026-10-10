package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

record EntryContribution(
        int executedMoves,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad
) {

    EntryContribution {
        if (executedMoves < 0) {
            throw new IllegalArgumentException(
                    "Executed moves cannot be negative"
            );
        }

        Objects.requireNonNull(
                classicLoad,
                "Classic load is required"
        );
        Objects.requireNonNull(
                adjustedLoad,
                "Adjusted load is required"
        );

        if (classicLoad.signum() < 0) {
            throw new IllegalArgumentException(
                    "Classic load cannot be negative"
            );
        }

        if (adjustedLoad.signum() < 0) {
            throw new IllegalArgumentException(
                    "Adjusted load cannot be negative"
            );
        }
    }
}