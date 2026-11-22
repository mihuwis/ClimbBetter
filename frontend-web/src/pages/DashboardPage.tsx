import { Link, useLocation } from "react-router-dom";
import { useEffect, useRef } from "react";
import { useSessionFeed } from "../features/dashboard/model/useSessionFeed";
import { ActivityFeed } from "../features/dashboard/components/ActivityFeed";
import type {
  PersonalBests,
  ScaleBests,
} from "../features/dashboard/model/dashboard.types";
import { useResource } from "../shared/api/useResource";
import { useCurrentUser } from "../features/auth/hooks/useAuth";
import { RequestState } from "../shared/ui/RequestState";
import { dateLabel, number } from "../shared/lib/format";

function Bests({ title, data }: { title: string; data: ScaleBests }) {
  return (
    <section className="card">
      <p className="eyebrow">Lifetime personal bests</p>
      <h3>{title}</h3>
      {(["overall", "os", "flash", "rp"] as const).map((key) => {
        const best = data[key];
        return (
          <div className="stat-row" key={key}>
            <span>
              {key === "overall" ? "Highest grade" : key.toUpperCase()}
            </span>
            {best ? (
              <Link
                title={dateLabel(best.achievedOn)}
                to={`/sessions/${best.sessionId}`}
              >
                {best.gradeLabel} ↗
              </Link>
            ) : (
              <span className="muted">—</span>
            )}
          </div>
        );
      })}
    </section>
  );
}

export function DashboardPage() {
  const location = useLocation();
  const sessions = useSessionFeed();
  const { loading, error, nextCursor, loadMore } = sessions;
  const sentinel = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (loading || error || !nextCursor || !sentinel.current) return;
    const observer = new IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting)) loadMore();
    }, { rootMargin: "300px" });
    observer.observe(sentinel.current);
    return () => observer.disconnect();
  }, [loading, error, nextCursor, loadMore]);
  const bests = useResource<PersonalBests>("/dashboard/personal-bests");
  const user = useCurrentUser();
  return (
    <main className="workspace">
      <div className="page-header">
        <div>
          <p className="eyebrow">Your climbing journal</p>
          <h1>
            {user.data ? `${user.data.displayName}'s dashboard` : "Dashboard"}
          </h1>
          <p className="muted">A little progress, one session at a time.</p>
        </div>
        <div className="actions">
          <Link className="button secondary" to="/sessions/new">
            Add session
          </Link>
          <Link className="button primary" to="/sessions/record">
            Start now
          </Link>
        </div>
      </div>
      <div className="dashboard-layout dashboard-content">
        <aside className="left-column">
          <section className="card">
            <p className="eyebrow">Latest activity</p>
            {sessions.items[0] ? (
              <>
                <h3>{sessions.items[0].areaName}</h3>
                <p>{dateLabel(sessions.items[0].sessionDate)}</p>
                <Link
                  className="text-link"
                  to={`/sessions/${sessions.items[0].sessionId}`}
                  state={{ dashboardReturnKey: location.key }}
                >
                  Open latest session →
                </Link>
              </>
            ) : (
              <p className="muted">Your latest training will appear here.</p>
            )}
          </section>
          <section className="card">
            <h3>Make it a habit</h3>
            <p className="muted">
              Browse your training days and revisit a session.
            </p>
            <Link className="text-link" to="/calendar">
              Open calendar →
            </Link>
          </section>
        </aside>
        <section className="center-column">
          <div className="section-heading">
            <h2>Training history</h2>
            <span className="muted">{sessions.items.length} sessions loaded</span>
          </div>
          <RequestState
            loading={sessions.loading && sessions.items.length === 0}
            error={sessions.error}
            retry={sessions.retry}
          />
          {(sessions.items.length > 0 || (!sessions.loading && !sessions.error)) && <ActivityFeed activities={sessions.items} />}
          <div ref={sentinel} aria-hidden="true" style={{ height: 1 }} />
          {sessions.loading && sessions.items.length > 0 && <p role="status">Loading sessions…</p>}
          {!sessions.loading && !sessions.error && sessions.nextCursor && (
            <button className="secondary" onClick={sessions.loadMore}>Load more sessions</button>
          )}
          {!sessions.loading && !sessions.error && !sessions.nextCursor && sessions.items.length > 0 && (
            <p className="muted" role="status">You have reached the beginning of your journal.</p>
          )}
        </section>
        <aside className="right-column">
          <RequestState
            loading={bests.loading}
            error={bests.error}
            retry={bests.refresh}
          />
          {bests.data && (
            <>
              <Bests title="Bouldering" data={bests.data.boulder} />
              <Bests title="Routes & circuits" data={bests.data.route} />
            </>
          )}
          {sessions.items.length > 0 && (
            <section className="card">
              <h3>Visible sessions</h3>
              <div className="stat-row">
                <span>Sessions shown</span>
                <strong>{sessions.items.length}</strong>
              </div>
              <div className="stat-row">
                <span>Moves shown</span>
                <strong>
                  {number(
                    sessions.items.reduce((sum, s) => sum + s.totalMoves, 0),
                  )}
                </strong>
              </div>
              <p className="muted">Totals for the loaded sessions.</p>
            </section>
          )}
        </aside>
      </div>
    </main>
  );
}
