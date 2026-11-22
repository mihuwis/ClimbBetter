import type { CSSProperties } from "react";
import type {
  AchievementPyramid,
  PyramidLayer,
} from "../model/statistics.types";
import { pyramidLayerLabel } from "../model/pyramid.helpers";
import { number } from "../../../shared/lib/format";

export function PyramidChart({
  report,
  selectedBucket,
  onSelect,
}: {
  report: AchievementPyramid;
  selectedBucket: string | null;
  onSelect: (layer: PyramidLayer) => void;
}) {
  const largest = Math.max(
    1,
    ...report.layers.map((layer) => layer.climbCount),
  );
  const mixedCalibrations =
    report.gradeCalibrationVersions.length > 1 &&
    report.filters.view === "COMBINED";
  return (
    <ul
      className={`pyramid-layers ${report.filters.view === "COMBINED" ? "is-points" : ""}`}
      style={
        {
          "--pyramid-count-width": `${Math.max(3, number(largest).length)}ch`,
        } as CSSProperties
      }
      aria-label="Pyramid layers"
    >
      {report.layers.map((layer, index) => {
        const label = pyramidLayerLabel(layer);
        const calibrationStarts =
          mixedCalibrations &&
          (index === 0 ||
            report.layers[index - 1].gradeCalibrationVersion !==
              layer.gradeCalibrationVersion);
        return (
          <li key={layer.bucketKey}>
            {calibrationStarts && (
              <p className="eyebrow pyramid-calibration">
                Point calibration {layer.gradeCalibrationVersion}
              </p>
            )}
            <button
              className={`pyramid-layer ${selectedBucket === layer.bucketKey ? "is-selected" : ""} ${layer.climbCount === 0 ? "is-empty" : ""}`}
              aria-label={`${label}: ${layer.climbCount} ${layer.climbCount === 1 ? "climb" : "climbs"}${mixedCalibrations ? `, calibration ${layer.gradeCalibrationVersion}` : ""}`}
              aria-pressed={selectedBucket === layer.bucketKey}
              aria-controls={selectedBucket ? "pyramid-climbs" : undefined}
              onClick={() => onSelect(layer)}
            >
              <span className="pyramid-grade">{label}</span>
              <span className="pyramid-bar-track" aria-hidden="true">
                <span
                  className="pyramid-bar"
                  style={
                    {
                      "--layer-width": `${(layer.climbCount / largest) * 100}%`,
                    } as CSSProperties
                  }
                />
              </span>
              <span className="pyramid-count">{number(layer.climbCount)}</span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
