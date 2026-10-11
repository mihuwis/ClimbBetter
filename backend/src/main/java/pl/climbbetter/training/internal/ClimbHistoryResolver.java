package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Repository
class ClimbHistoryResolver {

    private final JdbcClient jdbcClient;

    ClimbHistoryResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ClimbHistory resolve(
            UUID userId,
            UUID climbId,
            LocalDate sessionDate
    ) {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(climbId, "Climb ID is required");
        Objects.requireNonNull(sessionDate, "Session date is required");

        LocalDate historyStartDate = sessionDate.minusYears(2);

        return jdbcClient.sql("""
                        SELECT
                            COUNT(*) FILTER (
                                WHERE session.session_date >= :historyStartDate
                            ) AS contacts_two_years,
                            COUNT(*) AS contacts_all_time
                        FROM training.training_entries entry
                        JOIN training.training_sessions session
                          ON session.id = entry.session_id
                        WHERE session.user_id = :userId
                          AND entry.climb_id = :climbId
                          AND session.session_date <= :sessionDate
                          AND entry.result_type IN ('ASCENT', 'ATTEMPT')
                        """)
                .param("historyStartDate", historyStartDate)
                .param("sessionDate", sessionDate)
                .param("userId", userId)
                .param("climbId", climbId)
                .query((resultSet, rowNumber) -> new ClimbHistory(
                        Math.toIntExact(
                                resultSet.getLong("contacts_two_years")
                        ),
                        Math.toIntExact(
                                resultSet.getLong("contacts_all_time")
                        )
                ))
                .single();
    }

    record ClimbHistory(
            int contactsTwoYears,
            int contactsAllTime
    ) {
    }
}