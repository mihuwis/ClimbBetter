import { useContext } from "react";
import { AuthContext } from "../context/auth.context";

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("AuthProvider is required.");
  return context;
}

export function useCurrentUser() {
  const auth = useAuth();
  return { data: auth.profile, loading: auth.status === "checking", error: auth.error, refresh: auth.retry };
}
