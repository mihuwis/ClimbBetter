package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.reporting.DashboardQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.HashSet;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/dashboard-session.sql")
public class DashboardQueryIntegrationTest {
    @Autowired
    DashboardQuery dashboardQuery;

    @Autowired
    JdbcClient jdbcClient;

    private int nextSessionSlot;

    @Test
    void paginatesTiedDatesWithoutRepeatsAfterBoundaryDeletionAndNewInsert() {
        for (int i = 1; i <= 44; i++) cloneSession(new UUID(0x4000000000000000L, i), false, false);
        UUID foreign = new UUID(0x7000000000000000L, 1);
        UUID deleted = new UUID(0x7000000000000000L, 2);
        cloneSession(foreign, true, false);
        cloneSession(deleted, false, true);
        var first = dashboardQuery.findSessionsPage(20, null);
        assertEquals(20, first.items().size());
        UUID boundary = first.items().getLast().sessionId();
        jdbcClient.sql("UPDATE training.training_sessions SET deleted_at = now() WHERE id = :id")
                .param("id", boundary).update();
        UUID newer = new UUID(0x6000000000000000L, 1);
        cloneSession(newer, false, false);
        var collected = new ArrayList<>(first.items());
        String cursor = first.nextCursor();
        int requests = 0;
        while (cursor != null) {
            assertTrue(++requests < 10);
            var page = dashboardQuery.findSessionsPage(20, cursor);
            collected.addAll(page.items());
            cursor = page.nextCursor();
        }
        var ids = collected.stream().map(s -> s.sessionId()).toList();
        assertEquals(45, ids.size());
        assertEquals(45, new HashSet<>(ids).size());
        assertFalse(ids.contains(foreign));
        assertFalse(ids.contains(deleted));
        assertFalse(ids.contains(newer));
        assertEquals(newer, dashboardQuery.findSessionsPage(20, null).items().getFirst().sessionId());
    }

    @Test
    void endsAtAnExactPageAndRejectsInvalidPagination() {
        for (int i = 1; i <= 19; i++) cloneSession(new UUID(0x4000000000000000L, i), false, false);
        var page = dashboardQuery.findSessionsPage(20, null);
        assertEquals(20, page.items().size());
        assertNull(page.nextCursor());
        assertThrows(IllegalArgumentException.class, () -> dashboardQuery.findSessionsPage(0, null));
        assertThrows(IllegalArgumentException.class, () -> dashboardQuery.findSessionsPage(101, null));
        assertThrows(IllegalArgumentException.class, () -> dashboardQuery.findSessionsPage(20, "invalid"));
    }

    private void cloneSession(UUID id, boolean foreign, boolean deleted) {
        if (foreign) jdbcClient.sql("""
                INSERT INTO identity.users(id, display_name, created_at)
                VALUES ('00000000-0000-0000-0000-000000000002', 'Other climber', now())
                ON CONFLICT (id) DO NOTHING
                """).update();
        jdbcClient.sql("""
                INSERT INTO training.training_sessions (
                    id, client_session_id, user_id, area_id, session_name, session_date, time_zone_id,
                    duration_minutes, location_name_snapshot, total_moves, classic_load, adjusted_load,
                    average_move_intensity, created_at, updated_at, version, deleted_at, started_at, ended_at)
                SELECT :id, :id, CAST(:user AS uuid), area_id, 'Paged session', session_date, time_zone_id,
                    5, location_name_snapshot, total_moves, classic_load, adjusted_load,
                    average_move_intensity, created_at, updated_at, version,
                    CASE WHEN :deleted THEN now() ELSE NULL END,
                    TIMESTAMPTZ '2026-10-10 00:00:00Z' + :slot * INTERVAL '5 minutes',
                    TIMESTAMPTZ '2026-10-10 00:05:00Z' + :slot * INTERVAL '5 minutes'
                FROM training.training_sessions WHERE id = '20000000-0000-0000-0000-000000000001'
                """).param("id", id).param("user", foreign ? "00000000-0000-0000-0000-000000000002"
                        : "00000000-0000-0000-0000-000000000001")
                .param("deleted", deleted).param("slot", nextSessionSlot++).update();
    }

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
                () -> assertEquals(
                        new BigDecimal("12.5000000000"),
                        session.averageMoveIntensity()
                ),
                // The fixture has no qualifying ascent entries: stored summary labels
                // alone are not evidence of an achievement.
                () -> assertNull(session.maxBoulderGrade()),
                () -> assertNull(session.maxRouteGrade()),
                () -> assertEquals("v1", session.calculationModelVersion())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "v1, v1, v1",
            "v2, v2, v2",
            "v1, v2, mixed"
    })
    void reportsEntryModelVersionsWithoutRecalculatingStoredLoads(
            String firstVersion,
            String secondVersion,
            String expectedVersion
    ) {
        insertWarmUpEntry(0, firstVersion);
        insertWarmUpEntry(1, secondVersion);

        var session = dashboardQuery.findRecentSessions().getFirst();

        assertAll(
                () -> assertEquals(expectedVersion, session.calculationModelVersion()),
                () -> assertEquals(new BigDecimal("850.0000"), session.classicLoad()),
                () -> assertEquals(new BigDecimal("920.5000"), session.adjustedLoad()),
                () -> assertEquals(
                        new BigDecimal("12.5000000000"), session.averageMoveIntensity()
                )
        );
    }

    private void insertWarmUpEntry(int entryOrder, String modelVersion) {
        jdbcClient.sql("""
                        INSERT INTO training.training_entries (
                            id, session_id, client_entry_id, entry_order,
                            result_type, executed_moves, classic_load, adjusted_load,
                            calculation_model_version, created_at
                        )
                        VALUES (
                            :id, :sessionId, :clientEntryId, :entryOrder,
                            'WARMUP', 20, 0, 0,
                            :modelVersion, TIMESTAMPTZ '2026-10-10 18:00:00+00'
                        )
                        """)
                .param("id", UUID.randomUUID())
                .param("sessionId", UUID.fromString("20000000-0000-0000-0000-000000000001"))
                .param("clientEntryId", UUID.randomUUID())
                .param("entryOrder", entryOrder)
                .param("modelVersion", modelVersion)
                .update();
    }
}
