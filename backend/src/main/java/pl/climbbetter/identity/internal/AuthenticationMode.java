package pl.climbbetter.identity.internal;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.util.Locale;

enum AuthenticationMode {
    FIREBASE, EMULATOR, STUB;

    static AuthenticationMode from(Environment environment) {
        AuthenticationMode mode;
        try {
            mode = valueOf(environment.getProperty("climbbetter.auth.mode", "firebase").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("CB_AUTH_MODE must be firebase, emulator or stub", exception);
        }
        if (mode != FIREBASE && !environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw new IllegalStateException("Emulator and stub authentication are available only in local/test profiles");
        }
        return mode;
    }
}
