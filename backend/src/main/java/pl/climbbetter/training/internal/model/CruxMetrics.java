package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;

public record CruxMetrics(
        int hardMoveCount,
        int easyMoveCount,
        BigDecimal hardMoveIntensity,
        BigDecimal easyMoveIntensity,
        String baseMoveGradeLabel,
        String hardMoveGradeLabel,
        String easyMoveGradeLabel
) {

    public CruxMetrics {
        if (hardMoveCount < 0 || easyMoveCount < 0) {
            throw new IllegalArgumentException(
                    "Crux move counts cannot be negative"
            );
        }

        requirePositive(hardMoveIntensity, "Hard move intensity");
        requirePositive(easyMoveIntensity, "Easy move intensity");
        requireLabel(baseMoveGradeLabel, "Base move grade");
        requireLabel(hardMoveGradeLabel, "Hard move grade");
        requireLabel(easyMoveGradeLabel, "Easy move grade");
    }

    private static void requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive"
            );
        }
    }

    private static void requireLabel(String label, String fieldName) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }
    }
}
