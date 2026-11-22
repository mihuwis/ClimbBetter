package pl.climbbetter.identity.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
class IdentityAccountService {
    private final JdbcClient jdbc;
    IdentityAccountService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    Optional<AccountProfile> find(VerifiedFirebaseIdentity identity) {
        return jdbc.sql("""
                SELECT u.id AS user_id, u.display_name, u.profile_completed_at IS NOT NULL AS profile_complete
                FROM identity.user_identities i JOIN identity.users u ON u.id = i.user_id
                WHERE i.issuer = :issuer AND i.subject = :subject
                """).param("issuer", identity.issuer()).param("subject", identity.subject())
                .query(AccountProfile.class).optional();
    }

    @Transactional(readOnly = true)
    AccountProfile get(UUID userId) {
        return jdbc.sql("""
                SELECT id AS user_id, display_name, profile_completed_at IS NOT NULL AS profile_complete
                FROM identity.users WHERE id = :id
                """).param("id", userId).query(AccountProfile.class).optional()
                .orElseThrow(() -> new IdentityAccessException(409, "ACCOUNT_NOT_PROVISIONED", "Create your ClimbBetter profile first."));
    }

    @Transactional
    AccountProfile bootstrap(VerifiedFirebaseIdentity identity) {
        // Serialize this identity's creation across concurrent requests and application instances.
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:identity, 0))")
                .param("identity", identity.issuer() + "|" + identity.subject())
                .query((rs, row) -> 0).single();
        Optional<AccountProfile> existing = find(identity);
        if (existing.isPresent()) return existing.get();
        UUID userId = UUID.randomUUID();
        String name = identity.displayName();
        if (name == null || name.isBlank()) name = "New climber";
        name = name.strip();
        if (name.length() > 80) name = name.substring(0, 80);
        jdbc.sql("INSERT INTO identity.users(id, display_name, created_at) VALUES (:id, :name, now())")
                .param("id", userId).param("name", name).update();
        jdbc.sql("""
                INSERT INTO identity.user_identities(id, user_id, issuer, subject, created_at)
                VALUES (:id, :userId, :issuer, :subject, now())
                """).param("id", UUID.randomUUID()).param("userId", userId)
                .param("issuer", identity.issuer()).param("subject", identity.subject()).update();
        return get(userId);
    }

    @Transactional
    AccountProfile update(UUID userId, String displayName) {
        String name = displayName == null ? "" : displayName.strip();
        if (name.isBlank() || name.length() > 80) {
            throw new IdentityAccessException(400, "INVALID_DISPLAY_NAME", "Name must contain between 1 and 80 characters.");
        }
        int changed = jdbc.sql("""
                UPDATE identity.users SET display_name = :name,
                    profile_completed_at = COALESCE(profile_completed_at, now()) WHERE id = :id
                """).param("id", userId).param("name", name).update();
        if (changed != 1) throw new IdentityAccessException(409, "ACCOUNT_NOT_PROVISIONED", "Create your ClimbBetter profile first.");
        return get(userId);
    }
}
