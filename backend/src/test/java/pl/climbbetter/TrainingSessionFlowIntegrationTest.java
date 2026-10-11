package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrainingSessionFlowIntegrationTest {

    private static final UUID CLIENT_SESSION_ID =
            UUID.fromString(
                    "40000000-0000-0000-0000-000000000001"
            );

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void createsSessionInPostgresqlAndReturnsItOnDashboard()
            throws Exception {
        mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clientSessionId":
                                    "40000000-0000-0000-0000-000000000001",
                                  "sessionName": "Bronx bouldering",
                                  "sessionDate": "2026-10-11",
                                  "timeZoneId": "Europe/Warsaw",
                                  "startedAt": "2026-10-11T08:00:00Z",
                                  "endedAt": "2026-10-11T10:00:00Z",
                                  "clientAreaId":
                                    "41000000-0000-0000-0000-000000000001",
                                  "areaName": "Bronx Climbing Gym",
                                  "entries": [
                                    {
                                      "clientEntryId":
                                        "42000000-0000-0000-0000-000000000001",
                                      "entryOrder": 0,
                                      "resultType": "WARMUP",
                                      "executedMoves": 20
                                    },
                                    {
                                      "clientEntryId":
                                        "42000000-0000-0000-0000-000000000002",
                                      "entryOrder": 1,
                                      "resultType": "ASCENT",
                                      "clientClimbId":
                                        "43000000-0000-0000-0000-000000000001",
                                      "climbName": "Green Traverse",
                                      "climbType": "BOULDER",
                                      "gradeLabel": "5",
                                      "effortProfileCode": "BOULDER",
                                      "ascentMode": "OS",
                                      "totalMoves": 8,
                                      "executedMoves": 8,
                                      "historyConflictConfirmed": false
                                    },
                                    {
                                      "clientEntryId":
                                        "42000000-0000-0000-0000-000000000003",
                                      "entryOrder": 2,
                                      "resultType": "ASCENT",
                                      "clientClimbId":
                                        "43000000-0000-0000-0000-000000000002",
                                      "climbName": "Blue Corner",
                                      "climbType": "BOULDER",
                                      "gradeLabel": "6A",
                                      "effortProfileCode": "BOULDER",
                                      "ascentMode": "RP",
                                      "totalMoves": 8,
                                      "executedMoves": 8,
                                      "historyConflictConfirmed": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sessionId").isNotEmpty());

        Long storedEntryCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM training.training_entries entry
                        JOIN training.training_sessions session
                          ON session.id = entry.session_id
                        WHERE session.client_session_id = :clientSessionId
                        """)
                .param("clientSessionId", CLIENT_SESSION_ID)
                .query(Long.class)
                .single();

        assertEquals(3L, storedEntryCount);

        UUID storedSessionId = jdbcClient.sql("""
                        SELECT id
                        FROM training.training_sessions
                        WHERE client_session_id = :clientSessionId
                        """)
                .param("clientSessionId", CLIENT_SESSION_ID)
                .query(UUID.class)
                .single();

        mockMvc.perform(get(
                        "/api/v1/training/sessions/" + storedSessionId
                ))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sessionName")
                                .value("Bronx bouldering")
                )
                .andExpect(jsonPath("$.entries.length()").value(3))
                .andExpect(
                        jsonPath("$.entries[0].resultType")
                                .value("WARMUP")
                )
                .andExpect(
                        jsonPath("$.entries[0].entryOrder")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.entries[1].climbName")
                                .value("Green Traverse")
                )
                .andExpect(
                        jsonPath("$.entries[1].calculation.moveIntensity")
                                .isNumber()
                )
                .andExpect(
                        jsonPath("$.entries[2].climbName")
                                .value("Blue Corner")
                )
                .andExpect(
                        jsonPath("$.entries[2].history.familiaritySource")
                                .value("DECLARED_PRIOR_KNOWLEDGE")
                );

        mockMvc.perform(get(
                        "/api/v1/training/sessions/"
                                + "ffffffff-ffff-ffff-ffff-ffffffffffff"
                ))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/dashboard/sessions"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].sessionName")
                                .value("Bronx bouldering")
                )
                .andExpect(
                        jsonPath("$[0].sessionDate")
                                .value("2026-10-11")
                )
                .andExpect(
                        jsonPath("$[0].areaName")
                                .value("Bronx Climbing Gym")
                )
                .andExpect(
                        jsonPath("$[0].durationMinutes")
                                .value(120)
                )
                .andExpect(
                        jsonPath("$[0].totalMoves")
                                .value(36)
                )
                .andExpect(
                        jsonPath("$[0].maxBoulderGrade")
                                .value("6A")
                )
                .andExpect(
                        jsonPath("$[0].classicLoad")
                                .isNumber()
                )
                .andExpect(
                        jsonPath("$[0].adjustedLoad")
                                .isNumber()
                )
                .andExpect(
                        jsonPath("$[0].averageMoveIntensity")
                                .value(14.0625)
                );

        mockMvc.perform(get("/api/v1/dashboard/personal-bests"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.boulder.overall.gradeLabel")
                                .value("6A")
                )
                .andExpect(
                        jsonPath("$.boulder.overall.achievedOn")
                                .value("2026-10-11")
                )
                .andExpect(
                        jsonPath("$.boulder.os.gradeLabel")
                                .value("5")
                )
                .andExpect(
                        jsonPath("$.boulder.flash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.boulder.rp.gradeLabel")
                                .value("6A")
                )
                .andExpect(
                        jsonPath("$.boulder.rpVsOsGap")
                                .value(2)
                );
    }

    @Test
@Transactional(propagation = Propagation.NOT_SUPPORTED)
void rollsBackWholeSessionWhenOneEntryIsInvalid()
        throws Exception {
    UUID clientSessionId = UUID.fromString(
            "50000000-0000-0000-0000-000000000001"
    );
    UUID clientAreaId = UUID.fromString(
            "51000000-0000-0000-0000-000000000001"
    );
    UUID clientClimbId = UUID.fromString(
            "53000000-0000-0000-0000-000000000001"
    );

    mockMvc.perform(post("/api/v1/training/sessions")
                    .contentType("application/json")
                    .content("""
                            {
                              "clientSessionId":
                                "50000000-0000-0000-0000-000000000001",
                              "sessionName": "Invalid session",
                              "sessionDate": "2026-10-11",
                              "timeZoneId": "Europe/Warsaw",
                              "startedAt": "2026-10-11T08:00:00Z",
                              "endedAt": "2026-10-11T09:00:00Z",
                              "clientAreaId":
                                "51000000-0000-0000-0000-000000000001",
                              "areaName": "Rollback Gym",
                              "entries": [
                                {
                                  "clientEntryId":
                                    "52000000-0000-0000-0000-000000000001",
                                  "entryOrder": 0,
                                  "resultType": "WARMUP",
                                  "executedMoves": 20
                                },
                                {
                                  "clientEntryId":
                                    "52000000-0000-0000-0000-000000000002",
                                  "entryOrder": 1,
                                  "resultType": "ASCENT",
                                  "clientClimbId":
                                    "53000000-0000-0000-0000-000000000001",
                                  "climbName": "Invalid Boulder",
                                  "climbType": "BOULDER",
                                  "gradeLabel": "5",
                                  "effortProfileCode": "BOULDER",
                                  "ascentMode": "OS",
                                  "protectionMode": "LEAD",
                                  "totalMoves": 8,
                                  "executedMoves": 8
                                }
                              ]
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.code")
                            .value("INVALID_TRAINING_SESSION")
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Protection mode is allowed "
                                            + "only for a route"
                            )
            );

    Long storedSessionCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM training.training_sessions
                    WHERE client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(Long.class)
            .single();

    Long storedAreaCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM catalog.areas
                    WHERE client_area_id = :clientAreaId
                    """)
            .param("clientAreaId", clientAreaId)
            .query(Long.class)
            .single();

    Long storedClimbCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM catalog.climbs
                    WHERE client_climb_id = :clientClimbId
                    """)
            .param("clientClimbId", clientClimbId)
            .query(Long.class)
            .single();

    assertEquals(0L, storedSessionCount);
    assertEquals(0L, storedAreaCount);
    assertEquals(0L, storedClimbCount);
}

@Test
void returnsExistingSessionForIdenticalRetry()
        throws Exception {
    UUID clientSessionId = UUID.fromString(
            "60000000-0000-0000-0000-000000000001"
    );

    String requestBody = """
            {
              "clientSessionId":
                "60000000-0000-0000-0000-000000000001",
              "sessionName": "Retry session",
              "sessionDate": "2026-10-12",
              "timeZoneId": "Europe/Warsaw",
              "startedAt": "2026-10-12T08:00:00Z",
              "endedAt": "2026-10-12T09:00:00Z",
              "clientAreaId":
                "61000000-0000-0000-0000-000000000001",
              "areaName": "Retry Gym",
              "entries": [
                {
                  "clientEntryId":
                    "62000000-0000-0000-0000-000000000001",
                  "entryOrder": 0,
                  "resultType": "ASCENT",
                  "clientClimbId":
                    "63000000-0000-0000-0000-000000000001",
                  "climbName": "Retry Boulder",
                  "climbType": "BOULDER",
                  "gradeLabel": "5",
                  "effortProfileCode": "BOULDER",
                  "ascentMode": "OS",
                  "totalMoves": 8,
                  "executedMoves": 8
                }
              ]
            }
            """;

    mockMvc.perform(post("/api/v1/training/sessions")
                    .contentType("application/json")
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").isNotEmpty());

    UUID storedSessionId = jdbcClient.sql("""
                    SELECT id
                    FROM training.training_sessions
                    WHERE client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(UUID.class)
            .single();

    mockMvc.perform(post("/api/v1/training/sessions")
                    .contentType("application/json")
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(
                    jsonPath("$.sessionId")
                            .value(storedSessionId.toString())
            );

    Long storedSessionCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM training.training_sessions
                    WHERE client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(Long.class)
            .single();

    Long storedEntryCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM training.training_entries entry
                    JOIN training.training_sessions session
                      ON session.id = entry.session_id
                    WHERE session.client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(Long.class)
            .single();

    assertEquals(1L, storedSessionCount);
    assertEquals(1L, storedEntryCount);
}

@Test
void rejectsChangedRequestWithExistingClientSessionId()
        throws Exception {
    UUID clientSessionId = UUID.fromString(
            "70000000-0000-0000-0000-000000000001"
    );

    String originalRequest = """
            {
              "clientSessionId":
                "70000000-0000-0000-0000-000000000001",
              "sessionName": "Original retry session",
              "sessionDate": "2026-10-13",
              "timeZoneId": "Europe/Warsaw",
              "startedAt": "2026-10-13T08:00:00Z",
              "endedAt": "2026-10-13T09:00:00Z",
              "clientAreaId":
                "71000000-0000-0000-0000-000000000001",
              "areaName": "Conflict Gym",
              "entries": [
                {
                  "clientEntryId":
                    "72000000-0000-0000-0000-000000000001",
                  "entryOrder": 0,
                  "resultType": "WARMUP",
                  "executedMoves": 20
                }
              ]
            }
            """;

    String changedRequest = originalRequest.replace(
            "Original retry session",
            "Changed retry session"
    );

    mockMvc.perform(post("/api/v1/training/sessions")
                    .contentType("application/json")
                    .content(originalRequest))
            .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/training/sessions")
                    .contentType("application/json")
                    .content(changedRequest))
            .andExpect(status().isConflict())
            .andExpect(
                    jsonPath("$.code")
                            .value("CLIENT_SESSION_ID_CONFLICT")
            );

    Long storedSessionCount = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM training.training_sessions
                    WHERE client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(Long.class)
            .single();

    String storedSessionName = jdbcClient.sql("""
                    SELECT session_name
                    FROM training.training_sessions
                    WHERE client_session_id = :clientSessionId
                    """)
            .param("clientSessionId", clientSessionId)
            .query(String.class)
            .single();

    assertEquals(1L, storedSessionCount);
    assertEquals(
            "Original retry session",
            storedSessionName
    );
}
}
