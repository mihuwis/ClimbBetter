package pl.climbbetter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.PeriodTrainingSummary;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PeriodTrainingSummaryIntegrationTest {
    private static final String ENDPOINT = "/api/v1/statistics/summary";
    private static final LocalDate NOVEMBER = LocalDate.of(2024, 11, 1);

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CurrentUserProvider currentUserProvider;

    private UUID userId;
    private UUID clientAreaId;

    @BeforeEach
    void useIsolatedUser() {
        userId = UUID.randomUUID();
        clientAreaId = UUID.randomUUID();
        insertUser(userId);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
    }

    @Test
    void emptyLeapMonthHasEveryDayAndNoInventedAveragesOrAchievements() throws Exception {
        PeriodTrainingSummary report = report("MONTH", 2024, 2, "ALL");
        assertEquals(LocalDate.of(2024, 2, 1), report.windowStart());
        assertEquals(LocalDate.of(2024, 2, 29), report.windowEnd());
        assertEquals(29, report.buckets().size());
        assertEquals(0, report.summary().sessionCount());
        assertEquals(0, report.summary().executedMoves());
        assertEquals(0, report.uniqueClimbCount());
        assertEquals(0, report.warmupMoves());
        decimal("0", report.summary().classicLoad());
        decimal("0", report.summary().adjustedLoad());
        assertNull(report.summary().averageDurationMinutes());
        assertNull(report.summary().averageClassicLoad());
        assertNull(report.summary().averageAdjustedLoad());
        assertNull(report.summary().maxClassicLoad());
        assertNull(report.summary().maxAdjustedLoad());
        assertEquals(2, report.disciplineBreakdown().size());
        assertEquals(3, report.styleBreakdown().size());
        assertTrue(report.disciplineBreakdown().stream().allMatch(item -> item.executedMoves() == 0));
        assertTrue(report.styleBreakdown().stream().allMatch(item -> item.climbCount() == 0));
        assertNull(report.highlights().maxClassicSession());
        assertNull(report.highlights().maxAdjustedSession());
        assertNull(report.highlights().boulders().hardest());
        assertNull(report.highlights().routesAndCircuits().hardest());
        for (int i = 0; i < report.buckets().size(); i++) {
            var bucket = report.buckets().get(i);
            assertEquals(LocalDate.of(2024, 2, i + 1), bucket.windowStart());
            assertEquals(bucket.windowStart(), bucket.windowEnd());
            assertEquals(0, bucket.metrics().sessionCount());
            assertTrue(bucket.sessions().isEmpty());
            assertNull(bucket.metrics().averageAdjustedLoad());
        }
    }

    @Test
    void mixedSessionsKeepFullTimeOnceWhileMovesAndLoadsFollowTheDisciplineFilter() throws Exception {
        var boulder = entry("BOULDER", "6B", "ASCENT", "OS", 8, 8);
        var ropeAttempt = entry("ROUTE", "7a", "ATTEMPT", "RP", 20, 12);
        var circuit = entry("CIRCUIT", "6a", "ASCENT", "RP", 20, 20);
        UUID sessionId = create(NOVEMBER.plusDays(2), 8, 120, List.of(warmup(30), boulder, ropeAttempt, circuit));
        snapshotLoads(boulder, "10", "100");
        snapshotLoads(ropeAttempt, "20", "200");
        snapshotLoads(circuit, "30", "300");
        String before = storedState();

        PeriodTrainingSummary all = report("MONTH", 2024, 11, "ALL");
        PeriodTrainingSummary boulders = report("MONTH", 2024, 11, "BOULDER");
        PeriodTrainingSummary routes = report("MONTH", 2024, 11, "ROUTE_AND_CIRCUIT");
        assertAll(
                () -> assertEquals(1, all.summary().sessionCount()),
                () -> assertEquals(1, boulders.summary().sessionCount()),
                () -> assertEquals(1, routes.summary().sessionCount()),
                () -> assertEquals(120, all.summary().durationMinutes()),
                () -> assertEquals(120, boulders.summary().durationMinutes()),
                () -> assertEquals(120, routes.summary().durationMinutes()),
                () -> assertEquals(70, all.summary().executedMoves()),
                () -> assertEquals(8, boulders.summary().executedMoves()),
                () -> assertEquals(32, routes.summary().executedMoves()),
                () -> assertEquals(30, all.warmupMoves()),
                () -> assertEquals(0, boulders.warmupMoves()),
                () -> assertEquals(2, all.uniqueClimbCount()),
                () -> assertEquals(1, boulders.uniqueClimbCount()),
                () -> assertEquals(1, routes.uniqueClimbCount())
        );
        decimal("60", all.summary().classicLoad());
        decimal("600", all.summary().adjustedLoad());
        decimal("10", boulders.summary().classicLoad());
        decimal("100", boulders.summary().adjustedLoad());
        decimal("50", routes.summary().classicLoad());
        decimal("500", routes.summary().adjustedLoad());
        assertEquals(8, disciplineMoves(all, "BOULDER"));
        assertEquals(32, disciplineMoves(all, "ROUTE_AND_CIRCUIT"));
        assertEquals(0, disciplineMoves(boulders, "ROUTE_AND_CIRCUIT"));
        var day = bucket(all, NOVEMBER.plusDays(2));
        assertEquals(1, day.sessions().size());
        assertEquals(sessionId, day.sessions().getFirst().sessionId());
        assertEquals(120, bucket(routes, NOVEMBER.plusDays(2)).sessions().getFirst().durationMinutes());
        decimal("500", bucket(routes, NOVEMBER.plusDays(2)).sessions().getFirst().adjustedLoad());
        assertEquals(before, storedState(), "Reading different views must preserve snapshots, requests and audit");
    }

    @Test
    void maximaUseWholeSessionLoadsAndPeriodAveragesUseSessionsRatherThanDailyAverages() throws Exception {
        var first = entry("BOULDER", "6B", "ASCENT", "RP", 8, 8);
        var second = entry("BOULDER", "6A", "ASCENT", "RP", 8, 8);
        UUID twoEntrySession = create(NOVEMBER, 8, 60, List.of(first, second));
        snapshotLoads(first, "5", "20");
        snapshotLoads(second, "5", "30");
        var third = entry("BOULDER", "6A", "ASCENT", "RP", 8, 8);
        UUID maxClassic = create(NOVEMBER, 12, 20, List.of(third));
        snapshotLoads(third, "40", "10");
        var fourth = entry("BOULDER", "6B", "ASCENT", "RP", 8, 8);
        UUID maxAdjusted = create(NOVEMBER.plusDays(1), 8, 100, List.of(fourth));
        snapshotLoads(fourth, "10", "100");

        var report = report("MONTH", 2024, 11, "ALL");
        assertEquals(3, report.summary().sessionCount());
        assertEquals(2, report.summary().activeDays());
        assertEquals(180, report.summary().durationMinutes());
        decimal("60", report.summary().averageDurationMinutes());
        decimal("60", report.summary().classicLoad());
        decimal("20", report.summary().averageClassicLoad());
        decimal("160", report.summary().adjustedLoad());
        decimal("53.3333", report.summary().averageAdjustedLoad());
        decimal("40", report.summary().maxClassicLoad());
        decimal("100", report.summary().maxAdjustedLoad());
        var day = bucket(report, NOVEMBER);
        assertEquals(2, day.metrics().sessionCount());
        decimal("60", day.metrics().adjustedLoad());
        decimal("30", day.metrics().averageAdjustedLoad());
        decimal("50", day.metrics().maxAdjustedLoad());
        assertEquals(2, day.sessions().size());
        assertTrue(day.sessions().stream().anyMatch(session -> session.sessionId().equals(twoEntrySession)));
        assertEquals(maxClassic, report.highlights().maxClassicSession().sessionId());
        assertEquals(maxAdjusted, report.highlights().maxAdjustedSession().sessionId());
    }

    @Test
    void successfulStylesAreExclusiveUniqueAndChosenOnlyAfterFilteringThePeriod() throws Exception {
        var oldRouteOs = entry("ROUTE", "7a", "ASCENT", "OS", 20, 20);
        create(NOVEMBER.minusDays(1), 8, 60, List.of(oldRouteOs));
        create(NOVEMBER, 8, 60, List.of(repeat(oldRouteOs, "ASCENT", "RP")));

        var os = entry("BOULDER", "6B", "ASCENT", "OS", 8, 8);
        create(NOVEMBER.plusDays(1), 8, 60, List.of(os));
        create(NOVEMBER.plusDays(2), 8, 60, List.of(repeat(os, "ASCENT", "RP")));
        var flash = entry("BOULDER", "6C", "ASCENT", "FLASH", 8, 8);
        create(NOVEMBER.plusDays(3), 8, 60, List.of(flash));
        create(NOVEMBER.plusDays(4), 8, 60, List.of(repeat(flash, "ASCENT", "RP")));
        var failedOs = entry("BOULDER", "6A", "ATTEMPT", "OS", 8, 4);
        create(NOVEMBER.plusDays(5), 8, 60, List.of(failedOs));
        create(NOVEMBER.plusDays(6), 8, 60, List.of(repeat(failedOs, "ASCENT", "RP")));
        var topRope = entry("ROUTE", "8a", "ASCENT", "OS", 20, 20);
        topRope.put("protectionMode", "TOP_ROPE");
        create(NOVEMBER.plusDays(7), 8, 60, List.of(topRope));
        create(NOVEMBER.plusDays(8), 8, 60, List.of(entry("ROUTE", "8b", "ATTEMPT", "OS", 20, 12)));

        var report = report("MONTH", 2024, 11, "ALL");
        assertEquals(4, report.uniqueClimbCount());
        assertEquals(1, styleCount(report, "OS"));
        assertEquals(1, styleCount(report, "FLASH"));
        assertEquals(2, styleCount(report, "RP"));
        assertEquals(report.uniqueClimbCount(), report.styleBreakdown().stream().mapToLong(PeriodTrainingSummary.StyleShare::climbCount).sum());
        assertEquals("6C", report.highlights().boulders().hardest().gradeLabel());
        assertEquals("6B", report.highlights().boulders().highestOs().gradeLabel());
        assertEquals("6C", report.highlights().boulders().highestFlash().gradeLabel());
        assertEquals("6C", report.highlights().boulders().highestRp().gradeLabel());
        assertEquals("7a", report.highlights().routesAndCircuits().hardest().gradeLabel());
        assertNull(report.highlights().routesAndCircuits().highestOs());
        assertEquals("7a", report.highlights().routesAndCircuits().highestRp().gradeLabel());
        assertTrue(report.summary().adjustedLoad().signum() > 0);
    }

    @Test
    void annualStylesDeduplicateAcrossMonthsInsteadOfAddingMonthlyUniqueCounts() throws Exception {
        var climb = entry("BOULDER", "6B", "ASCENT", "OS", 8, 8);
        create(LocalDate.of(2024, 1, 15), 8, 60, List.of(climb));
        create(LocalDate.of(2024, 2, 15), 8, 60, List.of(repeat(climb, "ASCENT", "RP")));
        create(LocalDate.of(2024, 3, 15), 8, 60, List.of(repeat(climb, "ASCENT", "RP")));

        var january = report("MONTH", 2024, 1, "ALL");
        var february = report("MONTH", 2024, 2, "ALL");
        var march = report("MONTH", 2024, 3, "ALL");
        var year = report("YEAR", 2024, null, "ALL");
        assertEquals(3, january.uniqueClimbCount() + february.uniqueClimbCount() + march.uniqueClimbCount());
        assertEquals(1, year.uniqueClimbCount());
        assertEquals(1, styleCount(year, "OS"));
        assertEquals(0, styleCount(year, "RP"));
        assertEquals(0, styleCount(february, "OS"));
        assertEquals(1, styleCount(february, "RP"));
        assertEquals(12, year.buckets().size());
        assertEquals(3, year.summary().sessionCount());
        assertEquals(3, year.buckets().stream().mapToLong(bucket -> bucket.metrics().sessionCount()).sum());
    }

    @Test
    void calendarBoundariesIncludeLeapDayAndAssignAnOvernightSessionToItsSavedTrainingDate() throws Exception {
        create(LocalDate.of(2024, 1, 31), 8, 10, List.of(warmup(2)));
        create(LocalDate.of(2024, 2, 1), 8, 10, List.of(warmup(3)));
        create(LocalDate.of(2024, 2, 29), 8, 10, List.of(warmup(5)));
        create(LocalDate.of(2024, 3, 1), 8, 10, List.of(warmup(7)));
        UUID overnight = create(LocalDate.of(2024, 12, 31), 23, 120, List.of(warmup(11)));
        create(LocalDate.of(2025, 1, 1), 8, 10, List.of(warmup(13)));

        var february = report("MONTH", 2024, 2, "ALL");
        assertEquals(29, february.buckets().size());
        assertEquals(2, february.summary().sessionCount());
        assertEquals(8, february.summary().executedMoves());
        assertEquals(1, bucket(february, LocalDate.of(2024, 2, 29)).metrics().sessionCount());
        var year = report("YEAR", 2024, null, "ALL");
        assertEquals(LocalDate.of(2024, 1, 1), year.windowStart());
        assertEquals(LocalDate.of(2024, 12, 31), year.windowEnd());
        assertEquals(12, year.buckets().size());
        assertEquals(5, year.summary().sessionCount());
        assertEquals(28, year.summary().executedMoves());
        var december = bucket(year, LocalDate.of(2024, 12, 1));
        assertEquals(LocalDate.of(2024, 12, 31), december.windowEnd());
        assertEquals(1, december.metrics().sessionCount());
        assertEquals(120, december.metrics().durationMinutes());
        assertEquals(overnight, december.sessions().getFirst().sessionId());
        decimal("0", year.summary().averageAdjustedLoad());
        decimal("0", year.summary().maxAdjustedLoad());
        assertEquals(0, year.uniqueClimbCount());
        assertEquals(0, report("MONTH", 2024, 2, "BOULDER").summary().sessionCount());
    }

    @Test
    void deletedForeignAndOutOfPeriodDataAreExcludedAndSavedSnapshotsRemainUnchanged() throws Exception {
        var savedClimb = entry("BOULDER", "6B", "ASCENT", "OS", 8, 8);
        UUID included = create(NOVEMBER, 8, 60, List.of(savedClimb));
        snapshotLoads(savedClimb, "11.1111", "22.2222");
        UUID deleted = create(NOVEMBER.plusDays(1), 8, 60, List.of(entry("ROUTE", "8a", "ASCENT", "OS", 20, 20)));
        mockMvc.perform(delete("/api/v1/training/sessions/{id}", deleted).param("expectedVersion", "0"))
                .andExpect(status().isNoContent());
        create(NOVEMBER.minusDays(1), 8, 60, List.of(entry("ROUTE", "8a", "ASCENT", "OS", 20, 20)));
        create(NOVEMBER.plusMonths(1), 8, 60, List.of(entry("ROUTE", "8a", "ASCENT", "OS", 20, 20)));
        UUID foreign = UUID.randomUUID();
        insertUser(foreign);
        when(currentUserProvider.currentUserId()).thenReturn(foreign);
        UUID ownArea = clientAreaId;
        clientAreaId = UUID.randomUUID();
        create(NOVEMBER, 8, 60, List.of(entry("ROUTE", "8a", "ASCENT", "OS", 20, 20)));
        clientAreaId = ownArea;
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        jdbcClient.sql("UPDATE catalog.climbs SET name = 'Changed current catalog name' WHERE owner_id = :id AND client_climb_id = :clientId")
                .param("id", userId).param("clientId", savedClimb.get("clientClimbId")).update();
        String before = storedState();

        var report = report("MONTH", 2024, 11, "ALL");
        assertEquals(1, report.summary().sessionCount());
        assertEquals(1, report.uniqueClimbCount());
        decimal("11.1111", report.summary().classicLoad());
        decimal("22.2222", report.summary().adjustedLoad());
        assertEquals(included, report.highlights().boulders().hardest().sessionId());
        assertEquals(savedClimb.get("climbName"), report.highlights().boulders().hardest().climbName());
        assertEquals("6B", report.highlights().boulders().hardest().gradeLabel());
        assertNull(report.highlights().routesAndCircuits().hardest());
        assertEquals(before, storedState(), "The read model must not recalculate stored results or add revisions");
    }

    @Test
    void reportsAllSessionsBeyondTheDashboardLimit() throws Exception {
        for (int day = 0; day < 21; day++) create(NOVEMBER.plusDays(day), 8, 10, List.of(warmup(1)));
        String dashboard = mockMvc.perform(get("/api/v1/dashboard/sessions"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(20, objectMapper.readTree(dashboard).size());
        var report = report("MONTH", 2024, 11, "ALL");
        assertEquals(21, report.summary().sessionCount());
        assertEquals(21, report.summary().activeDays());
        assertEquals(210, report.summary().durationMinutes());
        assertEquals(21, report.summary().executedMoves());
        assertEquals(21, report.buckets().stream().mapToInt(bucket -> bucket.sessions().size()).sum());
        assertEquals(21, report.warmupMoves());
        assertEquals(0, report.uniqueClimbCount());
        decimal("0", report.summary().classicLoad());
        decimal("0", report.summary().adjustedLoad());
    }

    @ParameterizedTest
    @CsvSource({
            "MONTH,2024,0,ALL", "MONTH,2024,13,ALL", "MONTH,2024,,ALL",
            "YEAR,2024,2,ALL", "MONTH,0,2,ALL", "MONTH,9999,2,ALL",
            "MONTH,2024,2,ROUTE", "WEEK,2024,2,ALL", "MONTH,abc,2,ALL"
    })
    void invalidPeriodsAndFiltersReturnAStableBadRequest(String period, String year, String month, String discipline) throws Exception {
        MockHttpServletRequestBuilder request = get(ENDPOINT).param("period", period).param("year", year).param("discipline", discipline);
        if (month != null) request.param("month", month);
        mockMvc.perform(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_SUMMARY_REQUEST"));
    }

    private PeriodTrainingSummary report(String period, int year, Integer month, String discipline) throws Exception {
        MockHttpServletRequestBuilder request = get(ENDPOINT).param("period", period).param("year", String.valueOf(year)).param("discipline", discipline);
        if (month != null) request.param("month", month.toString());
        String body = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, PeriodTrainingSummary.class);
    }

    private UUID create(LocalDate day, int hour, int durationMinutes, List<Map<String, Object>> entries) throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        var start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC);
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            var entry = new LinkedHashMap<>(entries.get(i));
            entry.put("entryOrder", i);
            ordered.add(entry);
        }
        Map<String, Object> request = Map.of(
                "clientSessionId", clientSessionId, "sessionName", "Period session " + clientSessionId,
                "sessionDate", day.toString(), "timeZoneId", "UTC",
                "startedAt", start.toString(), "endedAt", start.plusSeconds(durationMinutes * 60L).toString(),
                "clientAreaId", clientAreaId, "areaName", "Period area " + clientAreaId, "entries", ordered);
        String body = mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("sessionId").asText());
    }

    private Map<String, Object> entry(String type, String grade, String result, String style, int total, int executed) {
        UUID climb = UUID.randomUUID();
        var entry = new LinkedHashMap<String, Object>();
        entry.put("clientEntryId", UUID.randomUUID());
        entry.put("clientClimbId", climb);
        entry.put("climbName", "Period climb " + climb);
        entry.put("resultType", result);
        entry.put("climbType", type);
        entry.put("gradeLabel", grade);
        entry.put("effortProfileCode", type.equals("BOULDER") ? "BOULDER" : type.equals("CIRCUIT") ? "CIRCUIT" : "MEDIUM_CONTINUOUS");
        entry.put("ascentMode", style);
        entry.put("totalMoves", total);
        entry.put("executedMoves", executed);
        entry.put("historyConflictConfirmed", false);
        if (type.equals("ROUTE")) entry.put("protectionMode", "LEAD");
        return entry;
    }

    private Map<String, Object> repeat(Map<String, Object> source, String result, String style) {
        var copy = new LinkedHashMap<>(source);
        copy.put("clientEntryId", UUID.randomUUID());
        copy.put("resultType", result);
        copy.put("ascentMode", style);
        if (result.equals("ASCENT")) copy.put("executedMoves", copy.get("totalMoves"));
        return copy;
    }

    private Map<String, Object> warmup(int moves) {
        return Map.of("clientEntryId", UUID.randomUUID(), "resultType", "WARMUP", "executedMoves", moves);
    }

    private void snapshotLoads(Map<String, Object> entry, String classic, String adjusted) {
        // Fixed historical outputs are intentional: a reporting read must not recalculate them.
        jdbcClient.sql("UPDATE training.training_entries SET classic_load = :classic, adjusted_load = :adjusted WHERE client_entry_id = :entryId")
                .param("classic", new BigDecimal(classic)).param("adjusted", new BigDecimal(adjusted))
                .param("entryId", entry.get("clientEntryId")).update();
        jdbcClient.sql("""
                UPDATE training.training_sessions session
                SET classic_load = (SELECT sum(entry.classic_load) FROM training.training_entries entry WHERE entry.session_id = session.id),
                    adjusted_load = (SELECT sum(entry.adjusted_load) FROM training.training_entries entry WHERE entry.session_id = session.id)
                WHERE session.id = (SELECT session_id FROM training.training_entries WHERE client_entry_id = :entryId)
                """).param("entryId", entry.get("clientEntryId")).update();
    }

    private long styleCount(PeriodTrainingSummary report, String style) {
        return report.styleBreakdown().stream().filter(item -> item.style().equals(style)).findFirst().orElseThrow().climbCount();
    }

    private long disciplineMoves(PeriodTrainingSummary report, String discipline) {
        return report.disciplineBreakdown().stream().filter(item -> item.discipline().name().equals(discipline)).findFirst().orElseThrow().executedMoves();
    }

    private PeriodTrainingSummary.Bucket bucket(PeriodTrainingSummary report, LocalDate start) {
        return report.buckets().stream().filter(bucket -> bucket.windowStart().equals(start)).findFirst().orElseThrow();
    }

    private void decimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "Expected " + expected + ", got " + actual);
    }

    private void insertUser(UUID id) {
        jdbcClient.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Period summary test', now())")
                .param("id", id).update();
    }

    private String storedState() {
        return jdbcClient.sql("""
                SELECT md5(
                  coalesce((SELECT string_agg(to_jsonb(session)::text, '' ORDER BY session.id)
                    FROM training.training_sessions session WHERE user_id = :userId), '') ||
                  coalesce((SELECT string_agg(to_jsonb(entry)::text, '' ORDER BY entry.id)
                    FROM training.training_entries entry JOIN training.training_sessions session ON session.id = entry.session_id
                    WHERE session.user_id = :userId), '') ||
                  coalesce((SELECT string_agg(to_jsonb(revision)::text, '' ORDER BY revision.id)
                    FROM training.training_session_revisions revision JOIN training.training_sessions session ON session.id = revision.session_id
                    WHERE session.user_id = :userId), ''))
                """).param("userId", userId).query(String.class).single();
    }
}
