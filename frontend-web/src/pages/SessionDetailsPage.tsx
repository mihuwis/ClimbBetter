import { useParams } from 'react-router-dom';
import { sessionDetailsMock } from '../shared/mock/dashboard.mock';

export function SessionDetailsPage() {
  const { sessionId } = useParams();

  const session = sessionDetailsMock.find((item) => item.id === sessionId);

  if (!session) {
    return (
      <main className="page-layout">
        <h1>Session not found</h1>
      </main>
    );
  }

  const totalPoints = session.entries.reduce(
    (sum, entry) => sum + entry.points,
    0
  );

  const totalMoves = session.entries.reduce(
    (sum, entry) => sum + entry.executedMoves,
    0
  );

  return (
    <main className="page-layout">
      <div className="page-header">
        <div>
          <p className="muted">
            {session.sessionDate} · {session.discipline} · {session.location}
          </p>
          <h1>{session.sessionName}</h1>
        </div>
      </div>

      <div className="session-details-layout">
        <section className="session-details-main">
          <section className="card">
            <h2>Session summary</h2>

            <div className="session-meta">
              <div>
                <strong>{totalPoints}</strong>
                <span>Total points</span>
              </div>
              <div>
                <strong>{totalMoves}</strong>
                <span>Moves</span>
              </div>
              <div>
                <strong>{session.duration}</strong>
                <span>Duration</span>
              </div>
            </div>
          </section>

          <section className="card">
            <h2>Entries</h2>

            <div className="entries-list">
              {session.entries.map((entry) => (
                <article className="entry-row" key={entry.id}>
                  <div>
                    <h3>{entry.climbName}</h3>
                    <p className="muted">
                      {entry.grade} · {entry.style}
                    </p>

                    {entry.notes && (
                      <p className="notes">{entry.notes}</p>
                    )}
                  </div>

                  <div className="entry-metrics">
                    <span>
                      {entry.executedMoves} / {entry.totalMoves} moves
                    </span>
                    <strong>{entry.points} pts</strong>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </section>

        <aside className="day-summary-panel">
          <section className="card">
            <h3>Achievements</h3>

            <div className="badges">
              {session.achievements.map((achievement) => (
                <span key={achievement}>{achievement}</span>
              ))}
            </div>
          </section>

          <section className="card">
            <h3>Notes</h3>
            <p>{session.notes}</p>
          </section>
        </aside>
      </div>
    </main>
  );
}