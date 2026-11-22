import { test, expect, type Page } from '@playwright/test';

type Filters = { view: string; period: string; style: string; asOf: string };
type Report = ReturnType<typeof pyramid>;
type ApiOptions = {
  report?: (filters: Filters, report: Report) => unknown;
  layerCount?: number;
  failReport?: boolean;
  failClimbs?: boolean;
  delayRp?: number;
};

function pyramid(filters: Filters) {
  const combined = filters.view === 'COMBINED';
  const boulder = filters.view === 'BOULDER';
  const scale = boulder ? 'BOULDER_FONT' : 'ROUTE_FRENCH';
  const labels = boulder ? ['7A', '6C+', '6C', '6B+', '6B'] : ['7a', '6c+', '6c', '6b+', '6b'];
  return {
    filters,
    windowStart: filters.period === 'ALL_TIME' ? null : `${Number(filters.asOf.slice(0, 4)) - 2}${filters.asOf.slice(4)}`,
    windowEnd: filters.asOf,
    uniqueClimbCount: combined ? 3 : 11,
    layers: combined
      ? [{ bucketKey: 'points:1:92.5', gradeLabel: null, gradeIndex: null, gradePoints: 92.5, gradeCalibrationVersion: 1, climbCount: 1 },
        { bucketKey: 'points:1:90', gradeLabel: null, gradeIndex: null, gradePoints: 90, gradeCalibrationVersion: 1, climbCount: 2 }]
      : labels.map((gradeLabel, index) => ({ bucketKey: `grade:${scale}:${15 - index}`, gradeLabel, gradeIndex: 15 - index, gradePoints: null, gradeCalibrationVersion: null, climbCount: [1, 0, 3, 2, 5][index] })),
    peak: combined
      ? { gradeLabel: null, gradeIndex: null, gradePoints: 92.5, gradeCalibrationVersion: 1 }
      : { gradeLabel: labels[0], gradeIndex: 15, gradePoints: null, gradeCalibrationVersion: null },
    levelAssessment: combined ? null : {
      gradeLabel: labels[1], gradeIndex: 14, method: 'PYRAMID_SUPPORT', confidence: 'MEDIUM', sampleSize: 11,
      supportScore: 3, supportAtOrAboveCount: 1, supportMinusOneCount: 3, supportMinusTwoCount: 2, algorithmVersion: 'pyramid-support-v1',
    },
    gradeCalibrationVersions: [1], assessmentUnavailableReason: combined ? 'COMBINED_SCALE' : null, peakUnavailableReason: null,
  };
}

