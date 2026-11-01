import type { SessionSummary } from '../../training/model/session.types';
import { ActivityCard } from './ActivityCard';
export function ActivityFeed({ activities }: { activities: SessionSummary[] }) {
  return activities.length ? <div className="stack">{activities.map(activity =>
    <ActivityCard key={activity.sessionId} activity={activity} />)}</div>
    : <section className="card empty-state"><h2>Your first session starts here</h2><p>Record a past training or start climbing now.</p></section>;
}
