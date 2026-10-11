package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.training.internal.model.FamiliarityBand;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
class CalculationRuleResolver {

    private final JdbcClient jdbcClient;

    CalculationRuleResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ResolvedStyleRule resolveStyle(
            String resultType,
            String ascentMode,
            int priorContacts
    ) {
        FamiliarityBand recordedBand =
                FamiliarityBand.fromPriorContactCount(
                        priorContacts
                );

        FamiliarityBand effectiveBand = recordedBand;
        String familiaritySource;

        if (priorContacts == 0 && "RP".equals(ascentMode)) {
            effectiveBand = FamiliarityBand.LOW;
            familiaritySource = "DECLARED_PRIOR_KNOWLEDGE";
        } else if (priorContacts == 0) {
            familiaritySource = "NO_RECORDED_HISTORY";
        } else {
            familiaritySource = "RECORDED_HISTORY";
        }

        boolean historyConflict =
                priorContacts > 0
                        && (
                        "OS".equals(ascentMode)
                                || "FLASH".equals(ascentMode)
                );

        String familiaritySelector =
                "RP".equals(ascentMode)
                        ? effectiveBand.name()
                        : "ANY";

        FamiliarityBand finalEffectiveBand = effectiveBand;

        return jdbcClient.sql("""
                        SELECT
                            id,
                            code,
                            multiplier
                        FROM training.style_rules
                        WHERE result_type = :resultType
                          AND ascent_mode = :ascentMode
                          AND familiarity_selector = :familiaritySelector
                          AND is_active = true
                        """)
                .param("resultType", resultType)
                .param("ascentMode", ascentMode)
                .param(
                        "familiaritySelector",
                        familiaritySelector
                )
                .query((resultSet, rowNumber) ->
                        new ResolvedStyleRule(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getString("code"),
                                resultSet.getBigDecimal(
                                        "multiplier"
                                ),
                                finalEffectiveBand,
                                familiaritySource,
                                historyConflict
                        )
                )
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active style rule for "
                                + resultType
                                + ", "
                                + ascentMode
                                + ", "
                                + familiaritySelector
                ));
    }

    ResolvedRelativeEffort resolveRelativeEffort(
            int gradeIndex,
            int currentLevelIndex
    ) {
        if (gradeIndex < 0 || currentLevelIndex < 0) {
            throw new IllegalArgumentException(
                    "Grade indexes cannot be negative"
            );
        }

        int relativeGradeDifference =
                gradeIndex - currentLevelIndex;

        int ruleDifference = Math.max(
                -6,
                Math.min(5, relativeGradeDifference)
        );

        boolean outsideCalibratedRange =
                relativeGradeDifference < -6
                        || relativeGradeDifference > 5;

        return jdbcClient.sql("""
                        SELECT
                            id,
                            multiplier
                        FROM training.relative_effort_rules
                        WHERE grade_difference = :ruleDifference
                          AND is_active = true
                        """)
                .param("ruleDifference", ruleDifference)
                .query((resultSet, rowNumber) ->
                        new ResolvedRelativeEffort(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                relativeGradeDifference,
                                resultSet.getBigDecimal(
                                        "multiplier"
                                ),
                                outsideCalibratedRange
                        )
                )
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active relative effort rule for difference "
                                + ruleDifference
                ));
    }

    record ResolvedStyleRule(
            UUID id,
            String code,
            BigDecimal multiplier,
            FamiliarityBand familiarityBand,
            String familiaritySource,
            boolean historyConflict
    ) {
    }

    record ResolvedRelativeEffort(
            UUID id,
            int relativeGradeDifference,
            BigDecimal multiplier,
            boolean outsideCalibratedRange
    ) {
    }
}