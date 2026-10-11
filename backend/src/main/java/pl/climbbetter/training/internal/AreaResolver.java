package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
class AreaResolver {

    private final JdbcClient jdbcClient;
    private final Clock clock;

    AreaResolver(
            JdbcClient jdbcClient,
            Clock clock
    ) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
    }

    ResolvedArea resolve(
            UUID userId,
            UUID areaId,
            UUID clientAreaId,
            String areaName
    ) {
        if (areaId != null) {
            return findExisting(userId, areaId);
        }

        if (clientAreaId == null) {
            throw new IllegalArgumentException(
                    "Area ID or client area ID is required"
            );
        }

        if (areaName == null || areaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Area name is required for a new area"
            );
        }

        return findByClientId(userId, clientAreaId)
                .orElseGet(() -> createDraft(
                        userId,
                        clientAreaId,
                        areaName.trim()
                ));
    }

    private ResolvedArea findExisting(
            UUID userId,
            UUID areaId
    ) {
        return jdbcClient.sql("""
                        SELECT id, name
                        FROM catalog.areas
                        WHERE id = :areaId
                          AND is_archived = false
                          AND resolution_status <> 'MERGED'
                          AND (
                              is_public = true
                              OR owner_id = :userId
                          )
                        """)
                .param("areaId", areaId)
                .param("userId", userId)
                .query((resultSet, rowNumber) -> new ResolvedArea(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name")
                ))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Area does not exist or is not available"
                ));
    }

    private java.util.Optional<ResolvedArea> findByClientId(
            UUID userId,
            UUID clientAreaId
    ) {
        return jdbcClient.sql("""
                        SELECT id, name
                        FROM catalog.areas
                        WHERE owner_id = :userId
                          AND client_area_id = :clientAreaId
                          AND is_archived = false
                        """)
                .param("userId", userId)
                .param("clientAreaId", clientAreaId)
                .query((resultSet, rowNumber) -> new ResolvedArea(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name")
                ))
                .optional();
    }

    private ResolvedArea createDraft(
            UUID userId,
            UUID clientAreaId,
            String areaName
    ) {
        UUID areaId = UUID.randomUUID();
        OffsetDateTime now = clock.instant()
                .atOffset(ZoneOffset.UTC);

        jdbcClient.sql("""
                        INSERT INTO catalog.areas (
                            id,
                            client_area_id,
                            owner_id,
                            name,
                            area_type,
                            facility_type,
                            resolution_status,
                            is_public,
                            is_archived,
                            created_at,
                            updated_at,
                            version
                        )
                        VALUES (
                            :id,
                            :clientAreaId,
                            :ownerId,
                            :name,
                            'OTHER',
                            NULL,
                            'DRAFT',
                            false,
                            false,
                            :now,
                            :now,
                            0
                        )
                        """)
                .param("id", areaId)
                .param("clientAreaId", clientAreaId)
                .param("ownerId", userId)
                .param("name", areaName)
                .param("now", now)
                .update();

        return new ResolvedArea(areaId, areaName);
    }

    record ResolvedArea(
            UUID id,
            String name
    ) {
    }
}
