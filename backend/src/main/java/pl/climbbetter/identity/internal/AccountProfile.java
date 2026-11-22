package pl.climbbetter.identity.internal;

import java.util.UUID;

record AccountProfile(UUID userId, String displayName, boolean profileComplete) { }
