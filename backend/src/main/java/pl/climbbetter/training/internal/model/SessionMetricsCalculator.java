package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

final class SessionMetricsCalculator {

    SessionMetrics calculate(
            List<EntryContribution> contributions
    ) {
        Objects.requireNonNull(
                contributions,
                "Entry contributions are required"
        );

        int totalMoves = 0;
        BigDecimal classicLoad = BigDecimal.ZERO;
        BigDecimal adjustedLoad = BigDecimal.ZERO;

        for (EntryContribution contribution : contributions) {
            Objects.requireNonNull(
                    contribution,
                    "Entry contribution is required"
            );

            totalMoves += contribution.executedMoves();
            classicLoad = classicLoad.add(
                    contribution.classicLoad()
            );
            adjustedLoad = adjustedLoad.add(
                    contribution.adjustedLoad()
            );
        }

        return new SessionMetrics(
                totalMoves,
                classicLoad.setScale(4, RoundingMode.HALF_UP),
                adjustedLoad.setScale(4, RoundingMode.HALF_UP)
        );
    }
}