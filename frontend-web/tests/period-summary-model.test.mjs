import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  readSummaryFilters, summarySearchParams, summaryReportPath, shiftSummaryPeriod,
  readChartSettings, toggleLoadSeries, formatMinutes, summaryPeriodLabel, summaryBucketLabel,
} from '../src/features/statistics/model/period-summary.helpers.ts';

const defaults = { period: 'MONTH', year: 2026, month: 11, discipline: 'ALL' };
const localClock = { getFullYear: () => 2026, getMonth: () => 10, toISOString: () => '2026-10-31T23:30:00Z' };

test('summary defaults follow the local month and invalid URL values recover within supported bounds', () => {
  assert.deepEqual(readSummaryFilters(new URLSearchParams(), localClock), defaults);
  for (const query of ['period=ALL_TIME&year=0&month=13&discipline=ROUTE', 'year=9999&month=0',
    'year=-1&month=-1', 'year=1.5&month=1e1', 'year=99999999999999999&month=NaN']) {
    assert.deepEqual(readSummaryFilters(new URLSearchParams(query), localClock), defaults);
  }
});

test('all six period and discipline combinations survive navigation with chart and selected-day state', () => {
  for (const period of ['MONTH', 'YEAR']) {
    for (const discipline of ['ALL', 'BOULDER', 'ROUTE_AND_CIRCUIT']) {
      const filters = { period, year: 2024, month: 2, discipline };
      const chart = { barMetric: 'durationMinutes', lines: ['adjustedLoad', 'maxClassicLoad'] };
      const search = summarySearchParams(filters, chart, '2024-02-29');
      assert.equal(search.get('report'), 'summary');
      assert.equal(search.get('day'), '2024-02-29');
      assert.deepEqual(readSummaryFilters(search, localClock), filters);
      assert.deepEqual(readChartSettings(search), chart);
    }
  }
});

test('API paths omit UI state and a month for yearly summaries while retaining discipline', () => {
  const year = new URL(summaryReportPath({ ...defaults, period: 'YEAR', discipline: 'BOULDER' }), 'http://local');
  assert.equal(year.pathname, '/statistics/summary');
  assert.deepEqual([...year.searchParams.keys()].sort(), ['discipline', 'period', 'year']);
  assert.equal(year.searchParams.get('discipline'), 'BOULDER');
  const month = new URL(summaryReportPath(defaults), 'http://local');
  assert.equal(month.searchParams.get('month'), '11');
});

test('period navigation crosses years correctly and stays within API date bounds', () => {
  assert.deepEqual(shiftSummaryPeriod({ ...defaults, month: 12 }, 1), { ...defaults, year: 2027, month: 1 });
  assert.deepEqual(shiftSummaryPeriod({ ...defaults, month: 1 }, -1), { ...defaults, year: 2025, month: 12 });
  assert.deepEqual(shiftSummaryPeriod({ ...defaults, year: 1, month: 1 }, -1), { ...defaults, year: 1, month: 1 });
  assert.deepEqual(shiftSummaryPeriod({ ...defaults, year: 9998, month: 12 }, 1), { ...defaults, year: 9998, month: 12 });
  assert.equal(shiftSummaryPeriod({ ...defaults, period: 'YEAR' }, -1).year, 2025);
});

test('chart settings preserve a deliberate no-line selection and enforce three distinct valid load series', () => {
  assert.deepEqual(readChartSettings(new URLSearchParams()), { barMetric: 'executedMoves', lines: ['adjustedLoad'] });
  assert.deepEqual(readChartSettings(new URLSearchParams('bar=minutes&lines=')), { barMetric: 'executedMoves', lines: [] });
  assert.deepEqual(readChartSettings(new URLSearchParams('lines=adjustedLoad,adjustedLoad,invalid,maxClassicLoad,classicLoad,maxAdjustedLoad')).lines,
    ['adjustedLoad', 'maxClassicLoad', 'classicLoad']);
  const selected = ['adjustedLoad', 'maxClassicLoad', 'classicLoad'];
  assert.deepEqual(toggleLoadSeries(selected, 'averageClassicLoad'), selected);
  assert.deepEqual(toggleLoadSeries(selected, 'adjustedLoad'), ['maxClassicLoad', 'classicLoad']);
  assert.deepEqual(selected, ['adjustedLoad', 'maxClassicLoad', 'classicLoad']);
});

test('time formatting supports whole-period totals above 24 hours and month labels preserve leap days', () => {
  assert.equal(formatMinutes(0), '0 min');
  assert.equal(formatMinutes(1501), '25 h 1 min');
  assert.equal(formatMinutes(null), '—');
  assert.equal(summaryPeriodLabel({ ...defaults, year: 2024, month: 2 }), 'February 2024');
  assert.equal(summaryBucketLabel({ windowStart: '2024-02-29' }, 'MONTH'), '29 Feb');
});
