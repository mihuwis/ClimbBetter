CREATE TABLE training.effort_profiles (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    description TEXT NOT NULL,
    base_edl NUMERIC(8, 4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT effort_profiles_code_not_blank
        CHECK (length(trim(code)) > 0),

    CONSTRAINT effort_profiles_display_name_not_blank
        CHECK (length(trim(display_name)) > 0),

    CONSTRAINT effort_profiles_description_not_blank
        CHECK (length(trim(description)) > 0),

    CONSTRAINT effort_profiles_base_edl_positive
        CHECK (base_edl > 0),

    CONSTRAINT effort_profiles_code_unique
        UNIQUE (code)
);

WITH effort_profile_seed (
    code,
    display_name,
    description,
    base_edl
) AS (
    VALUES
        (
            'BOULDER',
            'Boulder',
            'Classic boulder, usually 4-10 moves',
            4.0
        ),
        (
            'SHORT_BOULDERY_ROUTE',
            'Short bouldery route',
            'Short route with a bouldery character, usually 4-12 moves',
            8.0
        ),
        (
            'SHORT_CONTINUOUS_ROUTE',
            'Short continuous route',
            'Short route with continuous climbing, usually 8-15 moves',
            10.0
        ),
        (
            'MEDIUM_CONTINUOUS',
            'Medium continuous',
            'Medium route without a distinct crux, usually 15-40 moves',
            28.0
        ),
        (
            'MEDIUM_CRUX',
            'Medium crux',
            'Medium route with a distinct crux and easier sections',
            15.0
        ),
        (
            'LONG_CONTINUOUS',
            'Long continuous',
            'Long route with continuous climbing',
            40.0
        ),
        (
            'LONG_CRUX',
            'Long crux',
            'Long route with a crux and rest positions',
            22.0
        ),
        (
            'CIRCUIT',
            'Circuit',
            'Training circuit on a climbing wall',
            20.0
        )
)
INSERT INTO training.effort_profiles (
    id,
    code,
    display_name,
    description,
    base_edl
)
SELECT
    md5('EFFORT_PROFILE:' || code)::UUID,
    code,
    display_name,
    description,
    base_edl
FROM effort_profile_seed;