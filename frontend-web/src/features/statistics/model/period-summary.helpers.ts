import type {
  PeriodBucket,
  PeriodSummaryFilters,
} from "./period-summary.types";

export const summaryDisciplineLabels = {
  ALL: "All",
  BOULDER: "Boulders",
  ROUTE_AND_CIRCUIT: "Routes & circuits",
} as const;

export const barMetrics = {
  sessionCount: "Sessions",
  durationMinutes: "Session time",
  executedMoves: "Executed moves",
} as const;

export const loadSeries = {
  adjustedLoad: { label: "Total Adjusted Load", color: "#17695c" },
  averageAdjustedLoad: { label: "Average Adjusted Load", color: "#3684a3" },
  maxAdjustedLoad: { label: "Max Adjusted Load", color: "#ac6b2e" },
  classicLoad: { label: "Total Classic Load", color: "#7e5fa6" },
  averageClassicLoad: { label: "Average Classic Load", color: "#a65c79" },
  maxClassicLoad: { label: "Max Classic Load", color: "#6c7c37" },
} as const;

export type BarMetric = keyof typeof barMetrics;
export type LoadSeries = keyof typeof loadSeries;
export interface SummaryChartSettings {
  barMetric: BarMetric;
  lines: LoadSeries[];
}

function integer(value: string | null, min: number, max: number, fallback: number) {
  if (!value || !/^\d+$/.test(value)) return fallback;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed >= min && parsed <= max
    ? parsed
    : fallback;
}

export function readSummaryFilters(search: URLSearchParams, now = new Date()): PeriodSummaryFilters {
  return {
    period: search.get("period") === "YEAR" ? "YEAR" : "MONTH",
    year: integer(search.get("year"), 1, 9998, now.getFullYear()),
    month: integer(search.get("month"), 1, 12, now.getMonth() + 1),
    discipline: search.get("discipline") === "BOULDER" || search.get("discipline") === "ROUTE_AND_CIRCUIT"
      ? search.get("discipline") as PeriodSummaryFilters["discipline"]
      : "ALL",
  };
}

export function readChartSettings(search: URLSearchParams): SummaryChartSettings {
  const rawBar = search.get("bar");
  const rawLines = search.get("lines");
  const validLines = rawLines === null ? ["adjustedLoad"]
    : rawLines.split(",").filter((line): line is LoadSeries => Object.hasOwn(loadSeries, line));
  return {
    barMetric: rawBar && Object.hasOwn(barMetrics, rawBar) ? rawBar as BarMetric : "executedMoves",
    lines: [...new Set(validLines)].slice(0, 3) as LoadSeries[],
  };
}

export function summarySearchParams(
  filters: PeriodSummaryFilters,
  settings?: SummaryChartSettings,
  selectedDay?: string,
) {
  const search = new URLSearchParams({
    report: "summary",
    period: filters.period,
    year: String(filters.year),
    month: String(filters.month),
    discipline: filters.discipline,
  });
  if (settings) {
    search.set("bar", settings.barMetric);
    search.set("lines", settings.lines.join(","));
  }
  if (selectedDay) search.set("day", selectedDay);
  return search;
}

export function summaryReportPath(filters: PeriodSummaryFilters) {
  const search = new URLSearchParams({ period: filters.period, year: String(filters.year), discipline: filters.discipline });
  if (filters.period === "MONTH") search.set("month", String(filters.month));
  return `/statistics/summary?${search}`;
}

export function shiftSummaryPeriod(filters: PeriodSummaryFilters, direction: number): PeriodSummaryFilters {
  if (filters.period === "YEAR") {
    return { ...filters, year: Math.max(1, Math.min(9998, filters.year + direction)) };
  }
  const total = (filters.year - 1) * 12 + filters.month - 1 + direction;
  const clamped = Math.max(0, Math.min(9998 * 12 - 1, total));
  return { ...filters, year: Math.floor(clamped / 12) + 1, month: clamped % 12 + 1 };
}

function localDay(value: string) {
  return new Date(`${value}T12:00:00`);
}

export function summaryPeriodLabel(filters: PeriodSummaryFilters) {
  if (filters.period === "YEAR") return String(filters.year);
  return new Intl.DateTimeFormat("en-GB", { month: "long", year: "numeric" })
    .format(localDay(`${String(filters.year).padStart(4, "0")}-${String(filters.month).padStart(2, "0")}-01`));
}

export function summaryBucketLabel(bucket: PeriodBucket, period: PeriodSummaryFilters["period"], short = false) {
  return new Intl.DateTimeFormat("en-GB", period === "YEAR"
    ? { month: short ? "short" : "long" }
    : short ? { day: "numeric" } : { day: "numeric", month: "short" })
    .format(localDay(bucket.windowStart));
}

export function formatMinutes(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value) || value < 0) return "—";
  const rounded = Math.round(value);
  const hours = Math.floor(rounded / 60);
  const minutes = rounded % 60;
  return hours ? `${hours} h${minutes ? ` ${minutes} min` : ""}` : `${minutes} min`;
}

export function toggleLoadSeries(selected: LoadSeries[], series: LoadSeries): LoadSeries[] {
  if (selected.includes(series)) return selected.filter((value) => value !== series);
  return selected.length < 3 ? [...selected, series] : selected;
}
