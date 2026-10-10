package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.reporting.DashboardQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/dashboard-session.sql")
public class DashboardQueryIntegrationTest {
    @Autowired
    DashboardQuery dashboardQuery;

    @Test
    void returnsCurrentUsersRecentSession() {
        var sessions = dashboardQuery.findRecentSessions();

        assertEquals(1, sessions.size());

        var session = sessions.getFirst();

        assertAll(
                () -> assertEquals(
                        UUID.fromString("20000000-0000-0000-0000-000000000001"),
                        session.sessionId()
                ),
                () -> assertEquals("Evening Training", session.sessionName()),
                () -> assertEquals(LocalDate.of(2026, 10, 10), session.sessionDate()),
                () -> assertEquals("My Gym", session.areaName()),
                () -> assertEquals(90, session.durationMinutes()),
                () -> assertEquals(120, session.totalMoves()),
                () -> assertEquals(new BigDecimal("850.0000"), session.classicLoad()),
                () -> assertEquals(new BigDecimal("920.5000"), session.adjustedLoad()),
                () -> assertEquals("7A", session.maxBoulderGrade()),
                () -> assertEquals("7c", session.maxRouteGrade())
        );
    }
}
