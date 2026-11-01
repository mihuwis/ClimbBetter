package pl.climbbetter.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;

public record CreateClimbCommand(
        @NotNull
        @Schema(description = "Stable client UUID; reuse it when retrying the same request")
        UUID clientClimbId,
        @NotBlank @Schema(example = "Żółta po krawądkach") String name,
        @NotBlank @Pattern(regexp = "BOULDER|ROUTE|CIRCUIT")
        @Schema(allowableValues = {"BOULDER", "ROUTE", "CIRCUIT"}, example = "BOULDER")
        String climbType,
        @Schema(description = "Optional for an ungraded catalog project", example = "5+")
        String gradeLabel,
        @Schema(example = "BOULDER") String effortProfileCode,
        @Positive
        @Schema(description = "Suggested length; defaults to 8 for BOULDER", example = "8")
        Integer suggestedMoveCount,
        @Positive @Digits(integer = 6, fraction = 2)
        @Schema(description = "Physical length in meters; default suggestion is 2 moves per meter", example = "15")
        BigDecimal lengthMeters,
        UUID sectorId,
        @Pattern(regexp = "BOULDER_FONT|ROUTE_FRENCH|KURTYKA")
        @Schema(description = "Defaults to the canonical system for climbType", example = "KURTYKA")
        String gradeSystemCode
) {
    public CreateClimbCommand(UUID clientClimbId, String name, String climbType,
                              String gradeLabel, String effortProfileCode, Integer suggestedMoveCount) {
        this(clientClimbId, name, climbType, gradeLabel, effortProfileCode,
                suggestedMoveCount, null, null, null);
    }
}
