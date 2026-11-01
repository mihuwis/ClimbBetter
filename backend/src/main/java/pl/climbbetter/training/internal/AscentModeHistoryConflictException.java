package pl.climbbetter.training.internal;

import java.util.UUID;

public final class AscentModeHistoryConflictException
        extends RuntimeException {

    private final UUID clientEntryId;
    private final String ascentMode;

    AscentModeHistoryConflictException(
            UUID clientEntryId,
            String ascentMode
    ) {
        super(
                "Ascent mode "
                        + ascentMode
                        + " conflicts with recorded climb history"
        );
        this.clientEntryId = clientEntryId;
        this.ascentMode = ascentMode;
    }

    public UUID clientEntryId() {
        return clientEntryId;
    }

    public String ascentMode() {
        return ascentMode;
    }
}