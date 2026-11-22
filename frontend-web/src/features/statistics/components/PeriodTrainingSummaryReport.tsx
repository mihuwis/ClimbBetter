import { useEffect, useRef } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { useResource } from "../../../shared/api/useResource";
import { RequestState } from "../../../shared/ui/RequestState";
import { dateLabel, number } from "../../../shared/lib/format";
import type { PeriodBucket, PeriodSummaryFilters, PeriodTrainingSummary } from "../model/period-summary.types";
import {
  formatMinutes,
  readChartSettings,
  readSummaryFilters,
  shiftSummaryPeriod,
  summaryDisciplineLabels,
  summaryPeriodLabel,
  summaryReportPath,
  summarySearchParams,
} from "../model/period-summary.helpers";
import type { SummaryChartSettings } from "../model/period-summary.helpers";
import { PeriodSummaryChart } from "./PeriodSummaryChart";
import { PeriodSummaryBreakdowns } from "./PeriodSummaryBreakdowns";
import { PeriodSummaryHighlights } from "./PeriodSummaryHighlights";

function SelectedDaySessions({ bucket, onClose }: { bucket: PeriodBucket; onClose: () => void }) {
  const location = useLocation();
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => { heading.current?.focus(); }, [bucket.windowStart]);
  return <section className="card period-selected-sessions" aria-labelledby="selected-day-sessions-title">
    <div className="section-heading">
      <h3 id="selected-day-sessions-title" ref={heading} tabIndex={-1}>Sessions on {dateLabel(bucket.windowStart)}</h3>
      <button className="subtle" onClick={onClose} aria-label="Close session list">×</button>
    </div>
    {bucket.sessions.length === 0 ? <p className="muted">No sessions on this day in the selected discipline.</p> : <ul className="period-session-list">
      {bucket.sessions.map((session) => <li key={session.sessionId}>
        <div><Link className="text-link" to={`/sessions/${session.sessionId}`} state={{ statisticsReturnTo: location.pathname + location.search }}>{session.sessionName}</Link>
          <p className="muted">{session.areaName} · {formatMinutes(session.durationMinutes)} · {number(session.executedMoves)} moves</p></div>
        <div className="period-session-load"><strong>{number(session.adjustedLoad)}</strong><span>Adjusted Load</span></div>
      </li>)}
    </ul>}
  </section>;
}

function SummaryMetrics({ report, discipline }: { report: PeriodTrainingSummary; discipline: PeriodSummaryFilters["discipline"] }) {
  const metrics = report.summary;
  const filtered = discipline !== "ALL";
  const items = [
    { label: "Sessions", value: number(metrics.sessionCount), detail: filtered ? "Sessions containing the selected discipline." : "All recorded sessions in this period." },
    { label: "Session time", value: formatMinutes(metrics.durationMinutes), detail: `Average ${formatMinutes(metrics.averageDurationMinutes)} per session. Includes rests.` },
    { label: "Executed moves", value: number(metrics.executedMoves), detail: !filtered ? `${number(report.warmupMoves)} warm-up moves included.` : "Moves from the selected discipline only." },
    { label: "Adjusted Load", value: number(metrics.adjustedLoad), detail: `${number(metrics.averageAdjustedLoad)} average per session.` },
    { label: "Classic Load", value: number(metrics.classicLoad), detail: `${number(metrics.averageClassicLoad)} average per session.` },
    { label: "Active days", value: number(metrics.activeDays), detail: `${number(report.uniqueClimbCount)} different climbs completed.` },
  ];
  return <div className="period-summary-metrics">{items.map((item) => <section className="card period-summary-metric" aria-label={item.label} key={item.label}>
    <span className="eyebrow">{item.label}</span><strong>{item.value}</strong><p className="muted">{item.detail}</p>
  </section>)}</div>;
}

