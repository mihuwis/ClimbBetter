package pl.climbbetter.identity;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();
}
