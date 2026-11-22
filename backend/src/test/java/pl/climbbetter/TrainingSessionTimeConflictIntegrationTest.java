package pl.climbbetter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.TrainingSessionDetails;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
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
class TrainingSessionTimeConflictIntegrationTest {

    private static final String SESSIONS = "/api/v1/training/sessions";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean CurrentUserProvider currentUserProvider;

    private final List<UUID> users = new ArrayList<>();
    private UUID userId;
    private UUID areaId;

    @BeforeEach
    void prepareUser() {
        userId = createUser();
        areaId = createArea(userId);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
    }

    @AfterEach
    void cleanup() {
        for (UUID owner : users) {
            for (String table : List.of("training_session_revisions", "training_entries")) {
                jdbc.sql("DELETE FROM training." + table + " " + """
                        WHERE session_id IN (SELECT id FROM training.training_sessions WHERE user_id = :id)
                        """).param("id", owner).update();
            }
            jdbc.sql("DELETE FROM training.training_sessions WHERE user_id = :id").param("id", owner).update();
            jdbc.sql("DELETE FROM catalog.areas WHERE owner_id = :id").param("id", owner).update();
            jdbc.sql("DELETE FROM identity.users WHERE id = :id").param("id", owner).update();
        }
    }

    @ParameterizedTest
    @CsvSource({"17:00,19:00", "19:00,21:00", "18:30,19:30", "17:00,21:00", "18:00,20:00"})
    void rejectsEveryKindOfOverlapOnCreate(String start, String end) throws Exception {
        create(request("18:00", "20:00"));

        mockMvc.perform(post(SESSIONS).contentType("application/json").content(request(start, end)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_TIME_CONFLICT"));

        assertEquals(1, activeSessionCount());
    }

    @ParameterizedTest
    @CsvSource({"16:00,18:00", "20:00,21:00"})
    void allowsSessionsThatOnlyTouchAtTheirBoundaries(String start, String end) throws Exception {
        create(request("18:00", "20:00"));
        create(request(start, end));
        assertEquals(2, activeSessionCount());
    }

    @Test
    void rejectsOverlapAtAnotherPlace() throws Exception {
        create(request("18:00", "20:00"));
        UUID otherArea = createArea(userId);
        mockMvc.perform(post(SESSIONS).contentType("application/json").content(request(
                        Instant.parse("2020-02-10T19:00:00Z"), Instant.parse("2020-02-10T21:00:00Z"),
                        "UTC", otherArea)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_TIME_CONFLICT"));
    }

    @Test
    void rejectedEditPreservesTimesEntriesVersionAndAudit() throws Exception {
        UUID editedId = create(request("10:00", "11:00"));
        create(request("18:00", "20:00"));
        TrainingSessionDetails before = details(editedId);
        String edited = withTimes(before, "19:00", "21:00");

        mockMvc.perform(put(SESSIONS + "/" + editedId).contentType("application/json")
                        .content(updateRequest(edited, before.version())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_TIME_CONFLICT"));

        assertEquals(before, details(editedId));
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM training.training_session_revisions WHERE session_id = :id")
                .param("id", editedId).query(Long.class).single());
    }

    @Test
    void allowsEditingOwnSessionAndMovingItToAnAdjacentTime() throws Exception {
        UUID id = create(request("10:00", "11:00"));
        create(request("18:00", "20:00"));
        TrainingSessionDetails before = details(id);

        mockMvc.perform(put(SESSIONS + "/" + id).contentType("application/json")
                        .content(updateRequest(withTimes(before, "10:00", "11:00"), before.version())))
                .andExpect(status().isOk());
        TrainingSessionDetails unchangedTimes = details(id);
        mockMvc.perform(put(SESSIONS + "/" + id).contentType("application/json")
                        .content(updateRequest(withTimes(unchangedTimes, "20:00", "21:00"), unchangedTimes.version())))
                .andExpect(status().isOk());
        assertEquals(Instant.parse("2020-02-10T20:00:00Z"), details(id).startedAt());

        // Moving a session releases its previous time interval.
        create(request("10:00", "11:00"));
    }

    @Test
    void identicalRetryReturnsTheExistingSession() throws Exception {
        String command = request("18:00", "20:00");
        UUID id = create(command);
        assertEquals(id, create(command));
        assertEquals(1, activeSessionCount());
    }

    @Test
    void deletionReleasesTheTimeButDatabasePreventsRestoringAConflictingSession() throws Exception {
        UUID id = create(request("18:00", "20:00"));
        mockMvc.perform(delete(SESSIONS + "/" + id).param("expectedVersion", "0"))
                .andExpect(status().isNoContent());
        create(request("18:00", "20:00"));
        assertEquals(1, activeSessionCount());

        var exception = assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql(
                "UPDATE training.training_sessions SET deleted_at = NULL WHERE id = :id")
                .param("id", id).update());
        assertExclusionViolation(exception);
    }

    @Test
    void allowsDifferentUsersToTrainAtTheSameTime() throws Exception {
        create(request("18:00", "20:00"));
        UUID other = createUser();
        UUID otherArea = createArea(other);
        when(currentUserProvider.currentUserId()).thenReturn(other);
        create(request(Instant.parse("2020-02-10T18:00:00Z"), Instant.parse("2020-02-10T20:00:00Z"),
                "UTC", otherArea));
    }

    @Test
    void comparesActualTimesAcrossMidnightAndDifferentTimeZones() throws Exception {
        create(request(Instant.parse("2020-02-10T23:30:00Z"), Instant.parse("2020-02-11T01:00:00Z"),
                "UTC", areaId));
        String overlap = request(Instant.parse("2020-02-11T00:30:00Z"), Instant.parse("2020-02-11T02:00:00Z"),
                "America/New_York", areaId);
        mockMvc.perform(post(SESSIONS).contentType("application/json").content(overlap))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_TIME_CONFLICT"));
        create(request(Instant.parse("2020-02-11T23:30:00Z"), Instant.parse("2020-02-12T01:00:00Z"),
                "UTC", areaId));
    }

    @Test
    void databaseAlsoRejectsDirectWritesThatBypassTheApi() throws Exception {
        UUID id = create(request("10:00", "11:00"));
        create(request("18:00", "20:00"));
        var exception = assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("""
                UPDATE training.training_sessions
                SET started_at = TIMESTAMPTZ '2020-02-10 19:00:00Z',
                    ended_at = TIMESTAMPTZ '2020-02-10 21:00:00Z'
                WHERE id = :id
                """).param("id", id).update());
        assertExclusionViolation(exception);
        assertEquals(Instant.parse("2020-02-10T10:00:00Z"), details(id).startedAt());
    }

    @Test
    void concurrentCreatesCannotSaveTwoOverlappingSessions() throws Exception {
        String first = request("18:00", "20:00");
        String second = request("19:00", "21:00");
        assertOneTimeConflict(race(() -> postResult(first), () -> postResult(second)));
        assertEquals(1, activeSessionCount());
    }

    @Test
    void concurrentCreateAndEditCannotClaimTheSameTime() throws Exception {
        UUID id = create(request("10:00", "11:00"));
        TrainingSessionDetails before = details(id);
        String edited = updateRequest(withTimes(before, "18:00", "20:00"), before.version());
        String added = request("18:00", "20:00");

        assertOneTimeConflict(race(() -> postResult(added), () -> mockMvc.perform(
                put(SESSIONS + "/" + id).contentType("application/json").content(edited)).andReturn()));
    }

    @Test
    void concurrentEditsCannotMoveTwoSessionsIntoTheSameTime() throws Exception {
        UUID first = create(request("10:00", "11:00"));
        UUID second = create(request("12:00", "13:00"));
        TrainingSessionDetails beforeFirst = details(first);
        TrainingSessionDetails beforeSecond = details(second);
        String left = updateRequest(withTimes(beforeFirst, "18:00", "20:00"), beforeFirst.version());
        String right = updateRequest(withTimes(beforeSecond, "18:00", "20:00"), beforeSecond.version());

        assertOneTimeConflict(race(
                () -> mockMvc.perform(put(SESSIONS + "/" + first).contentType("application/json").content(left)).andReturn(),
                () -> mockMvc.perform(put(SESSIONS + "/" + second).contentType("application/json").content(right)).andReturn()));
        assertEquals(2, activeSessionCount());
    }

    @Test
    void concurrentIdenticalRetriesReturnOneSession() throws Exception {
        String command = request("18:00", "20:00");
        List<MvcResult> results = race(() -> postResult(command), () -> postResult(command));
        for (MvcResult result : results) assertEquals(201, result.getResponse().getStatus());
        assertEquals(results.getFirst().getResponse().getContentAsString(), results.getLast().getResponse().getContentAsString());
        assertEquals(1, activeSessionCount());
    }

    private void assertOneTimeConflict(List<MvcResult> results) throws Exception {
        assertEquals(1, results.stream().filter(result -> result.getResponse().getStatus() < 300).count());
        MvcResult conflict = results.stream().filter(result -> result.getResponse().getStatus() == 409)
                .findFirst().orElseThrow();
        assertEquals("SESSION_TIME_CONFLICT", mapper.readTree(conflict.getResponse().getContentAsString())
                .get("code").asString());
    }

    private List<MvcResult> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> { start.await(); return first.call(); });
            var right = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return List.of(left.get(20, TimeUnit.SECONDS), right.get(20, TimeUnit.SECONDS));
        }
    }

