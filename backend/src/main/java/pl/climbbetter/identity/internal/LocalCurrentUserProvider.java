package pl.climbbetter.identity.internal;


import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.climbbetter.identity.CurrentUserProvider;

import java.util.UUID;

@Component
@Profile({"local", "test"})
class LocalCurrentUserProvider implements CurrentUserProvider {
    private final UUID userId;

    LocalCurrentUserProvider(
            @Value("${climbbetter.local.user-id:00000000-0000-0000-0000-000000000001}")
            UUID userId
    ) {
        this.userId = userId;
    }

    @Override
    public UUID currentUserId() {
        return userId;
    }
}
