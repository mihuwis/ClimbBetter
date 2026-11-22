import { Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { number } from "../../../shared/lib/format";
import type { PeriodTrainingSummary } from "../model/period-summary.types";

interface RingItem { label: string; value: number; fill: string }

function SummaryRing({ items, unit }: { items: RingItem[]; unit: string }) {
  const total = items.reduce((sum, item) => sum + item.value, 0);
  return (
    <div className="period-ring-layout">
      <div className="period-ring-graphic" aria-hidden="true">
        {total > 0 ? <ResponsiveContainer width="100%" height={190}>
          <PieChart accessibilityLayer={false}>
            <Pie data={items} dataKey="value" nameKey="label" innerRadius={62} outerRadius={86}
              paddingAngle={3} stroke="none" isAnimationActive={false} />
            <Tooltip formatter={(value) => number(Number(value))} />
          </PieChart>
        </ResponsiveContainer> : <div className="period-ring-empty" />}
        <div className="period-ring-center"><strong>{number(total)}</strong><span>{unit}</span></div>
      </div>
      <ul className="period-ring-legend">
        {items.map((item) => <li key={item.label}>
          <span className="period-series-color" style={{ background: item.fill }} aria-hidden="true" />
          <div><span>{item.label}</span><strong>{number(item.value)} <small>{total ? `${Math.round(item.value / total * 100)}%` : "—"}</small></strong></div>
        </li>)}
      </ul>
    </div>
  );
}

export function PeriodSummaryBreakdowns({ report }: { report: PeriodTrainingSummary }) {
  const disciplineItems: RingItem[] = report.disciplineBreakdown.map((item) => ({
    label: item.discipline === "BOULDER" ? "Boulders" : "Routes & circuits",
    value: item.executedMoves,
    fill: item.discipline === "BOULDER" ? "#2d7c68" : "#c58c49",
  }));
  const colors = { OS: "#2d7c68", FLASH: "#5b9fbe", RP: "#c58c49" };
  const styleItems: RingItem[] = report.styleBreakdown.map((item) => ({
    label: item.style === "FLASH" ? "Flash" : item.style,
    value: item.climbCount,
    fill: colors[item.style],
  }));
  return (
    <div className="period-breakdown-grid">
      <section className="card period-breakdown-card" aria-labelledby="discipline-breakdown-title">
        <p className="eyebrow">Where the work went</p>
        <h3 id="discipline-breakdown-title">Climbing disciplines</h3>
        <SummaryRing items={disciplineItems} unit="moves" />
        <p className="muted period-metric-note">Share of climbing moves, including attempts and top-rope training.
          {report.warmupMoves > 0 ? ` ${number(report.warmupMoves)} warm-up moves are shown separately from this split.` : " Warm-ups have no assigned discipline."}</p>
      </section>
      <section className="card period-breakdown-card" aria-labelledby="style-breakdown-title">
        <p className="eyebrow">Completed climbs</p>
        <h3 id="style-breakdown-title">Successful styles</h3>
        <SummaryRing items={styleItems} unit="unique climbs" />
        <p className="muted period-metric-note">Each climb counts once, using its best successful style in this period: OS, then Flash, then RP. Attempts and top-rope climbing are excluded.</p>
      </section>
    </div>
  );
}
