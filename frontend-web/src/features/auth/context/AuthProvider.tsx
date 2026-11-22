import { useCallback, useEffect, useState, type ReactNode } from "react";
import { onIdTokenChanged } from "firebase/auth";
import { ApiError } from "../../../shared/api/http";
import { advanceAuthSession, assertAuthSession, authSession } from "../../../shared/api/auth-session";
import { authMode, existingAccountOnly, firebaseAuth, firebaseSetupError } from "../../../shared/lib/firebase";
import { bootstrapProfile, getProfile, saveProfile } from "../../profile/api/profile";
import { signOutUser } from "../services/auth.service";
import { AuthContext, type AuthState } from "./auth.context";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    status: firebaseSetupError ? "error" : "checking", profile: null,
    error: firebaseSetupError, generation: authSession().generation,
  });

  const load = useCallback(async () => {
    const generation = authSession().generation;
    try {
      const profile = authMode === "stub" || existingAccountOnly ? await getProfile() : await bootstrapProfile();
      assertAuthSession(generation);
      // Existing business fixtures use the explicit local stub and predate onboarding.
      if (authMode === "stub") profile.profileComplete = true;
      setState({ status: profile.profileComplete ? "ready" : "profileRequired", profile, error: null, generation });
    } catch (error) {
      if (authSession().generation !== generation) return;
      const linkRequired = existingAccountOnly && error instanceof ApiError && error.code === "ACCOUNT_NOT_PROVISIONED";
      setState({ status: linkRequired ? "linkRequired" : "error", profile: null,
        error: linkRequired ? null : "Could not load your journal. Please retry.", generation });
    }
  }, []);

  useEffect(() => {
    if (firebaseSetupError) return;
    if (authMode === "stub") {
      let active = true;
      queueMicrotask(() => { if (active) void load(); });
      return () => { active = false; advanceAuthSession(); };
    }
    if (!firebaseAuth) return;
    let lastUid: string | null | undefined;
    const unsubscribe = onIdTokenChanged(firebaseAuth, user => {
      // Token renewal for the same user must not abort its own HTTP retry.
      if (lastUid === (user?.uid ?? null)) return;
      lastUid = user?.uid ?? null;
      advanceAuthSession();
      const generation = authSession().generation;
      setState({ status: user ? "checking" : "signedOut", profile: null, error: null, generation });
      if (user) void load();
    });
    const expired = () => { void signOutUser(); };
    window.addEventListener("climbbetter:auth-expired", expired);
    return () => {
      unsubscribe();
      window.removeEventListener("climbbetter:auth-expired", expired);
      advanceAuthSession();
    };
  }, [load]);

  const logout = useCallback(async () => {
    advanceAuthSession();
    setState({ status: "signedOut", profile: null, error: null, generation: authSession().generation });
    if (authMode !== "stub") await signOutUser();
  }, []);

  const completeProfile = useCallback(async (displayName: string) => {
    const generation = authSession().generation;
    const profile = await saveProfile(displayName);
    assertAuthSession(generation);
    setState({ status: "ready", profile, error: null, generation });
  }, []);

  return <AuthContext.Provider value={{ ...state, retry: load, logout, completeProfile }}>{children}</AuthContext.Provider>;
}
