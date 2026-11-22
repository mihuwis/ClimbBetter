import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { PeriodBucket, SummaryPeriod } from "../model/period-summary.types";
import {
  barMetrics,
  formatMinutes,
  loadSeries,
  summaryBucketLabel,
  toggleLoadSeries,
} from "../model/period-summary.helpers";
import type { BarMetric, LoadSeries, SummaryChartSettings } from "../model/period-summary.helpers";
import { number } from "../../../shared/lib/format";

export function PeriodSummaryChart({ buckets, period, settings, onSettingsChange, onSelectBucket }: {
  buckets: PeriodBucket[];
  period: SummaryPeriod;
  settings: SummaryChartSettings;
  onSettingsChange: (settings: SummaryChartSettings) => void;
  onSelectBucket: (bucket: PeriodBucket) => void;
}) {
  const data = buckets.map((bucket) => ({
    ...bucket.metrics,
    label: summaryBucketLabel(bucket, period, true),
    fullLabel: summaryBucketLabel(bucket, period),
  }));
  return (
    <section className="card period-summary-chart" aria-labelledby="period-chart-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Volume & effort</p>
          <h3 id="period-chart-title">Training across the {period === "MONTH" ? "month" : "year"}</h3>
        </div>
        <span className="muted">{period === "MONTH" ? "Daily totals" : "Monthly totals"}</span>
      </div>
      <p className="muted period-chart-hint">
        {period === "MONTH" ? "Select a day to see its sessions." : "Select a month to explore its days."}
      </p>
      <div className="period-chart-axis-labels" aria-hidden="true"><span>{barMetrics[settings.barMetric]}</span><span>{settings.lines.length > 0 ? "Load" : ""}</span></div>
      <div className="period-chart-canvas" role="group" aria-label="Training volume and load chart">
        <ResponsiveContainer width="100%" height={340}>
          <ComposedChart data={data} margin={{ top: 12, right: 0, left: 0, bottom: 0 }}
            onClick={(state) => {
              const index = Number(state.activeTooltipIndex);
              if (state.activeTooltipIndex != null && Number.isInteger(index) && buckets[index]) onSelectBucket(buckets[index]);
            }}>
            <CartesianGrid stroke="#e8eee8" vertical={false} />
            <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fontSize: 12, fill: "#627069" }} minTickGap={12} />
            <YAxis yAxisId="volume" width={66} tickLine={false} axisLine={false}
              allowDecimals={false} tick={{ fontSize: 11, fill: "#627069" }}
              tickFormatter={(value: number) => settings.barMetric === "durationMinutes" ? formatMinutes(value) : number(value)} />
            {settings.lines.length > 0 && <YAxis yAxisId="load" orientation="right" width={64} tickLine={false} axisLine={false}
              tick={{ fontSize: 11, fill: "#627069" }} tickFormatter={(value: number) => number(value)} />}
            <Tooltip contentStyle={{ borderRadius: 10, borderColor: "#d5e0d7", fontSize: 13 }}
              labelFormatter={(_, payload) => payload[0]?.payload.fullLabel ?? ""}
              formatter={(value, name) => [settings.barMetric === "durationMinutes" && name === barMetrics.durationMinutes
                ? formatMinutes(Number(value)) : number(Number(value)), String(name)]} />
            <Legend wrapperStyle={{ paddingTop: 14, fontSize: 12 }} />
            <Bar yAxisId="volume" dataKey={settings.barMetric} name={barMetrics[settings.barMetric]}
              fill="#a2c2af" maxBarSize={32} radius={[4, 4, 0, 0]} isAnimationActive={false} />
            {settings.lines.map((key) => <Line key={key} yAxisId="load" type="linear" dataKey={key}
              name={loadSeries[key].label} stroke={loadSeries[key].color} strokeWidth={2.5}
              strokeDasharray={key.toLowerCase().includes("classic") ? "6 4" : undefined}
              dot={false} activeDot={{ r: 4 }} connectNulls={false} isAnimationActive={false} />)}
          </ComposedChart>
        </ResponsiveContainer>
      </div>
      <details className="period-chart-settings" open>
        <summary>Chart settings</summary>
        <div className="period-chart-controls">
          <label>
            Bar metric
            <select value={settings.barMetric} onChange={(event) => onSettingsChange({ ...settings, barMetric: event.target.value as BarMetric })}>
              {Object.entries(barMetrics).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
            </select>
          </label>
          <fieldset>
            <legend>Load lines · choose up to 3</legend>
            <div className="period-line-options">
              {Object.entries(loadSeries).map(([key, item]) => {
                const series = key as LoadSeries;
                const selected = settings.lines.includes(series);
                return <label className="checkbox-row" key={key}>
                  <input type="checkbox" checked={selected} disabled={!selected && settings.lines.length >= 3}
                    onChange={() => onSettingsChange({ ...settings, lines: toggleLoadSeries(settings.lines, series) })} />
                  <span className="period-series-color" style={{ background: item.color }} aria-hidden="true" />
                  {item.label}
                </label>;
              })}
            </div>
          </fieldset>
        </div>
      </details>
      <p className="muted period-metric-note">Maximum and average load refer to sessions within each {period === "MONTH" ? "day" : "month"}. Rest days have no maximum or average.</p>
      <details className="period-chart-data">
        <summary>View chart data</summary>
        <div className="period-table-scroll">
          <table>
            <caption className="sr-only">Training totals by {period === "MONTH" ? "day" : "month"}</caption>
            <thead><tr><th scope="col">{period === "MONTH" ? "Day" : "Month"}</th><th scope="col">Sessions</th><th scope="col">Time</th><th scope="col">Moves</th><th scope="col">Adjusted Load</th><th scope="col">Classic Load</th></tr></thead>
            <tbody>{buckets.map((bucket) => <tr key={bucket.windowStart}>
              <th scope="row"><button className="text-link" onClick={() => onSelectBucket(bucket)}
                aria-label={`${period === "MONTH" ? "Show sessions on" : "Explore"} ${summaryBucketLabel(bucket, period)}`}>{summaryBucketLabel(bucket, period)}</button></th>
              <td>{number(bucket.metrics.sessionCount)}</td><td>{formatMinutes(bucket.metrics.durationMinutes)}</td>
              <td>{number(bucket.metrics.executedMoves)}</td><td>{number(bucket.metrics.adjustedLoad)}</td><td>{number(bucket.metrics.classicLoad)}</td>
            </tr>)}</tbody>
          </table>
        </div>
      </details>
    </section>
  );
}
