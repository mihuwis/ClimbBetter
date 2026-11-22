package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AchievementPyramidIntegrationTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate AS_OF = LocalDate.of(2005, 11, 3);
    private static final String SUMMARY = "/api/v1/statistics/pyramid";
    private static final String CLIMBS = SUMMARY + "/climbs";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;

    private final UUID clientAreaId = UUID.randomUUID();
    private final Map<UUID, Map<String, Object>> commands = new LinkedHashMap<>();

    @Test
    void emptyAchievementHistoryHasNoInventedLevelOrClimbs() throws Exception {
        JsonNode result = report("BOULDER", "LAST_TWO_YEARS", "RP");
        assertAll(
                () -> assertEquals(0, result.get("uniqueClimbCount").asInt()),
                () -> assertEquals(0, result.get("layers").size()),
                () -> assertTrue(absent(result, "peak")),
                () -> assertEquals("NO_ASCENTS", result.get("levelAssessment").get("method").asText()),
                () -> assertEquals("UNASSESSED", result.get("levelAssessment").get("confidence").asText()),
                () -> assertTrue(absent(result.get("levelAssessment"), "gradeLabel")),
                () -> assertEquals("2003-11-03", result.get("windowStart").asText()),
                () -> assertEquals(AS_OF.toString(), result.get("windowEnd").asText())
        );
    }

    @Test
    void allTwelveFilterCombinationsApplyWindowStyleDisciplineAndUniqueClimbRules() throws Exception {
        Map<String, Object> oldOs = entry("ROUTE", "7a", "OS");
        createSession(AS_OF.minusYears(5), List.of(oldOs));
        List<Map<String, Object>> recent = new ArrayList<>();
        recent.add(repeat(oldOs, "RP", "7a"));
        recent.add(entry("ROUTE", "7a", "FLASH"));
        recent.add(entry("ROUTE", "7b", "OS"));
        recent.add(entry("BOULDER", "6A+", "OS"));
        recent.add(entry("BOULDER", "6B", "FLASH"));
        recent.add(entry("CIRCUIT", "7a", "RP"));
        for (String style : List.of("OS", "FLASH", "RP")) {
            Map<String, Object> topRope = entry("ROUTE", "7c", style);
            topRope.put("protectionMode", "TOP_ROPE");
            recent.add(topRope);
        }
        Map<String, Object> attempt = entry("ROUTE", "8a", "RP");
        attempt.put("resultType", "ATTEMPT");
        recent.add(attempt);
        recent.add(new LinkedHashMap<>(Map.of("clientEntryId", UUID.randomUUID(), "resultType", "WARMUP", "executedMoves", 40)));
        createSession(AS_OF.minusDays(1), recent);
        String storedBefore = storedState();

        for (String period : List.of("ALL_TIME", "LAST_TWO_YEARS")) {
            for (String style : List.of("RP", "OS")) {
                for (String view : List.of("BOULDER", "ROUTE_AND_CIRCUIT", "COMBINED")) {
                    int expected = switch (view) {
                        case "BOULDER" -> style.equals("RP") ? 2 : 1;
                        case "ROUTE_AND_CIRCUIT" -> style.equals("RP") ? 4 : period.equals("ALL_TIME") ? 2 : 1;
                        default -> style.equals("RP") ? 6 : period.equals("ALL_TIME") ? 3 : 2;
                    };
                    JsonNode result = report(view, period, style);
                    String context = view + "/" + period + "/" + style;
                    assertEquals(expected, result.get("uniqueClimbCount").asInt(), context);
                    assertEquals(expected, layerCount(result), context + " layer total");
                    assertEquals(view, result.get("filters").get("view").asText());
                    assertEquals(period, result.get("filters").get("period").asText());
                    assertEquals(style, result.get("filters").get("style").asText());
                    if (view.equals("COMBINED")) {
                        assertTrue(absent(result, "levelAssessment"));
                        assertEquals("COMBINED_SCALE", result.get("assessmentUnavailableReason").asText());
                    }
                }
            }
        }
        assertEquals(storedBefore, storedState(), "Reading all reports must preserve stored metrics, fingerprints and revisions");
    }

    @Test
    void repeatedOldRouteCountsOncePerWindowAndUsesFirstQualifyingSnapshot() throws Exception {
        Map<String, Object> original = entry("ROUTE", "6c", "OS");
        UUID oldSession = createSession(AS_OF.minusYears(5), List.of(original));
        List<Map<String, Object>> repeats = List.of(repeat(original, "RP", "7a"),
                repeat(original, "RP", "7a"), repeat(original, "RP", "7a"));
        UUID recentSession = createSession(AS_OF.minusDays(1), repeats);

        JsonNode allTime = report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP");
        JsonNode recent = report("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "RP");
        assertEquals(1, allTime.get("uniqueClimbCount").asInt());
        assertEquals(1, recent.get("uniqueClimbCount").asInt());
        assertEquals("6c", allTime.get("peak").get("gradeLabel").asText());
        assertEquals("7a", recent.get("peak").get("gradeLabel").asText());
        assertEquals(1, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "OS").get("uniqueClimbCount").asInt());
        assertEquals(0, report("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "OS").get("uniqueClimbCount").asInt());

        JsonNode allItem = page("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP", "grade:ROUTE_FRENCH:13", 0, 50).get("items").get(0);
        JsonNode recentItem = page("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "RP", "grade:ROUTE_FRENCH:15", 0, 50).get("items").get(0);
        assertEquals(oldSession.toString(), allItem.get("sessionId").asText());
        assertEquals(recentSession.toString(), recentItem.get("sessionId").asText());
        assertEquals("OS", allItem.get("ascentMode").asText());
        assertEquals("RP", recentItem.get("ascentMode").asText());
        assertEquals(allItem.get("climbId").asText(), recentItem.get("climbId").asText());
    }

    @Test
    void inclusiveLeapYearWindowExcludesOtherDeletedAndFutureSessions() throws Exception {
        LocalDate leapAsOf = LocalDate.of(2004, 2, 29);
        createSession(LocalDate.of(2002, 2, 27), List.of(entry("ROUTE", "6c", "RP")));
        Map<String, Object> boundary = entry("ROUTE", "7a", "RP");
        createSession(LocalDate.of(2002, 2, 28), List.of(boundary));
        createSession(leapAsOf, List.of(entry("ROUTE", "7a+", "RP")));
        createSession(leapAsOf.plusDays(1), List.of(entry("ROUTE", "7c", "RP")));
        UUID deleted = createSession(LocalDate.of(2003, 1, 1), List.of(entry("ROUTE", "7c", "RP")));
        UUID foreign = createSession(LocalDate.of(2003, 1, 2), List.of(entry("ROUTE", "7c", "RP")));
        UUID otherUser = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO identity.users (id,display_name,created_at) VALUES (:id,'Other pyramid user',now())")
                .param("id", otherUser).update();
        jdbcClient.sql("UPDATE training.training_sessions SET user_id = :userId WHERE id = :id")
                .param("userId", otherUser).param("id", foreign).update();
        jdbcClient.sql("UPDATE training.training_sessions SET deleted_at = now() WHERE id = :id")
                .param("id", deleted).update();
        jdbcClient.sql("UPDATE catalog.climbs SET is_archived = true WHERE client_climb_id = :id")
                .param("id", boundary.get("clientClimbId")).update();

        JsonNode result = report("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "RP", leapAsOf);
        assertEquals("2002-02-28", result.get("windowStart").asText());
        assertEquals("2004-02-29", result.get("windowEnd").asText());
        assertEquals(2, result.get("uniqueClimbCount").asInt());
        assertEquals("7a+", result.get("peak").get("gradeLabel").asText());
        assertEquals(3, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP", leapAsOf).get("uniqueClimbCount").asInt());
    }

    @Test
    void entryOrderAndArchivedCatalogCannotReplaceTheFirstSavedGrade() throws Exception {
        Map<String, Object> first = entry("ROUTE", "7a", "RP");
        createSession(AS_OF, List.of(first, repeat(first, "RP", "7b")));
        jdbcClient.sql("""
                        UPDATE catalog.climbs SET is_archived = true,
                            grade_id = (SELECT id FROM catalog.grades WHERE scale_code='ROUTE_FRENCH' AND label='7c')
                        WHERE client_climb_id=:id
                        """).param("id", first.get("clientClimbId")).update();

        JsonNode result = report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP");
        assertEquals(1, result.get("uniqueClimbCount").asInt());
        assertEquals("7a", result.get("peak").get("gradeLabel").asText());
        JsonNode item = page("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP", "grade:ROUTE_FRENCH:15", 0, 50).get("items").get(0);
        assertEquals(first.get("clientEntryId").toString(), clientEntryId(item.get("entryId").asText()));
        assertEquals("7a", item.get("gradeLabel").asText());
    }

    @Test
    void combinedLayersUseExactPointsAndSeparateDifferentCalibrationVersions() throws Exception {
        Map<String, Object> circuit = entry("CIRCUIT", "7a", "RP");
        createSession(AS_OF, List.of(entry("ROUTE", "7a", "RP"), entry("BOULDER", "6A+", "RP"),
                entry("BOULDER", "6B", "RP"), circuit));
        JsonNode first = report("COMBINED", "ALL_TIME", "RP");
        assertEquals(4, first.get("uniqueClimbCount").asInt());
        assertEquals(3, bucket(first, "points:1:90").get("climbCount").asInt());
        assertEquals(1, bucket(first, "points:1:92.5").get("climbCount").asInt());
        assertEquals(92.5, first.get("peak").get("gradePoints").asDouble());

        jdbcClient.sql("UPDATE training.training_entries SET grade_calibration_version=2 WHERE client_entry_id=:id")
                .param("id", circuit.get("clientEntryId")).update();
        JsonNode mixed = report("COMBINED", "ALL_TIME", "RP");
        assertEquals(2, mixed.get("gradeCalibrationVersions").size());
        assertEquals(2, bucket(mixed, "points:1:90").get("climbCount").asInt());
        assertEquals(1, bucket(mixed, "points:2:90").get("climbCount").asInt());
        assertTrue(absent(mixed, "peak"));
        assertEquals("MIXED_POINT_CALIBRATIONS", mixed.get("peakUnavailableReason").asText());
        assertTrue(absent(mixed, "levelAssessment"));
        assertEquals(1, page("COMBINED", "ALL_TIME", "RP", "points:2:90", 0, 50).get("totalCount").asInt());
    }

    @ParameterizedTest
    @CsvSource({"1,6c+,3.0,11", "2,7b,3.25,12"})
    void calculatesBothAgreedPyramidExamples(int example, String expectedGrade, double expectedSupport, int expectedCount) throws Exception {
        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Integer> counts = example == 1
                ? Map.of("7a", 1, "6c", 3, "6b+", 2, "6b", 5)
                : Map.of("6c", 5, "7a", 3, "7a+", 1, "7b", 2, "7b+", 1);
        counts.forEach((grade, count) -> {
            for (int i = 0; i < count; i++) entries.add(entry("ROUTE", grade, "RP"));
        });
        createSession(AS_OF, entries);
        JsonNode result = report("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "RP");
        JsonNode estimate = result.get("levelAssessment");
        assertAll(
                () -> assertEquals(expectedCount, result.get("uniqueClimbCount").asInt()),
                () -> assertEquals(expectedGrade, estimate.get("gradeLabel").asText()),
                () -> assertEquals(expectedSupport, estimate.get("supportScore").asDouble()),
                () -> assertEquals("PYRAMID_SUPPORT", estimate.get("method").asText()),
                () -> assertEquals("MEDIUM", estimate.get("confidence").asText()),
                () -> assertEquals(expectedCount, estimate.get("sampleSize").asInt()),
                () -> assertEquals(example == 1 ? "7a" : "7b+", result.get("peak").get("gradeLabel").asText())
        );
        if (example == 1) assertEquals(0, bucket(result, "grade:ROUTE_FRENCH:14").get("climbCount").asInt(), "Unclimbed dictionary grades remain visible between attained grades");
    }

    @Test
    void smallSampleDoesNotBecomeMatureByCountingRepeatsAttemptsOrTopRope() throws Exception {
        Map<String, Object> ascent = entry("ROUTE", "7a", "RP");
        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(ascent);
        for (int i = 0; i < 10; i++) {
            entries.add(repeat(ascent, "RP", "7a"));
            Map<String, Object> training = entry("ROUTE", "8a", "RP");
            training.put("protectionMode", "TOP_ROPE");
            entries.add(training);
            Map<String, Object> attempt = entry("ROUTE", "8b", "RP");
            attempt.put("resultType", "ATTEMPT");
            entries.add(attempt);
        }
        createSession(AS_OF, entries);
        JsonNode estimate = report("ROUTE_AND_CIRCUIT", "LAST_TWO_YEARS", "RP").get("levelAssessment");
        assertEquals(1, estimate.get("sampleSize").asInt());
        assertEquals("LOW_SAMPLE_MAX", estimate.get("method").asText());
        assertEquals("LOW", estimate.get("confidence").asText());
        assertEquals("7a", estimate.get("gradeLabel").asText());
    }

    @Test
    void pagesDistinctClimbsWithEqualNamesAndMatchingLayerCount() throws Exception {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> item = entry("ROUTE", "7a", "RP");
            item.put("climbName", "Same name");
            entries.add(item);
        }
        entries.add(repeat(entries.getFirst(), "RP", "7a"));
        createSession(AS_OF, entries);
        JsonNode summary = report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP");
        String key = "grade:ROUTE_FRENCH:15";
        assertEquals(4, bucket(summary, key).get("climbCount").asInt());
        JsonNode first = page("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP", key, 0, 2);
        JsonNode second = page("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP", key, 2, 2);
        assertEquals(4, first.get("totalCount").asInt());
        assertEquals(2, first.get("items").size());
        assertEquals(2, first.get("nextOffset").asInt());
        assertEquals(2, second.get("items").size());
        assertTrue(absent(second, "nextOffset"));
        Set<String> ids = new HashSet<>();
        for (JsonNode response : List.of(first, second)) {
            for (int i = 0; i < response.get("items").size(); i++) {
                JsonNode item = response.get("items").get(i);
                ids.add(item.get("climbId").asText());
                assertEquals("Same name", item.get("climbName").asText());
                assertEquals(AS_OF.toString(), item.get("achievedOn").asText());
            }
        }
        assertEquals(4, ids.size());
    }

    @Test
    void reportUsesFullHistoryBeyondDashboardLimit() throws Exception {
        for (int i = 0; i < 22; i++) createSession(AS_OF.minusDays(22 - i), List.of(entry("ROUTE", "7a", "RP")));
        assertEquals(22, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP").get("uniqueClimbCount").asInt());
        mockMvc.perform(get("/api/v1/dashboard/sessions")).andExpect(status().isOk());
    }

    @Test
    void reflectsSessionEditsAndSoftDeletionWithoutAStalePyramidCache() throws Exception {
        Map<String, Object> ascent = entry("ROUTE", "7a", "RP");
        UUID session = createSession(AS_OF, List.of(ascent));
        assertEquals(1, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP").get("uniqueClimbCount").asInt());
        Map<String, Object> attempted = new LinkedHashMap<>(ascent);
        attempted.put("resultType", "ATTEMPT");
        Map<String, Object> edited = new LinkedHashMap<>(commands.get(session));
        edited.put("entries", ordered(List.of(attempted)));
        mockMvc.perform(put("/api/v1/training/sessions/" + session).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("expectedVersion", 0,
                                "changeReason", "Correct ascent to a training attempt", "session", edited))))
                .andExpect(status().isOk());
        assertEquals(0, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP").get("uniqueClimbCount").asInt());
        UUID another = createSession(AS_OF, List.of(entry("ROUTE", "7a", "RP")));
        assertEquals(1, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP").get("uniqueClimbCount").asInt());
        mockMvc.perform(delete("/api/v1/training/sessions/" + another).param("expectedVersion", "0"))
                .andExpect(status().isNoContent());
        assertEquals(0, report("ROUTE_AND_CIRCUIT", "ALL_TIME", "RP").get("uniqueClimbCount").asInt());
        assertEquals(2L, jdbcClient.sql("SELECT COUNT(*) FROM training.training_entries WHERE session_id IN (:first,:second)")
                .param("first", session).param("second", another).query(Long.class).single());
    }

    @Test
    void rejectsInvalidFiltersDatesBucketKeysAndPagination() throws Exception {
        mockMvc.perform(get(SUMMARY).param("view", "UNKNOWN")).andExpect(status().isBadRequest());
        mockMvc.perform(get(SUMMARY).param("period", "LAST_WEEK")).andExpect(status().isBadRequest());
        mockMvc.perform(get(SUMMARY).param("style", "FLASH")).andExpect(status().isBadRequest());
        mockMvc.perform(get(SUMMARY).param("asOf", "2005-02-30")).andExpect(status().isBadRequest());
        mockMvc.perform(get(CLIMBS).param("bucketKey", "invalid")).andExpect(status().isBadRequest());
        mockMvc.perform(get(CLIMBS).param("bucketKey", "grade:BOULDER_FONT:7").param("offset", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(CLIMBS).param("bucketKey", "grade:BOULDER_FONT:7").param("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode report(String view, String period, String style) throws Exception {
        return report(view, period, style, AS_OF);
    }

    private JsonNode report(String view, String period, String style, LocalDate asOf) throws Exception {
        String response = mockMvc.perform(get(SUMMARY).param("view", view).param("period", period)
                        .param("style", style).param("asOf", asOf.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode page(String view, String period, String style, String bucket, int offset, int limit) throws Exception {
        String response = mockMvc.perform(get(CLIMBS).param("view", view).param("period", period)
                        .param("style", style).param("asOf", AS_OF.toString()).param("bucketKey", bucket)
                        .param("offset", Integer.toString(offset)).param("limit", Integer.toString(limit)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private UUID createSession(LocalDate date, List<Map<String, Object>> entries) throws Exception {
        int hour = 8 + (int) commands.values().stream()
                .filter(existing -> date.toString().equals(existing.get("sessionDate"))).count();
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("clientSessionId", UUID.randomUUID());
        command.put("sessionName", "Pyramid fixture " + UUID.randomUUID());
        command.put("sessionDate", date.toString());
        command.put("timeZoneId", "Europe/Warsaw");
        command.put("startedAt", date.atTime(hour, 0).toInstant(ZoneOffset.UTC).toString());
        command.put("endedAt", date.atTime(hour + 1, 0).toInstant(ZoneOffset.UTC).toString());
        command.put("clientAreaId", clientAreaId);
        command.put("areaName", "Pyramid area " + clientAreaId);
        command.put("entries", ordered(entries));
        String response = mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(response).get("sessionId").asText());
        commands.put(id, command);
        return id;
    }

    private List<Map<String, Object>> ordered(List<Map<String, Object>> entries) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Map<String, Object> entry = new LinkedHashMap<>(entries.get(i));
            entry.put("entryOrder", i);
            result.add(entry);
        }
        return result;
    }

    private Map<String, Object> entry(String type, String grade, String style) {
        UUID climb = UUID.randomUUID();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientEntryId", UUID.randomUUID());
        result.put("resultType", "ASCENT");
        result.put("clientClimbId", climb);
        result.put("climbName", "Pyramid climb " + climb);
        result.put("climbType", type);
        result.put("gradeLabel", grade);
        result.put("effortProfileCode", type.equals("BOULDER") ? "BOULDER" : type.equals("CIRCUIT") ? "CIRCUIT" : "MEDIUM_CONTINUOUS");
        result.put("ascentMode", style);
        result.put("totalMoves", type.equals("BOULDER") ? 8 : 20);
        result.put("executedMoves", type.equals("BOULDER") ? 8 : 20);
        result.put("historyConflictConfirmed", false);
        if (type.equals("ROUTE")) result.put("protectionMode", "LEAD");
        return result;
    }

    private Map<String, Object> repeat(Map<String, Object> original, String style, String grade) {
        Map<String, Object> result = new LinkedHashMap<>(original);
        result.put("clientEntryId", UUID.randomUUID());
        result.put("ascentMode", style);
        result.put("gradeLabel", grade);
        return result;
    }

    private JsonNode bucket(JsonNode response, String key) {
        JsonNode layers = response.get("layers");
        for (int i = 0; i < layers.size(); i++) {
            JsonNode layer = layers.get(i);
            if (layer.get("bucketKey").asText().equals(key)) return layer;
        }
        throw new AssertionError("Missing bucket " + key + " in " + response);
    }

    private int layerCount(JsonNode response) {
        int result = 0;
        JsonNode layers = response.get("layers");
        for (int i = 0; i < layers.size(); i++) result += layers.get(i).get("climbCount").asInt();
        return result;
    }

    private boolean absent(JsonNode node, String name) {
        return node.get(name) == null || node.get(name).isNull();
    }

    private String clientEntryId(String id) {
        return jdbcClient.sql("SELECT client_entry_id::text FROM training.training_entries WHERE id=:id")
                .param("id", UUID.fromString(id)).query(String.class).single();
    }

    private String storedState() {
        return jdbcClient.sql("""
                        SELECT md5(
                            COALESCE((SELECT string_agg(to_jsonb(s)::text, '' ORDER BY s.id)
                                FROM training.training_sessions s WHERE user_id=:userId), '') ||
                            COALESCE((SELECT string_agg(to_jsonb(e)::text, '' ORDER BY e.id)
                                FROM training.training_entries e JOIN training.training_sessions s ON s.id=e.session_id
                                WHERE s.user_id=:userId), '') ||
                            COALESCE((SELECT string_agg(to_jsonb(r)::text, '' ORDER BY r.id)
                                FROM training.training_session_revisions r JOIN training.training_sessions s ON s.id=r.session_id
                                WHERE s.user_id=:userId), ''))
                        """).param("userId", USER_ID).query(String.class).single();
    }
}
