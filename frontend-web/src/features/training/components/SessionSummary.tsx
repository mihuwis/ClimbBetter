import type { SessionSummary as Summary } from "../model/session.types";
import { number } from "../../../shared/lib/format";
export function SessionSummary({ session }: { session: Summary }) {
  return (
    <section className="card">
      <div className="metrics-grid">
        <div>
          <span>Adjusted Load</span>
          <strong>{number(session.adjustedLoad)}</strong>
        </div>
        <div>
          <span>Classic Load</span>
          <strong>{number(session.classicLoad)}</strong>
        </div>
        <div>
          <span>Executed moves</span>
          <strong>{number(session.totalMoves)}</strong>
        </div>
        <div>
          <span>Duration</span>
          <strong>{session.durationMinutes} min</strong>
        </div>
        <div>
          <span>Average move intensity</span>
          <strong>{number(session.averageMoveIntensity)}</strong>
        </div>
        <div>
          <span>Highest completed</span>
          <strong>
            {[
              session.maxBoulderGrade && `Boulder ${session.maxBoulderGrade}`,
              session.maxRouteGrade && `Route ${session.maxRouteGrade}`,
            ]
              .filter(Boolean)
              .join(" · ") || "—"}
          </strong>
        </div>
      </div>
      {session.calculationModelVersion === "v1" && (
        <p className="muted model-note">
          This session uses an earlier calculation model.
        </p>
      )}
      {session.calculationModelVersion === "mixed" && (
        <p className="muted model-note">
          This session contains results from different calculation models.
        </p>
      )}
    </section>
  );
}
