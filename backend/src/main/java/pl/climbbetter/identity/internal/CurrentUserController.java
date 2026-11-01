package pl.climbbetter.identity.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.identity.CurrentUserProvider;

import java.util.UUID;

@RestController
class CurrentUserController {
    private final CurrentUserProvider currentUserProvider;
    private final JdbcClient jdbcClient;

    CurrentUserController(CurrentUserProvider currentUserProvider, JdbcClient jdbcClient) {
        this.currentUserProvider = currentUserProvider;
        this.jdbcClient = jdbcClient;
    }

    @GetMapping("/api/v1/identity/me")
    CurrentUserResponse currentUser() {
        return jdbcClient.sql("SELECT id AS user_id, display_name FROM identity.users WHERE id = :id")
                .param("id", currentUserProvider.currentUserId()).query(CurrentUserResponse.class).single();
    }

    record CurrentUserResponse(UUID userId, String displayName) { }
}
