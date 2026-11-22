package pl.climbbetter.identity.internal;

import java.util.UUID;

record IdentityPrincipal(VerifiedFirebaseIdentity identity, UUID userId, boolean profileComplete) { }
