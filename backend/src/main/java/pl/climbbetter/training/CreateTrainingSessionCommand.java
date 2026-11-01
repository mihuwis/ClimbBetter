package pl.climbbetter.training;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTrainingSessionCommand(
        @NotNull UUID clientSessionId,
        String sessionName,
        @NotNull LocalDate sessionDate,
        @NotBlank String timeZoneId,
        @NotNull Instant startedAt,
        @NotNull Instant endedAt,
        UUID areaId,
        UUID clientAreaId,
        String areaName,
        @NotEmpty List<@NotNull @Valid TrainingEntryCommand> entries
) {
}
