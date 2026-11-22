import { localDate } from '../../../shared/lib/format.ts';
import type { PyramidFilters, PyramidPeriod, PyramidPeak, PyramidStyle, PyramidView } from './statistics.types';

export { localDate };

export const pyramidViewLabels: Record<PyramidView, string> = {
  BOULDER: 'Boulders',
  ROUTE_AND_CIRCUIT: 'Routes & circuits',
  COMBINED: 'Combined points',
};

export const pyramidPeriodLabels: Record<PyramidPeriod, string> = {
  LAST_TWO_YEARS: 'Last 2 years',
  ALL_TIME: 'All time',
};

export const pyramidStyleLabels: Record<PyramidStyle, string> = {
  RP: 'RP + OS + Flash',
  OS: 'OS only',
};

function validDate(value: string | null): value is string {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  if (year < 1 || month < 1 || month > 12 || day < 1) return false;
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const monthDays = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return day <= monthDays[month - 1];
}

export function readPyramidFilters(search: URLSearchParams, now = new Date()): PyramidFilters {
  const view = search.get('view');
  const period = search.get('period');
  const style = search.get('style');
  const asOf = search.get('asOf');
  return {
    view: view === 'ROUTE_AND_CIRCUIT' || view === 'COMBINED' ? view : 'BOULDER',
    period: period === 'ALL_TIME' ? period : 'LAST_TWO_YEARS',
    style: style === 'OS' ? style : 'RP',
    asOf: validDate(asOf) ? asOf : localDate(now),
  };
}

export function pyramidSearchParams(filters: PyramidFilters): URLSearchParams {
  return new URLSearchParams({
    view: filters.view,
    period: filters.period,
    style: filters.style,
    asOf: filters.asOf,
  });
}

/** Paths are relative to the existing shared API client's /api/v1 base. */
export function pyramidReportPath(filters: PyramidFilters): string {
  return `/statistics/pyramid?${pyramidSearchParams(filters)}`;
}

export function pyramidClimbsPath(
  filters: PyramidFilters,
  bucketKey: string,
  offset = 0,
  limit = 50,
): string {
  const search = pyramidSearchParams(filters);
  search.set('bucketKey', bucketKey);
  search.set('offset', String(offset));
  search.set('limit', String(limit));
  return `/statistics/pyramid/climbs?${search}`;
}

/** Formatting never merges point buckets or rounds 92.5 into 90. */
export function formatPyramidPoints(value: number | null): string {
  return value == null ? '—' : new Intl.NumberFormat('en-GB', { maximumFractionDigits: 20 }).format(value);
}

export function pyramidLayerLabel(layer: PyramidPeak): string {
  return layer.gradeLabel ?? (layer.gradePoints == null ? '—' : `${formatPyramidPoints(layer.gradePoints)} points`);
}
