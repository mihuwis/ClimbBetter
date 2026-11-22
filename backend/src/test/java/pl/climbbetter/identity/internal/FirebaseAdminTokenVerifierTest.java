package pl.climbbetter.identity.internal;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FirebaseAdminTokenVerifierTest {
    @Test
    void realAdminSdkRejectsMalformedTokenAndAnotherProjectsClaims() {
        FirebaseOptions options = FirebaseOptions.builder().setProjectId("climbbetter-auth-test")
                .setCredentials(GoogleCredentials.create(new AccessToken("unused-test-credential", new Date(Long.MAX_VALUE))))
                .build();
        try (var verifier = new FirebaseAdminTokenVerifier(FirebaseApp.initializeApp(options, UUID.randomUUID().toString()), "climbbetter-auth-test")) {
            IdentityAccessException malformed = assertThrows(IdentityAccessException.class, () -> verifier.verify("not-a-jwt"));
            assertEquals(401, malformed.status());
            long now = System.currentTimeMillis() / 1000;
            String wrongProject = encoded("{\"alg\":\"RS256\",\"kid\":\"test-key\"}") + "." + encoded("""
                    {"aud":"different-project","iss":"https://securetoken.google.com/different-project",
                     "sub":"someone","iat":%d,"exp":%d,"auth_time":%d}
                    """.formatted(now, now + 3600, now)) + "." + encoded("invalid-signature");
            IdentityAccessException wrong = assertThrows(IdentityAccessException.class, () -> verifier.verify(wrongProject));
            assertEquals(401, wrong.status());
        }
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
