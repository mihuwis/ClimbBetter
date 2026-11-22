package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrainingModelV2IntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;

    @Test
    void storesV3ReferencesWithV2IntensitiesSeparateDisciplineLevelsAndZeroWarmup() throws Exception {
        UUID boulderEntry = UUID.randomUUID();
        UUID routeEntry = UUID.randomUUID();
        UUID circuitEntry = UUID.randomUUID();
        UUID warmupEntry = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                Map.of("clientEntryId", warmupEntry, "entryOrder", 0,
                        "resultType", "WARMUP", "executedMoves", 20),
                entry(boulderEntry, 1, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4),
                entry(routeEntry, 2, "ROUTE", "7a", "MEDIUM_CONTINUOUS", "ASCENT", 20, 20),
                entry(circuitEntry, 3, "CIRCUIT", "7a", "CIRCUIT", "ASCENT", 20, 20)
        ));

        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.totalMoves").value(64))
                .andExpect(jsonPath("$.adjustedLoad").value(375.7))
                .andExpect(jsonPath("$.averageMoveIntensity").value(6.5681818182))
                .andExpect(jsonPath("$.entries[0].calculation.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.effectiveCurrentLevelIndex").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.adjustedLoad").value(0))
                .andExpect(jsonPath("$.entries[1].calculation.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.entries[1].calculation.profileIntensityIndex").value(10))
                .andExpect(jsonPath("$.entries[1].calculation.edlCount").doesNotExist())
                .andExpect(jsonPath("$.entries[1].calculation.crux").doesNotExist())
                .andExpect(jsonPath("$.entries[1].calculation.moveIntensity").value(9.25))
                .andExpect(jsonPath("$.entries[1].calculation.currentLevelGradeLabel").value("6B"))
                .andExpect(jsonPath("$.entries[1].calculation.relativeGradeDifference").value(0))
                .andExpect(jsonPath("$.entries[1].calculation.styleMultiplier").value(1.3))
                .andExpect(jsonPath("$.entries[1].calculation.adjustedLoad").value(48.1))
                .andExpect(jsonPath("$.entries[2].calculation.profileIntensityIndex").value(7))
                .andExpect(jsonPath("$.entries[2].calculation.currentLevelGradeLabel").value("7a"))
                .andExpect(jsonPath("$.entries[2].calculation.relativeGradeDifference").value(0))
                .andExpect(jsonPath("$.entries[2].calculation.moveIntensity").value(6.3))
                .andExpect(jsonPath("$.entries[2].calculation.adjustedLoad").value(163.8))
                .andExpect(jsonPath("$.entries[3].calculation.profileIntensityIndex").value(7))
                .andExpect(jsonPath("$.entries[3].calculation.adjustedLoad").value(163.8));

        assertAll(
                () -> assertStoredDecimal(boulderEntry, "profile_intensity_index", "10.0000"),
                () -> assertStoredDecimal(boulderEntry, "move_intensity", "9.2500000000"),
                () -> assertStoredDecimal(boulderEntry, "adjusted_load", "48.1000"),
                () -> assertStoredDecimal(routeEntry, "adjusted_load", "163.8000"),
                () -> assertStoredDecimal(circuitEntry, "profile_intensity_index", "7.0000"),
                () -> assertStoredDecimal(warmupEntry, "adjusted_load", "0.0000")
        );
        assertEquals(4L, jdbcClient.sql("""
                        SELECT COUNT(*) FROM training.training_entries
                        WHERE session_id = :id AND calculation_model_version = 'v3'
                          AND base_edl IS NULL AND edl_count IS NULL
                        """).param("id", sessionId).query(Long.class).single());
    }

    @Test
    void persistsRoundedCruxSplitForAscentAndPartialAttemptAndReadsSnapshots() throws Exception {
        UUID ascent = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                entry(ascent, 0, "ROUTE", "7a", "MEDIUM_CRUX", "ASCENT", 30, 30),
                entry(attempt, 1, "ROUTE", "7a", "LONG_CRUX", "ATTEMPT", 50, 40)
        ));

        StoredCrux ascentSnapshot = readCrux(ascent);
        StoredCrux attemptSnapshot = readCrux(attempt);
        assertCruxSnapshot(ascentSnapshot, 5, 25, "7.0000");
        assertCruxSnapshot(attemptSnapshot, 6, 34, "5.0000");
        assertTrue(ascentSnapshot.hardIntensity().compareTo(ascentSnapshot.easyIntensity()) > 0);
        assertTrue(attemptSnapshot.hardIntensity().compareTo(attemptSnapshot.easyIntensity()) > 0);

        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].calculation.crux.hardMoveCount").value(5))
                .andExpect(jsonPath("$.entries[0].calculation.crux.easyMoveCount").value(25))
                .andExpect(jsonPath("$.entries[0].calculation.crux.baseMoveGradeLabel").value("6B"))
                .andExpect(jsonPath("$.entries[0].calculation.crux.hardMoveGradeLabel").value("6C"))
                .andExpect(jsonPath("$.entries[0].calculation.crux.easyMoveGradeLabel").value("6A"))
                .andExpect(jsonPath("$.entries[1].resultType").value("ATTEMPT"))
                .andExpect(jsonPath("$.entries[1].totalMoves").value(50))
                .andExpect(jsonPath("$.entries[1].executedMoves").value(40))
                .andExpect(jsonPath("$.entries[1].calculation.crux.hardMoveCount").value(6))
                .andExpect(jsonPath("$.entries[1].calculation.crux.easyMoveCount").value(34));

        // Reading a saved entry must not resolve its calculation from today's profile values.
        jdbcClient.sql("""
                        UPDATE training.effort_profiles SET profile_intensity_index = 99
                        WHERE code IN ('MEDIUM_CRUX', 'LONG_CRUX')
                        """).update();
        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").value(7))
                .andExpect(jsonPath("$.entries[1].calculation.profileIntensityIndex").value(5));
        assertEquals(ascentSnapshot, readCrux(ascent));
        assertEquals(attemptSnapshot, readCrux(attempt));
    }

    @Test
    void readsV2SnapshotAndRetriesOriginalRequestWithoutRecalculatingIt() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        String request = sessionRequest(clientSessionId, List.of(
                entry(entryId, 0, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4)
        ));
        UUID sessionId = createSession(request);
        convertSessionFixtureToV2(sessionId);

        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationModelVersion").value("v2"))
                .andExpect(jsonPath("$.entries[0].calculation.calculationModelVersion").value("v2"))
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").value(10))
                .andExpect(jsonPath("$.entries[0].calculation.effectiveCurrentLevelIndex").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.adjustedLoad").value(48.1));
        mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationModelVersion").value("v2"))
                .andExpect(jsonPath("$.entries[0].calculation.effectiveCurrentLevelIndex").doesNotExist());
        assertStoredDecimal(entryId, "adjusted_load", "48.1000");
    }

    @Test
    void readsLegacySnapshotAndRetriesOriginalRequestWithoutRecalculatingIt() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        String request = sessionRequest(clientSessionId, List.of(
                entry(entryId, 0, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4)
        ));
        UUID sessionId = createSession(request);

        convertBoulderFixtureToLegacy(sessionId, entryId);

        mockMvc.perform(get(detailsUrl(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].calculation.calculationModelVersion").value("v1"))
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.edlCount").value(4.8))
                .andExpect(jsonPath("$.entries[0].calculation.adjustedLoad").value(100.2083));
        mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
        assertStoredDecimal(entryId, "adjusted_load", "100.2083");
        assertTrue(jdbcClient.sql("""
                        SELECT profile_intensity_index IS NULL FROM training.training_entries
                        WHERE client_entry_id = :id
                        """).param("id", entryId).query(Boolean.class).single());
    }

    @Test
    void editingLegacySessionConvertsToV3AndAuditsBothCalculationModels() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        String request = sessionRequest(clientSessionId, List.of(
                entry(entryId, 0, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4)
        ));
        UUID sessionId = createSession(request);
        convertBoulderFixtureToLegacy(sessionId, entryId);

        var edited = (tools.jackson.databind.node.ObjectNode) objectMapper.readTree(request).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) edited.get("entries").get(0)).put("executedMoves", 6);
        ((tools.jackson.databind.node.ObjectNode) edited.get("entries").get(0)).put("totalMoves", 6);
        mockMvc.perform(put(detailsUrl(sessionId)).contentType("application/json")
                        .content(updateRequest(sessionId, edited)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.totalMoves").value(6))
                .andExpect(jsonPath("$.entries[0].calculation.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").value(10))
                .andExpect(jsonPath("$.entries[0].calculation.gradePoints").value(92.5))
                .andExpect(jsonPath("$.entries[0].calculation.baseEdl").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.edlCount").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.moveIntensity").value(9.25))
                .andExpect(jsonPath("$.entries[0].calculation.adjustedLoad").value(72.15));

        assertStoredDecimal(entryId, "adjusted_load", "72.1500");
        assertEquals("v1", jdbcClient.sql("""
                        SELECT before_state ->> 'calculationModelVersion'
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(String.class).single());
        assertEquals("v3", jdbcClient.sql("""
                        SELECT after_state ->> 'calculationModelVersion'
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(String.class).single());
        assertEquals(0, new BigDecimal("4").compareTo(jdbcClient.sql("""
                        SELECT (before_state -> 'entries' -> 0 -> 'calculation' ->> 'baseEdl')::numeric
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(BigDecimal.class).single()));
        assertEquals(0, new BigDecimal("92.5").compareTo(jdbcClient.sql("""
                        SELECT (after_state -> 'entries' -> 0 -> 'calculation' ->> 'gradePoints')::numeric
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(BigDecimal.class).single()));
        assertEquals(0, new BigDecimal("10").compareTo(jdbcClient.sql("""
                        SELECT (after_state -> 'entries' -> 0 -> 'calculation'
                                ->> 'profileIntensityIndex')::numeric
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(BigDecimal.class).single()));
        assertTrue(jdbcClient.sql("""
                        SELECT after_state -> 'entries' -> 0 -> 'calculation'
                               ->> 'edlCount' IS NULL
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(Boolean.class).single());
    }

    @Test
    void editingV2AttemptRecalculatesV3CruxCountsAndAuditsBothSnapshots() throws Exception {
        UUID entryId = UUID.randomUUID();
        String request = sessionRequest(UUID.randomUUID(), List.of(
                entry(entryId, 0, "ROUTE", "7a", "LONG_CRUX", "ATTEMPT", 50, 40)
        ));
        UUID sessionId = createSession(request);
        convertSessionFixtureToV2(sessionId);
        assertCruxSnapshot(readCrux(entryId), 6, 34, "5.0000");

        var edited = (tools.jackson.databind.node.ObjectNode) objectMapper.readTree(request).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) edited.get("entries").get(0)).put("executedMoves", 41);
        mockMvc.perform(put(detailsUrl(sessionId)).contentType("application/json")
                        .content(updateRequest(sessionId, edited)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.entries[0].calculation.calculationModelVersion").value("v3"))
                .andExpect(jsonPath("$.entries[0].calculation.profileIntensityIndex").value(5))
                .andExpect(jsonPath("$.entries[0].calculation.edlCount").doesNotExist())
                .andExpect(jsonPath("$.entries[0].calculation.crux.hardMoveCount").value(7))
                .andExpect(jsonPath("$.entries[0].calculation.crux.easyMoveCount").value(34));
        assertCruxSnapshot(readCrux(entryId), 7, 34, "5.0000");
        assertEquals("v2", jdbcClient.sql("""
                        SELECT before_state ->> 'calculationModelVersion'
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(String.class).single());
        assertEquals("v3", jdbcClient.sql("""
                        SELECT after_state ->> 'calculationModelVersion'
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(String.class).single());
        assertEquals(6, jdbcClient.sql("""
                        SELECT (before_state -> 'entries' -> 0 -> 'calculation' -> 'crux'
                                ->> 'hardMoveCount')::integer
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(Integer.class).single());
        assertEquals(7, jdbcClient.sql("""
                        SELECT (after_state -> 'entries' -> 0 -> 'calculation' -> 'crux'
                                ->> 'hardMoveCount')::integer
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(Integer.class).single());
        assertTrue(jdbcClient.sql("""
                        SELECT after_state -> 'entries' -> 0 -> 'calculation' -> 'crux'
                               ->> 'hardMoveIntensity' IS NOT NULL
                        FROM training.training_session_revisions WHERE session_id = :id
                        """).param("id", sessionId).query(Boolean.class).single());
    }

    @Test
    void databaseRejectsAssessedV2EntryWithoutProfileIndex() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                entry(entryId, 0, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4)));
        convertSessionFixtureToV2(sessionId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
                        UPDATE training.training_entries SET profile_intensity_index = NULL
                        WHERE client_entry_id = :id
                        """).param("id", entryId).update());
    }

    @Test
    void databaseRejectsIncompleteCruxSnapshot() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                entry(entryId, 0, "ROUTE", "7a", "MEDIUM_CRUX", "ASCENT", 30, 30)));
        convertSessionFixtureToV2(sessionId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
                        UPDATE training.training_entries SET hard_move_count = NULL
                        WHERE client_entry_id = :id
                        """).param("id", entryId).update());
    }

    @Test
    void databaseRejectsCruxCountsThatDoNotUseRoundedExecutedMoves() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                entry(entryId, 0, "ROUTE", "7a", "LONG_CRUX", "ATTEMPT", 50, 40)));
        convertSessionFixtureToV2(sessionId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
                        UPDATE training.training_entries SET hard_move_count = 8, easy_move_count = 32
                        WHERE client_entry_id = :id
                        """).param("id", entryId).update());
    }

    @Test
    void databaseRejectsLegacyEdlFieldsOnV2Entry() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                entry(entryId, 0, "BOULDER", "6B", "BOULDER", "ASCENT", 4, 4)));
        convertSessionFixtureToV2(sessionId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
                        UPDATE training.training_entries SET base_edl = 4, edl_count = 4.8
                        WHERE client_entry_id = :id
                        """).param("id", entryId).update());
    }

    private UUID createSession(List<Map<String, Object>> entries) throws Exception {
        return createSession(sessionRequest(UUID.randomUUID(), entries));
    }

    private UUID createSession(String request) throws Exception {
        String response = mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("sessionId").asText());
    }

    private String sessionRequest(UUID clientSessionId, List<Map<String, Object>> entries) {
        UUID areaId = UUID.randomUUID();
        return objectMapper.writeValueAsString(Map.of(
                "clientSessionId", clientSessionId,
                "sessionName", "Model v2 " + clientSessionId,
                "sessionDate", "2002-02-03",
                "timeZoneId", "Europe/Warsaw",
                "startedAt", "2002-02-03T08:00:00Z",
                "endedAt", "2002-02-03T09:00:00Z",
                "clientAreaId", areaId,
                "areaName", "Model v2 area " + areaId,
                "entries", entries
        ));
    }

    private Map<String, Object> entry(UUID entryId, int order, String climbType,
                                      String grade, String profile, String result,
                                      int totalMoves, int executedMoves) {
        UUID climbId = UUID.randomUUID();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("clientEntryId", entryId);
        entry.put("entryOrder", order);
        entry.put("resultType", result);
        entry.put("clientClimbId", climbId);
        entry.put("climbName", "Model v2 climb " + climbId);
        entry.put("climbType", climbType);
        entry.put("gradeLabel", grade);
        entry.put("effortProfileCode", profile);
        entry.put("ascentMode", "RP");
        entry.put("totalMoves", totalMoves);
        entry.put("executedMoves", executedMoves);
        if (climbType.equals("ROUTE")) {
            entry.put("protectionMode", "LEAD");
        }
        return entry;
    }

    private String detailsUrl(UUID sessionId) {
        return "/api/v1/training/sessions/" + sessionId;
    }

    private String updateRequest(UUID sessionId, tools.jackson.databind.JsonNode session) {
        long version = jdbcClient.sql("SELECT version FROM training.training_sessions WHERE id = :id")
                .param("id", sessionId).query(Long.class).single();
        return objectMapper.writeValueAsString(Map.of(
                "expectedVersion", version,
                "changeReason", "Correct model v2 integration fixture moves",
                "session", session
        ));
    }

    private void convertBoulderFixtureToLegacy(UUID sessionId, UUID entryId) {
        // Simulate persisted pre-v2 data without changing or rerunning old Flyway scripts.
        jdbcClient.sql("""
                        UPDATE training.training_entries
                        SET calculation_model_version = 'v1', profile_intensity_index = NULL,
                            effective_current_level_index = NULL, effective_relative_grade_diff = NULL,
                            level_reference_source = NULL, level_reference_algorithm_version = NULL,
                            level_reference_horizon_days = NULL, level_reference_sample_size = NULL,
                            level_reference_history_weight = NULL,
                            base_edl = 4, edl_count = 4.8,
                            move_intensity = 19.2708333333, adjusted_load = 100.2083
                        WHERE client_entry_id = :id
                        """).param("id", entryId).update();
        jdbcClient.sql("""
                        UPDATE training.training_sessions
                        SET adjusted_load = 100.2083, average_move_intensity = 19.2708333333
                        WHERE id = :id
                        """).param("id", sessionId).update();
    }

    private void convertSessionFixtureToV2(UUID sessionId) {
        // V2 shares the intensity calculation but predates the continuous reference metadata.
        jdbcClient.sql("""
                        UPDATE training.training_entries
                        SET calculation_model_version = 'v2',
                            effective_current_level_index = NULL, effective_relative_grade_diff = NULL,
                            level_reference_source = NULL, level_reference_algorithm_version = NULL,
                            level_reference_horizon_days = NULL, level_reference_sample_size = NULL,
                            level_reference_history_weight = NULL
                        WHERE session_id = :id
                        """).param("id", sessionId).update();
    }

    private void assertStoredDecimal(UUID entryId, String column, String expected) {
        BigDecimal actual = jdbcClient.sql("SELECT " + column + " FROM training.training_entries "
                        + "WHERE client_entry_id = :id")
                .param("id", entryId).query(BigDecimal.class).single();
        assertEquals(new BigDecimal(expected), actual);
    }

    private StoredCrux readCrux(UUID entryId) {
        return jdbcClient.sql("""
                        SELECT profile_intensity_index, hard_move_count, easy_move_count,
                               hard_move_intensity, easy_move_intensity, move_intensity,
                               adjusted_load, style_multiplier, relative_effort_multiplier,
                               base_move_grade_label, hard_move_grade_label, easy_move_grade_label
                        FROM training.training_entries WHERE client_entry_id = :id
                        """).param("id", entryId).query((rs, row) -> new StoredCrux(
                        rs.getBigDecimal("profile_intensity_index"),
                        rs.getInt("hard_move_count"), rs.getInt("easy_move_count"),
                        rs.getBigDecimal("hard_move_intensity"), rs.getBigDecimal("easy_move_intensity"),
                        rs.getBigDecimal("move_intensity"), rs.getBigDecimal("adjusted_load"),
                        rs.getBigDecimal("style_multiplier"), rs.getBigDecimal("relative_effort_multiplier"),
                        rs.getString("base_move_grade_label"), rs.getString("hard_move_grade_label"),
                        rs.getString("easy_move_grade_label")
                )).single();
    }

    private void assertCruxSnapshot(StoredCrux actual, int hard, int easy, String profileIndex) {
        BigDecimal work = actual.hardIntensity().multiply(BigDecimal.valueOf(hard))
                .add(actual.easyIntensity().multiply(BigDecimal.valueOf(easy)));
        assertAll(
                () -> assertEquals(new BigDecimal(profileIndex), actual.profileIndex()),
                () -> assertEquals(hard, actual.hardMoves()),
                () -> assertEquals(easy, actual.easyMoves()),
                () -> assertEquals(work.divide(BigDecimal.valueOf(hard + easy), 10,
                        RoundingMode.HALF_UP), actual.averageIntensity()),
                () -> assertEquals(work.multiply(actual.style()).multiply(actual.relative())
                        .setScale(4, RoundingMode.HALF_UP), actual.adjustedLoad()),
                () -> assertEquals("6B", actual.baseGrade()),
                () -> assertEquals("6C", actual.hardGrade()),
                () -> assertEquals("6A", actual.easyGrade())
        );
    }

    private record StoredCrux(BigDecimal profileIndex, int hardMoves, int easyMoves,
                              BigDecimal hardIntensity, BigDecimal easyIntensity,
                              BigDecimal averageIntensity, BigDecimal adjustedLoad,
                              BigDecimal style, BigDecimal relative,
                              String baseGrade, String hardGrade, String easyGrade) {
    }
}
