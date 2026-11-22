import type { AchievementPyramid } from "../model/statistics.types";
import { formatPyramidPoints } from "../model/pyramid.helpers";
import { number } from "../../../shared/lib/format";

export function PyramidAssessment({ report }: { report: AchievementPyramid }) {
  const combined = report.filters.view === "COMBINED";
  const assessment = report.levelAssessment;
  const supported = assessment?.method === "PYRAMID_SUPPORT";
  return (
    <>
      <section className="card statistics-metric">
        <span className="eyebrow">
          {combined ? "Highest points" : "Highest grade"}
        </span>
        <strong>
          {combined
            ? report.peak?.gradePoints == null
              ? "—"
              : formatPyramidPoints(report.peak.gradePoints)
            : (report.peak?.gradeLabel ?? "—")}
        </strong>
        <p className="muted">
          {report.peakUnavailableReason === "MIXED_POINT_CALIBRATIONS"
            ? "Different point calibrations are shown separately; there is no shared maximum."
            : "Highest achievement in the selected period and style."}
        </p>
      </section>
      <section className="card statistics-metric statistics-assessment">
        <span className="eyebrow">
          {assessment && assessment.method !== "NO_ASCENTS" && !supported
            ? "Provisional level"
            : "Supported level"}
        </span>
        <strong>{assessment?.gradeLabel ?? "—"}</strong>
        {combined ? (
          <p className="muted">
            Combined points compare achievements. A shared level estimate is not
            defined.
          </p>
        ) : !assessment || assessment.method === "NO_ASCENTS" ? (
          <p className="muted">No completed climbs to estimate a level.</p>
        ) : (
          <>
            <span
              className={`pyramid-confidence ${assessment.confidence === "LOW" ? "low" : ""}`}
            >
              {assessment.confidence === "HIGH"
                ? "High"
                : assessment.confidence === "MEDIUM"
                  ? "Medium"
                  : "Low"}{" "}
              confidence
            </span>
            <p className="muted">
              {supported
                ? "Weighted Pyramid Support from different climbs in this view."
                : assessment.method === "LOW_SAMPLE_MAX"
                  ? "Fewer than 10 different climbs. This estimate uses your highest grade."
                  : "The pyramid has insufficient support. This estimate uses your highest grade."}
            </p>
            <details>
              <summary>How this level is estimated</summary>
              <p className="muted">
                Based on {number(assessment.sampleSize)} different climbs in the
                selected period and style.
              </p>
              {supported && (
                <>
                  <p className="muted">
                    Support score:{" "}
                    <strong>{number(assessment.supportScore)}</strong>{" "}
                    (threshold 3). Up to 2 climbs at or above this level carry
                    weight 1, up to 4 one grade step below carry weight 0.5, and
                    up to 8 two steps below carry weight 0.25. A grade step here
                    includes a plus grade.
                  </p>
                  <dl className="statistics-support">
                    <dt>At or above</dt>
                    <dd>{number(assessment.supportAtOrAboveCount)}</dd>
                    <dt>One step below</dt>
                    <dd>{number(assessment.supportMinusOneCount)}</dd>
                    <dt>Two steps below</dt>
                    <dd>{number(assessment.supportMinusTwoCount)}</dd>
                  </dl>
                </>
              )}
              <p className="muted">
                This estimate describes the selected history. It does not change
                the reference level used for training load.
              </p>
            </details>
          </>
        )}
      </section>
    </>
  );
}