async function api(page: Page, options: ApiOptions = {}) {
  const requests: URL[] = [];
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url()), path = url.pathname.replace('/api/v1', '');
    requests.push(url);
    if (path === '/identity/me') return route.fulfill({ json: { userId: 'mihu-1', displayName: 'Mihu' } });
    if (path.startsWith('/training/sessions/')) return route.fulfill({ json: {
      sessionId: 'session-1', sessionName: 'Source climbing session', sessionDate: '2026-10-15', areaName: 'Bronx', areaId: 'area-1',
      clientSessionId: 'client-session-1', version: 0, timeZoneId: 'Europe/Warsaw', durationMinutes: 60, totalMoves: 20,
      classicLoad: 90, adjustedLoad: 126, averageMoveIntensity: 6.3, maxBoulderGrade: null, maxRouteGrade: '7a', calculationModelVersion: 'v2', entries: [],
    } });
    if (path !== '/statistics/pyramid' && path !== '/statistics/pyramid/climbs') return route.fulfill({ json: [] });
    const filters: Filters = { view: url.searchParams.get('view')!, period: url.searchParams.get('period')!, style: url.searchParams.get('style')!, asOf: url.searchParams.get('asOf')! };
    if (path === '/statistics/pyramid') {
      if (options.failReport) return route.fulfill({ status: 503, json: { detail: 'Statistics are temporarily unavailable.' } });
      if (options.delayRp && filters.style === 'RP') await new Promise(resolve => setTimeout(resolve, options.delayRp));
      const report = pyramid(filters);
      if (options.layerCount) { report.layers[0].climbCount = options.layerCount; report.uniqueClimbCount += options.layerCount - 1; }
      return route.fulfill({ json: options.report?.(filters, report) ?? report }).catch(() => {});
    }
    if (options.failClimbs) return route.fulfill({ status: 503, json: { detail: 'Climbs are temporarily unavailable.' } });
    const offset = Number(url.searchParams.get('offset') ?? 0), limit = Number(url.searchParams.get('limit') ?? 50);
    const bucketKey = url.searchParams.get('bucketKey')!, combined = filters.view === 'COMBINED';
    const totalCount = options.layerCount ?? (bucketKey === 'points:1:90' ? 2 : 1);
    const items = Array.from({ length: Math.max(0, Math.min(totalCount - offset, limit)) }, (_, i) => {
      const index = offset + i;
      return { climbId: `climb-${index}`, climbName: index === 0 ? 'First snapshot climb' : `Distinct climb ${index + 1}`, areaId: 'area-1', areaName: index === 0 ? 'A very long climbing area name to check the small screen layout' : 'Bronx',
        climbType: combined && i === 0 ? 'BOULDER' : 'ROUTE', gradeScaleCode: combined && i === 0 ? 'BOULDER_FONT' : 'ROUTE_FRENCH',
        gradeLabel: combined && i === 0 ? (bucketKey.endsWith('92.5') ? '6B' : '6A+') : '7a', gradePoints: bucketKey.endsWith('92.5') ? 92.5 : 90,
        gradeCalibrationVersion: 1, achievedOn: '2026-10-15', ascentMode: index === 0 ? 'OS' : 'RP', sessionId: 'session-1', entryId: `entry-${index}` };
    });
    return route.fulfill({ json: { filters, bucketKey, totalCount, offset, limit, nextOffset: offset + limit < totalCount ? offset + limit : null, items } });
  });
  return requests;
}

const routeUrl = '/statistics?view=ROUTE_AND_CIRCUIT&period=LAST_TWO_YEARS&style=RP&asOf=2026-11-04';

