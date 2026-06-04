import type { ActivityFeedItem } from '../../../shared/types/dashboard.types';
import { ActivityCard } from './ActivityCard';

type ActivityFeedProps = {
  activities: ActivityFeedItem[];
};

export function ActivityFeed({ activities }: ActivityFeedProps) {
  return (
    <>
      <div className="feed-header">
        <select>
          <option>My activities</option>
          <option>Friends activities</option>
        </select>
      </div>

      {activities.map((activity) => (
        <ActivityCard key={activity.id} activity={activity} />
      ))}
    </>
  );
}