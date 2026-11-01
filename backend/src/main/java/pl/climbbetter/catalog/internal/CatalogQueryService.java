package pl.climbbetter.catalog.internal;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.climbbetter.identity.CurrentUserProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    private static final String CLIMB_SELECT = """
            SELECT climb.id, climb.area_id, climb.name, climb.climb_type,
                   grade.scale_code AS grade_scale_code, grade.label AS grade_label,
                   profile.code AS effort_profile_code,
                   climb.suggested_move_count, climb.is_public,
                   climb.length_meters, climb.sector_id, sector.name AS sector_name,
                   COALESCE(climb.input_grade_system_code, grade.scale_code) AS grade_system_code,
                   COALESCE(climb.input_grade_label, grade.label) AS input_grade_label,
                   climb.grade_mapping_version
            FROM catalog.climbs climb
            LEFT JOIN catalog.grades grade ON grade.id = climb.grade_id
            LEFT JOIN catalog.sectors sector ON sector.id = climb.sector_id
            LEFT JOIN training.effort_profiles profile
              ON profile.id = climb.default_effort_profile_id AND profile.is_active = true
            """;

    private final JdbcClient jdbcClient;
    private final CurrentUserProvider currentUserProvider;

    CatalogQueryService(JdbcClient jdbcClient, CurrentUserProvider currentUserProvider) {
        this.jdbcClient = jdbcClient;
        this.currentUserProvider = currentUserProvider;
    }

    List<AreaOption> findAreas(String query, int limit) {
        validateLimit(limit);
        return jdbcClient.sql("""
                        SELECT id, name, area_type, facility_type, is_public,
                               resolution_status, country_code, region
                        FROM catalog.areas
                        WHERE is_archived = false
                          AND resolution_status <> 'MERGED'
                          AND (is_public = true OR owner_id = :userId)
                          AND position(lower(:query) in lower(name)) > 0
                        ORDER BY lower(name), id
                        LIMIT :limit
                        """)
                .param("userId", currentUserProvider.currentUserId())
                .param("query", query.trim())
                .param("limit", limit)
                .query(AreaOption.class)
                .list();
    }

    List<ClimbOption> findClimbs(UUID areaId, String query, String climbType, int limit) {
        return findClimbs(areaId, query, climbType, limit, null);
    }

    List<ClimbOption> findClimbs(UUID areaId, String query, String climbType, int limit, UUID sectorId) {
        validateLimit(limit);
        if (climbType != null) {
            scaleFor(climbType);
        }
        UUID userId = currentUserProvider.currentUserId();
        requireAvailableArea(userId, areaId);
        requireAvailableSector(areaId, sectorId);
        return jdbcClient.sql(CLIMB_SELECT + """
                        WHERE climb.area_id = :areaId
                          AND climb.is_archived = false
                          AND (climb.sector_id IS NULL OR sector.is_archived = false)
                          AND (CAST(:sectorId AS uuid) IS NULL OR climb.sector_id = :sectorId)
                          AND (climb.is_public = true OR climb.owner_id = :userId)
                          AND position(lower(:query) in lower(climb.name)) > 0
                          AND (:climbType = '' OR climb.climb_type = :climbType)
                        ORDER BY lower(climb.name), climb.id
                        LIMIT :limit
                        """)
                .param("areaId", areaId)
                .param("sectorId", sectorId)
                .param("userId", userId)
                .param("query", query.trim())
                .param("climbType", climbType == null ? "" : climbType)
                .param("limit", limit)
                .query(ClimbOption.class)
                .list();
    }

    ClimbOption findClimb(UUID areaId, UUID climbId) {
        UUID userId = currentUserProvider.currentUserId();
        requireAvailableArea(userId, areaId);
        return jdbcClient.sql(CLIMB_SELECT + """
                        WHERE climb.id = :climbId
                          AND climb.area_id = :areaId
                          AND climb.is_archived = false
                          AND (climb.sector_id IS NULL OR sector.is_archived = false)
                          AND (climb.is_public = true OR climb.owner_id = :userId)
                        """)
                .param("climbId", climbId)
                .param("areaId", areaId)
                .param("userId", userId)
                .query(ClimbOption.class)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    List<SectorOption> findSectors(UUID areaId) {
        requireAvailableArea(currentUserProvider.currentUserId(), areaId);
        return jdbcClient.sql("""
                        SELECT id, area_id, name FROM catalog.sectors
                        WHERE area_id = :areaId AND is_archived = false
                        ORDER BY lower(name), id
                        """).param("areaId", areaId).query(SectorOption.class).list();
    }

    void requireAvailableSector(UUID areaId, UUID sectorId) {
        if (sectorId == null) {
            return;
        }
        boolean available = jdbcClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM catalog.sectors
                            WHERE id = :sectorId AND area_id = :areaId AND is_archived = false
                        )
                        """).param("sectorId", sectorId).param("areaId", areaId)
                .query(Boolean.class).single();
        if (!available) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    List<GradeOption> findGrades(String climbType) {
        return jdbcClient.sql("""
                        SELECT id, scale_code, label, grade_index
                        FROM catalog.grades
                        WHERE is_active = true AND scale_code = :scaleCode
                        ORDER BY grade_index
                        """)
                .param("scaleCode", scaleFor(climbType))
                .query(GradeOption.class)
                .list();
    }

    List<EffortProfileOption> findEffortProfiles() {
        return jdbcClient.sql("""
                        SELECT code, display_name, description, base_edl
                        FROM training.effort_profiles
                        WHERE is_active = true
                        ORDER BY code
                        """)
                .query(EffortProfileOption.class)
                .list();
    }

    void requireAvailableArea(UUID userId, UUID areaId) {
        boolean available = jdbcClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM catalog.areas
                            WHERE id = :areaId
                              AND is_archived = false
                              AND resolution_status <> 'MERGED'
                              AND (is_public = true OR owner_id = :userId)
                        )
                        """)
                .param("areaId", areaId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
        if (!available) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Limit must be between 1 and 50"
            );
        }
    }

    String scaleFor(String climbType) {
        if (climbType == null || !Set.of("BOULDER", "ROUTE", "CIRCUIT").contains(climbType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Climb type must be BOULDER, ROUTE or CIRCUIT"
            );
        }
        return "BOULDER".equals(climbType) ? "BOULDER_FONT" : "ROUTE_FRENCH";
    }

    public record AreaOption(
            UUID id, String name, String areaType, String facilityType,
            boolean isPublic, String resolutionStatus, String countryCode, String region
    ) { }

    public record ClimbOption(
            UUID id, UUID areaId, String name, String climbType,
            String gradeScaleCode, String gradeLabel, String effortProfileCode,
            Integer suggestedMoveCount, boolean isPublic,
            BigDecimal lengthMeters, UUID sectorId, String sectorName,
            String gradeSystemCode, String inputGradeLabel, Integer gradeMappingVersion
    ) { }

    public record GradeOption(UUID id, String scaleCode, String label, int gradeIndex) { }

    public record SectorOption(UUID id, UUID areaId, String name) { }

    public record EffortProfileOption(
            String code, String displayName, String description, BigDecimal baseEdl
    ) { }
}
