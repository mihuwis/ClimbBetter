package pl.climbbetter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompletedMovesIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CurrentUserProvider currentUserProvider;

    private UUID userId;
    private int nextSessionSlot;

    @BeforeEach
    void createIsolatedUser() {
        userId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Completed moves test', now())")
                .param("id", userId).update();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
    }

    @AfterEach
    void removeIsolatedData() {
        jdbcClient.sql("DELETE FROM training.training_session_revisions WHERE changed_by_user_id = :id").param("id", userId).update();
        jdbcClient.sql("DELETE FROM training.training_sessions WHERE user_id = :id").param("id", userId).update();
        jdbcClient.sql("DELETE FROM catalog.climbs WHERE owner_id = :id").param("id", userId).update();
        jdbcClient.sql("DELETE FROM catalog.areas WHERE owner_id = :id").param("id", userId).update();
        jdbcClient.sql("DELETE FROM identity.users WHERE id = :id").param("id", userId).update();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 10})
    void rejectsDifferentCompletedMovesAndRollsBackSessionAreaClimbAndEntries(int executedMoves) throws Exception {
        Map<String, Object> request = sessionRequest("ASCENT", executedMoves);
        mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRAINING_SESSION"))
                .andExpect(jsonPath("$.detail").value(containsString("Completed climbs require exactly 8")));

        assertEquals(0L, count("SELECT count(*) FROM training.training_sessions WHERE user_id = :id"));
        assertEquals(0L, count("SELECT count(*) FROM catalog.areas WHERE owner_id = :id"));
        assertEquals(0L, count("SELECT count(*) FROM catalog.climbs WHERE owner_id = :id"));
        assertEquals(0L, count("SELECT count(*) FROM training.training_entries entry JOIN training.training_sessions session ON session.id = entry.session_id WHERE session.user_id = :id"));
    }

    @Test
    void rejectsIncompleteCompletedPutWithoutChangingStoredEntriesVersionOrAudit() throws Exception {
        Map<String, Object> request = sessionRequest("ASCENT", 8);
        UUID sessionId = createSession(request);
        String before = storedSession(sessionId);
        String beforeEntries = storedEntries(sessionId);
        String beforeRevisions = storedRevisions(sessionId);
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>) ((List<?>) request.get("entries")).getFirst();
        entry.put("executedMoves", 2);

        mockMvc.perform(put("/api/v1/training/sessions/" + sessionId).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "expectedVersion", 0,
                                "changeReason", "Try to save an incomplete completed climb",
                                "session", request
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRAINING_SESSION"));
        assertEquals(before, storedSession(sessionId));
        assertEquals(beforeEntries, storedEntries(sessionId));
        assertEquals(beforeRevisions, storedRevisions(sessionId));
        mockMvc.perform(get("/api/v1/training/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.entries[0].executedMoves").value(8));
    }

    @Test
    void acceptsFullCompletedClimbsAndPartialOrRepeatedAttemptsThroughTheApi() throws Exception {
        UUID completed = createSession(sessionRequest("ASCENT", 8));
        UUID partial = createSession(sessionRequest("ATTEMPT", 2));
        UUID repeated = createSession(sessionRequest("ATTEMPT", 10));
        mockMvc.perform(get("/api/v1/training/sessions/" + completed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].executedMoves").value(8));
        mockMvc.perform(get("/api/v1/training/sessions/" + partial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].resultType").value("ATTEMPT"))
                .andExpect(jsonPath("$.entries[0].executedMoves").value(2));
        mockMvc.perform(get("/api/v1/training/sessions/" + repeated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].resultType").value("ATTEMPT"))
                .andExpect(jsonPath("$.entries[0].executedMoves").value(10));
    }

    @Test
    void readsHistoricalIncompleteCompletedClimbsAndPreservesTheirIdenticalRetry() throws Exception {
        Map<String, Object> request = sessionRequest("ASCENT", 8);
        UUID sessionId = createSession(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>) ((List<?>) request.get("entries")).getFirst();
        entry.put("executedMoves", 2);
        String oldRequest = objectMapper.writeValueAsString(request);

        // Simulate an old accepted request without invoking a present-day edit/recalculation.
        CreateTrainingSessionCommand oldCommand = objectMapper.readValue(oldRequest, CreateTrainingSessionCommand.class);
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(objectMapper.writeValueAsBytes(oldCommand)));
        jdbcClient.sql("UPDATE training.training_entries SET executed_moves = 2 WHERE session_id = :id")
                .param("id", sessionId).update();
        jdbcClient.sql("UPDATE training.training_sessions SET request_fingerprint = :fingerprint WHERE id = :id")
                .param("id", sessionId).param("fingerprint", fingerprint).update();
        String before = storedSession(sessionId);
        String beforeEntries = storedEntries(sessionId);
        String beforeRevisions = storedRevisions(sessionId);

        mockMvc.perform(get("/api/v1/training/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].resultType").value("ASCENT"))
                .andExpect(jsonPath("$.entries[0].totalMoves").value(8))
                .andExpect(jsonPath("$.entries[0].executedMoves").value(2));
        mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json").content(oldRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
        assertEquals(before, storedSession(sessionId));
        assertEquals(beforeEntries, storedEntries(sessionId));
        assertEquals(beforeRevisions, storedRevisions(sessionId));
        assertEquals(1L, count("SELECT count(*) FROM training.training_sessions WHERE user_id = :id"));
    }

    private UUID createSession(Map<String, Object> request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("sessionId").asText());
    }

    private Map<String, Object> sessionRequest(String result, int executedMoves) {
        Instant start = Instant.parse("2001-02-03T08:00:00Z").plusSeconds(nextSessionSlot++ * 3600L);
        UUID area = UUID.randomUUID();
        UUID climb = UUID.randomUUID();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("clientEntryId", UUID.randomUUID());
        entry.put("entryOrder", 0);
        entry.put("resultType", result);
        entry.put("clientClimbId", climb);
        entry.put("climbName", "Completed moves climb " + climb);
        entry.put("climbType", "BOULDER");
        entry.put("gradeLabel", "6B");
        entry.put("effortProfileCode", "BOULDER");
        entry.put("ascentMode", "RP");
        entry.put("totalMoves", 8);
        entry.put("executedMoves", executedMoves);
        entry.put("historyConflictConfirmed", false);
        return new LinkedHashMap<>(Map.of(
                "clientSessionId", UUID.randomUUID(), "sessionName", "Completed moves session",
                "sessionDate", "2001-02-03", "timeZoneId", "UTC",
                "startedAt", start.toString(), "endedAt", start.plusSeconds(3600).toString(),
                "clientAreaId", area, "areaName", "Completed moves area " + area,
                "entries", List.of(entry)
        ));
    }

    private long count(String sql) {
        return jdbcClient.sql(sql).param("id", userId).query(Long.class).single();
    }

    private String storedSession(UUID sessionId) {
        return jdbcClient.sql("SELECT to_jsonb(session)::text FROM training.training_sessions session WHERE id = :id")
                .param("id", sessionId).query(String.class).single();
    }

    private String storedEntries(UUID sessionId) {
        return jdbcClient.sql("SELECT coalesce(jsonb_agg(to_jsonb(entry) ORDER BY entry_order)::text, '[]') FROM training.training_entries entry WHERE session_id = :id")
                .param("id", sessionId).query(String.class).single();
    }

    private String storedRevisions(UUID sessionId) {
        return jdbcClient.sql("SELECT coalesce(jsonb_agg(to_jsonb(revision) ORDER BY revision_number)::text, '[]') FROM training.training_session_revisions revision WHERE session_id = :id")
                .param("id", sessionId).query(String.class).single();
    }
}
