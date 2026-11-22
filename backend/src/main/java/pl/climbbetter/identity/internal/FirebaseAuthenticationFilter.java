package pl.climbbetter.identity.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseTokenVerifier verifier;
    private final IdentityAccountService accounts;

    FirebaseAuthenticationFilter(FirebaseTokenVerifier verifier, IdentityAccountService accounts) {
        this.verifier = verifier;
        this.accounts = accounts;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !path(request).startsWith("/api/v1/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7) || header.substring(7).isBlank()) {
                throw new IdentityAccessException(401, "AUTH_REQUIRED", "Please sign in.");
            }
            VerifiedFirebaseIdentity identity = verifier.verify(header.substring(7));
            AccountProfile profile = accounts.find(identity).orElse(null);
            String path = path(request);
            boolean bootstrap = path.equals("/api/v1/identity/bootstrap") && request.getMethod().equals("POST");
            if (profile == null && !bootstrap) {
                throw new IdentityAccessException(409, "ACCOUNT_NOT_PROVISIONED", "Create your ClimbBetter profile first.");
            }
            if (profile != null && !profile.profileComplete() && !path.startsWith("/api/v1/identity/")) {
                throw new IdentityAccessException(403, "PROFILE_INCOMPLETE", "Please complete your profile.");
            }
            IdentityPrincipal principal = new IdentityPrincipal(identity, profile == null ? null : profile.userId(),
                    profile != null && profile.profileComplete());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
            SecurityContextHolder.setContext(context);
        } catch (IdentityAccessException error) {
            IdentityErrorWriter.write(response, error);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
