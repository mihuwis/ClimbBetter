import { test, expect } from '@playwright/test';
import { writeFile } from 'node:fs/promises';
import type { PeriodTrainingSummary, PeriodSummaryFilters } from '../../src/features/statistics/model/period-summary.types';
import { formatMinutes, summaryBucketLabel, summaryReportPath } from '../../src/features/statistics/model/period-summary.helpers';
import { number } from '../../src/shared/lib/format';

// Explicit opt-in. Uses the live API; creates, edits and deletes no training data.
test('period report agrees with real history across six views and returns from a source session', async ({ page }, testInfo) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  const historyResponse = await page.request.get('/api/v1/dashboard/sessions');
  expect(historyResponse.ok()).toBe(true);
  const history: { sessionDate: string }[] = await historyResponse.json();
  const date = history[0]?.sessionDate ?? new Date().toISOString().slice(0, 10);
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  await page.goto(`/statistics?report=summary&period=YEAR&year=${year}&month=${month}&discipline=ALL`);
  await expect(page.getByRole('heading', { name: 'Training summary', exact: true })).toBeVisible();
  const verified: { period: string; discipline: string; sessions: number; moves: number }[] = [];
  for (const period of ['YEAR', 'MONTH'] as const) {
    await page.getByRole('combobox', { name: 'Period', exact: true }).selectOption(period);
    for (const discipline of ['ALL', 'BOULDER', 'ROUTE_AND_CIRCUIT'] as const) {
      await page.getByRole('combobox', { name: 'Discipline', exact: true }).selectOption(discipline);
      const filters: PeriodSummaryFilters = { period, year, month, discipline };
      const response = await page.request.get(`/api/v1${summaryReportPath(filters)}`);
      expect(response.ok()).toBe(true);
      const report: PeriodTrainingSummary = await response.json();
      for (const [label, value] of [
        ['Sessions', number(report.summary.sessionCount)], ['Session time', formatMinutes(report.summary.durationMinutes)],
        ['Executed moves', number(report.summary.executedMoves)], ['Adjusted Load', number(report.summary.adjustedLoad)],
        ['Classic Load', number(report.summary.classicLoad)], ['Active days', number(report.summary.activeDays)],
      ]) {
        await expect(page.locator(`.period-summary-metric[aria-label="${label}"] strong`).first()).toHaveText(value);
      }
      verified.push({ period, discipline, sessions: report.summary.sessionCount, moves: report.summary.executedMoves });
      if (period === 'YEAR' && discipline === 'ALL') {
        await page.screenshot({ path: testInfo.outputPath('period-summary-real-desktop.png'), fullPage: true });
      }
    }
  }
  await page.getByRole('combobox', { name: 'Discipline', exact: true }).selectOption('ALL');
  const monthly: PeriodTrainingSummary = await (await page.request.get(`/api/v1${summaryReportPath({ period: 'MONTH', year, month, discipline: 'ALL' })}`)).json();
  const active = monthly.buckets.find(bucket => bucket.sessions.length > 0);
  if (active) {
    await page.getByText('View chart data', { exact: true }).click();
    await page.getByRole('button', { name: `Show sessions on ${summaryBucketLabel(active, 'MONTH')}`, exact: true }).click();
    const session = active.sessions[0];
    const returnUrl = page.url();
    await page.locator(`.period-selected-sessions a[href="/sessions/${session.sessionId}"]`).click();
    await expect(page.getByRole('heading', { name: session.sessionName, exact: true })).toBeVisible();
    await page.getByRole('link', { name: /Back to statistics/ }).click();
    await expect(page).toHaveURL(returnUrl);
    await expect(page.getByRole('heading', { name: /Sessions on/ })).toBeVisible();
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Training summary', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('period-summary-real-mobile.png'), fullPage: true });
  expect(errors).toEqual([]);
  await writeFile(testInfo.outputPath('period-summary-real-api.json'), JSON.stringify({ result: 'PASS', views: verified }, null, 2));
});
