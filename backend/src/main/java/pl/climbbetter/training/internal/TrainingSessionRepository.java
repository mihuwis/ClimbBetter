package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.training.TrainingEntryCommand;
import pl.climbbetter.training.TrainingSessionDetails;
import pl.climbbetter.training.TrainingSessionDetails.CalculationDetails;
import pl.climbbetter.training.TrainingSessionDetails.EntryDetails;
import pl.climbbetter.training.TrainingSessionDetails.HistoryDetails;
import pl.climbbetter.training.internal.model.TrainingMetricsFacade.CalculatedEntry;
import pl.climbbetter.training.internal.model.TrainingMetricsFacade.CalculatedSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
class TrainingSessionRepository {

    private final JdbcClient jdbcClient;

    TrainingSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<ExistingSession> findByClientSessionId(
            UUID userId,
            UUID clientSessionId
    ) {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(
                clientSessionId,
                "Client session ID is required"
        );

        return jdbcClient.sql("""
                        SELECT id, request_fingerprint
                        FROM training.training_sessions
                        WHERE user_id = :userId
                          AND client_session_id = :clientSessionId
                        """)
                .param("userId", userId)
                .param("clientSessionId", clientSessionId)
                .query((resultSet, rowNumber) ->
                        new ExistingSession(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getString(
                                        "request_fingerprint"
                                )
                        )
                )
                .optional();
    }

