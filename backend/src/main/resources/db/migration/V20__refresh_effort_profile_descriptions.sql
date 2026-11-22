-- Length ranges are approximate guidance; they do not select the effort profile automatically.
UPDATE training.effort_profiles AS profile
SET description = updated.description
FROM (
    VALUES
        (
            'BOULDER',
            'Classic boulder, usually 4-10 moves; may be shorter or slightly longer'
        ),
        (
            'SHORT_BOULDERY_ROUTE',
            'Short bouldery route, usually 4-12 moves; effectively a boulder with a rope'
        ),
        (
            'MEDIUM_CONTINUOUS',
            'Medium sustained route, usually 15-30 moves without a distinct crux'
        ),
        (
            'MEDIUM_CRUX',
            'Route around 12-20 metres with one or two crux sections, rests and easier sections'
        ),
        (
            'LONG_CONTINUOUS',
            'Long sustained climb, usually over 35-40 moves; may exceed 100 moves'
        ),
        (
            'LONG_CRUX',
            'Route longer than 25 metres with a crux, rest positions and substantial easier sections'
        ),
        (
            'CIRCUIT',
            'Training circuit on an indoor climbing wall'
        )
) AS updated(code, description)
WHERE profile.code = updated.code;
