import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";
import { errorMessage } from "../shared/api/http";

export function ProfileSetupPage() {
  const auth = useAuth();
  const [name, setName] = useState(auth.profile?.displayName ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (auth.status === "ready") return <Navigate to="/" replace />;
  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(null);
    try { await auth.completeProfile(name.trim()); }
    catch (error) { setError(errorMessage(error)); }
    finally { setBusy(false); }
  }
  return <main className="auth-screen"><section className="card auth-card">
    <h1>Make this journal yours</h1>
    <p className="muted">Choose the name you want to use in ClimbBetter.</p>
    <form onSubmit={event => { void save(event); }}>
      <label className="auth-name">Your name<input autoComplete="nickname" autoFocus required maxLength={80} value={name} onChange={event => setName(event.target.value)} /></label>
      {error && <p role="alert">{error}</p>}
      <button disabled={busy || !name.trim()}>{busy ? "Saving…" : "Open my journal"}</button>
    </form>
    <button className="secondary" onClick={() => { void auth.logout(); }}>Sign out</button>
  </section></main>;
}
