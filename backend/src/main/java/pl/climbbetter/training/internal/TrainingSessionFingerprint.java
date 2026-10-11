package pl.climbbetter.training.internal;

import org.springframework.stereotype.Component;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.TrainingEntryCommand;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
class TrainingSessionFingerprint {

    private final ObjectMapper objectMapper;

    TrainingSessionFingerprint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String calculate(CreateTrainingSessionCommand command) {
        try {
            byte[] canonicalRequest = objectMapper.writeValueAsBytes(
                    normalize(command)
            );

            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(canonicalRequest);

            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot calculate training session fingerprint",
                    exception
            );
        }
    }

    private CreateTrainingSessionCommand normalize(
            CreateTrainingSessionCommand command
    ) {
        List<TrainingEntryCommand> orderedEntries =
                command.entries().stream()
                        .sorted(Comparator.comparingInt(
                                TrainingEntryCommand::entryOrder
                        ))
                        .map(this::normalizeEntry)
                        .toList();

        return new CreateTrainingSessionCommand(
                command.clientSessionId(),
                normalizeOptionalText(command.sessionName()),
                command.sessionDate(),
                command.timeZoneId(),
                command.startedAt(),
                command.endedAt(),
                command.areaId(),
                command.clientAreaId(),
                normalizeOptionalText(command.areaName()),
                orderedEntries
        );
    }

    private TrainingEntryCommand normalizeEntry(
            TrainingEntryCommand entry
    ) {
        return new TrainingEntryCommand(
                entry.clientEntryId(),
                entry.entryOrder(),
                entry.resultType(),
                entry.climbId(),
                entry.clientClimbId(),
                normalizeOptionalText(entry.climbName()),
                entry.climbType(),
                entry.gradeLabel(),
                entry.effortProfileCode(),
                entry.ascentMode(),
                entry.protectionMode(),
                entry.totalMoves(),
                entry.executedMoves(),
                entry.notes(),
                Boolean.TRUE.equals(
                        entry.historyConflictConfirmed()
                )
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}