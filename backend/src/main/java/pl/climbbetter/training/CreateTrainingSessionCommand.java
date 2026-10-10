package pl.climbbetter.training;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTrainingSessionCommand(
        UUID clientSessionId,
        String sessionName,
        LocalDate sessionDate,
        String timeZoneId,
        Instant startedAt,
        Instant endedAt,
        UUID areaId,
        UUID clientAreaId,
        String areaName,
        List<TrainingEntryCommand> entries
) {
}