    private MvcResult postResult(String request) throws Exception {
        return mockMvc.perform(post(SESSIONS).contentType("application/json").content(request)).andReturn();
    }

    private UUID create(String request) throws Exception {
        String response = mockMvc.perform(post(SESSIONS).contentType("application/json").content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(response).get("sessionId").asString());
    }

    private TrainingSessionDetails details(UUID id) throws Exception {
        return mapper.readValue(mockMvc.perform(get(SESSIONS + "/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), TrainingSessionDetails.class);
    }

    private String request(String start, String end) {
        return request(Instant.parse("2020-02-10T" + start + ":00Z"),
                Instant.parse("2020-02-10T" + end + ":00Z"), "UTC", areaId);
    }

    private String request(Instant start, Instant end, String zone, UUID area) {
        return mapper.writeValueAsString(Map.of(
                "clientSessionId", UUID.randomUUID(), "sessionName", "Time conflict test",
                "sessionDate", start.atZone(ZoneId.of(zone)).toLocalDate(), "timeZoneId", zone,
                "startedAt", start, "endedAt", end, "areaId", area,
                "entries", List.of(Map.of("clientEntryId", UUID.randomUUID(), "entryOrder", 0,
                        "resultType", "WARMUP", "executedMoves", 20))));
    }

    private String withTimes(TrainingSessionDetails session, String start, String end) {
        var command = mapper.readTree(request(start, end));
        var object = (tools.jackson.databind.node.ObjectNode) command;
        object.put("clientSessionId", session.clientSessionId().toString());
        return mapper.writeValueAsString(object);
    }

    private String updateRequest(String session, long version) {
        return mapper.writeValueAsString(Map.of("expectedVersion", version,
                "changeReason", "Correct session time", "session", mapper.readTree(session)));
    }

    private int activeSessionCount() {
        return jdbc.sql("SELECT count(*) FROM training.training_sessions WHERE user_id = :id AND deleted_at IS NULL")
                .param("id", userId).query(Integer.class).single();
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, 'Time conflict test', now())")
                .param("id", id).update();
        users.add(id);
        return id;
    }

    private UUID createArea(UUID owner) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO catalog.areas(id, owner_id, name, area_type, facility_type, resolution_status,
                                          is_public, is_archived, created_at, updated_at, version)
                VALUES (:id, :owner, 'Time conflict gym', 'ARTIFICIAL', 'MIXED_GYM', 'RESOLVED',
                        false, false, now(), now(), 0)
                """).param("id", id).param("owner", owner).update();
        return id;
    }

    private void assertExclusionViolation(DataIntegrityViolationException exception) {
        SQLException sql = assertInstanceOf(SQLException.class, exception.getMostSpecificCause());
        assertEquals("23P01", sql.getSQLState());
    }
}
