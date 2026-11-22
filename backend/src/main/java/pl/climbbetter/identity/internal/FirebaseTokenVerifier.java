package pl.climbbetter.identity.internal;

interface FirebaseTokenVerifier extends AutoCloseable {
    VerifiedFirebaseIdentity verify(String token);
    @Override
    default void close() { }
}
