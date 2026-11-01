package pl.climbbetter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.catalog.CreateClimbCommand;
import pl.climbbetter.catalog.internal.CatalogQueryService.ClimbOption;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.TrainingEntryCommand;
import pl.climbbetter.training.TrainingSessionDetails;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "climbbetter.local.user-id=00000000-0000-0000-0000-000000000002")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogClimbScenariosIntegrationTest {

    private static final UUID MIHU = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private UUID areaId;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void prepareMihuAndPublicBronx() {
        areaId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO identity.users (id, display_name, created_at)
                        VALUES (:id, 'Mihu', now()) ON CONFLICT (id) DO NOTHING
                        """).param("id", MIHU).update();
        jdbcClient.sql("""
                        INSERT INTO catalog.areas (
                            id, owner_id, name, area_type, facility_type, resolution_status,
                            is_public, is_archived, created_at, updated_at, version
                        ) VALUES (
                            :id, :ownerId, 'Bronx', 'ARTIFICIAL', 'BOULDER_GYM', 'RESOLVED',
                            true, false, now(), now(), 0
                        )
                        """).param("id", areaId).param("ownerId", MIHU).update();
    }

    @Test
    void scenarioARecordsThreeNewClimbsAndRepeatsThreeFromPreviousSession() throws Exception {
        List<TrainingEntryCommand> previousEntries = List.of(
                entry(null, UUID.randomUUID(), "Old green", "OS", 0),
                entry(null, UUID.randomUUID(), "Old blue", "OS", 1),
                entry(null, UUID.randomUUID(), "Old yellow", "OS", 2)
        );
        TrainingSessionDetails previous = saveSession("2015-01-01", previousEntries);
        String found = mockMvc.perform(get(climbsUrl()).param("q", "Old"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        ClimbOption[] existing = objectMapper.readValue(found, ClimbOption[].class);

        List<TrainingEntryCommand> currentEntries = new ArrayList<>(List.of(
                entry(null, UUID.randomUUID(), "New red", "OS", 0),
                entry(null, UUID.randomUUID(), "New orange", "OS", 1),
                entry(null, UUID.randomUUID(), "New black", "OS", 2)
        ));
        for (int i = 0; i < existing.length; i++) {
            currentEntries.add(entry(existing[i].id(), null, null, "RP", i + 3));
        }
        TrainingSessionDetails current = saveSession("2015-01-02", currentEntries);
        assertEquals(6, current.entries().size());
        assertEquals(48, current.totalMoves());
        for (int i = 3; i < 6; i++) {
            assertEquals(1, current.entries().get(i).history().contactCountBeforeTwoYears());
            assertEquals(2, current.entries().get(i).history().attemptOrdinalAllTime());
        }
        assertEquals(6L, climbCount());
        assertEquals(3, previous.entries().size());
        mockMvc.perform(get(climbsUrl()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void scenarioBSavesThreePrivateClimbsWithoutTrainingAndReusesOneInALaterSession() throws Exception {
        long sessionsBefore = sessionCount();
        CreateClimbCommand firstCommand = new CreateClimbCommand(
                UUID.randomUUID(), "Planned yellow", "BOULDER", "5+", "BOULDER", null
        );
        ClimbOption first = saveClimb(firstCommand);
        ClimbOption second = saveClimb(new CreateClimbCommand(
                UUID.randomUUID(), "Planned blue", "BOULDER", "6A", "BOULDER", 9
        ));
        saveClimb(new CreateClimbCommand(
                UUID.randomUUID(), "Planned green", "BOULDER", "5", "BOULDER", 8
        ));
        assertEquals(8, first.suggestedMoveCount());
        assertEquals(false, first.isPublic());
        assertEquals(first.id(), saveClimb(firstCommand).id());
        assertNotEquals(first.id(), second.id());
        assertEquals(3L, climbCount());
        assertEquals(sessionsBefore, sessionCount());
        assertEquals(0L, jdbcClient.sql("""
                        SELECT count(*) FROM training.training_entries entry
                        JOIN catalog.climbs climb ON climb.id = entry.climb_id
                        WHERE climb.area_id = :id
                        """).param("id", areaId).query(Long.class).single());
        mockMvc.perform(get(climbsUrl()).param("q", "Planned"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get(climbsUrl() + "/" + first.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gradeLabel").value("5+"));

        // Saving a catalog item does not count as previous contact with the climb.
        TrainingEntryCommand plannedEntry = new TrainingEntryCommand(
                UUID.randomUUID(), 0, "ASCENT", first.id(), null, null,
                "BOULDER", "5+", "BOULDER", "OS", null, 8, 8, null, false
        );
        TrainingSessionDetails session = saveSession("2015-01-01", List.of(plannedEntry));
        assertEquals(first.id(), session.entries().getFirst().climbId());
        assertEquals(0, session.entries().getFirst().history().contactCountBeforeTwoYears());
        assertEquals(3L, climbCount());

        mockMvc.perform(post(climbsUrl()).contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateClimbCommand(
                                firstCommand.clientClimbId(), "Different name", "BOULDER",
                                "5+", "BOULDER", 8
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_CLIMB_ID_CONFLICT"));
    }

    @Test
    void rejectsUnavailableAreasAndInvalidGradesAndKeepsIdenticalNamesSeparate() throws Exception {
        CreateClimbCommand command = new CreateClimbCommand(
                UUID.randomUUID(), "Same name", "BOULDER", "5", "BOULDER", 8
        );
        ClimbOption first = saveClimb(command);
        ClimbOption other = saveClimb(new CreateClimbCommand(
                UUID.randomUUID(), "Same name", "BOULDER", "5", "BOULDER", 8
        ));
        assertNotEquals(first.id(), other.id());
        mockMvc.perform(post(climbsUrl()).contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateClimbCommand(
                                UUID.randomUUID(), "Invalid grade", "BOULDER", "6a", "BOULDER", 8
                        ))))
                .andExpect(status().isBadRequest());
        jdbcClient.sql("""
                        UPDATE catalog.areas SET is_public = false,
                            owner_id = '00000000-0000-0000-0000-000000000001'
                        WHERE id = :id
                        """).param("id", areaId).update();
        mockMvc.perform(post(climbsUrl()).contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(climbsUrl() + "/" + first.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void storesOutdoorLengthSectorAndOriginalGradeWithFrenchMapping() throws Exception {
        UUID sectorId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO catalog.sectors (id, area_id, name, created_at, updated_at)
                        VALUES (:id, :areaId, 'Filar Abazego', now(), now())
                        """).param("id", sectorId).param("areaId", areaId).update();
        CreateClimbCommand command = new CreateClimbCommand(
                UUID.randomUUID(), "Zacięcie w Abazym", "ROUTE", "VI.2+", null, null,
                new BigDecimal("22"), sectorId, "KURTYKA"
        );
        ClimbOption route = saveClimb(command);
        assertEquals("6c+", route.gradeLabel());
        assertEquals("VI.2+", route.inputGradeLabel());
        assertEquals("KURTYKA", route.gradeSystemCode());
        assertEquals(1, route.gradeMappingVersion());
        assertEquals(44, route.suggestedMoveCount());
        assertEquals(new BigDecimal("22.00"), route.lengthMeters());
        assertEquals(sectorId, route.sectorId());
        assertEquals(route.id(), saveClimb(command).id());

        ClimbOption overridden = saveClimb(new CreateClimbCommand(
                UUID.randomUUID(), "Custom move count", "ROUTE", "VI.5+/6", null, 35,
                new BigDecimal("15"), sectorId, "KURTYKA"
        ));
        assertEquals("8a+", overridden.gradeLabel());
        assertEquals(35, overridden.suggestedMoveCount());
        ClimbOption project = saveClimb(new CreateClimbCommand(
                UUID.randomUUID(), "projekt", "ROUTE", null, null, null,
                new BigDecimal("20"), sectorId, "KURTYKA"
        ));
        assertEquals(null, project.gradeLabel());
        assertEquals(40, project.suggestedMoveCount());
        mockMvc.perform(get(climbsUrl()).param("sectorId", sectorId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/v1/catalog/areas/{areaId}/sectors", areaId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Filar Abazego"));
        mockMvc.perform(post(climbsUrl()).contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateClimbCommand(
                                UUID.randomUUID(), "Missing sector", "ROUTE", "6a", null, null,
                                new BigDecimal("15"), UUID.randomUUID(), "ROUTE_FRENCH"
                        ))))
                .andExpect(status().isNotFound());
    }

    private ClimbOption saveClimb(CreateClimbCommand command) throws Exception {
        String response = mockMvc.perform(post(climbsUrl()).contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, ClimbOption.class);
    }

    private TrainingSessionDetails saveSession(String date, List<TrainingEntryCommand> entries)
            throws Exception {
        var command = new CreateTrainingSessionCommand(
                UUID.randomUUID(), "Bronx session", LocalDate.parse(date), "Europe/Warsaw",
                Instant.parse(date + "T08:00:00Z"), Instant.parse(date + "T09:00:00Z"),
                areaId, null, null, entries
        );
        String location = mockMvc.perform(post("/api/v1/training/sessions")
                        .contentType("application/json").content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        String response = mockMvc.perform(get(location)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, TrainingSessionDetails.class);
    }

    private TrainingEntryCommand entry(UUID climbId, UUID clientClimbId, String name,
                                       String mode, int order) {
        return new TrainingEntryCommand(
                UUID.randomUUID(), order, "ASCENT", climbId, clientClimbId, name,
                "BOULDER", "5", "BOULDER", mode, null, 8, 8, null, false
        );
    }

    private String climbsUrl() {
        return "/api/v1/catalog/areas/" + areaId + "/climbs";
    }

    private long climbCount() {
        return jdbcClient.sql("SELECT count(*) FROM catalog.climbs WHERE area_id = :id")
                .param("id", areaId).query(Long.class).single();
    }

    private long sessionCount() {
        return jdbcClient.sql("SELECT count(*) FROM training.training_sessions WHERE user_id = :id")
                .param("id", MIHU).query(Long.class).single();
    }
}