export function PeriodTrainingSummaryReport() {
  const [search, setSearch] = useSearchParams();
  const filters = readSummaryFilters(search);
  const settings = readChartSettings(search);
  const result = useResource<PeriodTrainingSummary>(summaryReportPath(filters));
  const selected = filters.period === "MONTH" ? result.data?.buckets.find((bucket) => bucket.windowStart === search.get("day")) : undefined;
  const previous = shiftSummaryPeriod(filters, -1);
  const next = shiftSummaryPeriod(filters, 1);
  const previousDisabled = previous.year === filters.year && previous.month === filters.month;
  const nextDisabled = next.year === filters.year && next.month === filters.month;

  function changeFilters(nextFilters: PeriodSummaryFilters) {
    setSearch(summarySearchParams(nextFilters, settings), { preventScrollReset: true });
  }
  function changeSettings(nextSettings: SummaryChartSettings) {
    setSearch(summarySearchParams(filters, nextSettings, selected?.windowStart), { replace: true, preventScrollReset: true });
  }
  function selectBucket(bucket: PeriodBucket) {
    if (filters.period === "YEAR") {
      changeFilters({ ...filters, period: "MONTH", month: Number(bucket.windowStart.slice(5, 7)) });
    } else {
      setSearch(summarySearchParams(filters, settings, bucket.windowStart), { preventScrollReset: true });
    }
  }
  return <section id="training-summary" className="statistics-report period-summary-report" aria-labelledby="period-summary-title">
    <div className="section-heading">
      <div><p className="eyebrow">Your training, in perspective</p><h2 id="period-summary-title">Training summary</h2></div>
      <button onClick={result.refresh} disabled={result.loading}>Refresh report</button>
    </div>
    <p className="muted statistics-intro">Explore your training volume, load and successful climbs across a month or a year.</p>
    <div className="card period-summary-filters">
      <label>Period<select value={filters.period} onChange={(event) => changeFilters({ ...filters, period: event.target.value as PeriodSummaryFilters["period"] })}>
        <option value="MONTH">Month</option><option value="YEAR">Year</option>
      </select></label>
      <label>Year<input type="number" min={1} max={9998} value={filters.year} onChange={(event) => {
        const year = Number(event.target.value);
        if (Number.isInteger(year) && year >= 1 && year <= 9998) changeFilters({ ...filters, year });
      }} /></label>
      {filters.period === "MONTH" && <label>Month<select value={filters.month} onChange={(event) => changeFilters({ ...filters, month: Number(event.target.value) })}>
        {Array.from({ length: 12 }, (_, index) => <option key={index} value={index + 1}>
          {new Intl.DateTimeFormat("en-GB", { month: "long" }).format(new Date(2024, index, 1))}
        </option>)}
      </select></label>}
      <label>Discipline<select value={filters.discipline} onChange={(event) => changeFilters({ ...filters, discipline: event.target.value as PeriodSummaryFilters["discipline"] })}>
        {Object.entries(summaryDisciplineLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
      </select></label>
    </div>
    <div className="period-summary-navigation">
      <div className="period-step-buttons"><button aria-label="Previous period" disabled={previousDisabled} onClick={() => changeFilters(previous)}>←</button>
        <button aria-label="Next period" disabled={nextDisabled} onClick={() => changeFilters(next)}>→</button></div>
      <h3>{summaryPeriodLabel(filters)}</h3>
      <span className="muted">{summaryDisciplineLabels[filters.discipline]}</span>
    </div>
    {filters.discipline !== "ALL" && <p className="notice period-time-note">Time shown is the full time of sessions containing {filters.discipline === "BOULDER" ? "boulders" : "routes or circuits"}, including rests. Mixed sessions appear in both discipline views; their times and session counts cannot be added together.</p>}
    <RequestState loading={result.loading} error={result.error} retry={result.refresh} />
    {result.data && <>
      <SummaryMetrics report={result.data} discipline={filters.discipline} />
      {result.data.summary.sessionCount === 0 && <section className="card period-summary-empty">
        <h3>No training in this period</h3><p className="muted">Choose another period or discipline, or add a session to start building your history.</p>
        <Link className="text-link" to="/sessions/new">Add session</Link>
      </section>}
      <PeriodSummaryChart buckets={result.data.buckets} period={filters.period} settings={settings} onSettingsChange={changeSettings} onSelectBucket={selectBucket} />
      {selected && <SelectedDaySessions bucket={selected} onClose={() => setSearch(summarySearchParams(filters, settings), { preventScrollReset: true })} />}
      {result.data.summary.sessionCount > 0 && <><PeriodSummaryHighlights report={result.data} /><PeriodSummaryBreakdowns report={result.data} /></>}
    </>}
  </section>;
}
