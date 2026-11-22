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

    public CalculatedEntry calculateWarmUp(int executedMoves) {
        return calculateWarmUp(executedMoves, "v2");
    }

    public CalculatedEntry calculateAssessedV2(
            String climbType,
            String resultType,
            String ascentMode,
            String protectionMode,
            BigDecimal gradePoints,
            EffortIntensityProfile intensityProfile,
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

        if ("ASCENT".equals(resultType) && executedMoves != totalMoves) {
            throw new IllegalArgumentException(
                    "Completed climbs require exactly " + totalMoves
                            + " executed moves; use Attempt for a partial climb"
            );
        }

        EntryMetrics metrics = assessedEntryCalculator.calculateV2(
                gradePoints,
                intensityProfile,
                styleMultiplier,
                relativeEffortMultiplier,
                new MoveCount(totalMoves, executedMoves)
        );

        return new CalculatedEntry(
                executedMoves,
                metrics.moveIntensity(),
                metrics.classicLoad(),
                metrics.adjustedLoad(),
                "v2",
                metrics.profileIntensityIndex(),
                metrics.crux()
        );
    }

    public CalculatedEntry calculateWarmUp(
            int executedMoves,
            String modelVersion
    ) {
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "Calculation model version is required"
            );
        }

        WarmUpEntry warmUpEntry = new WarmUpEntry(executedMoves);

        return new CalculatedEntry(
                executedMoves,
                null,
                warmUpEntry.classicLoad(),
                warmUpEntry.adjustedLoad(),
                modelVersion,
                null,
                null
        );
    }

    /** V3 changes the level reference; the intensity and crux calculations remain those of v2. */
    public CalculatedEntry calculateAssessedV3(
            String climbType,
            String resultType,
            String ascentMode,
            String protectionMode,
            BigDecimal gradePoints,
            EffortIntensityProfile intensityProfile,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            int totalMoves,
            int executedMoves
    ) {
        CalculatedEntry calculation = calculateAssessedV2(
                climbType, resultType, ascentMode, protectionMode, gradePoints, intensityProfile,
                styleMultiplier, relativeEffortMultiplier, totalMoves, executedMoves
        );
        return new CalculatedEntry(
                calculation.executedMoves(), calculation.moveIntensity(), calculation.classicLoad(),
                calculation.adjustedLoad(), "v3", calculation.profileIntensityIndex(), calculation.crux()
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
            BigDecimal moveIntensity,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            String calculationModelVersion,
            BigDecimal profileIntensityIndex,
            CruxMetrics crux
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
