import { useState } from 'react';
import { buildMonthCalendar } from '../features/training/calendar/calendar.utils';
import type { SessionSummary } from '../features/training/model/session.types';
import { ActivityCard } from '../features/dashboard/components/ActivityCard';
import { useResource } from '../shared/api/useResource';
import { RequestState } from '../shared/ui/RequestState';
import { dateLabel, localDate } from '../shared/lib/format';

export function CalendarPage() {
  const [month, setMonth] = useState(() => localDate().slice(0, 7));
  const [selection, setSelection] = useState(localDate);
  const year = Number(month.slice(0, 4)), monthIndex = Number(month.slice(5, 7)) - 1;
  const days = buildMonthCalendar(year, monthIndex);
  const from = days[0].date, to = days[days.length - 1].date;
  const result = useResource<SessionSummary[]>(`/training/sessions?from=${from}&to=${to}`);
  const selected = selection.startsWith(month) ? selection : `${month}-01`;
  const sessions = result.data?.filter(session => session.sessionDate === selected) ?? [];
  function today() {
    const date = localDate(); setMonth(date.slice(0, 7)); setSelection(date);
  }
  function changeMonth(delta: number) {
    setMonth(localDate(new Date(year, monthIndex + delta, 1)).slice(0, 7));
  }
  return <main className="workspace"><div className="page-header"><div><p className="eyebrow">Your training rhythm</p><h1>Calendar</h1><p className="muted">Choose a day to explore your sessions.</p></div><div className="actions"><button onClick={() => changeMonth(-1)} aria-label="Previous month">←</button><button onClick={today}>Today</button><button onClick={() => changeMonth(1)} aria-label="Next month">→</button></div></div>
    <RequestState loading={result.loading} error={result.error} retry={result.refresh}/>
    <div className="calendar-workspace"><section className="card"><div className="section-heading"><h2>{new Intl.DateTimeFormat('en-GB', { month: 'long', year: 'numeric' }).format(new Date(year, monthIndex, 1))}</h2><span className="muted">{result.data?.filter(s => s.sessionDate.startsWith(month)).length ?? '—'} sessions</span></div>
      <div className="calendar-weekdays">{['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(day => <span key={day}>{day}</span>)}</div>
      <div className="calendar-grid">{days.map(day => {
        const activities = result.data?.filter(s => s.sessionDate === day.date) ?? [];
        return <button key={day.date} aria-label={`${dateLabel(day.date)}, ${activities.length} sessions`} aria-pressed={day.date === selected}
          className={`calendar-day ${!day.isCurrentMonth ? 'outside-month' : ''} ${activities.length ? 'has-activity' : ''} ${day.date === selected ? 'day-selected' : ''}`}
          onClick={() => { setSelection(day.date); if (!day.isCurrentMonth) setMonth(day.date.slice(0, 7)); }}>
          <strong>{day.dayOfMonth}</strong>{activities.length > 0 && <><span className="calendar-dot"/><span>{activities.length} {activities.length === 1 ? 'session' : 'sessions'}</span></>}
        </button>;
      })}</div></section>
      <aside className="stack"><h2>{dateLabel(selected)}</h2>{sessions.map(session => <ActivityCard activity={session} key={session.sessionId}/>)}
        {result.data && sessions.length === 0 && <section className="card"><h3>A little recovery</h3><p className="muted">No sessions recorded for this day.</p></section>}</aside></div>
  </main>;
}
