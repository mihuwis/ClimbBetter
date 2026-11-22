import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  formatPyramidPoints,
  localDate,
  pyramidClimbsPath,
  pyramidLayerLabel,
  pyramidReportPath,
  pyramidSearchParams,
  readPyramidFilters,
} from '../src/features/statistics/model/pyramid.helpers.ts';

const now = new Date(2026, 10, 4, 0, 15);
const defaults = { view: 'BOULDER', period: 'LAST_TWO_YEARS', style: 'RP', asOf: '2026-11-04' };

test('pyramid defaults send the local calendar date instead of a UTC conversion', () => {
  assert.equal(localDate(now), '2026-11-04');
  const localClock = {
    getFullYear: () => 2026,
    getMonth: () => 10,
    getDate: () => 4,
    toISOString: () => '2026-11-03T23:15:00.000Z',
  };
  assert.deepEqual(readPyramidFilters(new URLSearchParams(), localClock), defaults);
});

test('all twelve supported filter combinations survive a URL round trip', () => {
  for (const view of ['BOULDER', 'ROUTE_AND_CIRCUIT', 'COMBINED']) {
    for (const period of ['LAST_TWO_YEARS', 'ALL_TIME']) {
      for (const style of ['RP', 'OS']) {
        const filters = { view, period, style, asOf: '2024-02-29' };
        assert.deepEqual(readPyramidFilters(pyramidSearchParams(filters), now), filters);
      }
    }
  }
});

test('invalid filters and impossible dates recover to supported defaults', () => {
  assert.deepEqual(readPyramidFilters(new URLSearchParams('view=all&period=MONTH&style=FLASH&asOf=2026-02-29'), now), defaults);
  for (const asOf of ['0000-01-01', '2026-13-01', '2026-04-31', '2026-00-01', '2026-01-00', '2026-1-4', '1900-02-29']) {
    assert.equal(readPyramidFilters(new URLSearchParams({ asOf }), now).asOf, defaults.asOf);
  }
  for (const asOf of ['0001-01-01', '2000-02-29', '9999-12-31']) {
    assert.equal(readPyramidFilters(new URLSearchParams({ asOf }), now).asOf, asOf);
  }
});

test('report and paginated layer requests keep the same filters and preserve bucket keys', () => {
  const filters = { view: 'COMBINED', period: 'ALL_TIME', style: 'OS', asOf: '2026-11-04' };
  const report = new URL(pyramidReportPath(filters), 'http://local');
  const climbs = new URL(pyramidClimbsPath(filters, 'points:1:92.5', 50, 25), 'http://local');
  assert.equal(report.pathname, '/statistics/pyramid');
  assert.equal(climbs.pathname, '/statistics/pyramid/climbs');
  for (const [key, value] of Object.entries(filters)) assert.equal(climbs.searchParams.get(key), value);
  assert.equal(climbs.searchParams.get('bucketKey'), 'points:1:92.5');
  assert.equal(climbs.searchParams.get('offset'), '50');
  assert.equal(climbs.searchParams.get('limit'), '25');
  const firstPage = new URL(pyramidClimbsPath(defaults, 'grade:BOULDER_FONT:9'), 'http://local');
  assert.equal(firstPage.searchParams.get('offset'), '0');
  assert.equal(firstPage.searchParams.get('limit'), '50');
});

test('point presentation preserves separate 90 and 92.5 layers and snapshot labels', () => {
  assert.equal(formatPyramidPoints(90), '90');
  assert.equal(formatPyramidPoints(92.5), '92.5');
  assert.equal(formatPyramidPoints(null), '—');
  assert.equal(pyramidLayerLabel({ gradeLabel: '6B', gradePoints: null }), '6B');
  assert.equal(pyramidLayerLabel({ gradeLabel: null, gradePoints: 92.5 }), '92.5 points');
});
