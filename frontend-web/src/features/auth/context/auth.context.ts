import { createContext } from "react";
import type { CurrentUser } from "../../profile/model/profile.types";

export type AuthStatus = "checking" | "signedOut" | "profileRequired" | "ready" | "linkRequired" | "error";
export type AuthState = { status: AuthStatus; profile: CurrentUser | null; error: string | null; generation: number };
export type AuthContextValue = AuthState & {
  retry: () => Promise<void>;
  logout: () => Promise<void>;
  completeProfile: (displayName: string) => Promise<void>;
};
export const AuthContext = createContext<AuthContextValue | null>(null);
