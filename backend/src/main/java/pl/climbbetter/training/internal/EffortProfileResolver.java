package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
class EffortProfileResolver {

    private final JdbcClient jdbcClient;

    EffortProfileResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ResolvedEffortProfile resolve(String profileCode) {
        if (profileCode == null || profileCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Effort profile code is required"
            );
        }

        return jdbcClient.sql("""
                        SELECT
                            id,
                            code,
                            display_name,
                            base_edl
                        FROM training.effort_profiles
                        WHERE code = :profileCode
                          AND is_active = true
                        """)
                .param("profileCode", profileCode)
                .query((resultSet, rowNumber) ->
                        new ResolvedEffortProfile(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("code"),
                                resultSet.getString("display_name"),
                                resultSet.getBigDecimal("base_edl")
                        )
                )
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown active effort profile: " + profileCode
                ));
    }

    record ResolvedEffortProfile(
            UUID id,
            String code,
            String displayName,
            BigDecimal baseEdl
    ) {
    }
}