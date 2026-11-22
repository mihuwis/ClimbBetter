import { Link, useLocation } from "react-router-dom";
import { dateLabel, number } from "../../../shared/lib/format";
import { formatMinutes } from "../model/period-summary.helpers";
import type { PeriodAscent, PeriodDisciplineAchievements, PeriodSession, PeriodTrainingSummary } from "../model/period-summary.types";

function SessionHighlight({ title, session, loadKey, returnTo }: {
  title: string;
  session: PeriodSession | null;
  loadKey: "classicLoad" | "adjustedLoad";
  returnTo: string;
}) {
  return <section className="card period-highlight-card">
    <p className="eyebrow">Session highlight</p>
    <h3>{title}</h3>
    {session ? <>
      <strong className="period-highlight-value">{number(session[loadKey])}</strong>
      <Link className="text-link" to={`/sessions/${session.sessionId}`} state={{ statisticsReturnTo: returnTo }}>{session.sessionName}</Link>
      <p className="muted">{dateLabel(session.sessionDate)} · {session.areaName}</p>
      <p className="period-highlight-detail">{formatMinutes(session.durationMinutes)} · {number(session.executedMoves)} moves</p>
    </> : <p className="muted">No sessions in this period.</p>}
  </section>;
}

function AscentLink({ ascent, returnTo }: { ascent: PeriodAscent; returnTo: string }) {
  return <Link className="text-link" to={`/sessions/${ascent.sessionId}`} state={{ statisticsReturnTo: returnTo }}>
    <strong>{ascent.gradeLabel}</strong> · {ascent.climbName}
  </Link>;
}

function ClimbHighlights({ title, achievements, returnTo }: {
  title: string;
  achievements: PeriodDisciplineAchievements;
  returnTo: string;
}) {
  const hardest = achievements.hardest;
  const styles = [
    ["OS", achievements.highestOs],
    ["Flash", achievements.highestFlash],
    ["RP", achievements.highestRp],
  ] as const;
  return <section className="card period-highlight-card">
    <p className="eyebrow">Climbing highlight</p>
    <h3>{title}</h3>
    {hardest ? <>
      <div className="period-hardest-ascent"><strong>{hardest.gradeLabel}</strong><span className="period-style-badge">{hardest.ascentMode === "FLASH" ? "Flash" : hardest.ascentMode}</span></div>
      <Link className="text-link" to={`/sessions/${hardest.sessionId}`} state={{ statisticsReturnTo: returnTo }}>{hardest.climbName}</Link>
      <p className="muted">{dateLabel(hardest.sessionDate)} · {hardest.areaName}</p>
      <dl className="period-style-bests">{styles.map(([style, ascent]) => <div key={style}>
        <dt>Highest {style}</dt><dd>{ascent ? <AscentLink ascent={ascent} returnTo={returnTo} /> : "—"}</dd>
      </div>)}</dl>
    </> : <p className="muted">No completed climbs in this view.</p>}
  </section>;
}

export function PeriodSummaryHighlights({ report }: { report: PeriodTrainingSummary }) {
  const location = useLocation();
  const returnTo = location.pathname + location.search;
  return <section className="period-highlights" aria-labelledby="period-highlights-title">
    <div className="section-heading"><div><p className="eyebrow">Standout moments</p><h3 id="period-highlights-title">Highlights of the period</h3></div></div>
    <div className="period-highlights-grid">
      <SessionHighlight title="Highest Adjusted Load" session={report.highlights.maxAdjustedSession} loadKey="adjustedLoad" returnTo={returnTo} />
      <SessionHighlight title="Highest Classic Load" session={report.highlights.maxClassicSession} loadKey="classicLoad" returnTo={returnTo} />
      <ClimbHighlights title="Hardest boulder" achievements={report.highlights.boulders} returnTo={returnTo} />
      <ClimbHighlights title="Hardest route or circuit" achievements={report.highlights.routesAndCircuits} returnTo={returnTo} />
    </div>
    <p className="muted period-metric-note">Climbing achievements require a successful ascent. Top-rope climbing is training and is excluded from these grade highlights.</p>
  </section>;
}
