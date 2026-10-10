package pl.climbbetter.reporting.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.reporting.DashboardSessionSummary;

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
                        resultSet.getString("max_boulder_grade"),
                        resultSet.getString("max_route_grade")
                ))
                .list();
    }
}