test('statistics navigation and all twelve filter combinations use the long-term report API', async ({ page }) => {
  const requests = await api(page);
  await page.goto('/statistics');
  await expect(page.getByRole('heading', { name: 'Statistics', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Achievement pyramid', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Statistics', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('combobox', { name: 'Discipline', exact: true })).toHaveValue('BOULDER');
  await expect(page.getByRole('combobox', { name: 'Period', exact: true })).toHaveValue('LAST_TWO_YEARS');
  await expect(page.getByRole('combobox', { name: 'Style', exact: true })).toHaveValue('RP');
  for (const view of ['BOULDER', 'ROUTE_AND_CIRCUIT', 'COMBINED']) {
    await page.getByRole('combobox', { name: 'Discipline', exact: true }).selectOption(view);
    for (const period of ['ALL_TIME', 'LAST_TWO_YEARS']) {
      await page.getByRole('combobox', { name: 'Period', exact: true }).selectOption(period);
      for (const style of ['OS', 'RP']) {
        await page.getByRole('combobox', { name: 'Style', exact: true }).selectOption(style);
        await expect.poll(() => requests.some(url => url.pathname.endsWith('/pyramid') && url.searchParams.get('view') === view && url.searchParams.get('period') === period && url.searchParams.get('style') === style)).toBe(true);
        await expect(page.getByRole('button', { name: view === 'COMBINED' ? '92.5 points: 1 climb' : view === 'BOULDER' ? '7A: 1 climb' : '7a: 1 climb', exact: true })).toBeVisible();
      }
    }
  }
  expect(requests.filter(url => url.pathname.endsWith('/pyramid')).every(url => /^\d{4}-\d{2}-\d{2}$/.test(url.searchParams.get('asOf') ?? ''))).toBe(true);
});

test('native pyramid keeps grade gaps and separates the supported level from the highest ascent', async ({ page }, testInfo) => {
  await api(page);
  await page.goto(routeUrl);
  await expect(page.getByRole('button', { name: '7a: 1 climb', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '6c+: 0 climbs', exact: true })).toBeVisible();
  await expect(page.locator('.statistics-assessment').getByText('6c+', { exact: true })).toBeVisible();
  await expect(page.getByText(/medium confidence/i)).toBeVisible();
  await page.getByText('How this level is estimated', { exact: true }).click();
  await expect(page.getByText(/3(?:\.00)?\s*\/\s*6|support.*3|3.*support/i).first()).toBeVisible();
  await expect(page.getByText(/top.rope.*(?:excluded|count)|(?:excluded|count).*top.rope/i)).toBeVisible();
  await page.getByRole('button', { name: '7a: 1 climb', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Climbs at 7a', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: /First snapshot climb/ })).toHaveCount(1);
  await page.screenshot({ path: testInfo.outputPath('statistics-native-desktop.png'), fullPage: true });
});

test('combined exact points keep 90 and 92.5 distinct and preserve original grade scales', async ({ page }) => {
  const requests = await api(page);
  await page.goto('/statistics?view=COMBINED&period=ALL_TIME&style=RP&asOf=2026-11-04');
  await page.getByRole('button', { name: '90 points: 2 climbs', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Climbs at 90 points', exact: true })).toBeVisible();
  await expect(page.getByText('6A+', { exact: true })).toBeVisible();
  await expect(page.getByText('7a', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '92.5 points: 1 climb', exact: true })).toBeVisible();
  await expect.poll(() => requests.some(url => url.pathname.endsWith('/climbs') && url.searchParams.get('bucketKey') === 'points:1:90')).toBe(true);
  await expect(page.getByText(/(?:no|without|unavailable|not).*?(?:shared|combined|supported).*?(?:level|estimate)|(?:shared|combined).*?(?:level|estimate).*?(?:not|unavailable|defined)/i).first()).toBeVisible();
});

test('mixed point calibrations show separate groups and no shared highest result', async ({ page }) => {
  await api(page, { report: (_filters, report) => ({ ...report, uniqueClimbCount: 4, gradeCalibrationVersions: [1, 2], peak: null, peakUnavailableReason: 'MIXED_POINT_CALIBRATIONS',
    layers: [...report.layers, { bucketKey: 'points:2:90', gradeLabel: null, gradeIndex: null, gradePoints: 90, gradeCalibrationVersion: 2, climbCount: 1 }] }) });
  await page.goto('/statistics?view=COMBINED');
  await expect(page.getByText(/calibration 1/i).first()).toBeVisible();
  await expect(page.getByText(/calibration 2/i).first()).toBeVisible();
  await expect(page.getByText(/calibration.*(?:compar|maximum|highest)|(?:compar|maximum|highest).*calibration/i).first()).toBeVisible();
});

test('layer pagination and session navigation preserve the filters and selected page', async ({ page }) => {
  const requests = await api(page, { layerCount: 51 });
  await page.goto(routeUrl);
  await page.getByRole('button', { name: '7a: 51 climbs', exact: true }).click();
  await expect(page.getByRole('link', { name: /First snapshot climb/ })).toBeVisible();
  await page.getByRole('button', { name: 'Next page', exact: true }).click();
  await expect(page.getByRole('link', { name: /Distinct climb 51/ })).toBeVisible();
  await expect.poll(() => requests.some(url => url.pathname.endsWith('/climbs') && url.searchParams.get('offset') === '50' && url.searchParams.get('limit') === '50')).toBe(true);
  const selectedUrl = page.url();
  await page.reload();
  await expect(page.getByRole('link', { name: /Distinct climb 51/ })).toBeVisible();
  await page.getByRole('link', { name: /Distinct climb 51/ }).click();
  await expect(page.getByRole('heading', { name: 'Source climbing session', exact: true })).toBeVisible();
  await page.goBack();
  await expect(page).toHaveURL(selectedUrl);
  await expect(page.getByRole('link', { name: /Distinct climb 51/ })).toBeVisible();
  await page.getByRole('link', { name: /Distinct climb 51/ }).click();
  await page.getByRole('link', { name: 'Back to statistics', exact: false }).click();
  await expect(page).toHaveURL(selectedUrl);
  await expect(page.getByRole('link', { name: /Distinct climb 51/ })).toBeVisible();
  await page.getByRole('button', { name: 'Previous page', exact: true }).click();
  await expect(page.getByRole('link', { name: /First snapshot climb/ })).toBeVisible();
  await page.getByRole('combobox', { name: 'Style', exact: true }).selectOption('OS');
  await expect(page).not.toHaveURL(/[?&](?:bucket|offset)=/);
});

test('report and drilldown errors allow a retry without discarding the filters', async ({ page }) => {
  const options = { failReport: true, failClimbs: true };
  await api(page, options);
  await page.goto(routeUrl);
  await expect(page.getByRole('alert')).toContainText('Statistics are temporarily unavailable.');
  options.failReport = false;
  await page.getByRole('button', { name: 'Try again', exact: true }).click();
  await page.getByRole('button', { name: '7a: 1 climb', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Climbs are temporarily unavailable.');
  options.failClimbs = false;
  await page.getByRole('button', { name: 'Try again', exact: true }).click();
  await expect(page.getByRole('link', { name: /First snapshot climb/ })).toBeVisible();
  await expect(page.getByRole('combobox', { name: 'Discipline', exact: true })).toHaveValue('ROUTE_AND_CIRCUIT');
});

test('empty history and low-sample or fallback estimates are explained', async ({ page }) => {
  let mode = 'empty';
  await api(page, { report: (_filters, report) => mode === 'empty' ? { ...report, uniqueClimbCount: 0, layers: [], peak: null,
    levelAssessment: { ...report.levelAssessment, gradeLabel: null, gradeIndex: null, method: 'NO_ASCENTS', confidence: 'UNASSESSED', sampleSize: 0, supportScore: null, supportAtOrAboveCount: null, supportMinusOneCount: null, supportMinusTwoCount: null } }
    : { ...report, levelAssessment: { ...report.levelAssessment, gradeLabel: '7a', gradeIndex: 15, method: mode === 'small' ? 'LOW_SAMPLE_MAX' : 'PEAK_FALLBACK', confidence: 'LOW', sampleSize: mode === 'small' ? 3 : 11,
      supportScore: null, supportAtOrAboveCount: null, supportMinusOneCount: null, supportMinusTwoCount: null } } });
  await page.goto(routeUrl);
  await expect(page.getByText(/no (?:qualifying )?(?:ascents|achievements|climbs)/i).first()).toBeVisible();
  mode = 'small';
  await page.reload();
  await expect(page.getByText(/low confidence/i)).toBeVisible();
  await expect(page.getByText(/(?:fewer than 10|small sample|only 3|1.?9|at least 10)/i).first()).toBeVisible();
  mode = 'fallback';
  await page.reload();
  await expect(page.getByText(/low confidence/i)).toBeVisible();
  await expect(page.getByText(/fallback|insufficient support|(?:not|no).*?(?:support|threshold)|(?:support|threshold).*?(?:not|no)/i).first()).toBeVisible();
});

test('rapid filter changes never replace the current OS report with an older RP response', async ({ page }) => {
  await api(page, { delayRp: 500, report: (filters, report) => filters.style === 'RP' ? { ...report, uniqueClimbCount: 999 } : report });
  await page.goto(routeUrl);
  await page.getByRole('combobox', { name: 'Style', exact: true }).selectOption('OS');
  await expect(page.getByRole('button', { name: '7a: 1 climb', exact: true })).toBeVisible();
  await page.waitForTimeout(650);
  await expect(page.getByText('999', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('combobox', { name: 'Style', exact: true })).toHaveValue('OS');
});

test('mobile pyramid has no horizontal overflow and layers work from the keyboard', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await api(page);
  await page.goto(routeUrl);
  const layer = page.getByRole('button', { name: '7a: 1 climb', exact: true });
  await layer.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Climbs at 7a', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: /First snapshot climb/ })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('statistics-native-mobile.png'), fullPage: true });
});
