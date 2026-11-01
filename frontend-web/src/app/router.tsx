import { Route, Routes, Link } from 'react-router-dom';
import { AppLayout } from './AppLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { CalendarPage } from '../pages/CalendarPage';
import { AreasPage } from '../pages/AreasPage';
import { SessionDetailsPage } from '../pages/SessionDetailsPage';
import { SessionEditPage } from '../pages/SessionEditPage';
import { NewSessionPage } from '../pages/NewSessionPage';

export function AppRoutes() {
  return <Routes><Route element={<AppLayout/>}>
    <Route index element={<DashboardPage/>}/><Route path="calendar" element={<CalendarPage/>}/><Route path="areas" element={<AreasPage/>}/>
    <Route path="sessions/new" element={<NewSessionPage/>}/><Route path="sessions/record" element={<NewSessionPage live/>}/>
    <Route path="sessions/:sessionId" element={<SessionDetailsPage/>}/><Route path="sessions/:sessionId/edit" element={<SessionEditPage/>}/>
    <Route path="*" element={<main className="workspace"><h1>Page not found</h1><Link to="/">Back to dashboard</Link></main>}/>
  </Route></Routes>;
}
