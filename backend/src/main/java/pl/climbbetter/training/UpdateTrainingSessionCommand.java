package pl.climbbetter.training;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTrainingSessionCommand(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank String changeReason,
        @NotNull @Valid CreateTrainingSessionCommand session
) {
}
