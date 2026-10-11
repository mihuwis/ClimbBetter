package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.training.TrainingEntryCommand;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
class ClimbResolver {

    private final JdbcClient jdbcClient;
    private final Clock clock;

    ClimbResolver(
            JdbcClient jdbcClient,
            Clock clock
    ) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
    }

    ResolvedClimb resolve(
            UUID userId,
            UUID areaId,
            TrainingEntryCommand entry,
            GradeResolver.ResolvedGrade grade,
            EffortProfileResolver.ResolvedEffortProfile effortProfile
    ) {
        if (entry.climbId() != null) {
            if (entry.clientClimbId() != null) {
                throw new IllegalArgumentException(
                        "Use either climb ID or client climb ID"
                );
            }

            return findById(userId, entry.climbId())
                    .map(climb -> validate(
                            climb,
                            areaId,
                            entry.climbType()
                    ))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Climb does not exist or is not available"
                    ));
        }

        if (entry.clientClimbId() == null) {
            throw new IllegalArgumentException(
                    "Climb ID or client climb ID is required"
            );
        }

        if (entry.climbName() == null
                || entry.climbName().isBlank()) {
            throw new IllegalArgumentException(
                    "Climb name is required for a new climb"
            );
        }

        return findByClientId(userId, entry.clientClimbId())
                .map(climb -> validate(
                        climb,
                        areaId,
                        entry.climbType()
                ))
                .orElseGet(() -> createPrivateClimb(
                        userId,
                        areaId,
                        entry,
                        grade,
                        effortProfile
                ));
    }

    private Optional<ResolvedClimb> findById(
            UUID userId,
            UUID climbId
    ) {
        return jdbcClient.sql("""
                        SELECT
                            id,
                            area_id,
                            name,
                            climb_type
                        FROM catalog.climbs
                        WHERE id = :climbId
                          AND is_archived = false
                          AND (
                              is_public = true
                              OR owner_id = :userId
                          )
                        """)
                .param("climbId", climbId)
                .param("userId", userId)
                .query(this::mapClimb)
                .optional();
    }

    private Optional<ResolvedClimb> findByClientId(
            UUID userId,
            UUID clientClimbId
    ) {
        return jdbcClient.sql("""
                        SELECT
                            id,
                            area_id,
                            name,
                            climb_type
                        FROM catalog.climbs
                        WHERE owner_id = :userId
                          AND client_climb_id = :clientClimbId
                          AND is_archived = false
                        """)
                .param("userId", userId)
                .param("clientClimbId", clientClimbId)
                .query(this::mapClimb)
                .optional();
    }

    private ResolvedClimb createPrivateClimb(
            UUID userId,
            UUID areaId,
            TrainingEntryCommand entry,
            GradeResolver.ResolvedGrade grade,
            EffortProfileResolver.ResolvedEffortProfile effortProfile
    ) {
        UUID climbId = UUID.randomUUID();
        OffsetDateTime now = clock.instant()
                .atOffset(ZoneOffset.UTC);
        String climbName = entry.climbName().trim();

        jdbcClient.sql("""
                        INSERT INTO catalog.climbs (
                            id,
                            client_climb_id,
                            owner_id,
                            area_id,
                            name,
                            climb_type,
                            grade_id,
                            default_effort_profile_id,
                            suggested_move_count,
                            is_public,
                            is_verified,
                            is_archived,
                            created_at,
                            updated_at,
                            version
                        )
                        VALUES (
                            :id,
                            :clientClimbId,
                            :ownerId,
                            :areaId,
                            :name,
                            :climbType,
                            :gradeId,
                            :effortProfileId,
                            :suggestedMoveCount,
                            false,
                            false,
                            false,
                            :now,
                            :now,
                            0
                        )
                        """)
                .param("id", climbId)
                .param("clientClimbId", entry.clientClimbId())
                .param("ownerId", userId)
                .param("areaId", areaId)
                .param("name", climbName)
                .param("climbType", entry.climbType())
                .param("gradeId", grade.id())
                .param("effortProfileId", effortProfile.id())
                .param(
                        "suggestedMoveCount",
                        entry.totalMoves()
                )
                .param("now", now)
                .update();

        return new ResolvedClimb(
                climbId,
                areaId,
                climbName,
                entry.climbType()
        );
    }

    private ResolvedClimb validate(
            ResolvedClimb climb,
            UUID expectedAreaId,
            String expectedClimbType
    ) {
        if (!climb.areaId().equals(expectedAreaId)) {
            throw new IllegalArgumentException(
                    "Climb does not belong to the session area"
            );
        }

        if (!climb.climbType().equals(expectedClimbType)) {
            throw new IllegalArgumentException(
                    "Climb type does not match the catalog"
            );
        }

        return climb;
    }

    private ResolvedClimb mapClimb(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        return new ResolvedClimb(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("area_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("climb_type")
        );
    }

    record ResolvedClimb(
            UUID id,
            UUID areaId,
            String name,
            String climbType
    ) {
    }
}
