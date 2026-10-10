package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;

final class AssessedEntryCalculator {

    private final EdlCalculator edlCalculator =
            new EdlCalculator();

    private final MoveIntensityCalculator moveIntensityCalculator =
            new MoveIntensityCalculator();

    private final ClassicLoadCalculator classicLoadCalculator =
            new ClassicLoadCalculator();

    private final AdjustedLoadCalculator adjustedLoadCalculator =
            new AdjustedLoadCalculator();

    EntryMetrics calculate(
            BigDecimal gradePoints,
            BigDecimal baseEdl,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            MoveCount moveCount
    ) {
        BigDecimal edlCount = edlCalculator.calculate(
                baseEdl,
                moveCount
        );

        BigDecimal moveIntensity =
                moveIntensityCalculator.calculate(
                        gradePoints,
                        edlCount
                );

        BigDecimal classicLoad =
                classicLoadCalculator.calculate(
                        gradePoints,
                        styleMultiplier,
                        moveCount
                );

        BigDecimal adjustedLoad =
                adjustedLoadCalculator.calculate(
                        moveIntensity,
                        styleMultiplier,
                        relativeEffortMultiplier,
                        moveCount
                );

        return new EntryMetrics(
                edlCount,
                moveIntensity,
                classicLoad,
                adjustedLoad
        );
    }
}