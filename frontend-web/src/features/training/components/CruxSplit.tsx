import type { CruxDetails } from "../model/session.types";
import { number } from "../../../shared/lib/format";

export function CruxSplit({ crux }: { crux: CruxDetails }) {
  return (
    <section className="crux-split" aria-label="Estimated difficulty split">
      <h4>Estimated difficulty split</h4>
      <p className="muted">Model estimate for this entry's executed moves.</p>
      <table className="crux-split-table">
        <thead>
          <tr>
            <th scope="col">Section</th>
            <th scope="col">Moves</th>
            <th scope="col">Estimated grade</th>
            <th scope="col">Intensity</th>
          </tr>
        </thead>
        <tbody>
          <tr className="crux-split-row">
            <th scope="row">Harder</th>
            <td>{number(crux.hardMoveCount)}</td>
            <td>Font {crux.hardMoveGradeLabel}</td>
            <td>{number(crux.hardMoveIntensity)}</td>
          </tr>
          <tr className="crux-split-row">
            <th scope="row">Easier</th>
            <td>{number(crux.easyMoveCount)}</td>
            <td>Font {crux.easyMoveGradeLabel}</td>
            <td>{number(crux.easyMoveIntensity)}</td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}
