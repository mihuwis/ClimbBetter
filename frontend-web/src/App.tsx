import { BrowserRouter, Routes, Route } from 'react-router-dom';

import { AppLayout } from './app/AppLayout';

import { DashboardPage } from './pages/DashboardPage';
import { CalendarPage } from './pages/CalendarPage';
import { AreasPage } from './pages/AreasPage';
import { SessionDetailsPage } from './pages/SessionDetailsPage';


function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/areas" element={<AreasPage />} />
          <Route path="/sessions/:sessionId" element={<SessionDetailsPage />}/>
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}

export default App;