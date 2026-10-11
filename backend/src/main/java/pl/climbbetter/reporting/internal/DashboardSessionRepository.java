package pl.climbbetter.reporting.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.reporting.DashboardSessionSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository 
class DashboardSessionRepository {
    private final JdbcClient jdbcClient;

    DashboardSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<DashboardSessionSummary> findRecentSessions(UUID userId) {
        return jdbcClient.sql("""
                        SELECT
                            id,
                            session_name,
                            session_date,
                            location_name_snapshot,
                            duration_minutes,
                            total_moves,
                            classic_load,
                            adjusted_load,
                            average_move_intensity,
                            max_boulder_grade,
                            max_route_grade
                        FROM training.training_sessions
                        WHERE user_id = :userId
                        ORDER BY session_date DESC, created_at DESC
                        LIMIT 20
                        """)
                .param("userId", userId)
                .query((resultSet, rowNumber) -> new DashboardSessionSummary(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("session_name"),
                        resultSet.getObject("session_date", java.time.LocalDate.class),
                        resultSet.getString("location_name_snapshot"),
                        resultSet.getInt("duration_minutes"),
                        resultSet.getInt("total_moves"),
                        resultSet.getBigDecimal("classic_load"),
                        resultSet.getBigDecimal("adjusted_load"),
                        resultSet.getBigDecimal(
                                "average_move_intensity"
                        ),
                        resultSet.getString("max_boulder_grade"),
                        resultSet.getString("max_route_grade")
                ))
                .list();
    }

    List<PersonalBestRow> findPersonalBests(UUID userId) {
        return jdbcClient.sql("""
                        WITH ranked_ascents AS (
                            SELECT
                                entry.grade_scale_code,
                                entry.ascent_mode,
                                entry.grade_label,
                                entry.grade_index,
                                session.session_date,
                                session.id AS session_id,
                                ROW_NUMBER() OVER (
                                    PARTITION BY entry.grade_scale_code
                                    ORDER BY
                                        entry.grade_index DESC,
                                        session.session_date ASC,
                                        COALESCE(
                                            session.started_at,
                                            session.created_at
                                        ) ASC,
                                        entry.entry_order ASC
                                ) AS overall_rank,
                                ROW_NUMBER() OVER (
                                    PARTITION BY
                                        entry.grade_scale_code,
                                        entry.ascent_mode
                                    ORDER BY
                                        entry.grade_index DESC,
                                        session.session_date ASC,
                                        COALESCE(
                                            session.started_at,
                                            session.created_at
                                        ) ASC,
                                        entry.entry_order ASC
                                ) AS style_rank
                            FROM training.training_entries entry
                            JOIN training.training_sessions session
                              ON session.id = entry.session_id
                            WHERE session.user_id = :userId
                              AND entry.result_type = 'ASCENT'
                        )
                        SELECT
                            grade_scale_code,
                            'OVERALL' AS category,
                            grade_label,
                            grade_index,
                            session_date,
                            session_id
                        FROM ranked_ascents
                        WHERE overall_rank = 1

                        UNION ALL

                        SELECT
                            grade_scale_code,
                            ascent_mode AS category,
                            grade_label,
                            grade_index,
                            session_date,
                            session_id
                        FROM ranked_ascents
                        WHERE style_rank = 1
                        """)
                .param("userId", userId)
                .query((resultSet, rowNumber) ->
                        new PersonalBestRow(
                                resultSet.getString(
                                        "grade_scale_code"
                                ),
                                resultSet.getString("category"),
                                resultSet.getString("grade_label"),
                                resultSet.getInt("grade_index"),
                                resultSet.getObject(
                                        "session_date",
                                        LocalDate.class
                                ),
                                resultSet.getObject(
                                        "session_id",
                                        UUID.class
                                )
                        )
                )
                .list();
    }

    record PersonalBestRow(
            String scaleCode,
            String category,
            String gradeLabel,
            int gradeIndex,
            LocalDate achievedOn,
            UUID sessionId
    ) {
    }
}
