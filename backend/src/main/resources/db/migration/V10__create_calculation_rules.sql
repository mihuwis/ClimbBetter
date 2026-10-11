CREATE TABLE training.style_rules (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL,
    result_type TEXT NOT NULL,
    ascent_mode TEXT NOT NULL,
    familiarity_selector TEXT NOT NULL,
    multiplier NUMERIC(8, 4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT style_rules_code_not_blank
        CHECK (length(trim(code)) > 0),

    CONSTRAINT style_rules_result_type_valid
        CHECK (result_type IN ('ASCENT', 'ATTEMPT')),

    CONSTRAINT style_rules_ascent_mode_valid
        CHECK (ascent_mode IN ('OS', 'FLASH', 'RP')),

    CONSTRAINT style_rules_familiarity_valid
        CHECK (
            familiarity_selector IN (
                'ANY',
                'LOW',
                'NORMAL',
                'ESTABLISHED'
            )
        ),

    CONSTRAINT style_rules_multiplier_positive
        CHECK (multiplier > 0),

    CONSTRAINT style_rules_code_unique
        UNIQUE (code),

    CONSTRAINT style_rules_combination_unique
        UNIQUE (
            result_type,
            ascent_mode,
            familiarity_selector
        )
);

WITH style_rule_seed (
    code,
    result_type,
    ascent_mode,
    familiarity_selector,
    multiplier
) AS (
    VALUES
        ('ASCENT_OS', 'ASCENT', 'OS', 'ANY', 1.50),
        ('ASCENT_FLASH', 'ASCENT', 'FLASH', 'ANY', 1.50),
        ('ATTEMPT_OS', 'ATTEMPT', 'OS', 'ANY', 1.50),
        ('ATTEMPT_FLASH', 'ATTEMPT', 'FLASH', 'ANY', 1.50),

        ('ASCENT_RP_LOW', 'ASCENT', 'RP', 'LOW', 1.30),
        ('ASCENT_RP_NORMAL', 'ASCENT', 'RP', 'NORMAL', 1.00),
        ('ASCENT_RP_ESTABLISHED', 'ASCENT', 'RP', 'ESTABLISHED', 0.75),

        ('ATTEMPT_RP_LOW', 'ATTEMPT', 'RP', 'LOW', 1.20),
        ('ATTEMPT_RP_NORMAL', 'ATTEMPT', 'RP', 'NORMAL', 0.90),
        ('ATTEMPT_RP_ESTABLISHED', 'ATTEMPT', 'RP', 'ESTABLISHED', 0.50)
)
INSERT INTO training.style_rules (
    id,
    code,
    result_type,
    ascent_mode,
    familiarity_selector,
    multiplier
)
SELECT
    md5('STYLE_RULE:' || code)::UUID,
    code,
    result_type,
    ascent_mode,
    familiarity_selector,
    multiplier
FROM style_rule_seed;

CREATE TABLE training.relative_effort_rules (
    id UUID PRIMARY KEY,
    grade_difference INTEGER NOT NULL,
    multiplier NUMERIC(8, 4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT relative_effort_difference_valid
        CHECK (grade_difference BETWEEN -6 AND 5),

    CONSTRAINT relative_effort_multiplier_positive
        CHECK (multiplier > 0),

    CONSTRAINT relative_effort_difference_unique
        UNIQUE (grade_difference)
);

WITH relative_effort_seed (
    grade_difference,
    multiplier
) AS (
    VALUES
        (-6, 0.20),
        (-5, 0.25),
        (-4, 0.35),
        (-3, 0.45),
        (-2, 0.55),
        (-1, 0.80),
        ( 0, 1.00),
        ( 1, 1.30),
        ( 2, 1.80),
        ( 3, 2.60),
        ( 4, 3.50),
        ( 5, 5.00)
)
INSERT INTO training.relative_effort_rules (
    id,
    grade_difference,
    multiplier
)
SELECT
    md5(
        'RELATIVE_EFFORT:'
        || grade_difference::TEXT
    )::UUID,
    grade_difference,
    multiplier
FROM relative_effort_seed;

ALTER TABLE training.training_entries
    ADD COLUMN familiarity_source TEXT,

    ADD COLUMN history_conflict_detected BOOLEAN,

    ADD COLUMN history_conflict_confirmed BOOLEAN;

ALTER TABLE training.training_entries
    ADD CONSTRAINT training_entries_familiarity_source_valid
        CHECK (
            familiarity_source IS NULL
            OR familiarity_source IN (
                'NO_RECORDED_HISTORY',
                'RECORDED_HISTORY',
                'DECLARED_PRIOR_KNOWLEDGE'
            )
        ),

    ADD CONSTRAINT training_entries_history_conflict_consistent
        CHECK (
            (
                history_conflict_detected IS NULL
                AND history_conflict_confirmed IS NULL
            )
            OR
            (
                history_conflict_detected IS NOT NULL
                AND history_conflict_confirmed IS NOT NULL
                AND history_conflict_detected
                    = history_conflict_confirmed
            )
        ),

    ADD CONSTRAINT training_entries_rule_context_by_variant
        CHECK (
            (
                result_type = 'WARMUP'
                AND familiarity_source IS NULL
                AND history_conflict_detected IS NULL
                AND history_conflict_confirmed IS NULL
            )
            OR
            (
                result_type IN ('ASCENT', 'ATTEMPT')
                AND familiarity_source IS NOT NULL
                AND history_conflict_detected IS NOT NULL
                AND history_conflict_confirmed IS NOT NULL
            )
        );