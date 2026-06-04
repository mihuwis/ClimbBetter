import { ActivityFeed } from '../features/dashboard/components/ActivityFeed';
import { ProfileSummaryCard } from '../features/dashboard/components/ProfileSummaryCard';
import { DisciplineSummaryCard } from '../features/dashboard/components/DisciplineSummaryCard';
import {
  activityFeedMock,
  profileSummaryMock,
  disciplineSummaryMock
} from '../shared/mock/dashboard.mock';
export function DashboardPage() {
  return (
    <main className="dashboard-layout">
        <aside className="left-column">
          <ProfileSummaryCard profile={profileSummaryMock} />

          <section className="card">
            <h3>Latest activity</h3>
            <p><strong>Bronx Boulder Session</strong></p>
            <p className="muted">20 May 2026</p>
          </section>

        <DisciplineSummaryCard disciplines={disciplineSummaryMock} />
        </aside>

      <section className="center-column">
            <ActivityFeed activities={activityFeedMock} />
      </section>

        <aside className="right-column">
          <section className="card">
            <h3>Goals</h3>
            <p>Climb 3 times this week</p>
            <strong>2 / 3 sessions</strong>
          </section>

          <section className="card">
            <h3>Monthly moves</h3>
            <p>420 / 1000 moves</p>
          </section>

          <section className="card">
            <h3>Future</h3>
            <p className="muted">Challenges, partners and community features will appear here later.</p>
          </section>
        </aside>
    </main>
  );
}