    void insertSession(SessionRow session) {
        Objects.requireNonNull(session, "Session is required");

        int insertedRows = jdbcClient.sql("""
                        INSERT INTO training.training_sessions (
                            id,
                            client_session_id,
                            request_fingerprint,
                            user_id,
                            area_id,
                            session_name,
                            session_date,
                            time_zone_id,
                            duration_minutes,
                            started_at,
                            ended_at,
                            location_name_snapshot,
                            total_moves,
                            classic_load,
                            adjusted_load,
                            average_move_intensity,
                            max_boulder_grade,
                            max_route_grade,
                            created_at,
                            updated_at,
                            version
                        )
                        VALUES (
                            :id,
                            :clientSessionId,
                            :requestFingerprint,
                            :userId,
                            :areaId,
                            :sessionName,
                            :sessionDate,
                            :timeZoneId,
                            :durationMinutes,
                            :startedAt,
                            :endedAt,
                            :locationName,
                            :totalMoves,
                            :classicLoad,
                            :adjustedLoad,
                            :averageMoveIntensity,
                            :maxBoulderGrade,
                            :maxRouteGrade,
                            :createdAt,
                            :createdAt,
                            0
                        )
                        """)
                .param("id", session.id())
                .param(
                        "clientSessionId",
                        session.clientSessionId()
                )
                .param(
                        "requestFingerprint",
                        session.requestFingerprint()
                )
                .param("userId", session.userId())
                .param("areaId", session.areaId())
                .param("sessionName", session.sessionName())
                .param("sessionDate", session.sessionDate())
                .param("timeZoneId", session.timeZoneId())
                .param(
                        "durationMinutes",
                        session.durationMinutes()
                )
                .param(
                        "startedAt",
                        databaseTimestamp(session.startedAt())
                )
                .param(
                        "endedAt",
                        databaseTimestamp(session.endedAt())
                )
                .param("locationName", session.locationName())
                .param("totalMoves", session.totalMoves())
                .param("classicLoad", session.classicLoad())
                .param("adjustedLoad", session.adjustedLoad())
                .param(
                        "averageMoveIntensity",
                        session.averageMoveIntensity()
                )
                .param(
                        "maxBoulderGrade",
                        session.maxBoulderGrade()
                )
                .param(
                        "maxRouteGrade",
                        session.maxRouteGrade()
                )
                .param(
                        "createdAt",
                        databaseTimestamp(session.createdAt())
                )
                .update();

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Expected to insert exactly one training session"
            );
        }
    }

    void insertWarmUpEntry(
            UUID entryId,
            UUID sessionId,
            UUID clientEntryId,
            int entryOrder,
            int executedMoves,
            String notes,
            Instant createdAt
    ) {
        int insertedRows = jdbcClient.sql("""
                        INSERT INTO training.training_entries (
                            id,
                            session_id,
                            client_entry_id,
                            entry_order,
                            result_type,
                            executed_moves,
                            classic_load,
                            adjusted_load,
                            calculation_model_version,
                            notes,
                            created_at
                        )
                        VALUES (
                            :id,
                            :sessionId,
                            :clientEntryId,
                            :entryOrder,
                            'WARMUP',
                            :executedMoves,
                            0,
                            0,
                            'v1',
                            :notes,
                            :createdAt
                        )
                        """)
                .param("id", entryId)
                .param("sessionId", sessionId)
                .param("clientEntryId", clientEntryId)
                .param("entryOrder", entryOrder)
                .param("executedMoves", executedMoves)
                .param("notes", notes)
                .param(
                        "createdAt",
                        databaseTimestamp(createdAt)
                )
                .update();

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Expected to insert exactly one warm-up entry"
            );
        }
    }

    void insertAssessedEntry(
            UUID entryId,
            UUID sessionId,
            TrainingEntryCommand entry,
            ClimbResolver.ResolvedClimb climb,
            GradeResolver.ResolvedGrade grade,
            EffortProfileResolver.ResolvedEffortProfile effortProfile,
            ClimbHistoryResolver.ClimbHistory history,
            int attemptOrdinal,
            boolean fastRp,
            ClimberLevelResolver.ResolvedLevel level,
            CalculationRuleResolver.ResolvedStyleRule styleRule,
            CalculationRuleResolver.ResolvedRelativeEffort relativeEffort,
            CalculatedEntry calculation,
            Instant createdAt
    ) {
        int insertedRows = jdbcClient.sql("""
                        INSERT INTO training.training_entries (
                            id,
                            session_id,
                            client_entry_id,
                            entry_order,
                            result_type,
                            climb_id,
                            climb_type,
                            ascent_mode,
                            protection_mode,
                            climb_name_snapshot,
                            grade_id,
                            grade_scale_code,
                            grade_label,
                            grade_index,
                            grade_points,
                            grade_calibration_version,
                            effort_profile_id,
                            effort_profile_code,
                            effort_profile_name_snapshot,
                            base_edl,
                            total_moves,
                            executed_moves,
                            edl_count,
                            move_intensity,
                            style_multiplier,
                            relative_effort_multiplier,
                            classic_load,
                            adjusted_load,
                            calculation_model_version,
                            contact_count_before_two_years,
                            attempt_ordinal_all_time,
                            familiarity_band,
                            fast_rp,
                            current_level_grade_label,
                            current_level_index,
                            relative_grade_diff,
                            outside_calibrated_range,
                            familiarity_source,
                            history_conflict_detected,
                            history_conflict_confirmed,
                            notes,
                            created_at
                        )
                        VALUES (
                            :id,
                            :sessionId,
                            :clientEntryId,
                            :entryOrder,
                            :resultType,
                            :climbId,
                            :climbType,
                            :ascentMode,
                            :protectionMode,
                            :climbName,
                            :gradeId,
                            :gradeScaleCode,
                            :gradeLabel,
                            :gradeIndex,
                            :gradePoints,
                            :gradeCalibrationVersion,
                            :effortProfileId,
                            :effortProfileCode,
                            :effortProfileName,
                            :baseEdl,
                            :totalMoves,
                            :executedMoves,
                            :edlCount,
                            :moveIntensity,
                            :styleMultiplier,
                            :relativeEffortMultiplier,
                            :classicLoad,
                            :adjustedLoad,
                            'v1',
                            :contactsTwoYears,
                            :attemptOrdinal,
                            :familiarityBand,
                            :fastRp,
                            :currentLevelGradeLabel,
                            :currentLevelIndex,
                            :relativeGradeDifference,
                            :outsideCalibratedRange,
                            :familiaritySource,
                            :historyConflictDetected,
                            :historyConflictConfirmed,
                            :notes,
                            :createdAt
                        )
                        """)
                .param("id", entryId)
                .param("sessionId", sessionId)
                .param(
                        "clientEntryId",
                        entry.clientEntryId()
                )
                .param("entryOrder", entry.entryOrder())
                .param("resultType", entry.resultType())
                .param("climbId", climb.id())
                .param("climbType", climb.climbType())
                .param("ascentMode", entry.ascentMode())
                .param(
                        "protectionMode",
                        entry.protectionMode()
                )
                .param("climbName", climb.name())
                .param("gradeId", grade.id())
                .param(
                        "gradeScaleCode",
                        grade.scaleCode()
                )
                .param("gradeLabel", grade.label())
                .param("gradeIndex", grade.gradeIndex())
                .param("gradePoints", grade.gradePoints())
                .param(
                        "gradeCalibrationVersion",
                        grade.calibrationVersion()
                )
                .param(
                        "effortProfileId",
                        effortProfile.id()
                )
                .param(
                        "effortProfileCode",
                        effortProfile.code()
                )
                .param(
                        "effortProfileName",
                        effortProfile.displayName()
                )
                .param("baseEdl", effortProfile.baseEdl())
                .param("totalMoves", entry.totalMoves())
                .param(
                        "executedMoves",
                        entry.executedMoves()
                )
                .param("edlCount", calculation.edlCount())
                .param(
                        "moveIntensity",
                        calculation.moveIntensity()
                )
                .param(
                        "styleMultiplier",
                        styleRule.multiplier()
                )
                .param(
                        "relativeEffortMultiplier",
                        relativeEffort.multiplier()
                )
                .param(
                        "classicLoad",
                        calculation.classicLoad()
                )
                .param(
                        "adjustedLoad",
                        calculation.adjustedLoad()
                )
                .param(
                        "contactsTwoYears",
                        history.contactsTwoYears()
                )
                .param("attemptOrdinal", attemptOrdinal)
                .param(
                        "familiarityBand",
                        styleRule.familiarityBand().name()
                )
                .param("fastRp", fastRp)
                .param(
                        "currentLevelGradeLabel",
                        level.gradeLabel()
                )
                .param(
                        "currentLevelIndex",
                        level.gradeIndex()
                )
                .param(
                        "relativeGradeDifference",
                        relativeEffort.relativeGradeDifference()
                )
                .param(
                        "outsideCalibratedRange",
                        relativeEffort.outsideCalibratedRange()
                )
                .param(
                        "familiaritySource",
                        styleRule.familiaritySource()
                )
                .param(
                        "historyConflictDetected",
                        styleRule.historyConflict()
                )
                .param(
                        "historyConflictConfirmed",
                        Boolean.TRUE.equals(
                                entry.historyConflictConfirmed()
                        )
                )
                .param("notes", entry.notes())
                .param(
                        "createdAt",
                        databaseTimestamp(createdAt)
                )
                .update();

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Expected to insert exactly one assessed entry"
            );
        }
    }

    void updateSessionSummary(
            UUID sessionId,
            CalculatedSession summary,
            String maxBoulderGrade,
            String maxRouteGrade,
            Instant updatedAt
    ) {
        Objects.requireNonNull(sessionId, "Session ID is required");
        Objects.requireNonNull(summary, "Session summary is required");
        Objects.requireNonNull(updatedAt, "Update time is required");

        int updatedRows = jdbcClient.sql("""
                        UPDATE training.training_sessions
                        SET total_moves = :totalMoves,
                            classic_load = :classicLoad,
                            adjusted_load = :adjustedLoad,
                            average_move_intensity = :averageMoveIntensity,
                            max_boulder_grade = :maxBoulderGrade,
                            max_route_grade = :maxRouteGrade,
                            updated_at = :updatedAt,
                            version = version + 1
                        WHERE id = :sessionId
                        """)
                .param("totalMoves", summary.totalMoves())
                .param("classicLoad", summary.classicLoad())
                .param("adjustedLoad", summary.adjustedLoad())
                .param(
                        "averageMoveIntensity",
                        summary.averageMoveIntensity()
                )
                .param("maxBoulderGrade", maxBoulderGrade)
                .param("maxRouteGrade", maxRouteGrade)
                .param(
                        "updatedAt",
                        databaseTimestamp(updatedAt)
                )
                .param("sessionId", sessionId)
                .update();

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Expected to update exactly one training session"
            );
        }
    }

    Optional<SessionDetailsRow> findDetailsSession(
            UUID userId,
            UUID sessionId
    ) {
        return jdbcClient.sql("""
                        SELECT
                            id,
                            session_name,
                            session_date,
                            time_zone_id,
                            started_at,
                            ended_at,
                            duration_minutes,
                            area_id,
                            location_name_snapshot,
                            total_moves,
                            classic_load,
                            adjusted_load,
                            average_move_intensity,
                            max_boulder_grade,
                            max_route_grade
                        FROM training.training_sessions
                        WHERE id = :sessionId
                          AND user_id = :userId
                        """)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .query((resultSet, rowNumber) ->
                        new SessionDetailsRow(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getString("session_name"),
                                resultSet.getObject(
                                        "session_date",
                                        LocalDate.class
                                ),
                                resultSet.getString("time_zone_id"),
                                toInstant(resultSet.getObject(
                                        "started_at",
                                        OffsetDateTime.class
                                )),
                                toInstant(resultSet.getObject(
                                        "ended_at",
                                        OffsetDateTime.class
                                )),
                                resultSet.getInt("duration_minutes"),
                                resultSet.getObject(
                                        "area_id",
                                        UUID.class
                                ),
                                resultSet.getString(
                                        "location_name_snapshot"
                                ),
                                resultSet.getInt("total_moves"),
                                resultSet.getBigDecimal("classic_load"),
                                resultSet.getBigDecimal("adjusted_load"),
                                resultSet.getBigDecimal(
                                        "average_move_intensity"
                                ),
                                resultSet.getString(
                                        "max_boulder_grade"
                                ),
                                resultSet.getString("max_route_grade")
                        )
                )
                .optional();
    }

    List<EntryDetails> findDetailsEntries(
            UUID userId,
            UUID sessionId
    ) {
        return jdbcClient.sql("""
                        SELECT
                            entry.id,
                            entry.client_entry_id,
                            entry.entry_order,
                            entry.result_type,
                            entry.climb_id,
                            entry.climb_name_snapshot,
                            entry.climb_type,
                            entry.grade_scale_code,
                            entry.grade_label,
                            entry.effort_profile_code,
                            entry.effort_profile_name_snapshot,
                            entry.ascent_mode,
                            entry.protection_mode,
                            entry.total_moves,
                            entry.executed_moves,
                            entry.notes,
                            entry.edl_count,
                            entry.move_intensity,
                            entry.style_multiplier,
                            entry.relative_effort_multiplier,
                            entry.classic_load,
                            entry.adjusted_load,
                            entry.current_level_grade_label,
                            entry.current_level_index,
                            entry.relative_grade_diff,
                            entry.outside_calibrated_range,
                            entry.calculation_model_version,
                            entry.contact_count_before_two_years,
                            entry.attempt_ordinal_all_time,
                            entry.familiarity_band,
                            entry.familiarity_source,
                            entry.fast_rp,
                            entry.history_conflict_detected,
                            entry.history_conflict_confirmed
                        FROM training.training_entries entry
                        JOIN training.training_sessions session
                          ON session.id = entry.session_id
                        WHERE session.id = :sessionId
                          AND session.user_id = :userId
                        ORDER BY entry.entry_order
                        """)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .query((resultSet, rowNumber) -> {
                    String resultType = resultSet.getString(
                            "result_type"
                    );

                    HistoryDetails history = "WARMUP".equals(resultType)
                            ? null
                            : new HistoryDetails(
                                    nullableInteger(
                                            resultSet,
                                            "contact_count_before_two_years"
                                    ),
                                    nullableInteger(
                                            resultSet,
                                            "attempt_ordinal_all_time"
                                    ),
                                    resultSet.getString("familiarity_band"),
                                    resultSet.getString(
                                            "familiarity_source"
                                    ),
                                    nullableBoolean(resultSet, "fast_rp"),
                                    nullableBoolean(
                                            resultSet,
                                            "history_conflict_detected"
                                    ),
                                    nullableBoolean(
                                            resultSet,
                                            "history_conflict_confirmed"
                                    )
                            );

                    CalculationDetails calculation =
                            new CalculationDetails(
                                    resultSet.getBigDecimal("edl_count"),
                                    resultSet.getBigDecimal(
                                            "move_intensity"
                                    ),
                                    resultSet.getBigDecimal(
                                            "style_multiplier"
                                    ),
                                    resultSet.getBigDecimal(
                                            "relative_effort_multiplier"
                                    ),
                                    resultSet.getBigDecimal("classic_load"),
                                    resultSet.getBigDecimal("adjusted_load"),
                                    resultSet.getString(
                                            "current_level_grade_label"
                                    ),
                                    nullableInteger(
                                            resultSet,
                                            "current_level_index"
                                    ),
                                    nullableInteger(
                                            resultSet,
                                            "relative_grade_diff"
                                    ),
                                    nullableBoolean(
                                            resultSet,
                                            "outside_calibrated_range"
                                    ),
                                    resultSet.getString(
                                            "calculation_model_version"
                                    )
                            );

                    return new EntryDetails(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject(
                                    "client_entry_id",
                                    UUID.class
                            ),
                            resultSet.getInt("entry_order"),
                            resultType,
                            resultSet.getObject("climb_id", UUID.class),
                            resultSet.getString("climb_name_snapshot"),
                            resultSet.getString("climb_type"),
                            resultSet.getString("grade_scale_code"),
                            resultSet.getString("grade_label"),
                            resultSet.getString("effort_profile_code"),
                            resultSet.getString(
                                    "effort_profile_name_snapshot"
                            ),
                            resultSet.getString("ascent_mode"),
                            resultSet.getString("protection_mode"),
                            nullableInteger(resultSet, "total_moves"),
                            resultSet.getInt("executed_moves"),
                            resultSet.getString("notes"),
                            calculation,
                            history
                    );
                })
                .list();
    }

    private OffsetDateTime databaseTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private Integer nullableInteger(
            java.sql.ResultSet resultSet,
            String columnName
    ) throws java.sql.SQLException {
        return resultSet.getObject(columnName, Integer.class);
    }

    private Boolean nullableBoolean(
            java.sql.ResultSet resultSet,
            String columnName
    ) throws java.sql.SQLException {
        return resultSet.getObject(columnName, Boolean.class);
    }

    record SessionDetailsRow(
            UUID sessionId,
            String sessionName,
            LocalDate sessionDate,
            String timeZoneId,
            Instant startedAt,
            Instant endedAt,
            int durationMinutes,
            UUID areaId,
            String areaName,
            int totalMoves,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            BigDecimal averageMoveIntensity,
            String maxBoulderGrade,
            String maxRouteGrade
    ) {

        TrainingSessionDetails toDetails(List<EntryDetails> entries) {
            return new TrainingSessionDetails(
                    sessionId,
                    sessionName,
                    sessionDate,
                    timeZoneId,
                    startedAt,
                    endedAt,
                    durationMinutes,
                    areaId,
                    areaName,
                    totalMoves,
                    classicLoad,
                    adjustedLoad,
                    averageMoveIntensity,
                    maxBoulderGrade,
                    maxRouteGrade,
                    entries
            );
        }
    }

    record ExistingSession(
            UUID id,
            String requestFingerprint
    ) {
    }

    record SessionRow(
            UUID id,
            UUID clientSessionId,
            String requestFingerprint,
            UUID userId,
            UUID areaId,
            String sessionName,
            LocalDate sessionDate,
            String timeZoneId,
            int durationMinutes,
            Instant startedAt,
            Instant endedAt,
            String locationName,
            int totalMoves,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            BigDecimal averageMoveIntensity,
            String maxBoulderGrade,
            String maxRouteGrade,
            Instant createdAt
    ) {
    }
}
