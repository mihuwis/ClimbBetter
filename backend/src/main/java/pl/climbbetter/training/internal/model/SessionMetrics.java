package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

record SessionMetrics(
        int totalMoves,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad
) {

    SessionMetrics {
        if (totalMoves < 0) {
            throw new IllegalArgumentException(
                    "Total moves cannot be negative"
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
    }
}