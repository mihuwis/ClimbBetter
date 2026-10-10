package pl.climbbetter.training.internal.model;

import java.util.Objects;

record EntryClassification(
        ClimbType climbType,
        ResultType resultType,
        AscentMode ascentMode,
        ProtectionMode protectionMode
) {
     EntryClassification {
        Objects.requireNonNull(climbType, "Climb type is required");
        Objects.requireNonNull(resultType, "Result type is required");

        if (resultType == ResultType.WARMUP) {
            throw new IllegalArgumentException(
                    "Warm-up uses a separate entry model"
            );
        }

        if (ascentMode == null) {
            throw new IllegalArgumentException(
                    "Ascent mode is required"
            );
        }

        if (climbType == ClimbType.ROUTE && protectionMode == null) {
            throw new IllegalArgumentException(
                    "Protection mode is required for a route"
            );
        }

        if (climbType != ClimbType.ROUTE && protectionMode != null) {
            throw new IllegalArgumentException(
                    "Protection mode is allowed only for a route"
            );
        }
    }

}
