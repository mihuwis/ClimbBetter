package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.climbbetter.training.TrainingSessionDetails;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingSessionCrudIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;

    // No outer test transaction: failed updates must really roll back in the service.
    @Test
    void createsReadsUpdatesAndDeletesSessionWithCatalogSelection() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID clientAreaId = UUID.randomUUID();
        UUID clientClimbId = UUID.randomUUID();
        String request = sessionRequest(clientSessionId, clientAreaId, clientClimbId);
        try {
            mockMvc.perform(post("/api/v1/training/sessions")
                            .contentType("application/json").content(request))
                    .andExpect(status().isCreated());
            UUID sessionId = jdbcClient.sql("""
                            SELECT id FROM training.training_sessions
                            WHERE client_session_id = :clientSessionId
                            """)
                    .param("clientSessionId", clientSessionId)
                    .query(UUID.class).single();
            String url = "/api/v1/training/sessions/" + sessionId;
            TrainingSessionDetails before = readDetails(url);
            assertEquals(8, before.totalMoves());

            mockMvc.perform(get("/api/v1/catalog/areas").param("q", clientAreaId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(before.areaId().toString()));
            mockMvc.perform(get("/api/v1/catalog/areas/{id}/climbs", before.areaId())
                            .param("q", clientClimbId.toString()).param("climbType", "BOULDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(before.entries().getFirst().climbId().toString()))
                    .andExpect(jsonPath("$[0].gradeLabel").value("5"))
                    .andExpect(jsonPath("$[0].effortProfileCode").value("BOULDER"));
            mockMvc.perform(get("/api/v1/catalog/grades").param("climbType", "BOULDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].label", hasItem("5")));
            mockMvc.perform(get("/api/v1/catalog/effort-profiles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].code", hasItem("BOULDER")));

            // Use server IDs returned by Details, just as the React edit form will.
            var editedSession = objectMapper.readTree(request).deepCopy();
            var editedObject = (tools.jackson.databind.node.ObjectNode) editedSession;
            editedObject.put("sessionName", "CRUD edited session");
            editedObject.remove("clientAreaId");
            editedObject.remove("areaName");
            editedObject.put("areaId", before.areaId().toString());
            var entry = (tools.jackson.databind.node.ObjectNode) editedSession.get("entries").get(0);
            entry.remove("clientClimbId");
            entry.remove("climbName");
            entry.put("climbId", before.entries().getFirst().climbId().toString());
            entry.put("executedMoves", 12);
            String editedRequest = objectMapper.writeValueAsString(editedSession);

            mockMvc.perform(put(url).contentType("application/json")
                            .content(updateRequest(editedRequest, before.version())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionName").value("CRUD edited session"))
                    .andExpect(jsonPath("$.version").value(before.version() + 1))
                    .andExpect(jsonPath("$.totalMoves").value(12))
                    .andExpect(jsonPath("$.entries.length()").value(1))
                    .andExpect(jsonPath("$.entries[0].history.contactCountBeforeTwoYears").value(0));
            TrainingSessionDetails after = readDetails(url);
            assertNotEquals(before.adjustedLoad(), after.adjustedLoad());
            assertEquals(1L, revisionCount(sessionId));
            assertEquals(8, jdbcClient.sql("""
                            SELECT (before_state ->> 'totalMoves')::integer
                            FROM training.training_session_revisions WHERE session_id = :id
                            """).param("id", sessionId).query(Integer.class).single());

            mockMvc.perform(put(url).contentType("application/json")
                            .content(updateRequest(editedRequest, before.version())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SESSION_VERSION_CONFLICT"));
            var invalidSession = (tools.jackson.databind.node.ObjectNode) editedSession.deepCopy();
            ((tools.jackson.databind.node.ObjectNode) invalidSession.get("entries").get(0))
                    .put("gradeLabel", "INVALID");
            mockMvc.perform(put(url).contentType("application/json")
                            .content(updateRequest(objectMapper.writeValueAsString(invalidSession), after.version())))
                    .andExpect(status().isBadRequest());
            assertEquals(after, readDetails(url));
            assertEquals(1L, revisionCount(sessionId));

            mockMvc.perform(delete(url).param("expectedVersion", Long.toString(before.version())))
                    .andExpect(status().isConflict());
            mockMvc.perform(delete(url).param("expectedVersion", Long.toString(after.version())))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get(url)).andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/dashboard/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].sessionId", not(hasItem(sessionId.toString()))));
            mockMvc.perform(get("/api/v1/dashboard/personal-bests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$..sessionId", not(hasItem(sessionId.toString()))));
            assertEquals(2L, revisionCount(sessionId));
            mockMvc.perform(post("/api/v1/training/sessions")
                            .contentType("application/json").content(request))
                    .andExpect(status().isConflict());
            mockMvc.perform(put(url).contentType("application/json")
                            .content(updateRequest(editedRequest, after.version())))
                    .andExpect(status().isNotFound());
            mockMvc.perform(delete(url).param("expectedVersion", Long.toString(after.version())))
                    .andExpect(status().isNotFound());
        } finally {
            cleanup(clientSessionId, clientAreaId, clientClimbId);
        }
    }

    @Test
    void hidesOtherUsersSessionsAndPrivateCatalogEntries() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID clientAreaId = UUID.randomUUID();
        UUID clientClimbId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String request = sessionRequest(clientSessionId, clientAreaId, clientClimbId);
        try {
            mockMvc.perform(post("/api/v1/training/sessions")
                            .contentType("application/json").content(request))
                    .andExpect(status().isCreated());
            UUID sessionId = jdbcClient.sql("""
                            SELECT id FROM training.training_sessions WHERE client_session_id = :id
                            """).param("id", clientSessionId).query(UUID.class).single();
            String url = "/api/v1/training/sessions/" + sessionId;
            TrainingSessionDetails details = readDetails(url);
            jdbcClient.sql("""
                            INSERT INTO identity.users (id, display_name, created_at)
                            VALUES (:id, 'Other CRUD user', now())
                            """).param("id", otherUserId).update();
            jdbcClient.sql("UPDATE training.training_sessions SET user_id = :userId WHERE id = :id")
                    .param("userId", otherUserId).param("id", sessionId).update();
            jdbcClient.sql("UPDATE catalog.areas SET owner_id = :userId WHERE id = :id")
                    .param("userId", otherUserId).param("id", details.areaId()).update();
            UUID climbId = details.entries().getFirst().climbId();
            jdbcClient.sql("UPDATE catalog.climbs SET owner_id = :userId WHERE id = :id")
                    .param("userId", otherUserId).param("id", climbId).update();

            mockMvc.perform(get(url)).andExpect(status().isNotFound());
            mockMvc.perform(put(url).contentType("application/json")
                            .content(updateRequest(request, details.version())))
                    .andExpect(status().isNotFound());
            mockMvc.perform(delete(url).param("expectedVersion", Long.toString(details.version())))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/catalog/areas").param("q", clientAreaId.toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            String climbsUrl = "/api/v1/catalog/areas/" + details.areaId() + "/climbs";
            mockMvc.perform(get(climbsUrl)).andExpect(status().isNotFound());

            jdbcClient.sql("UPDATE catalog.areas SET is_public = true WHERE id = :id")
                    .param("id", details.areaId()).update();
            mockMvc.perform(get("/api/v1/catalog/areas").param("q", clientAreaId.toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
            mockMvc.perform(get(climbsUrl))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            jdbcClient.sql("UPDATE catalog.climbs SET is_public = true WHERE id = :id")
                    .param("id", climbId).update();
            mockMvc.perform(get(climbsUrl))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(climbId.toString()));
            jdbcClient.sql("UPDATE catalog.climbs SET is_archived = true WHERE id = :id")
                    .param("id", climbId).update();
            mockMvc.perform(get(climbsUrl))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(get("/api/v1/catalog/areas").param("limit", "51"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(climbsUrl).param("climbType", "INVALID"))
                    .andExpect(status().isBadRequest());
        } finally {
            cleanup(clientSessionId, clientAreaId, clientClimbId);
            jdbcClient.sql("DELETE FROM identity.users WHERE id = :id")
                    .param("id", otherUserId).update();
        }
    }

    private TrainingSessionDetails readDetails(String url) throws Exception {
        String response = mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, TrainingSessionDetails.class);
    }

    @Test
    void calendarReturnsMoreThanTwentySessionsAndExcludesDeletedAndOtherUsers() throws Exception {
        UUID clientSessionId = UUID.randomUUID();
        UUID clientAreaId = UUID.randomUUID();
        UUID clientClimbId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String fixtureName = "Calendar " + UUID.randomUUID();
        try {
            mockMvc.perform(post("/api/v1/training/sessions").contentType("application/json")
                            .content(sessionRequest(clientSessionId, clientAreaId, clientClimbId)))
                    .andExpect(status().isCreated());
            // Copies exercise pagination and visibility without depending on unrelated fixtures.
            jdbcClient.sql("""
                    INSERT INTO identity.users (id, display_name, created_at)
                    VALUES (:id, 'Calendar other user', now())
                    """).param("id", otherUserId).update();
            jdbcClient.sql("""
                    INSERT INTO training.training_sessions
                      (id, client_session_id, user_id, area_id, session_name, session_date,
                       time_zone_id, duration_minutes, location_name_snapshot, total_moves,
                       classic_load, adjusted_load, created_at, updated_at, version,
                       started_at, ended_at, request_fingerprint, deleted_at)
                    SELECT gen_random_uuid(), gen_random_uuid(),
                           CASE WHEN n = 24 THEN :otherUserId ELSE user_id END,
                           area_id, :name, DATE '2001-02-14', time_zone_id, duration_minutes,
                           location_name_snapshot, total_moves, classic_load, adjusted_load,
                           created_at, updated_at, version,
                           TIMESTAMPTZ '2001-02-14 08:00:00Z', TIMESTAMPTZ '2001-02-14 09:00:00Z',
                           request_fingerprint, CASE WHEN n = 23 THEN now() ELSE NULL END
                    FROM training.training_sessions CROSS JOIN generate_series(1, 24) AS n
                    WHERE client_session_id = :clientId
                    """).param("otherUserId", otherUserId).param("name", fixtureName)
                    .param("clientId", clientSessionId).update();
            mockMvc.perform(get("/api/v1/training/sessions")
                            .param("from", "2001-02-14").param("to", "2001-02-14"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(22))
                    .andExpect(jsonPath("$[0].sessionName").value(fixtureName));
            mockMvc.perform(get("/api/v1/training/sessions")
                            .param("from", "2001-02-15").param("to", "2001-02-15"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(get("/api/v1/training/sessions")
                            .param("from", "2001-03-01").param("to", "2001-02-01"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/v1/training/sessions")
                            .param("from", "2001-01-01").param("to", "2003-01-01"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/v1/identity/me"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.displayName").isNotEmpty());
        } finally {
            jdbcClient.sql("DELETE FROM training.training_sessions WHERE session_name = :name")
                    .param("name", fixtureName).update();
            cleanup(clientSessionId, clientAreaId, clientClimbId);
            jdbcClient.sql("DELETE FROM identity.users WHERE id = :id").param("id", otherUserId).update();
        }
    }

    private String updateRequest(String session, long version) {
        return objectMapper.writeValueAsString(Map.of(
                "expectedVersion", version,
                "changeReason", "Correct executed moves",
                "session", objectMapper.readTree(session)
        ));
    }

    private long revisionCount(UUID sessionId) {
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM training.training_session_revisions
                        WHERE session_id = :id
                        """).param("id", sessionId).query(Long.class).single();
    }

    private void cleanup(UUID clientSessionId, UUID clientAreaId, UUID clientClimbId) {
        for (String table : new String[]{"training_session_revisions", "training_entries"}) {
            jdbcClient.sql("DELETE FROM training." + table + """
                             WHERE session_id IN (
                                 SELECT id FROM training.training_sessions
                                 WHERE client_session_id = :id
                             )
                            """).param("id", clientSessionId).update();
        }
        jdbcClient.sql("DELETE FROM training.training_sessions WHERE client_session_id = :id")
                .param("id", clientSessionId).update();
        jdbcClient.sql("DELETE FROM catalog.climbs WHERE client_climb_id = :id")
                .param("id", clientClimbId).update();
        jdbcClient.sql("DELETE FROM catalog.areas WHERE client_area_id = :id")
                .param("id", clientAreaId).update();
    }

    private String sessionRequest(UUID sessionId, UUID areaId, UUID climbId) {
        return """
                {
                  "clientSessionId": "%s",
                  "sessionName": "CRUD session",
                  "sessionDate": "2010-01-01",
                  "timeZoneId": "Europe/Warsaw",
                  "startedAt": "2010-01-01T08:00:00Z",
                  "endedAt": "2010-01-01T09:00:00Z",
                  "clientAreaId": "%s",
                  "areaName": "CRUD Area %s",
                  "entries": [{
                    "clientEntryId": "%s", "entryOrder": 0,
                    "resultType": "ASCENT", "clientClimbId": "%s",
                    "climbName": "CRUD Climb %s", "climbType": "BOULDER",
                    "gradeLabel": "5", "effortProfileCode": "BOULDER",
                    "ascentMode": "OS", "totalMoves": 8, "executedMoves": 8
                  }]
                }
                """.formatted(sessionId, areaId, areaId, UUID.randomUUID(), climbId, climbId);
    }
}
