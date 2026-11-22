package pl.climbbetter.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationModeTest {
    @Test
    void defaultsToFirebaseAndRefusesStubOrEmulatorOutsideLocalTests() {
        assertEquals(AuthenticationMode.FIREBASE, AuthenticationMode.from(new MockEnvironment()));
        for (String mode : new String[]{"stub", "emulator", "anything"}) {
            assertThrows(IllegalStateException.class, () -> AuthenticationMode.from(new MockEnvironment()
                    .withProperty("climbbetter.auth.mode", mode)));
        }
        var local = new MockEnvironment().withProperty("climbbetter.auth.mode", "stub");
        local.setActiveProfiles("local");
        assertEquals(AuthenticationMode.STUB, AuthenticationMode.from(local));
    }
}
