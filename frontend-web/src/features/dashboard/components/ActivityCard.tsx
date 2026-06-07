import type { ActivityFeedItem } from '../../../shared/types/dashboard.types';
import { Link } from 'react-router-dom';

type ActivityCardProps = {
  activity: ActivityFeedItem;
};

export function ActivityCard({ activity }: ActivityCardProps) {
  return (
    <article className="session-card">
      <p className="muted">
        {activity.sessionDate} · {activity.discipline} · {activity.location}
      </p>

      <h2>
        <Link to={`/sessions/${activity.id}`}>
          {activity.sessionName}
        </Link>
      </h2>

      <div className="session-meta">
        <div>
          <strong>{activity.score}</strong>
          <span>Score</span>
        </div>
        <div>
          <strong>{activity.maxGrade}</strong>
          <span>Max grade</span>
        </div>
        <div>
          <strong>{activity.moves}</strong>
          <span>Moves</span>
        </div>
      </div>

      <div className="grade-bars">
        <span style={{ height: '30%' }} />
        <span style={{ height: '45%' }} />
        <span style={{ height: '70%' }} />
        <span style={{ height: '50%' }} />
        <span style={{ height: '25%' }} />
      </div>

      <div className="badges">
        {activity.achievements.map((achievement) => (
          <span key={achievement}>{achievement}</span>
        ))}
      </div>

      <p className="notes">{activity.notes}</p>

      <div className="community-actions">
        <button disabled>Like</button>
        <button disabled>Comment</button>
      </div>
    </article>
  );
}