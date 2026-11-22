package pl.climbbetter.identity.internal;

record VerifiedFirebaseIdentity(String issuer, String subject, String displayName) { }
