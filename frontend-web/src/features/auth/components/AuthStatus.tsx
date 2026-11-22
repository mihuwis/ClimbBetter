import { useAuth } from "../hooks/useAuth";

export function AuthStatus() {
  const auth = useAuth();
  if (auth.status === "checking") return <main className="auth-screen"><section className="card auth-card" role="status">Opening your journal…</section></main>;
  const linking = auth.status === "linkRequired";
  return <main className="auth-screen"><section className="card auth-card">
    <h1>{linking ? "Connect your existing journal" : "Unable to open your journal"}</h1>
    <p role={linking ? "status" : "alert"}>{linking
      ? "You are signed in. Your existing journal needs to be connected to this account before you can continue."
      : auth.error}</p>
    <div className="auth-actions">
      <button onClick={() => { void auth.retry(); }}>Try again</button>
      <button className="secondary" onClick={() => { void auth.logout(); }}>Sign out</button>
    </div>
  </section></main>;
}
