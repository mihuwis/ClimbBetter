package pl.climbbetter.identity.internal;


import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.climbbetter.identity.CurrentUserProvider;

import java.util.UUID;

@Component
@Profile({"local", "test"})
class LocalCurrentUserProvider implements CurrentUserProvider{
    private static final UUID LOCAL_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID currentUserId() {
        return LOCAL_USER_ID;
    }
}
