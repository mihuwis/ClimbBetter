package pl.climbbetter.reporting.internal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

record DashboardSessionCursor(LocalDate date, Instant createdAt, UUID id) {
    static DashboardSessionCursor decode(String value) {
        if (value == null) return null;
        try {
            if (value.length() > 256) throw new IllegalArgumentException();
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 3) throw new IllegalArgumentException();
            return new DashboardSessionCursor(LocalDate.parse(parts[0]), Instant.parse(parts[1]), UUID.fromString(parts[2]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid session cursor");
        }
    }

    String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (date + "|" + createdAt + "|" + id).getBytes(StandardCharsets.UTF_8));
    }
}
