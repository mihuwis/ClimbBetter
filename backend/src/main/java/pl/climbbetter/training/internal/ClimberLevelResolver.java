package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
class ClimberLevelResolver {

    private final JdbcClient jdbcClient;

    ClimberLevelResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<ResolvedLevel> resolve(
            UUID userId,
            String scaleCode,
            LocalDate sessionDate
    ) {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(
                scaleCode,
                "Scale code is required"
        );
        Objects.requireNonNull(
                sessionDate,
                "Session date is required"
        );

        validateScaleCode(scaleCode);

        LocalDate historyStartDate = sessionDate.minusYears(2);

        return jdbcClient.sql("""
                        WITH history AS (
                            SELECT
                                entry.result_type,
                                entry.grade_label,
                                entry.grade_index
                            FROM training.training_entries entry
                            JOIN training.training_sessions session
                              ON session.id = entry.session_id
                            WHERE session.user_id = :userId
                              AND session.session_date >= :historyStartDate
                              AND session.session_date <= :sessionDate
                              AND entry.grade_scale_code = :scaleCode
                              AND entry.result_type IN (
                                  'ASCENT',
                                  'ATTEMPT'
                              )
                        ),
                        highest_ascent AS (
                            SELECT
                                grade_label,
                                grade_index
                            FROM history
                            WHERE result_type = 'ASCENT'
                            ORDER BY grade_index DESC
                            LIMIT 1
                        ),
                        attempt_fallback AS (
                            SELECT
                                grade.label AS grade_label,
                                grade.grade_index
                            FROM catalog.grades grade
                            WHERE grade.scale_code = :scaleCode
                              AND grade.is_active = true
                              AND grade.grade_index = (
                                  SELECT GREATEST(
                                      MIN(history.grade_index) - 1,
                                      0
                                  )
                                  FROM history
                              )
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM highest_ascent
                              )
                        )
                        SELECT
                            grade_label,
                            grade_index,
                            'HIGHEST_ASCENT' AS source
                        FROM highest_ascent

                        UNION ALL

                        SELECT
                            grade_label,
                            grade_index,
                            'ATTEMPT_FALLBACK' AS source
                        FROM attempt_fallback

                        LIMIT 1
                        """)
                .param("userId", userId)
                .param("scaleCode", scaleCode)
                .param(
                        "historyStartDate",
                        historyStartDate
                )
                .param("sessionDate", sessionDate)
                .query((resultSet, rowNumber) ->
                        new ResolvedLevel(
                                resultSet.getString(
                                        "grade_label"
                                ),
                                resultSet.getInt(
                                        "grade_index"
                                ),
                                resultSet.getString("source")
                        )
                )
                .optional();
    }

    ResolvedLevel inferFromSession(
            String scaleCode,
            List<GradeResolver.ResolvedGrade> ascentGrades,
            List<GradeResolver.ResolvedGrade> attemptGrades
    ) {
        Objects.requireNonNull(
                scaleCode,
                "Scale code is required"
        );
        Objects.requireNonNull(
                ascentGrades,
                "Ascent grades are required"
        );
        Objects.requireNonNull(
                attemptGrades,
                "Attempt grades are required"
        );

        validateScaleCode(scaleCode);
        validateGrades(scaleCode, ascentGrades);
        validateGrades(scaleCode, attemptGrades);

        Optional<GradeResolver.ResolvedGrade> highestAscent =
                ascentGrades.stream()
                        .max(Comparator.comparingInt(
                                GradeResolver.ResolvedGrade::gradeIndex
                        ));

        if (highestAscent.isPresent()) {
            GradeResolver.ResolvedGrade grade =
                    highestAscent.orElseThrow();

            return new ResolvedLevel(
                    grade.label(),
                    grade.gradeIndex(),
                    "LOW_SAMPLE_MAX"
            );
        }

        int lowestAttemptIndex = attemptGrades.stream()
                .mapToInt(
                        GradeResolver.ResolvedGrade::gradeIndex
                )
                .min()
                .orElseThrow(() -> new IllegalArgumentException(
                        "An ascent or attempt is required "
                                + "to infer climber level"
                ));

        int fallbackIndex = Math.max(
                lowestAttemptIndex - 1,
                0
        );

        return findActiveGrade(scaleCode, fallbackIndex);
    }

    private ResolvedLevel findActiveGrade(
            String scaleCode,
            int gradeIndex
    ) {
        return jdbcClient.sql("""
                        SELECT
                            label,
                            grade_index
                        FROM catalog.grades
                        WHERE scale_code = :scaleCode
                          AND grade_index = :gradeIndex
                          AND is_active = true
                        """)
                .param("scaleCode", scaleCode)
                .param("gradeIndex", gradeIndex)
                .query((resultSet, rowNumber) ->
                        new ResolvedLevel(
                                resultSet.getString("label"),
                                resultSet.getInt(
                                        "grade_index"
                                ),
                                "ATTEMPT_FALLBACK"
                        )
                )
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active fallback grade for "
                                + scaleCode
                                + " at index "
                                + gradeIndex
                ));
    }

    private void validateGrades(
            String expectedScaleCode,
            List<GradeResolver.ResolvedGrade> grades
    ) {
        boolean containsDifferentScale = grades.stream()
                .anyMatch(grade ->
                        !expectedScaleCode.equals(
                                grade.scaleCode()
                        )
                );

        if (containsDifferentScale) {
            throw new IllegalArgumentException(
                    "Cannot infer level from different grade scales"
            );
        }
    }

    private void validateScaleCode(String scaleCode) {
        if (!scaleCode.equals("BOULDER_FONT")
                && !scaleCode.equals("ROUTE_FRENCH")) {
            throw new IllegalArgumentException(
                    "Unsupported grade scale: " + scaleCode
            );
        }
    }

    record ResolvedLevel(
            String gradeLabel,
            int gradeIndex,
            String source
    ) {
    }
}