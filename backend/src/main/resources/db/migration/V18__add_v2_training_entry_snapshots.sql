-- Crux profiles use the intensity index of their continuous base grade.
UPDATE training.effort_profiles
SET profile_intensity_index = 7
WHERE code = 'MEDIUM_CRUX';

UPDATE training.effort_profiles
SET profile_intensity_index = 5
WHERE code = 'LONG_CRUX';

-- Historical v1 entries retain their EDL snapshots and receive no new values.
ALTER TABLE training.training_entries
    ADD COLUMN profile_intensity_index NUMERIC(8, 4),
    ADD COLUMN hard_move_count INTEGER,
    ADD COLUMN easy_move_count INTEGER,
    ADD COLUMN hard_move_intensity NUMERIC(16, 10),
    ADD COLUMN easy_move_intensity NUMERIC(16, 10),
    ADD COLUMN base_move_grade_label TEXT,
    ADD COLUMN hard_move_grade_label TEXT,
    ADD COLUMN easy_move_grade_label TEXT;

ALTER TABLE training.training_entries
    DROP CONSTRAINT training_entries_variant_valid,

    ADD CONSTRAINT training_entries_calculation_model_version_valid
        CHECK (calculation_model_version IN ('v1', 'v2')),

    ADD CONSTRAINT training_entries_variant_valid
        CHECK (
            (
                (
                    result_type = 'WARMUP'
                    AND executed_moves > 0
                    AND climb_type IS NULL
                    AND ascent_mode IS NULL
                    AND protection_mode IS NULL
                    AND climb_name_snapshot IS NULL
                    AND grade_label IS NULL
                    AND grade_points IS NULL
                    AND base_edl IS NULL
                    AND total_moves IS NULL
                    AND edl_count IS NULL
                    AND profile_intensity_index IS NULL
                    AND move_intensity IS NULL
                    AND style_multiplier IS NULL
                    AND relative_effort_multiplier IS NULL
                    AND classic_load = 0
                    AND adjusted_load = 0
                )
                OR
                (
                    result_type IN ('ASCENT', 'ATTEMPT')
                    AND climb_type IS NOT NULL
                    AND ascent_mode IS NOT NULL
                    AND climb_name_snapshot IS NOT NULL
                    AND length(trim(climb_name_snapshot)) > 0
                    AND grade_label IS NOT NULL
                    AND length(trim(grade_label)) > 0
                    AND grade_points IS NOT NULL
                    AND grade_points > 0
                    AND total_moves IS NOT NULL
                    AND total_moves > 0
                    AND move_intensity IS NOT NULL
                    AND move_intensity > 0
                    AND style_multiplier IS NOT NULL
                    AND style_multiplier >= 0
                    AND relative_effort_multiplier IS NOT NULL
                    AND relative_effort_multiplier >= 0
                    AND (
                        (
                            climb_type = 'ROUTE'
                            AND protection_mode IS NOT NULL
                        )
                        OR
                        (
                            climb_type <> 'ROUTE'
                            AND protection_mode IS NULL
                        )
                    )
                    AND (
                        (
                            calculation_model_version = 'v1'
                            AND base_edl IS NOT NULL
                            AND base_edl >= 0
                            AND edl_count IS NOT NULL
                            AND edl_count > 0
                            AND profile_intensity_index IS NULL
                        )
                        OR
                        (
                            calculation_model_version = 'v2'
                            AND base_edl IS NULL
                            AND edl_count IS NULL
                            AND profile_intensity_index IS NOT NULL
                            AND profile_intensity_index > 0
                        )
                    )
                )
            ) IS TRUE
        ),

    ADD CONSTRAINT training_entries_crux_snapshot_valid
        CHECK (
            (
                (
                    (
                        calculation_model_version = 'v1'
                        OR result_type = 'WARMUP'
                        OR (
                            calculation_model_version = 'v2'
                            AND result_type IN ('ASCENT', 'ATTEMPT')
                            AND effort_profile_code IS NOT NULL
                            AND effort_profile_code NOT IN (
                                'MEDIUM_CRUX', 'LONG_CRUX'
                            )
                        )
                    )
                    AND hard_move_count IS NULL
                    AND easy_move_count IS NULL
                    AND hard_move_intensity IS NULL
                    AND easy_move_intensity IS NULL
                    AND base_move_grade_label IS NULL
                    AND hard_move_grade_label IS NULL
                    AND easy_move_grade_label IS NULL
                )
                OR
                (
                    calculation_model_version = 'v2'
                    AND result_type IN ('ASCENT', 'ATTEMPT')
                    AND effort_profile_code IS NOT NULL
                    AND effort_profile_code IN ('MEDIUM_CRUX', 'LONG_CRUX')
                    AND hard_move_count IS NOT NULL
                    AND hard_move_count >= 0
                    AND easy_move_count IS NOT NULL
                    AND easy_move_count >= 0
                    AND hard_move_count::BIGINT + easy_move_count::BIGINT
                        = executed_moves::BIGINT
                    AND hard_move_count
                        = ceil(executed_moves::NUMERIC * 0.15)
                    AND hard_move_intensity IS NOT NULL
                    AND hard_move_intensity > 0
                    AND easy_move_intensity IS NOT NULL
                    AND easy_move_intensity > 0
                    AND base_move_grade_label IS NOT NULL
                    AND length(trim(base_move_grade_label)) > 0
                    AND hard_move_grade_label IS NOT NULL
                    AND length(trim(hard_move_grade_label)) > 0
                    AND easy_move_grade_label IS NOT NULL
                    AND length(trim(easy_move_grade_label)) > 0
                )
            ) IS TRUE
        );
