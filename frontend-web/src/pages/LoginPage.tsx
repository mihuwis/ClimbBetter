import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";
import { signInAsTestClimber, signInError, signInWithGoogle } from "../features/auth/services/auth.service";
import { testAccounts } from "../features/auth/model/test-accounts";
import { AuthStatus } from "../features/auth/components/AuthStatus";
import { authMode } from "../shared/lib/firebase";

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const from: unknown = location.state?.from;
  const target = typeof from === "string" && from.startsWith("/") && !from.startsWith("//")
    && !from.includes("\\") && !from.startsWith("/login") && !from.startsWith("/profile/setup") ? from : "/";
  if (auth.status === "ready") return <Navigate to={target} replace />;
  if (auth.status === "profileRequired") return <Navigate to="/profile/setup" replace />;
  if (auth.status !== "signedOut") return <AuthStatus />;

  async function login(testAccount?: string) {
    setBusy(true); setError(null);
    try { if (testAccount) await signInAsTestClimber(testAccount); else await signInWithGoogle(); }
    catch (error) { setError(signInError(error)); }
    finally { setBusy(false); }
  }
  return <main className="auth-screen"><section className="card auth-card">
    <div className="logo">Climb<span>Better</span></div>
    <h1>Your climbing journal</h1>
    <p className="muted">Keep your training, progress and climbing history together.</p>
    {authMode === "emulator" && <p className="auth-test-note">Local test accounts</p>}
    <button className="auth-google" disabled={busy} onClick={() => { void login(); }}>{busy ? "Signing in…" : "Continue with Google"}</button>
    {authMode === "emulator" && <div className="auth-actions">{testAccounts.map(account =>
      <button key={account.id} className="secondary" disabled={busy} onClick={() => { void login(account.id); }}>{account.name}</button>
    )}</div>}
    {error && <p role="alert">{error}</p>}
  </section></main>;
}
