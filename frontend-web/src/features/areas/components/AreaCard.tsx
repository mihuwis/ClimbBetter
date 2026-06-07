import type { AreaSummary } from '../../../shared/types/areas.types';

type AreaCardProps = {
  area: AreaSummary;
};

export function AreaCard({ area }: AreaCardProps) {
  return (
    <article className="card">
      <div className="area-header">
        <h3>{area.name}</h3>

        {area.isFavorite && <span>⭐</span>}
      </div>

      <p className="muted">
        {area.type} · {area.location}
      </p>

      <div className="area-stats">
        <span>{area.climbsCount} climbs</span>

        {area.sectorsCount && (
          <span>{area.sectorsCount} sectors</span>
        )}

        <span>{area.projectsCount} projects</span>
      </div>

      <div className="area-tags">
        {area.recentlyUsed && (
          <span className="area-tag">
            Recently used
          </span>
        )}

        {area.isOfficial && (
          <span className="area-tag">
            Official
          </span>
        )}
      </div>
    </article>
  );
}