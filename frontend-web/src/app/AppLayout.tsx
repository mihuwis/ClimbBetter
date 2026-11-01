import { NavLink, Outlet } from 'react-router-dom';
import { useResource } from '../shared/api/useResource';
import type { CurrentUser } from '../features/training/model/session.types';

export function AppLayout() {
  const user = useResource<CurrentUser>('/identity/me');
  return <div className="app"><header className="top-nav">
    <NavLink className="logo" to="/">Climb<span>Better</span></NavLink>
    <nav className="main-nav" aria-label="Main navigation"><NavLink to="/" end>Dashboard</NavLink><NavLink to="/calendar">Calendar</NavLink><NavLink to="/areas">Areas</NavLink></nav>
    <div className="user-menu">{user.data?.displayName ?? 'Climbing journal'}</div>
  </header><Outlet/></div>;
}
