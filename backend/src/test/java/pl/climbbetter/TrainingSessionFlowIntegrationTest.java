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
                                      "ascentMode": "OS",
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
                );
    }
}