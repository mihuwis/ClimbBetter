import { Link } from 'react-router-dom';
import { ActivityFeed } from '../features/dashboard/components/ActivityFeed';
import type { PersonalBests, ScaleBests } from '../features/dashboard/model/dashboard.types';
import type { CurrentUser, SessionSummary } from '../features/training/model/session.types';
import { useResource } from '../shared/api/useResource';
import { RequestState } from '../shared/ui/RequestState';
import { dateLabel, number } from '../shared/lib/format';

function Bests({ title, data }: { title: string; data: ScaleBests }) {
  return <section className="card"><p className="eyebrow">Lifetime personal bests</p><h3>{title}</h3>
    {(['overall', 'os', 'flash', 'rp'] as const).map(key => {
      const best = data[key];
      return <div className="stat-row" key={key}><span>{key === 'overall' ? 'Highest grade' : key.toUpperCase()}</span>
        {best ? <Link title={dateLabel(best.achievedOn)} to={`/sessions/${best.sessionId}`}>{best.gradeLabel} ↗</Link> : <span className="muted">—</span>}</div>;
    })}</section>;
}

export function DashboardPage() {
  const sessions = useResource<SessionSummary[]>('/dashboard/sessions');
  const bests = useResource<PersonalBests>('/dashboard/personal-bests');
  const user = useResource<CurrentUser>('/identity/me');
  return <main className="workspace">
    <div className="page-header"><div><p className="eyebrow">Your climbing journal</p><h1>{user.data ? `${user.data.displayName}'s dashboard` : 'Dashboard'}</h1><p className="muted">A little progress, one session at a time.</p></div>
      <div className="actions"><Link className="button secondary" to="/sessions/new">Add session</Link><Link className="button primary" to="/sessions/record">Start now</Link></div></div>
    <div className="dashboard-layout dashboard-content">
      <aside className="left-column"><section className="card"><p className="eyebrow">Latest activity</p>
        {sessions.data?.[0] ? <><h3>{sessions.data[0].areaName}</h3><p>{dateLabel(sessions.data[0].sessionDate)}</p><Link className="text-link" to={`/sessions/${sessions.data[0].sessionId}`}>Open latest session →</Link></> : <p className="muted">Your latest training will appear here.</p>}</section>
        <section className="card"><h3>Make it a habit</h3><p className="muted">Browse your training days and revisit a session.</p><Link className="text-link" to="/calendar">Open calendar →</Link></section></aside>
      <section className="center-column"><div className="section-heading"><h2>Recent sessions</h2><span className="muted">Up to 20 latest</span></div>
        <RequestState loading={sessions.loading} error={sessions.error} retry={sessions.refresh}/>
        {sessions.data && <ActivityFeed activities={sessions.data}/>}</section>
      <aside className="right-column"><RequestState loading={bests.loading} error={bests.error} retry={bests.refresh}/>
        {bests.data && <><Bests title="Bouldering" data={bests.data.boulder}/><Bests title="Routes & circuits" data={bests.data.route}/></>}
        {sessions.data && <section className="card"><h3>Visible sessions</h3><div className="stat-row"><span>Sessions shown</span><strong>{sessions.data.length}</strong></div><div className="stat-row"><span>Moves shown</span><strong>{number(sessions.data.reduce((sum, s) => sum + s.totalMoves, 0))}</strong></div><p className="muted">Totals for this recent list.</p></section>}</aside>
    </div>
  </main>;
}
