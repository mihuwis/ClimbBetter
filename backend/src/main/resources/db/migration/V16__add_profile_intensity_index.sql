ALTER TABLE training.effort_profiles
    ADD COLUMN profile_intensity_index NUMERIC(8, 4),
    ADD CONSTRAINT effort_profiles_intensity_index_positive
        CHECK (profile_intensity_index > 0);

UPDATE training.effort_profiles
SET profile_intensity_index = 10
WHERE code IN ('BOULDER', 'SHORT_BOULDERY_ROUTE');

UPDATE training.effort_profiles
SET profile_intensity_index = 7
WHERE code = 'MEDIUM_CONTINUOUS';

UPDATE training.effort_profiles
SET profile_intensity_index = 5
WHERE code = 'LONG_CONTINUOUS';