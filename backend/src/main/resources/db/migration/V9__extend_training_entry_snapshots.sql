ALTER TABLE training.training_entries
    ADD COLUMN grade_id UUID
        REFERENCES catalog.grades(id),

    ADD COLUMN grade_scale_code TEXT,

    ADD COLUMN grade_index INTEGER,

    ADD COLUMN grade_calibration_version INTEGER,

    ADD COLUMN effort_profile_id UUID
        REFERENCES training.effort_profiles(id),

    ADD COLUMN effort_profile_code TEXT,

    ADD COLUMN effort_profile_name_snapshot TEXT,

    ADD COLUMN contact_count_before_two_years INTEGER,

    ADD COLUMN attempt_ordinal_all_time INTEGER,

    ADD COLUMN familiarity_band TEXT,

    ADD COLUMN fast_rp BOOLEAN,

    ADD COLUMN current_level_grade_label TEXT,

    ADD COLUMN current_level_index INTEGER,

    ADD COLUMN relative_grade_diff INTEGER,

    ADD COLUMN outside_calibrated_range BOOLEAN,

    ADD COLUMN notes TEXT;

ALTER TABLE training.training_entries
    ADD CONSTRAINT training_entries_grade_scale_valid
        CHECK (
            grade_scale_code IS NULL
            OR grade_scale_code IN (
                'ROUTE_FRENCH',
                'BOULDER_FONT'
            )
        ),

    ADD CONSTRAINT training_entries_grade_index_non_negative
        CHECK (
            grade_index IS NULL
            OR grade_index >= 0
        ),

    ADD CONSTRAINT training_entries_grade_calibration_positive
        CHECK (
            grade_calibration_version IS NULL
            OR grade_calibration_version > 0
        ),

    ADD CONSTRAINT training_entries_contact_count_non_negative
        CHECK (
            contact_count_before_two_years IS NULL
            OR contact_count_before_two_years >= 0
        ),

    ADD CONSTRAINT training_entries_attempt_ordinal_positive
        CHECK (
            attempt_ordinal_all_time IS NULL
            OR attempt_ordinal_all_time > 0
        ),

    ADD CONSTRAINT training_entries_familiarity_band_valid
        CHECK (
            familiarity_band IS NULL
            OR familiarity_band IN (
                'FIRST_CONTACT',
                'LOW',
                'NORMAL',
                'ESTABLISHED'
            )
        ),

    ADD CONSTRAINT training_entries_current_level_index_non_negative
        CHECK (
            current_level_index IS NULL
            OR current_level_index >= 0
        );

ALTER TABLE training.training_entries
    ADD CONSTRAINT training_entries_snapshot_by_variant
        CHECK (
            (
                result_type = 'WARMUP'
                AND grade_id IS NULL
                AND grade_scale_code IS NULL
                AND grade_index IS NULL
                AND grade_calibration_version IS NULL
                AND effort_profile_id IS NULL
                AND effort_profile_code IS NULL
                AND effort_profile_name_snapshot IS NULL
                AND contact_count_before_two_years IS NULL
                AND attempt_ordinal_all_time IS NULL
                AND familiarity_band IS NULL
                AND fast_rp IS NULL
                AND current_level_grade_label IS NULL
                AND current_level_index IS NULL
                AND relative_grade_diff IS NULL
                AND outside_calibrated_range IS NULL
            )
            OR
            (
                result_type IN ('ASCENT', 'ATTEMPT')
                AND grade_id IS NOT NULL
                AND grade_scale_code IS NOT NULL
                AND grade_index IS NOT NULL
                AND grade_calibration_version IS NOT NULL
                AND effort_profile_id IS NOT NULL
                AND effort_profile_code IS NOT NULL
                AND length(trim(effort_profile_code)) > 0
                AND effort_profile_name_snapshot IS NOT NULL
                AND length(trim(effort_profile_name_snapshot)) > 0
                AND contact_count_before_two_years IS NOT NULL
                AND attempt_ordinal_all_time IS NOT NULL
                AND familiarity_band IS NOT NULL
                AND fast_rp IS NOT NULL
                AND current_level_grade_label IS NOT NULL
                AND length(trim(current_level_grade_label)) > 0
                AND current_level_index IS NOT NULL
                AND relative_grade_diff IS NOT NULL
                AND outside_calibrated_range IS NOT NULL
            )
        );

CREATE INDEX training_entries_user_level_history_index
    ON training.training_entries (
        grade_scale_code,
        grade_index
    )
    WHERE result_type = 'ASCENT';