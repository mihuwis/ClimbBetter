import { AchievementPyramidReport } from "../features/statistics/components/AchievementPyramidReport";
import { lazy, Suspense, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";

const PeriodTrainingSummaryReport = lazy(() => import("../features/statistics/components/PeriodTrainingSummaryReport")
  .then((module) => ({ default: module.PeriodTrainingSummaryReport })));

export function StatisticsPage() {
  const [search] = useSearchParams();
  const location = useLocation();
  const report = search.get("report") === "summary" ? "summary" : "pyramid";
  const [reportLinks, setReportLinks] = useState({ summary: "?report=summary", pyramid: "" });
  function rememberReport() {
    setReportLinks((previous) => ({ ...previous, [report]: location.search }));
  }
  return (
    <main className="workspace statistics-workspace">
      <div className="page-header">
        <div>
          <p className="eyebrow">Long-term progress</p>
          <h1>Statistics</h1>
          <p className="muted">Explore your climbing across longer periods.</p>
        </div>
      </div>
      <nav className="statistics-reports" aria-label="Statistics reports">
        <Link to={report === "summary" ? location.search : reportLinks.summary} onClick={rememberReport} aria-current={report === "summary" ? "page" : undefined}>
          Training summary
        </Link>
        <Link to={report === "pyramid" ? location.search || "/statistics" : reportLinks.pyramid || "/statistics"} onClick={rememberReport} aria-current={report === "pyramid" ? "page" : undefined}>
          Achievement pyramid
        </Link>
      </nav>
      {report === "summary" ? <Suspense fallback={<div className="card" role="status">Loading training summary…</div>}><PeriodTrainingSummaryReport /></Suspense> : <AchievementPyramidReport />}
    </main>
  );
}
