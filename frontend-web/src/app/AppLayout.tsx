import type { ReactNode } from 'react';

type AppLayoutProps = {
  children: ReactNode;
};

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="app">
      <header className="top-nav">
        <div className="logo">ClimbBetter</div>

        <nav className="main-nav">
          <a className="active" href="#">Dashboard</a>
          <a href="#">Calendar</a>
          <a href="#">Areas</a>
        </nav>

        <div className="user-menu">Michał</div>
      </header>

      {children}
    </div>
  );
}