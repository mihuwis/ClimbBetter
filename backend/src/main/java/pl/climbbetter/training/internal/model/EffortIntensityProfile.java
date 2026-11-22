package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

public record EffortIntensityProfile(
        BigDecimal profileIntensityIndex,
        String baseMoveGradeLabel,
        String hardMoveGradeLabel,
        String easyMoveGradeLabel,
        BigDecimal hardMoveIntensity,
        BigDecimal easyMoveIntensity
) {

    public EffortIntensityProfile {
        Objects.requireNonNull(
                profileIntensityIndex,
                "Profile intensity index is required"
        );

        if (profileIntensityIndex.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Profile intensity index must be positive"
            );
        }

        boolean containsCruxData = baseMoveGradeLabel != null
                || hardMoveGradeLabel != null
                || easyMoveGradeLabel != null
                || hardMoveIntensity != null
                || easyMoveIntensity != null;

        if (containsCruxData) {
            requireLabel(baseMoveGradeLabel, "Base move grade");
            requireLabel(hardMoveGradeLabel, "Hard move grade");
            requireLabel(easyMoveGradeLabel, "Easy move grade");
            requirePositive(hardMoveIntensity, "Hard move intensity");
            requirePositive(easyMoveIntensity, "Easy move intensity");
        }
    }

    public boolean isCrux() {
        return hardMoveIntensity != null;
    }

    private static void requireLabel(String label, String fieldName) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required for a crux profile"
            );
        }
    }

    private static void requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive for a crux profile"
            );
        }
    }
}
