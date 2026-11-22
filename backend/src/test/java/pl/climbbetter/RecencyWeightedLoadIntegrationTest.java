package pl.climbbetter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.TrainingSessionDetails;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecencyWeightedLoadIntegrationTest {
    private static final LocalDate DAY = LocalDate.of(2004, 1, 1);
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CurrentUserProvider currentUserProvider;
    private UUID userId;
    private UUID areaId;
    private int nextProbeHour = 8;

    @BeforeEach
    void useIsolatedUser() {
        userId = UUID.randomUUID();
        areaId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Recency test', now())")
                .param("id", userId).update();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
    }

    @Test
    void weightsDistinctAchievementsAndInterpolatesEffortWithoutDiscountingTheClimbGrade() throws Exception {
        UUID oldSession = create(request(DAY.minusDays(365), 8, List.of(route("7b", "ASCENT", "LEAD"))));
        seedBase(10, DAY.minusDays(1));
        var calculation = probe(DAY).entries().getFirst().calculation();
        decimal(BigDecimal.valueOf(index("7a")).add(new BigDecimal("0.5")), calculation.effectiveCurrentLevelIndex());
        decimal("-0.5", calculation.effectiveRelativeGradeDifference());
        decimal("0.9", calculation.relativeEffortMultiplier());
        assertEquals("RECENCY_WEIGHTED_PEAK", calculation.levelReferenceSource());
        assertEquals(11, calculation.levelReferenceSampleSize());
        assertEquals(730, calculation.levelReferenceHorizonDays());
        assertTrue(calculation.levelEstimatorVersion().contains("top-rope=false|weighted-small-sample=false"));
        decimal("1", calculation.levelReferenceHistoryWeight());
        decimal("90", calculation.gradePoints());
        decimal("6.3", calculation.moveIntensity());
        decimal(BigDecimal.TEN.multiply(calculation.moveIntensity()).multiply(calculation.styleMultiplier())
                .multiply(new BigDecimal("0.9")).setScale(4, RoundingMode.HALF_UP), calculation.adjustedLoad());
        var old = details(oldSession).entries().getFirst();
        assertEquals("7b", old.gradeLabel());
        assertEquals(index("7b"), storedInt(oldSession, "grade_index"));
        decimal("100", old.calculation().gradePoints());
    }

    @Test
    void approachesTheWindowBoundaryGraduallyWhenThereAreAtLeastTenOtherClimbs() throws Exception {
        create(request(DAY.minusDays(729), 8, List.of(route("7b", "ASCENT", "LEAD"))));
        seedBase(10, DAY.minusDays(1));
        var before = probe(DAY).entries().getFirst().calculation();
        var boundary = probe(DAY.plusDays(1)).entries().getFirst().calculation();
        var after = probe(DAY.plusDays(2)).entries().getFirst().calculation();
        BigDecimal base = BigDecimal.valueOf(index("7a"));
        assertTrue(before.effectiveCurrentLevelIndex().compareTo(base) > 0);
        assertTrue(before.effectiveCurrentLevelIndex().subtract(base).compareTo(new BigDecimal("0.00001")) < 0);
        decimal(base, boundary.effectiveCurrentLevelIndex());
        decimal(base, after.effectiveCurrentLevelIndex());
        assertEquals(11, before.levelReferenceSampleSize());
        assertEquals(10, boundary.levelReferenceSampleSize());
        assertTrue(before.adjustedLoad().subtract(boundary.adjustedLoad()).abs().compareTo(new BigDecimal("0.001")) < 0);
        decimal(boundary.adjustedLoad(), after.adjustedLoad());
    }

    @Test
    void preservesTheMaximumForFewerThanTenDistinctClimbs() throws Exception {
        create(request(DAY.minusDays(700), 8, List.of(route("7b", "ASCENT", "LEAD"))));
        seedBase(8, DAY.minusDays(1));
        var calculation = probe(DAY).entries().getFirst().calculation();
        decimal(BigDecimal.valueOf(index("7b")), calculation.effectiveCurrentLevelIndex());
        assertEquals("LOW_SAMPLE_MAX", calculation.levelReferenceSource());
        assertEquals(9, calculation.levelReferenceSampleSize());
        assertTrue(calculation.levelEstimatorVersion().startsWith("unweighted-low-sample-max-v1"));
    }

    @Test
    void repeatingAClimbRefreshesItsEvidenceWithoutIncreasingTheSampleSizeOrChangingSavedLoads() throws Exception {
        var high = route("7b", "ASCENT", "LEAD");
        create(request(DAY.minusDays(365), 8, List.of(high)));
        seedBase(10, DAY.minusDays(1));
        var first = probe(DAY);
        var refreshed = new LinkedHashMap<>(high);
        refreshed.put("clientEntryId", UUID.randomUUID());
        create(request(DAY.minusDays(1), 9, List.of(refreshed)));
        var second = probe(DAY);
        assertEquals(11, first.entries().getFirst().calculation().levelReferenceSampleSize());
        assertEquals(11, second.entries().getFirst().calculation().levelReferenceSampleSize());
        assertTrue(second.entries().getFirst().calculation().effectiveCurrentLevelIndex()
                .compareTo(first.entries().getFirst().calculation().effectiveCurrentLevelIndex()) > 0);
        var calculated = second.entries().getFirst().calculation();
        decimal(BigDecimal.TEN.multiply(calculated.moveIntensity()).multiply(calculated.styleMultiplier())
                .multiply(calculated.relativeEffortMultiplier()).setScale(4, RoundingMode.HALF_UP), calculated.adjustedLoad());
        assertEquals(first, details(first.sessionId()));
    }

    @Test
    void excludesTopRopeFromEvidenceWhileKeepingItsLoadAndSeparatingTheScales() throws Exception {
        UUID topRope = create(request(DAY.minusDays(2), 8, List.of(route("8a", "ASCENT", "TOP_ROPE"))));
        var training = details(topRope).entries().getFirst().calculation();
        assertTrue(training.adjustedLoad().signum() > 0);
        assertEquals(0, training.levelReferenceSampleSize());
        assertEquals("CURRENT_SESSION_FALLBACK", training.levelReferenceSource());
        decimal(BigDecimal.valueOf(index("8a") - 1), training.effectiveCurrentLevelIndex());
        create(request(DAY.minusDays(1), 8, List.of(route("7a", "ASCENT", "LEAD"), boulder("6B", "ASCENT"))));
        var route = probe(DAY).entries().getFirst().calculation();
        assertEquals(1, route.levelReferenceSampleSize());
        decimal(BigDecimal.valueOf(index("7a")), route.effectiveCurrentLevelIndex());
        UUID boulderSession = create(request(DAY, 9, List.of(boulder("6B", "ATTEMPT"))));
        var boulder = details(boulderSession).entries().getFirst().calculation();
        assertEquals(1, boulder.levelReferenceSampleSize());
        decimal("9.25", boulder.moveIntensity());
        assertNull(details(topRope).maxRouteGrade());
    }

    @Test
    void excludesFutureLaterTodayDeletedForeignAndTrainingOnlyEvidence() throws Exception {
        seedBase(1, DAY.minusDays(1));
        create(request(DAY.plusDays(1), 8, List.of(route("8a", "ASCENT", "LEAD"))));
        create(request(DAY, 12, List.of(route("8a", "ASCENT", "LEAD"))));
        UUID deleted = create(request(DAY.minusDays(1), 9, List.of(route("8a", "ASCENT", "LEAD"))));
        mockMvc.perform(delete("/api/v1/training/sessions/" + deleted).param("expectedVersion", "0")
                        .param("changeReason", "Exclude deleted evidence"))
                .andExpect(status().isNoContent());
        create(request(DAY.minusDays(1), 10, List.of(route("8a", "ATTEMPT", "LEAD"))));
        UUID foreign = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Foreign recency test', now())")
                .param("id", foreign).update();
        when(currentUserProvider.currentUserId()).thenReturn(foreign);
        UUID savedArea = areaId;
        areaId = UUID.randomUUID();
        create(request(DAY.minusDays(1), 8, List.of(route("8a", "ASCENT", "LEAD"))));
        areaId = savedArea;
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        var calculation = probe(DAY).entries().getFirst().calculation();
        assertEquals(1, calculation.levelReferenceSampleSize());
        decimal(BigDecimal.valueOf(index("7a")), calculation.effectiveCurrentLevelIndex());
    }

    @Test
    void editsExcludeTheirOwnOldEntriesAndRetriesKeepFrozenV3AndLegacyV2Snapshots() throws Exception {
        var request = request(DAY, 8, List.of(route("7b", "ASCENT", "LEAD")));
        UUID session = create(request);
        var replacement = route("7a", "ATTEMPT", "LEAD");
        replacement.put("entryOrder", 0);
        request.put("entries", List.of(replacement));
        String body = mockMvc.perform(put("/api/v1/training/sessions/" + session).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("expectedVersion", 0, "changeReason", "Correct grade", "session", request))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var updated = objectMapper.readValue(body, TrainingSessionDetails.class);
        assertEquals(0, updated.entries().getFirst().calculation().levelReferenceSampleSize());
        decimal(BigDecimal.valueOf(index("7a") - 1), updated.entries().getFirst().calculation().effectiveCurrentLevelIndex());
        create(request(DAY.minusDays(1), 8, List.of(route("8a", "ASCENT", "LEAD"))));
        assertEquals(session, create(request));
        assertEquals(updated, details(session));

        jdbcClient.sql("""
                UPDATE training.training_entries SET calculation_model_version = 'v2',
                  effective_current_level_index = NULL, effective_relative_grade_diff = NULL,
                  level_reference_source = NULL, level_reference_algorithm_version = NULL,
                  level_reference_horizon_days = NULL, level_reference_sample_size = NULL,
                  level_reference_history_weight = NULL WHERE session_id = :id
                """).param("id", session).update();
        var legacy = details(session);
        assertEquals("v2", legacy.calculationModelVersion());
        assertNull(legacy.entries().getFirst().calculation().effectiveCurrentLevelIndex());
        assertEquals(session, create(request));
        assertEquals(legacy, details(session));
    }

    private void seedBase(int count, LocalDate day) throws Exception {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) entries.add(route("7a", "ASCENT", "LEAD"));
        create(request(day, 8, entries));
    }

    private TrainingSessionDetails probe(LocalDate day) throws Exception {
        return details(create(request(day, nextProbeHour++, List.of(route("7a", "ATTEMPT", "LEAD")))));
    }

    private Map<String, Object> route(String grade, String result, String protection) {
        var entry = boulder(grade, result);
        entry.put("climbType", "ROUTE");
        entry.put("effortProfileCode", "MEDIUM_CONTINUOUS");
        entry.put("protectionMode", protection);
        return entry;
    }

    private Map<String, Object> boulder(String grade, String result) {
        UUID climb = UUID.randomUUID();
        var entry = new LinkedHashMap<String, Object>();
        entry.put("clientEntryId", UUID.randomUUID());
        entry.put("clientClimbId", climb);
        entry.put("climbName", "Recency climb " + climb);
        entry.put("climbType", "BOULDER");
        entry.put("gradeLabel", grade);
        entry.put("effortProfileCode", "BOULDER");
        entry.put("resultType", result);
        entry.put("ascentMode", "RP");
        entry.put("totalMoves", 20);
        entry.put("executedMoves", "ASCENT".equals(result) ? 20 : 10);
        entry.put("historyConflictConfirmed", false);
        return entry;
    }

    private Map<String, Object> request(LocalDate day, int hour, List<Map<String, Object>> entries) {
        for (int i = 0; i < entries.size(); i++) entries.get(i).put("entryOrder", i);
        return new LinkedHashMap<>(Map.of(
                "clientSessionId", UUID.randomUUID(), "sessionName", "Recency load test",
                "sessionDate", day.toString(), "timeZoneId", "UTC",
                "startedAt", day.atTime(hour, 0).toString() + ":00Z",
                "endedAt", day.atTime(hour + 1, 0).toString() + ":00Z",
                "clientAreaId", areaId, "areaName", "Recency test area " + areaId, "entries", entries));
    }

    private UUID create(Map<String, Object> request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("sessionId").asText());
    }

    private TrainingSessionDetails details(UUID session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/training/sessions/" + session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TrainingSessionDetails.class);
    }

    private int index(String grade) {
        return jdbcClient.sql("SELECT grade_index FROM catalog.grades WHERE scale_code = 'ROUTE_FRENCH' AND label = :label")
                .param("label", grade).query(Integer.class).single();
    }

    private int storedInt(UUID session, String column) {
        return jdbcClient.sql("SELECT " + column + " FROM training.training_entries WHERE session_id = :id")
                .param("id", session).query(Integer.class).single();
    }

    private void decimal(String expected, BigDecimal actual) { decimal(new BigDecimal(expected), actual); }
    private void decimal(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "Expected " + expected + ", got " + actual);
    }
}
