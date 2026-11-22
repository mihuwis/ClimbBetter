package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class AssessedEntryCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final BigDecimal HARD_MOVE_SHARE = new BigDecimal("0.15");

    private final MoveIntensityCalculator moveIntensityCalculator =
            new MoveIntensityCalculator();

    private final ClassicLoadCalculator classicLoadCalculator =
            new ClassicLoadCalculator();

    private final AdjustedLoadCalculator adjustedLoadCalculator =
            new AdjustedLoadCalculator();

    EntryMetrics calculateV2(
            BigDecimal gradePoints,
            EffortIntensityProfile intensityProfile,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            MoveCount moveCount
    ) {
        Objects.requireNonNull(gradePoints, "Grade points are required");
        Objects.requireNonNull(intensityProfile, "Intensity profile is required");
        Objects.requireNonNull(moveCount, "Move count is required");

        if (gradePoints.signum() <= 0) {
            throw new IllegalArgumentException("Grade points must be positive");
        }

        BigDecimal classicLoad = classicLoadCalculator.calculate(
                gradePoints,
                styleMultiplier,
                moveCount
        );

        CruxMetrics crux = null;
        BigDecimal moveIntensity;
        BigDecimal moveWork;

        if (intensityProfile.isCrux()) {
            int hardMoveCount = BigDecimal.valueOf(moveCount.executedMoves())
                    .multiply(HARD_MOVE_SHARE)
                    .setScale(0, RoundingMode.CEILING)
                    .intValueExact();
            int easyMoveCount = moveCount.executedMoves() - hardMoveCount;

            crux = new CruxMetrics(
                    hardMoveCount,
                    easyMoveCount,
                    intensityProfile.hardMoveIntensity(),
                    intensityProfile.easyMoveIntensity(),
                    intensityProfile.baseMoveGradeLabel(),
                    intensityProfile.hardMoveGradeLabel(),
                    intensityProfile.easyMoveGradeLabel()
            );

            moveWork = BigDecimal.valueOf(hardMoveCount)
                    .multiply(crux.hardMoveIntensity())
                    .add(
                            BigDecimal.valueOf(easyMoveCount)
                                    .multiply(crux.easyMoveIntensity())
                    );
            moveIntensity = moveCount.executedMoves() == 0
                    ? BigDecimal.ZERO.setScale(CALCULATION_SCALE)
                    : moveWork.divide(
                            BigDecimal.valueOf(moveCount.executedMoves()),
                            CALCULATION_SCALE,
                            RoundingMode.HALF_UP
                    );
        } else {
            moveIntensity = moveIntensityCalculator.calculateFromProfile(
                    gradePoints,
                    intensityProfile.profileIntensityIndex()
            );
            moveWork = BigDecimal.valueOf(moveCount.executedMoves())
                    .multiply(moveIntensity);
        }

        BigDecimal adjustedLoad = adjustedLoadCalculator.calculateFromWork(
                moveWork,
                styleMultiplier,
                relativeEffortMultiplier
        );

        return new EntryMetrics(
                moveIntensity,
                classicLoad,
                adjustedLoad,
                intensityProfile.profileIntensityIndex(),
                crux
        );
    }
}
