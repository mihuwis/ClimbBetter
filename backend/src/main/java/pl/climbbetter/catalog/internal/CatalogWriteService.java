package pl.climbbetter.catalog.internal;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.climbbetter.catalog.CreateClimbCommand;
import pl.climbbetter.catalog.ClimbMoveSuggestion;
import pl.climbbetter.identity.CurrentUserProvider;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;

@Service
class CatalogWriteService {

    private final JdbcClient jdbcClient;
    private final CurrentUserProvider currentUserProvider;
    private final CatalogQueryService queryService;
    private final Clock clock;

    CatalogWriteService(
            JdbcClient jdbcClient, CurrentUserProvider currentUserProvider,
            CatalogQueryService queryService, Clock clock
    ) {
        this.jdbcClient = jdbcClient;
        this.currentUserProvider = currentUserProvider;
        this.queryService = queryService;
        this.clock = clock;
    }

    @Transactional
    public CatalogQueryService.ClimbOption create(UUID areaId, CreateClimbCommand command) {
        UUID userId = currentUserProvider.currentUserId();
        queryService.requireAvailableArea(userId, areaId);
        queryService.requireAvailableSector(areaId, command.sectorId());
        String scaleCode = queryService.scaleFor(command.climbType());
        String systemCode = command.gradeSystemCode() == null ? scaleCode : command.gradeSystemCode();
        String inputLabel = command.gradeLabel() == null || command.gradeLabel().isBlank()
                ? null : command.gradeLabel().trim();
        ResolvedGrade grade = resolveGrade(scaleCode, systemCode, inputLabel);
        UUID profileId = command.effortProfileCode() == null || command.effortProfileCode().isBlank()
                ? null : jdbcClient.sql("""
                        SELECT id FROM training.effort_profiles
                        WHERE code = :code AND is_active = true
                        """)
                .param("code", command.effortProfileCode())
                .query(UUID.class).optional()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown active effort profile"
                ));
        Integer moves = ClimbMoveSuggestion.resolve(
                command.suggestedMoveCount(), command.lengthMeters(), command.climbType()
        );

        jdbcClient.sql("""
                        INSERT INTO catalog.climbs (
                            id, client_climb_id, owner_id, area_id, name, climb_type,
                            grade_id, default_effort_profile_id, suggested_move_count,
                            is_public, is_verified, is_archived, created_at, updated_at, version,
                            length_meters, sector_id, input_grade_system_code,
                            input_grade_label, grade_mapping_version
                        ) VALUES (
                            :id, :clientClimbId, :userId, :areaId, :name, :climbType,
                            :gradeId, :profileId, :moves,
                            false, false, false, :now, :now, 0,
                            :lengthMeters, :sectorId, :systemCode, :inputLabel, :mappingVersion
                        )
                        ON CONFLICT (owner_id, client_climb_id)
                            WHERE client_climb_id IS NOT NULL
                        DO NOTHING
                        """)
                .param("id", UUID.randomUUID())
                .param("clientClimbId", command.clientClimbId())
                .param("userId", userId).param("areaId", areaId)
                .param("name", command.name().trim()).param("climbType", command.climbType())
                .param("gradeId", grade.gradeId()).param("profileId", profileId)
                .param("lengthMeters", command.lengthMeters()).param("sectorId", command.sectorId())
                .param("systemCode", systemCode).param("inputLabel", inputLabel)
                .param("mappingVersion", grade.mappingVersion())
                .param("moves", moves).param("now", clock.instant().atOffset(ZoneOffset.UTC))
                .update();

        StoredClimb stored = jdbcClient.sql("""
                        SELECT id, area_id, name, climb_type, grade_id,
                               default_effort_profile_id, suggested_move_count, is_archived,
                               length_meters, sector_id,
                               COALESCE(input_grade_system_code,
                                   (SELECT scale_code FROM catalog.grades WHERE id = grade_id)
                               ) AS input_grade_system_code,
                               COALESCE(input_grade_label,
                                   (SELECT label FROM catalog.grades WHERE id = grade_id)
                               ) AS input_grade_label
                        FROM catalog.climbs
                        WHERE owner_id = :userId AND client_climb_id = :clientClimbId
                        """)
                .param("userId", userId).param("clientClimbId", command.clientClimbId())
                .query(StoredClimb.class).single();
        if (stored.isArchived() || !stored.areaId().equals(areaId)
                || !stored.name().equals(command.name().trim())
                || !stored.climbType().equals(command.climbType())
                || !Objects.equals(stored.gradeId(), grade.gradeId())
                || !Objects.equals(stored.defaultEffortProfileId(), profileId)
                || !Objects.equals(stored.suggestedMoveCount(), moves)
                || !Objects.equals(stored.sectorId(), command.sectorId())
                || !Objects.equals(stored.inputGradeSystemCode(), systemCode)
                || !Objects.equals(stored.inputGradeLabel(), inputLabel)
                || !sameLength(stored.lengthMeters(), command.lengthMeters())) {
            throw new ClientClimbIdConflictException();
        }
        return queryService.findClimb(areaId, stored.id());
    }

    private ResolvedGrade resolveGrade(String scaleCode, String systemCode, String label) {
        if (!systemCode.equals(scaleCode)
                && !("KURTYKA".equals(systemCode) && "ROUTE_FRENCH".equals(scaleCode))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade system does not match climb type");
        }
        if (label == null) {
            return new ResolvedGrade(null, null);
        }
        if ("KURTYKA".equals(systemCode)) {
            return jdbcClient.sql("""
                            SELECT alias.canonical_grade_id AS grade_id, alias.mapping_version
                            FROM catalog.grade_aliases alias
                            JOIN catalog.grades grade ON grade.id = alias.canonical_grade_id
                            WHERE alias.system_code = :systemCode AND alias.label = :label
                              AND alias.is_active = true AND grade.is_active = true
                              AND grade.scale_code = :scaleCode
                            ORDER BY alias.mapping_version DESC LIMIT 1
                            """)
                    .param("systemCode", systemCode).param("label", label).param("scaleCode", scaleCode)
                    .query(ResolvedGrade.class).optional()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unknown active grade mapping"
                    ));
        }
        UUID id = jdbcClient.sql("""
                        SELECT id FROM catalog.grades
                        WHERE scale_code = :scaleCode AND label = :label AND is_active = true
                        """).param("scaleCode", scaleCode).param("label", label)
                .query(UUID.class).optional()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown active grade for climb type"
                ));
        return new ResolvedGrade(id, null);
    }

    private boolean sameLength(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }

    private record ResolvedGrade(UUID gradeId, Integer mappingVersion) { }

    private record StoredClimb(
            UUID id, UUID areaId, String name, String climbType, UUID gradeId,
            UUID defaultEffortProfileId, Integer suggestedMoveCount, boolean isArchived,
            BigDecimal lengthMeters, UUID sectorId, String inputGradeSystemCode, String inputGradeLabel
    ) { }

    static class ClientClimbIdConflictException extends RuntimeException {
        ClientClimbIdConflictException() {
            super("Client climb ID was already used for a different or archived climb");
        }
    }
}
