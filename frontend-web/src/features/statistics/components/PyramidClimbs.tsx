import { Link, useLocation } from "react-router-dom";
import { useEffect, useRef } from "react";
import type {
  PyramidClimbsPage,
  PyramidFilters,
  PyramidLayer,
} from "../model/statistics.types";
import { pyramidClimbsPath, pyramidLayerLabel } from "../model/pyramid.helpers";
import { useResource } from "../../../shared/api/useResource";
import { RequestState } from "../../../shared/ui/RequestState";
import { dateLabel, number } from "../../../shared/lib/format";

const pageSize = 50;

export function PyramidClimbs({
  filters,
  layer,
  offset,
  onPage,
  onClose,
}: {
  filters: PyramidFilters;
  layer: PyramidLayer;
  offset: number;
  onPage: (offset: number) => void;
  onClose: () => void;
}) {
  const location = useLocation();
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  const result = useResource<PyramidClimbsPage>(
    pyramidClimbsPath(filters, layer.bucketKey, offset, pageSize),
  );
  const label = pyramidLayerLabel(layer);
  return (
    <section
      id="pyramid-climbs"
      className="card statistics-climbs"
      aria-labelledby="pyramid-climbs-title"
    >
      <div className="section-heading">
        <div>
          <p className="eyebrow">Selected layer</p>
          <h3 ref={heading} id="pyramid-climbs-title" tabIndex={-1}>
            Climbs at {label}
          </h3>
        </div>
        <button
          className="subtle"
          onClick={onClose}
          aria-label="Close climb list"
        >
          ×
        </button>
      </div>
      <p className="muted">First qualifying ascent per climb in this view.</p>
      <RequestState
        loading={result.loading}
        error={result.error}
        retry={result.refresh}
      />
      {result.data && (
        <>
          {result.data.totalCount !== layer.climbCount && (
            <p className="notice" role="status">
              Your history has changed. Refresh the report to update the layer
              counts.
            </p>
          )}
          {result.data.items.length === 0 ? (
            <p className="muted">No climbs on this page.</p>
          ) : (
            <ul className="pyramid-climb-list">
              {result.data.items.map((climb) => (
                <li key={climb.climbId}>
                  <div className="pyramid-climb-heading">
                    <Link
                      className="text-link"
                      to={`/sessions/${climb.sessionId}`}
                      state={{
                        statisticsReturnTo: `${location.pathname}${location.search}`,
                      }}
                    >
                      {climb.climbName}
                    </Link>
                    <span className="pyramid-climb-grade">
                      {climb.gradeLabel}
                    </span>
                  </div>
                  <p className="muted">
                    {climb.areaName} ·{" "}
                    {climb.climbType === "BOULDER"
                      ? "Boulder · Font"
                      : climb.climbType === "CIRCUIT"
                        ? "Circuit · French"
                        : "Route · French"}
                  </p>
                  <p className="muted">
                    <time dateTime={climb.achievedOn}>
                      {dateLabel(climb.achievedOn)}
                    </time>{" "}
                    ·{" "}
                    {climb.ascentMode === "FLASH" ? "Flash" : climb.ascentMode}
                  </p>
                </li>
              ))}
            </ul>
          )}
          <div className="pyramid-pagination">
            <span className="muted" role="status">
              {result.data.items.length
                ? `${number(result.data.offset + 1)}–${number(result.data.offset + result.data.items.length)} of ${number(result.data.totalCount)}`
                : `${number(result.data.totalCount)} climbs`}
            </span>
            <div className="actions">
              <button
                disabled={result.data.offset === 0}
                onClick={() =>
                  onPage(Math.max(0, result.data!.offset - pageSize))
                }
                aria-label="Previous page"
              >
                Previous
              </button>
              <button
                disabled={result.data.nextOffset == null}
                onClick={() => {
                  if (result.data?.nextOffset != null)
                    onPage(result.data.nextOffset);
                }}
                aria-label="Next page"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </section>
  );
}
