import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";
import { authMode } from "../shared/lib/firebase";

export function AppLayout() {
  const auth = useAuth();
  const location = useLocation();
  const dashboardState = { dashboardReturnKey: location.state?.dashboardReturnKey };
  return (
    <div className="app">
      <header className="top-nav">
        <NavLink className="logo" to="/" state={dashboardState}>
          Climb<span>Better</span>
        </NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          <NavLink to="/" end state={dashboardState}>
            Dashboard
          </NavLink>
          <NavLink to="/calendar">Calendar</NavLink>
          <NavLink to="/statistics">Statistics</NavLink>
          <NavLink to="/areas">Areas</NavLink>
        </nav>
        <div className="user-menu">
          {authMode === "emulator" && <span className="auth-test-note">Test data</span>}
          {auth.profile?.displayName ?? "Climbing journal"}
          <button className="secondary" onClick={() => { void auth.logout(); }}>Sign out</button>
        </div>
      </header>
      <Outlet />
    </div>
  );
}
