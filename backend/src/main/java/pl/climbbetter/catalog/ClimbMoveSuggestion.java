package pl.climbbetter.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ClimbMoveSuggestion {

    private ClimbMoveSuggestion() { }

    public static Integer resolve(Integer explicitMoves, BigDecimal lengthMeters, String climbType) {
        if (lengthMeters != null && lengthMeters.signum() <= 0) {
            throw new IllegalArgumentException("Length in meters must be positive");
        }
        if (explicitMoves != null) {
            if (explicitMoves <= 0) {
                throw new IllegalArgumentException("Suggested moves must be positive");
            }
            return explicitMoves;
        }
        if (lengthMeters != null) {
            try {
                return lengthMeters.multiply(BigDecimal.valueOf(2))
                        .setScale(0, RoundingMode.CEILING).intValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Length is outside the supported range", exception);
            }
        }
        return "BOULDER".equals(climbType) ? 8 : null;
    }
}
