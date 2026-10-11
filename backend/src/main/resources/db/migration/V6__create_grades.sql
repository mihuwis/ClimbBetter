CREATE TABLE catalog.grades (
    id UUID PRIMARY KEY,
    scale_code TEXT NOT NULL,
    label TEXT NOT NULL,
    grade_index INTEGER NOT NULL,
    grade_points NUMERIC(8, 4) NOT NULL,
    calibration_version INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT grades_scale_code_valid
        CHECK (scale_code IN ('ROUTE_FRENCH', 'BOULDER_FONT')),

    CONSTRAINT grades_label_not_blank
        CHECK (length(trim(label)) > 0),

    CONSTRAINT grades_index_non_negative
        CHECK (grade_index >= 0),

    CONSTRAINT grades_points_positive
        CHECK (grade_points > 0),

    CONSTRAINT grades_calibration_version_positive
        CHECK (calibration_version > 0),

    CONSTRAINT grades_scale_label_unique
        UNIQUE (scale_code, label),

    CONSTRAINT grades_scale_index_unique
        UNIQUE (scale_code, grade_index)
);

WITH grade_seed (
    scale_code,
    label,
    grade_points,
    grade_index
) AS (
    VALUES
        ('ROUTE_FRENCH', '1',   10.0,  0),
        ('ROUTE_FRENCH', '2',   20.0,  1),
        ('ROUTE_FRENCH', '3',   25.0,  2),
        ('ROUTE_FRENCH', '4a',  30.0,  3),
        ('ROUTE_FRENCH', '4b',  35.0,  4),
        ('ROUTE_FRENCH', '4c',  40.0,  5),
        ('ROUTE_FRENCH', '5a',  45.0,  6),
        ('ROUTE_FRENCH', '5b',  50.0,  7),
        ('ROUTE_FRENCH', '5c',  55.0,  8),
        ('ROUTE_FRENCH', '6a',  60.0,  9),
        ('ROUTE_FRENCH', '6a+', 65.0, 10),
        ('ROUTE_FRENCH', '6b',  70.0, 11),
        ('ROUTE_FRENCH', '6b+', 75.0, 12),
        ('ROUTE_FRENCH', '6c',  80.0, 13),
        ('ROUTE_FRENCH', '6c+', 85.0, 14),
        ('ROUTE_FRENCH', '7a',  90.0, 15),
        ('ROUTE_FRENCH', '7a+', 95.0, 16),
        ('ROUTE_FRENCH', '7b', 100.0, 17),
        ('ROUTE_FRENCH', '7b+',105.0, 18),
        ('ROUTE_FRENCH', '7c', 110.0, 19),
        ('ROUTE_FRENCH', '7c+',115.0, 20),
        ('ROUTE_FRENCH', '8a', 120.0, 21),
        ('ROUTE_FRENCH', '8a+',125.0, 22),
        ('ROUTE_FRENCH', '8b', 130.0, 23),
        ('ROUTE_FRENCH', '8b+',135.0, 24),
        ('ROUTE_FRENCH', '8c', 140.0, 25),
        ('ROUTE_FRENCH', '8c+',145.0, 26),
        ('ROUTE_FRENCH', '9a', 150.0, 27),
        ('ROUTE_FRENCH', '9a+',155.0, 28),
        ('ROUTE_FRENCH', '9b', 160.0, 29),
        ('ROUTE_FRENCH', '9b+',165.0, 30),
        ('ROUTE_FRENCH', '9c', 170.0, 31),

        ('BOULDER_FONT', '2',   25.0,  0),
        ('BOULDER_FONT', '3',   40.0,  1),
        ('BOULDER_FONT', '4-',  55.0,  2),
        ('BOULDER_FONT', '4',   62.5,  3),
        ('BOULDER_FONT', '4+',  67.5,  4),
        ('BOULDER_FONT', '5',   72.5,  5),
        ('BOULDER_FONT', '5+',  77.5,  6),
        ('BOULDER_FONT', '6A',  85.0,  7),
        ('BOULDER_FONT', '6A+', 90.0,  8),
        ('BOULDER_FONT', '6B',  92.5,  9),
        ('BOULDER_FONT', '6B+', 95.0, 10),
        ('BOULDER_FONT', '6C',  97.5, 11),
        ('BOULDER_FONT', '6C+',100.0, 12),
        ('BOULDER_FONT', '7A', 105.0, 13),
        ('BOULDER_FONT', '7A+',107.5, 14),
        ('BOULDER_FONT', '7B', 112.5, 15),
        ('BOULDER_FONT', '7B+',117.5, 16),
        ('BOULDER_FONT', '7C', 122.5, 17),
        ('BOULDER_FONT', '7C+',130.0, 18),
        ('BOULDER_FONT', '8A', 135.0, 19),
        ('BOULDER_FONT', '8A+',140.0, 20),
        ('BOULDER_FONT', '8B', 145.0, 21),
        ('BOULDER_FONT', '8B+',150.0, 22),
        ('BOULDER_FONT', '8C', 155.0, 23),
        ('BOULDER_FONT', '8C+',160.0, 24),
        ('BOULDER_FONT', '9A', 165.0, 25),
        ('BOULDER_FONT', '9A+',170.0, 26)
)
INSERT INTO catalog.grades (
    id,
    scale_code,
    label,
    grade_points,
    grade_index,
    calibration_version
)
SELECT
    md5(scale_code || ':' || label)::UUID,
    scale_code,
    label,
    grade_points,
    grade_index,
    1
FROM grade_seed;