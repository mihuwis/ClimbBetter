package pl.climbbetter.training.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.TrainingEntryCommand;
import pl.climbbetter.training.internal.model.TrainingMetricsFacade;
import pl.climbbetter.training.internal.model.TrainingMetricsFacade.CalculatedEntry;
import pl.climbbetter.training.internal.model.TrainingMetricsFacade.CalculatedSession;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TrainingSessionService {

    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final AreaResolver areaResolver;
    private final GradeResolver gradeResolver;
    private final EffortProfileResolver effortProfileResolver;
    private final ClimbResolver climbResolver;
    private final ClimbHistoryResolver climbHistoryResolver;
    private final ClimberLevelResolver climberLevelResolver;
    private final CalculationRuleResolver calculationRuleResolver;
    private final TrainingMetricsFacade trainingMetricsFacade;
    private final TrainingSessionRepository repository;

    TrainingSessionService(
            CurrentUserProvider currentUserProvider,
            Clock clock,
            AreaResolver areaResolver,
            GradeResolver gradeResolver,
            EffortProfileResolver effortProfileResolver,
            ClimbResolver climbResolver,
            ClimbHistoryResolver climbHistoryResolver,
            ClimberLevelResolver climberLevelResolver,
            CalculationRuleResolver calculationRuleResolver,
            TrainingMetricsFacade trainingMetricsFacade,
            TrainingSessionRepository repository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.areaResolver = areaResolver;
        this.gradeResolver = gradeResolver;
        this.effortProfileResolver = effortProfileResolver;
        this.climbResolver = climbResolver;
        this.climbHistoryResolver = climbHistoryResolver;
        this.climberLevelResolver = climberLevelResolver;
        this.calculationRuleResolver = calculationRuleResolver;
        this.trainingMetricsFacade = trainingMetricsFacade;
        this.repository = repository;
    }

    @Transactional
    public UUID create(CreateTrainingSessionCommand command) {
        validateCommand(command);

        UUID userId = currentUserProvider.currentUserId();

        UUID existingSessionId = repository
                .findIdByClientSessionId(
                        userId,
                        command.clientSessionId()
                )
                .orElse(null);

        if (existingSessionId != null) {
            return existingSessionId;
        }

        List<TrainingEntryCommand> orderedEntries =
                command.entries().stream()
                        .sorted(Comparator.comparingInt(
                                TrainingEntryCommand::entryOrder
                        ))
                        .toList();

        Map<Integer, GradeResolver.ResolvedGrade> grades =
                resolveGrades(orderedEntries);

        Map<String, ClimberLevelResolver.ResolvedLevel> levels =
                resolveLevels(
                        userId,
                        command.sessionDate(),
                        orderedEntries,
                        grades
                );

        AreaResolver.ResolvedArea area = areaResolver.resolve(
                userId,
                command.areaId(),
                command.clientAreaId(),
                command.areaName()
        );

        UUID sessionId = UUID.randomUUID();
        Instant createdAt = clock.instant();

        repository.insertSession(
                new TrainingSessionRepository.SessionRow(
                        sessionId,
                        command.clientSessionId(),
                        userId,
                        area.id(),
                        resolveSessionName(command, area),
                        command.sessionDate(),
                        command.timeZoneId(),
                        calculateDurationMinutes(
                                command.startedAt(),
                                command.endedAt()
                        ),
                        command.startedAt(),
                        command.endedAt(),
                        area.name(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null,
                        createdAt
                )
        );

        List<CalculatedEntry> calculatedEntries =
                new ArrayList<>();

        GradeResolver.ResolvedGrade maxBoulderGrade = null;
        GradeResolver.ResolvedGrade maxRouteGrade = null;

        for (TrainingEntryCommand entry : orderedEntries) {
            if ("WARMUP".equals(entry.resultType())) {
                CalculatedEntry calculation =
                        trainingMetricsFacade.calculateWarmUp(
                                entry.executedMoves()
                        );

                repository.insertWarmUpEntry(
                        UUID.randomUUID(),
                        sessionId,
                        entry.clientEntryId(),
                        entry.entryOrder(),
                        entry.executedMoves(),
                        entry.notes(),
                        createdAt
                );

                calculatedEntries.add(calculation);
                continue;
            }

            GradeResolver.ResolvedGrade grade =
                    grades.get(entry.entryOrder());

            EffortProfileResolver.ResolvedEffortProfile effortProfile =
                    effortProfileResolver.resolve(
                            entry.effortProfileCode()
                    );

            ClimbResolver.ResolvedClimb climb =
                    climbResolver.resolve(
                            userId,
                            area.id(),
                            entry,
                            grade,
                            effortProfile
                    );

            ClimbHistoryResolver.ClimbHistory history =
                    climbHistoryResolver.resolve(
                            userId,
                            climb.id(),
                            command.sessionDate()
                    );

            CalculationRuleResolver.ResolvedStyleRule styleRule =
                    calculationRuleResolver.resolveStyle(
                            entry.resultType(),
                            entry.ascentMode(),
                            history.contactsTwoYears()
                    );

            validateHistoryConfirmation(entry, styleRule);

            ClimberLevelResolver.ResolvedLevel level =
                    levels.get(grade.scaleCode());

            CalculationRuleResolver.ResolvedRelativeEffort
                    relativeEffort =
                    calculationRuleResolver.resolveRelativeEffort(
                            grade.gradeIndex(),
                            level.gradeIndex()
                    );

            CalculatedEntry calculation =
                    trainingMetricsFacade.calculateAssessed(
                            entry.climbType(),
                            entry.resultType(),
                            entry.ascentMode(),
                            entry.protectionMode(),
                            grade.gradePoints(),
                            effortProfile.baseEdl(),
                            styleRule.multiplier(),
                            relativeEffort.multiplier(),
                            requiredTotalMoves(entry),
                            entry.executedMoves()
                    );

            int attemptOrdinal =
                    history.contactsAllTime() + 1;

            boolean fastRp = isFastRp(
                    entry,
                    attemptOrdinal
            );

            repository.insertAssessedEntry(
                    UUID.randomUUID(),
                    sessionId,
                    entry,
                    climb,
                    grade,
                    effortProfile,
                    history,
                    attemptOrdinal,
                    fastRp,
                    level,
                    styleRule,
                    relativeEffort,
                    calculation,
                    createdAt
            );

            calculatedEntries.add(calculation);

            if ("ASCENT".equals(entry.resultType())) {
                if ("BOULDER".equals(entry.climbType())) {
                    maxBoulderGrade = higherGrade(
                            maxBoulderGrade,
                            grade
                    );
                } else {
                    maxRouteGrade = higherGrade(
                            maxRouteGrade,
                            grade
                    );
                }
            }
        }

        CalculatedSession summary =
                trainingMetricsFacade.calculateSession(
                        calculatedEntries
                );

        repository.updateSessionSummary(
                sessionId,
                summary,
                gradeLabel(maxBoulderGrade),
                gradeLabel(maxRouteGrade),
                clock.instant()
        );

        return sessionId;
    }

    private Map<Integer, GradeResolver.ResolvedGrade> resolveGrades(
            List<TrainingEntryCommand> entries
    ) {
        Map<Integer, GradeResolver.ResolvedGrade> grades =
                new HashMap<>();

        for (TrainingEntryCommand entry : entries) {
            if ("WARMUP".equals(entry.resultType())) {
                validateWarmUpEntry(entry);
                continue;
            }

            validateAssessedResultType(entry);

            grades.put(
                    entry.entryOrder(),
                    gradeResolver.resolve(
                            entry.climbType(),
                            entry.gradeLabel()
                    )
            );
        }

        return grades;
    }

    private Map<String, ClimberLevelResolver.ResolvedLevel>
    resolveLevels(
            UUID userId,
            LocalDate sessionDate,
            List<TrainingEntryCommand> entries,
            Map<Integer, GradeResolver.ResolvedGrade> grades
    ) {
        Map<String, ClimberLevelResolver.ResolvedLevel> levels =
                new HashMap<>();

        for (GradeResolver.ResolvedGrade grade : grades.values()) {
            String scaleCode = grade.scaleCode();

            if (levels.containsKey(scaleCode)) {
                continue;
            }

            ClimberLevelResolver.ResolvedLevel level =
                    climberLevelResolver.resolve(
                                    userId,
                                    scaleCode,
                                    sessionDate
                            )
                            .orElseGet(() ->
                                    climberLevelResolver
                                            .inferFromSession(
                                                    scaleCode,
                                                    gradesForResult(
                                                            entries,
                                                            grades,
                                                            scaleCode,
                                                            "ASCENT"
                                                    ),
                                                    gradesForResult(
                                                            entries,
                                                            grades,
                                                            scaleCode,
                                                            "ATTEMPT"
                                                    )
                                            )
                            );

            levels.put(scaleCode, level);
        }

        return levels;
    }

    private List<GradeResolver.ResolvedGrade> gradesForResult(
            List<TrainingEntryCommand> entries,
            Map<Integer, GradeResolver.ResolvedGrade> grades,
            String scaleCode,
            String resultType
    ) {
        return entries.stream()
                .filter(entry ->
                        resultType.equals(entry.resultType())
                )
                .map(entry -> grades.get(entry.entryOrder()))
                .filter(Objects::nonNull)
                .filter(grade ->
                        scaleCode.equals(grade.scaleCode())
                )
                .toList();
    }

    private void validateCommand(
            CreateTrainingSessionCommand command
    ) {
        Objects.requireNonNull(command, "Command is required");
        Objects.requireNonNull(
                command.clientSessionId(),
                "Client session ID is required"
        );
        Objects.requireNonNull(
                command.sessionDate(),
                "Session date is required"
        );
        Objects.requireNonNull(
                command.startedAt(),
                "Start time is required"
        );
        Objects.requireNonNull(
                command.endedAt(),
                "End time is required"
        );
        Objects.requireNonNull(
                command.entries(),
                "Entries are required"
        );

        if (command.timeZoneId() == null
                || command.timeZoneId().isBlank()) {
            throw new IllegalArgumentException(
                    "Time zone ID is required"
            );
        }

        ZoneId zoneId;

        try {
            zoneId = ZoneId.of(command.timeZoneId());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid time zone ID: "
                            + command.timeZoneId(),
                    exception
            );
        }

        LocalDate dateAtStart = command.startedAt()
                .atZone(zoneId)
                .toLocalDate();

        if (!dateAtStart.equals(command.sessionDate())) {
            throw new IllegalArgumentException(
                    "Session date must match the local start date"
            );
        }

        if (command.entries().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one training entry is required"
            );
        }

        validateEntryIdentifiers(command.entries());
        calculateDurationMinutes(
                command.startedAt(),
                command.endedAt()
        );
    }

    private void validateEntryIdentifiers(
            List<TrainingEntryCommand> entries
    ) {
        Set<UUID> clientEntryIds = new HashSet<>();
        Set<Integer> entryOrders = new HashSet<>();

        for (TrainingEntryCommand entry : entries) {
            Objects.requireNonNull(
                    entry,
                    "Training entry is required"
            );
            Objects.requireNonNull(
                    entry.clientEntryId(),
                    "Client entry ID is required"
            );

            if (entry.entryOrder() < 0) {
                throw new IllegalArgumentException(
                        "Entry order cannot be negative"
                );
            }

            if (!clientEntryIds.add(entry.clientEntryId())) {
                throw new IllegalArgumentException(
                        "Client entry IDs must be unique"
                );
            }

            if (!entryOrders.add(entry.entryOrder())) {
                throw new IllegalArgumentException(
                        "Entry orders must be unique"
                );
            }
        }
    }

    private void validateWarmUpEntry(
            TrainingEntryCommand entry
    ) {
        boolean containsAssessedData =
                entry.climbId() != null
                        || entry.clientClimbId() != null
                        || entry.climbName() != null
                        || entry.climbType() != null
                        || entry.gradeLabel() != null
                        || entry.effortProfileCode() != null
                        || entry.ascentMode() != null
                        || entry.protectionMode() != null
                        || entry.totalMoves() != null
                        || Boolean.TRUE.equals(
                            entry.historyConflictConfirmed()
);

        if (containsAssessedData) {
            throw new IllegalArgumentException(
                    "Warm-up cannot contain assessed climb data"
            );
        }
    }

    private void validateAssessedResultType(
            TrainingEntryCommand entry
    ) {
        if (!"ASCENT".equals(entry.resultType())
                && !"ATTEMPT".equals(entry.resultType())) {
            throw new IllegalArgumentException(
                    "Unsupported result type: "
                            + entry.resultType()
            );
        }
    }

    private void validateHistoryConfirmation(
            TrainingEntryCommand entry,
            CalculationRuleResolver.ResolvedStyleRule styleRule
    ) {
        boolean conflictConfirmed = Boolean.TRUE.equals(
                entry.historyConflictConfirmed()
        );

        if (styleRule.historyConflict()
                && !conflictConfirmed) {
            throw new IllegalArgumentException(
                    "Ascent mode conflicts with climb history "
                            + "and requires confirmation"
            );
        }

        if (!styleRule.historyConflict()
                && conflictConfirmed) {
            throw new IllegalArgumentException(
                    "History conflict confirmation was provided "
                            + "without a detected conflict"
            );
        }
    }

    private int requiredTotalMoves(
            TrainingEntryCommand entry
    ) {
        if (entry.totalMoves() == null) {
            throw new IllegalArgumentException(
                    "Total moves are required "
                            + "for an assessed entry"
            );
        }

        return entry.totalMoves();
    }

    private boolean isFastRp(
            TrainingEntryCommand entry,
            int attemptOrdinal
    ) {
        return "ASCENT".equals(entry.resultType())
                && "RP".equals(entry.ascentMode())
                && (
                attemptOrdinal == 2
                        || attemptOrdinal == 3
        );
    }

    private GradeResolver.ResolvedGrade higherGrade(
            GradeResolver.ResolvedGrade current,
            GradeResolver.ResolvedGrade candidate
    ) {
        if (current == null
                || candidate.gradeIndex() > current.gradeIndex()) {
            return candidate;
        }

        return current;
    }

    private String gradeLabel(
            GradeResolver.ResolvedGrade grade
    ) {
        return grade == null ? null : grade.label();
    }

    private String resolveSessionName(
            CreateTrainingSessionCommand command,
            AreaResolver.ResolvedArea area
    ) {
        if (command.sessionName() != null
                && !command.sessionName().isBlank()) {
            return command.sessionName().trim();
        }

        return area.name() + " - " + command.sessionDate();
    }

    private int calculateDurationMinutes(
            Instant startedAt,
            Instant endedAt
    ) {
        Duration duration = Duration.between(
                startedAt,
                endedAt
        );

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        long wholeMinutes = duration.toMinutes();

        if (!duration.equals(
                Duration.ofMinutes(wholeMinutes)
        )) {
            wholeMinutes++;
        }

        return Math.toIntExact(wholeMinutes);
    }
}