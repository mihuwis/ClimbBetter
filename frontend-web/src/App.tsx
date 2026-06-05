import { BrowserRouter, Routes, Route } from 'react-router-dom';

import { AppLayout } from './app/AppLayout';

import { DashboardPage } from './pages/DashboardPage';
import { CalendarPage } from './pages/CalendarPage';
import { AreasPage } from './pages/AreasPage';


function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/areas" element={<AreasPage />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}

export default App;