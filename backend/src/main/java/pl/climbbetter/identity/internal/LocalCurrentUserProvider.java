package pl.climbbetter.identity.internal;


import pl.climbbetter.identity.CurrentUserProvider;

import java.util.UUID;

class LocalCurrentUserProvider implements CurrentUserProvider {
    private final UUID userId;

    LocalCurrentUserProvider(UUID userId) {
        this.userId = userId;
    }

    @Override
    public UUID currentUserId() {
        return userId;
    }
}
