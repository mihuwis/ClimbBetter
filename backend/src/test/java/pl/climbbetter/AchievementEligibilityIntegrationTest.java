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
import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.training.TrainingSessionDetails;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AchievementEligibilityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    @Autowired DashboardQuery dashboardQuery;
    @MockitoBean CurrentUserProvider currentUserProvider;

    private UUID userId;

    @BeforeEach
    void useIsolatedUser() {
        userId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Achievement test', now())")
                .param("id", userId).update();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
    }

    @Test
    void topRopeIsTrainingWithoutMaxPersonalBestOrFastRpButKeepsContactsAndLoad() throws Exception {
        UUID climbId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                route(climbId, 0, "ATTEMPT", "8a", "TOP_ROPE"),
                route(climbId, 1, "ASCENT", "8a", "TOP_ROPE")
        ));

        TrainingSessionDetails details = details(sessionId);
        assertNull(details.maxRouteGrade());
        assertNull(details.maxBoulderGrade());
        assertEquals(30, details.totalMoves());
        assertTrue(details.adjustedLoad().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(1, details.entries().get(1).history().contactCountBeforeTwoYears());
        assertEquals(2, details.entries().get(1).history().attemptOrdinalAllTime());
        assertFalse(details.entries().get(1).history().fastRp());
        assertFalse(jdbcClient.sql("SELECT fast_rp FROM training.training_entries WHERE session_id = :id AND entry_order = 1")
                .param("id", sessionId).query(Boolean.class).single());
        assertNull(dashboardQuery.findRecentSessions().getFirst().maxRouteGrade());
        assertNull(dashboardQuery.findPersonalBests().route().overall());
        assertEquals(0L, jdbcClient.sql("SELECT count(*) FROM reporting.achievement_ascents WHERE session_id = :id")
                .param("id", sessionId).query(Long.class).single());
    }

    @Test
    void legacyTopRopeAchievementFieldsAreCorrectedInEveryReadWithoutMutatingSnapshots() throws Exception {
        UUID ropeClimb = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                route(ropeClimb, 0, "ATTEMPT", "8a", "TOP_ROPE"),
                route(ropeClimb, 1, "ASCENT", "8a", "TOP_ROPE"),
                route(UUID.randomUUID(), 2, "ASCENT", "7a", "LEAD")
        ));
        // Simulate old persisted achievement fields, rather than editing/recalculating the training.
        jdbcClient.sql("UPDATE training.training_sessions SET max_route_grade = '8a' WHERE id = :id")
                .param("id", sessionId).update();
        jdbcClient.sql("UPDATE training.training_entries SET fast_rp = true WHERE session_id = :id AND entry_order = 1")
                .param("id", sessionId).update();
        String rawBefore = rawSnapshot(sessionId);

        TrainingSessionDetails details = details(sessionId);
        assertEquals("7a", details.maxRouteGrade());
        assertFalse(details.entries().get(1).history().fastRp());
        assertEquals("7a", dashboardQuery.findRecentSessions().getFirst().maxRouteGrade());
        assertEquals("7a", dashboardQuery.findPersonalBests().route().overall().gradeLabel());
        assertEquals("7a", dashboardQuery.findPersonalBests().route().rp().gradeLabel());
        mockMvc.perform(get("/api/v1/training/sessions")
                        .param("from", "2002-02-03").param("to", "2002-02-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maxRouteGrade").value("7a"));

        assertEquals(rawBefore, rawSnapshot(sessionId));
        assertEquals("8a", jdbcClient.sql("SELECT max_route_grade FROM training.training_sessions WHERE id = :id")
                .param("id", sessionId).query(String.class).single());
        assertTrue(jdbcClient.sql("SELECT fast_rp FROM training.training_entries WHERE session_id = :id AND entry_order = 1")
                .param("id", sessionId).query(Boolean.class).single());
    }

    @Test
    void validLeadFastRpRemainsAnAchievement() throws Exception {
        UUID climbId = UUID.randomUUID();
        UUID sessionId = createSession(List.of(
                route(climbId, 0, "ATTEMPT", "7a", "LEAD"),
                route(climbId, 1, "ASCENT", "7a", "LEAD")
        ));
        TrainingSessionDetails details = details(sessionId);
        assertEquals("7a", details.maxRouteGrade());
        assertTrue(details.entries().get(1).history().fastRp());
        assertEquals("7a", dashboardQuery.findPersonalBests().route().rp().gradeLabel());
    }

    private UUID createSession(List<Map<String, Object>> entries) throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "clientSessionId", clientSessionId,
                "sessionName", "Achievement test " + clientSessionId,
                "sessionDate", "2002-02-03",
                "timeZoneId", "Europe/Warsaw",
                "startedAt", "2002-02-03T08:00:00Z",
                "endedAt", "2002-02-03T09:00:00Z",
                "clientAreaId", areaId,
                "areaName", "Achievement area " + areaId,
                "entries", entries
        ));
        String response = mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("sessionId").asText());
    }

    private Map<String, Object> route(UUID clientClimbId, int order, String result, String grade, String protection) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("clientEntryId", UUID.randomUUID());
        entry.put("entryOrder", order);
        entry.put("resultType", result);
        entry.put("clientClimbId", clientClimbId);
        entry.put("climbName", "Achievement route " + clientClimbId);
        entry.put("climbType", "ROUTE");
        entry.put("gradeLabel", grade);
        entry.put("effortProfileCode", "MEDIUM_CONTINUOUS");
        entry.put("ascentMode", "RP");
        entry.put("protectionMode", protection);
        entry.put("totalMoves", 20);
        entry.put("executedMoves", "ASCENT".equals(result) ? 20 : 10);
        return entry;
    }

    private TrainingSessionDetails details(UUID sessionId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/training/sessions/{id}", sessionId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, TrainingSessionDetails.class);
    }

    private String rawSnapshot(UUID sessionId) {
        return jdbcClient.sql("""
                        SELECT jsonb_build_object(
                            'session', to_jsonb(session),
                            'entries', (SELECT jsonb_agg(to_jsonb(entry) ORDER BY entry.entry_order)
                                        FROM training.training_entries entry WHERE entry.session_id = session.id),
                            'revisions', (SELECT jsonb_agg(to_jsonb(revision) ORDER BY revision.revision_number)
                                          FROM training.training_session_revisions revision WHERE revision.session_id = session.id)
                        )::text
                        FROM training.training_sessions session WHERE session.id = :id
                        """).param("id", sessionId).query(String.class).single();
    }
}
