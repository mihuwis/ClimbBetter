package pl.climbbetter.identity.internal;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.climbbetter.identity.CurrentUserProvider;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
class IdentityConfiguration {
    @Bean
    CurrentUserProvider currentUserProvider(Environment environment) {
        if (AuthenticationMode.from(environment) == AuthenticationMode.STUB) {
            return new LocalCurrentUserProvider(UUID.fromString(environment.getProperty("climbbetter.local.user-id",
                    "00000000-0000-0000-0000-000000000001")));
        }
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof IdentityPrincipal principal)) {
                throw new IdentityAccessException(401, "AUTH_REQUIRED", "Please sign in.");
            }
            if (principal.userId() == null) {
                throw new IdentityAccessException(409, "ACCOUNT_NOT_PROVISIONED", "Create your ClimbBetter profile first.");
            }
            return principal.userId();
        };
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${climbbetter.auth.mode:firebase}' != 'stub'")
    FirebaseTokenVerifier firebaseTokenVerifier(Environment environment) throws IOException {
        AuthenticationMode mode = AuthenticationMode.from(environment);
        String projectId = environment.getProperty("climbbetter.auth.project-id", "").strip();
        if (projectId.isEmpty()) throw new IllegalStateException("Set CB_FIREBASE_PROJECT_ID for Firebase authentication");
        String emulator = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        GoogleCredentials credentials;
        if (mode == AuthenticationMode.EMULATOR) {
            if (!projectId.startsWith("demo-")) throw new IllegalStateException("Emulator mode requires a demo- project ID");
            if (emulator == null) throw new IllegalStateException("Set FIREBASE_AUTH_EMULATOR_HOST for emulator mode");
            URI uri = URI.create("http://" + emulator);
            if (!("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))
                    || uri.getPort() < 1 || uri.getUserInfo() != null || !uri.getPath().isEmpty()
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalStateException("Auth Emulator must use a local host:port");
            }
            credentials = GoogleCredentials.create(new AccessToken("local-emulator", new Date(Long.MAX_VALUE)));
        } else {
            if (emulator != null && !emulator.isBlank()) throw new IllegalStateException("Remove FIREBASE_AUTH_EMULATOR_HOST in firebase mode");
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirebaseOptions options = FirebaseOptions.builder().setProjectId(projectId).setCredentials(credentials).build();
        return new FirebaseAdminTokenVerifier(FirebaseApp.initializeApp(options, "climbbetter-" + UUID.randomUUID()), projectId);
    }
}
