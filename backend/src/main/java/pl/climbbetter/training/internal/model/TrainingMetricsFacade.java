package pl.climbbetter.training.internal.model;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Component
public final class TrainingMetricsFacade {

    private static final int CALCULATION_SCALE = 10;

    private final AssessedEntryCalculator assessedEntryCalculator =
            new AssessedEntryCalculator();

    private final SessionMetricsCalculator sessionMetricsCalculator =
            new SessionMetricsCalculator();

    public CalculatedEntry calculateAssessed(
            String climbType,
            String resultType,
            String ascentMode,
            String protectionMode,
            BigDecimal gradePoints,
            BigDecimal baseEdl,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            int totalMoves,
            int executedMoves
    ) {
        validateClassification(
                climbType,
                resultType,
                ascentMode,
                protectionMode
        );

        MoveCount moveCount = new MoveCount(
                totalMoves,
                executedMoves
        );

        EntryMetrics metrics = assessedEntryCalculator.calculate(
                gradePoints,
                baseEdl,
                styleMultiplier,
                relativeEffortMultiplier,
                moveCount
        );

        return new CalculatedEntry(
                executedMoves,
                metrics.edlCount(),
                metrics.moveIntensity(),
                metrics.classicLoad(),
                metrics.adjustedLoad()
        );
    }

    public CalculatedEntry calculateWarmUp(int executedMoves) {
        WarmUpEntry warmUpEntry = new WarmUpEntry(executedMoves);

        return new CalculatedEntry(
                executedMoves,
                null,
                null,
                warmUpEntry.classicLoad(),
                warmUpEntry.adjustedLoad()
        );
    }

    public CalculatedSession calculateSession(
            List<CalculatedEntry> entries
    ) {
        Objects.requireNonNull(
                entries,
                "Calculated entries are required"
        );

        List<EntryContribution> contributions = entries.stream()
                .map(entry -> new EntryContribution(
                        entry.executedMoves(),
                        entry.classicLoad(),
                        entry.adjustedLoad()
                ))
                .toList();

        SessionMetrics sessionMetrics =
                sessionMetricsCalculator.calculate(contributions);

        BigDecimal weightedIntensity = BigDecimal.ZERO;
        int assessedMoves = 0;

        for (CalculatedEntry entry : entries) {
            if (entry.moveIntensity() == null
                    || entry.executedMoves() == 0) {
                continue;
            }

            weightedIntensity = weightedIntensity.add(
                    entry.moveIntensity().multiply(
                            BigDecimal.valueOf(
                                    entry.executedMoves()
                            )
                    )
            );

            assessedMoves += entry.executedMoves();
        }

        BigDecimal averageMoveIntensity =
                assessedMoves == 0
                        ? null
                        : weightedIntensity.divide(
                                BigDecimal.valueOf(
                                        assessedMoves
                                ),
                                CALCULATION_SCALE,
                                RoundingMode.HALF_UP
                        );

        return new CalculatedSession(
                sessionMetrics.totalMoves(),
                sessionMetrics.classicLoad(),
                sessionMetrics.adjustedLoad(),
                averageMoveIntensity
        );
    }

    private void validateClassification(
            String climbType,
            String resultType,
            String ascentMode,
            String protectionMode
    ) {
        new EntryClassification(
                requiredEnum(
                        ClimbType.class,
                        climbType,
                        "Climb type"
                ),
                requiredEnum(
                        ResultType.class,
                        resultType,
                        "Result type"
                ),
                requiredEnum(
                        AscentMode.class,
                        ascentMode,
                        "Ascent mode"
                ),
                optionalEnum(
                        ProtectionMode.class,
                        protectionMode,
                        "Protection mode"
                )
        );
    }

    private <E extends Enum<E>> E requiredEnum(
            Class<E> enumType,
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        return parseEnum(enumType, value, fieldName);
    }

    private <E extends Enum<E>> E optionalEnum(
            Class<E> enumType,
            String value,
            String fieldName
    ) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return parseEnum(enumType, value, fieldName);
    }

    private <E extends Enum<E>> E parseEnum(
            Class<E> enumType,
            String value,
            String fieldName
    ) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported "
                            + fieldName.toLowerCase()
                            + ": "
                            + value,
                    exception
            );
        }
    }

    public record CalculatedEntry(
            int executedMoves,
            BigDecimal edlCount,
            BigDecimal moveIntensity,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad
    ) {
    }

    public record CalculatedSession(
            int totalMoves,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            BigDecimal averageMoveIntensity
    ) {
    }
}