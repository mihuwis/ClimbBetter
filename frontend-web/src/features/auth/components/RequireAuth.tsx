import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { AuthStatus } from "./AuthStatus";

export function RequireAuth({ allowIncomplete = false }: { allowIncomplete?: boolean }) {
  const auth = useAuth();
  const location = useLocation();
  if (auth.status === "signedOut") return <Navigate to="/login" replace state={{ from: location.pathname + location.search + location.hash }} />;
  if (auth.status === "profileRequired" && !allowIncomplete) return <Navigate to="/profile/setup" replace />;
  if (auth.status !== "ready" && !(allowIncomplete && auth.status === "profileRequired")) return <AuthStatus />;
  return <Outlet key={`${auth.generation}:${auth.profile?.userId}`} />;
}
