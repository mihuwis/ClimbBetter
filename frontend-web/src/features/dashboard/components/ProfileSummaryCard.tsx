import type { ProfileSummary } from "../../../shared/types/dashboard.types";

type ProfileSummaryCardProps = {
    profile: ProfileSummary;
}

export function ProfileSummaryCard({ profile }: ProfileSummaryCardProps) {
  return (
    <section className="card">
      <h2>{profile.userName}</h2>
      <p className="muted">Climbing profile</p>

      <div className="stats-grid">
        <div>
          <strong>{ profile.sessionsCount }</strong>
          <span>Sessions</span>
        </div>
        <div>
          <strong>{profile.currentStreak}</strong>
          <span>Streak</span>
        </div>
        <div>
          <strong>{profile.sessionsThisWeek}</strong>
          <span>This week</span>
        </div>
      </div>
    </section>
  );
}