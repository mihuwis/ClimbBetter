package pl.climbbetter.reporting.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
class PeriodSummaryRepository {
    private final JdbcClient jdbcClient;

    PeriodSummaryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<SessionRow> findSessions(UUID userId, LocalDate start, LocalDate endExclusive, String discipline) {
        // Collapse the matching entries before summing duration: a mixed session still has one clock.
        return query("""
                SELECT session.id AS session_id, session.session_name, session.session_date,
                       session.location_name_snapshot AS area_name, session.duration_minutes,
                       SUM(entry.executed_moves) AS executed_moves,
                       SUM(entry.classic_load) AS classic_load,
                       SUM(entry.adjusted_load) AS adjusted_load,
                       COALESCE(SUM(entry.executed_moves) FILTER (WHERE entry.climb_type = 'BOULDER'), 0) AS boulder_moves,
                       COALESCE(SUM(entry.executed_moves) FILTER (WHERE entry.climb_type IN ('ROUTE', 'CIRCUIT')), 0) AS route_moves,
                       COALESCE(SUM(entry.executed_moves) FILTER (WHERE entry.result_type = 'WARMUP'), 0) AS warmup_moves
                FROM training.training_sessions session
                JOIN training.training_entries entry ON entry.session_id = session.id
                WHERE session.user_id = :userId
                  AND session.deleted_at IS NULL
                  AND session.session_date >= :start
                  AND session.session_date < :endExclusive
                  AND (:discipline = 'ALL'
                       OR (:discipline = 'BOULDER' AND entry.climb_type = 'BOULDER')
                       OR (:discipline = 'ROUTE_AND_CIRCUIT' AND entry.climb_type IN ('ROUTE', 'CIRCUIT')))
                GROUP BY session.id
                ORDER BY session.session_date, COALESCE(session.started_at, session.created_at), session.id
                """, userId, start, endExclusive, discipline)
                .query((row, number) -> new SessionRow(
                        row.getObject("session_id", UUID.class), row.getString("session_name"),
                        row.getObject("session_date", LocalDate.class), row.getString("area_name"),
                        row.getLong("duration_minutes"), row.getLong("executed_moves"),
                        row.getBigDecimal("classic_load"), row.getBigDecimal("adjusted_load"),
                        row.getLong("boulder_moves"), row.getLong("route_moves"), row.getLong("warmup_moves")
                )).list();
    }

    List<AscentRow> findAscents(UUID userId, LocalDate start, LocalDate endExclusive, String discipline) {
        // Period filtering precedes deduplication; an OS outside the window cannot replace a recent RP.
        return query("""
                SELECT session_id, id AS entry_id, climb_id, climb_name_snapshot, climb_type,
                       grade_label, grade_scale_code, grade_index, ascent_mode,
                       achievement_session_date AS session_date, achievement_area_name AS area_name
                FROM reporting.achievement_ascents
                WHERE achievement_user_id = :userId
                  AND achievement_session_date >= :start
                  AND achievement_session_date < :endExclusive
                  AND ascent_mode IN ('OS', 'FLASH', 'RP')
                  AND (:discipline = 'ALL'
                       OR (:discipline = 'BOULDER' AND climb_type = 'BOULDER')
                       OR (:discipline = 'ROUTE_AND_CIRCUIT' AND climb_type IN ('ROUTE', 'CIRCUIT')))
                ORDER BY grade_index DESC, achievement_session_date, achievement_session_started_at,
                         session_id, entry_order, id
                """, userId, start, endExclusive, discipline)
                .query((row, number) -> new AscentRow(
                        row.getObject("session_id", UUID.class), row.getObject("entry_id", UUID.class),
                        row.getObject("climb_id", UUID.class), row.getString("climb_name_snapshot"),
                        row.getString("climb_type"), row.getString("grade_label"), row.getString("grade_scale_code"),
                        row.getInt("grade_index"), row.getString("ascent_mode"),
                        row.getObject("session_date", LocalDate.class), row.getString("area_name")
                )).list();
    }

    private JdbcClient.StatementSpec query(
            String sql, UUID userId, LocalDate start, LocalDate endExclusive, String discipline
    ) {
        return jdbcClient.sql(sql).param("userId", userId).param("start", start)
                .param("endExclusive", endExclusive).param("discipline", discipline);
    }

    record SessionRow(UUID sessionId, String sessionName, LocalDate sessionDate, String areaName,
                      long durationMinutes, long executedMoves, BigDecimal classicLoad, BigDecimal adjustedLoad,
                      long boulderMoves, long routeMoves, long warmupMoves) { }

    record AscentRow(UUID sessionId, UUID entryId, UUID climbId, String climbName, String climbType,
                     String gradeLabel, String gradeScaleCode, int gradeIndex, String ascentMode,
                     LocalDate sessionDate, String areaName) { }
}
