import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

type AppLayoutProps = {
  children: ReactNode;
};

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="app">
      <header className="top-nav">
        <div className="logo">ClimbBetter</div>

        <nav className="main-nav">
          <NavLink to="/">Dashboard</NavLink>

          <NavLink to="/calendar">
            Calendar
          </NavLink>

          <NavLink to="/areas">
            Areas
          </NavLink>
        </nav>

        <div className="user-menu">Michał</div>
      </header>

      {children}
    </div>
  );
}