import { Link } from 'react-router-dom';
import type { SessionSummary } from '../../training/model/session.types';
import { dateLabel, number } from '../../../shared/lib/format';

export function ActivityCard({ activity }: { activity: SessionSummary }) {
  return <article className="session-card">
    <div className="eyebrow">{dateLabel(activity.sessionDate)} <span>·</span> {activity.areaName}</div>
    <h2><Link to={`/sessions/${activity.sessionId}`}>{activity.sessionName}</Link></h2>
    <div className="session-meta">
      <div><strong>{number(activity.adjustedLoad)}</strong><span>Adjusted Load</span></div>
      <div><strong>{number(activity.totalMoves)}</strong><span>Moves</span></div>
      <div><strong>{activity.durationMinutes} min</strong><span>Duration</span></div>
    </div>
    <div className="card-footer"><div className="badges">
      {activity.maxBoulderGrade && <span>Boulder · {activity.maxBoulderGrade}</span>}
      {activity.maxRouteGrade && <span>Route / circuit · {activity.maxRouteGrade}</span>}
      {!activity.maxBoulderGrade && !activity.maxRouteGrade && <span>No completed grade</span>}
    </div><Link className="text-link" to={`/sessions/${activity.sessionId}`}>View session →</Link></div>
  </article>;
}
