import { useSearchParams } from "react-router-dom";
import { useResource } from "../../../shared/api/useResource";
import { RequestState } from "../../../shared/ui/RequestState";
import { dateLabel, number } from "../../../shared/lib/format";
import type {
  AchievementPyramid,
  PyramidFilters,
  PyramidLayer,
} from "../model/statistics.types";
import {
  pyramidReportPath,
  pyramidSearchParams,
  readPyramidFilters,
} from "../model/pyramid.helpers";
import { PyramidChart } from "./PyramidChart";
import { PyramidAssessment } from "./PyramidAssessment";
import { PyramidClimbs } from "./PyramidClimbs";

export function AchievementPyramidReport() {
  const [search, setSearch] = useSearchParams();
  const filters = readPyramidFilters(search);
  const result = useResource<AchievementPyramid>(pyramidReportPath(filters));
  const bucket = search.get("bucket");
  const selected = result.data?.layers.find(
    (layer) => layer.bucketKey === bucket,
  );
  const rawOffset = search.get("offset") ?? "0";
  const offset =
    /^\d+$/.test(rawOffset) &&
    Number.isSafeInteger(Number(rawOffset)) &&
    Number(rawOffset) <= 2147483547
      ? Number(rawOffset)
      : 0;

  function changeFilter<K extends keyof PyramidFilters>(
    key: K,
    value: PyramidFilters[K],
  ) {
    setSearch(pyramidSearchParams({ ...filters, [key]: value }));
  }
  function selectLayer(layer: PyramidLayer) {
    const next = pyramidSearchParams(filters);
    next.set("bucket", layer.bucketKey);
    setSearch(next, { preventScrollReset: true });
  }
  function changePage(nextOffset: number) {
    const next = pyramidSearchParams(filters);
    if (selected) next.set("bucket", selected.bucketKey);
    if (nextOffset) next.set("offset", String(nextOffset));
    setSearch(next, { preventScrollReset: true });
  }
  function clearSelection() {
    document
      .querySelector<HTMLButtonElement>(".pyramid-layer.is-selected")
      ?.focus();
    setSearch(pyramidSearchParams(filters), { preventScrollReset: true });
  }

  return (
    <section
      id="achievement-pyramid"
      aria-labelledby="pyramid-title"
      className="statistics-report"
    >
      <div className="section-heading">
        <div>
          <p className="eyebrow">Your achievements</p>
          <h2 id="pyramid-title">Achievement pyramid</h2>
        </div>
        <button onClick={result.refresh} disabled={result.loading}>
          Refresh report
        </button>
      </div>
      <p className="muted statistics-intro">
        Each climb counts once in the selected period and style. Repeats add no
        extra entries. Top-rope climbing is training and is excluded.
      </p>
      <div className="card statistics-filters">
        <label>
          Discipline
          <select
            value={filters.view}
            onChange={(event) =>
              changeFilter("view", event.target.value as PyramidFilters["view"])
            }
          >
            <option value="BOULDER">Boulders</option>
            <option value="ROUTE_AND_CIRCUIT">Routes &amp; circuits</option>
            <option value="COMBINED">Combined · points</option>
          </select>
        </label>
        <label>
          Period
          <select
            value={filters.period}
            onChange={(event) =>
              changeFilter(
                "period",
                event.target.value as PyramidFilters["period"],
              )
            }
          >
            <option value="LAST_TWO_YEARS">Last 2 years</option>
            <option value="ALL_TIME">All time</option>
          </select>
        </label>
        <label>
          Style
          <select
            value={filters.style}
            onChange={(event) =>
              changeFilter(
                "style",
                event.target.value as PyramidFilters["style"],
              )
            }
          >
            <option value="RP">RP (includes OS &amp; Flash)</option>
            <option value="OS">OS only</option>
          </select>
        </label>
      </div>
      <RequestState
        loading={result.loading}
        error={result.error}
        retry={result.refresh}
      />
      {result.data && (
        <>
          <p className="muted statistics-period">
            {result.data.windowStart
              ? `${dateLabel(result.data.windowStart)} – ${dateLabel(result.data.windowEnd)}`
              : `All recorded history through ${dateLabel(result.data.windowEnd)}`}
            <span>
              {" "}
              · {filters.style === "RP" ? "RP, OS & Flash" : "OS only"}
            </span>
          </p>
          <div className="statistics-summary">
            <section className="card statistics-metric">
              <span className="eyebrow">Unique climbs</span>
              <strong>{number(result.data.uniqueClimbCount)}</strong>
              <p className="muted">Different climbs completed in this view.</p>
            </section>
            <PyramidAssessment report={result.data} />
          </div>
          {result.data.uniqueClimbCount === 0 ? (
            <section className="card statistics-empty">
              <h3>No achievements in this view</h3>
              <p className="muted">
                Try another discipline, style or period, or record a completed
                climb.
              </p>
            </section>
          ) : (
            <div className="statistics-pyramid-layout">
              <section
                className="card statistics-chart-card"
                aria-label="Pyramid by grade"
              >
                <div className="section-heading">
                  <h3>
                    {filters.view === "COMBINED"
                      ? "Climbs by points"
                      : "Climbs by grade"}
                  </h3>
                  <span className="muted">
                    {filters.view === "COMBINED" &&
                    result.data.gradeCalibrationVersions.length > 1
                      ? "Hardest first in each calibration"
                      : "Hardest at the top"}
                  </span>
                </div>
                <PyramidChart
                  report={result.data}
                  selectedBucket={selected?.bucketKey ?? null}
                  onSelect={selectLayer}
                />
                <p className="muted statistics-chart-caption">
                  Bar width shows the number of different climbs. Select a layer
                  to explore it.
                </p>
              </section>
              {selected ? (
                <PyramidClimbs
                  key={`${pyramidReportPath(filters)}:${selected.bucketKey}`}
                  filters={filters}
                  layer={selected}
                  offset={offset}
                  onPage={changePage}
                  onClose={clearSelection}
                />
              ) : (
                <aside className="card statistics-layer-placeholder">
                  <h3>Explore a layer</h3>
                  <p className="muted">
                    Select a grade or point value to see its climbs and original
                    sessions.
                  </p>
                </aside>
              )}
            </div>
          )}
          {bucket && !selected && result.data.uniqueClimbCount > 0 && (
            <p className="notice" role="status">
              This layer is no longer in the report. Select another layer.
            </p>
          )}
        </>
      )}
    </section>
  );
}
