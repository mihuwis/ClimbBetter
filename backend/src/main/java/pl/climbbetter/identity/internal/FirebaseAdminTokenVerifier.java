package pl.climbbetter.identity.internal;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {
    private final FirebaseApp app;
    private final FirebaseAuth auth;
    private final String issuer;

    FirebaseAdminTokenVerifier(FirebaseApp app, String projectId) {
        this.app = app;
        this.auth = FirebaseAuth.getInstance(app);
        this.issuer = "https://securetoken.google.com/" + projectId;
    }

    @Override
    public VerifiedFirebaseIdentity verify(String token) {
        try {
            FirebaseToken decoded = auth.verifyIdToken(token, true);
            return new VerifiedFirebaseIdentity(issuer, decoded.getUid(), decoded.getName());
        } catch (FirebaseAuthException exception) {
            AuthErrorCode code = exception.getAuthErrorCode();
            if (code == AuthErrorCode.INVALID_ID_TOKEN || code == AuthErrorCode.EXPIRED_ID_TOKEN
                    || code == AuthErrorCode.REVOKED_ID_TOKEN || code == AuthErrorCode.USER_DISABLED
                    || code == AuthErrorCode.USER_NOT_FOUND) {
                throw new IdentityAccessException(401, "INVALID_TOKEN", "Please sign in again.");
            }
            throw new IdentityAccessException(503, "AUTH_UNAVAILABLE", "Sign-in verification is temporarily unavailable. Please retry.");
        } catch (IllegalArgumentException exception) {
            throw new IdentityAccessException(401, "INVALID_TOKEN", "Please sign in again.");
        }
    }
    @Override
    public void close() { app.delete(); }
}
