import { test, expect } from '@playwright/test';
import { writeFile } from 'node:fs/promises';
import { pyramidLayerLabel, pyramidReportPath, pyramidClimbsPath } from '../../src/features/statistics/model/pyramid.helpers';
import type { AchievementPyramid, PyramidClimbsPage, PyramidFilters } from '../../src/features/statistics/model/statistics.types';
import { localDate, number } from '../../src/shared/lib/format';

// Explicit opt-in. Reads existing history only; does not create or edit sessions.
test('Statistics agrees with the real API in all twelve views and returns from source sessions', async ({ page }, testInfo) => {
  await page.goto('/statistics');
  await expect(page.getByRole('heading', { name: 'Statistics', exact: true })).toBeVisible();
  const asOf = await page.evaluate(() => {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  });
  const results: { filters: PyramidFilters; uniqueClimbCount: number; layers: number }[] = [];
  for (const view of ['BOULDER', 'COMBINED', 'ROUTE_AND_CIRCUIT'] as const) {
    await page.getByRole('combobox', { name: 'Discipline', exact: true }).selectOption(view);
    for (const period of ['LAST_TWO_YEARS', 'ALL_TIME'] as const) {
      await page.getByRole('combobox', { name: 'Period', exact: true }).selectOption(period);
      for (const style of ['OS', 'RP'] as const) {
        await page.getByRole('combobox', { name: 'Style', exact: true }).selectOption(style);
        const filters = { view, period, style, asOf };
        const response = await page.request.get(`/api/v1${pyramidReportPath(filters)}`);
        expect(response.ok()).toBe(true);
        const report: AchievementPyramid = await response.json();
        await expect(page.locator('.statistics-metric').filter({ hasText: 'Unique climbs' }).locator('strong')).toHaveText(number(report.uniqueClimbCount));
        if (!report.uniqueClimbCount) {
          await expect(page.getByRole('heading', { name: 'No achievements in this view' })).toBeVisible();
        } else {
          const layer = report.layers.find(item => item.climbCount > 0)!;
          const label = pyramidLayerLabel(layer);
          await page.getByRole('button', { name: `${label}: ${layer.climbCount} ${layer.climbCount === 1 ? 'climb' : 'climbs'}`, exact: true }).click();
          const climbsResponse = await page.request.get(`/api/v1${pyramidClimbsPath(filters, layer.bucketKey)}`);
          expect(climbsResponse.ok()).toBe(true);
          const climbs: PyramidClimbsPage = await climbsResponse.json();
          expect(climbs.totalCount).toBe(layer.climbCount);
          await expect(page.getByRole('heading', { name: `Climbs at ${label}`, exact: true })).toBeVisible();
          await expect(page.locator('.pyramid-climb-list li')).toHaveCount(climbs.items.length);
        }
        results.push({ filters, uniqueClimbCount: report.uniqueClimbCount, layers: report.layers.length });
      }
    }
  }
  await page.screenshot({ path: testInfo.outputPath('statistics-real-desktop.png'), fullPage: true });
  const source = page.locator('.pyramid-climb-list a').first();
  if (await source.count()) {
    const originalUrl = page.url();
    await source.click();
    await expect(page).toHaveURL(/\/sessions\/[^/]+$/);
    await page.getByRole('link', { name: 'Back to statistics', exact: false }).click();
    await expect(page).toHaveURL(originalUrl);
    await expect(page.locator('.pyramid-climb-list a').first()).toBeVisible();
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: testInfo.outputPath('statistics-real-mobile.png'), fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await writeFile(testInfo.outputPath('statistics-real-http-report.json'), JSON.stringify({ checkedAt: localDate(), results }, null, 2));
});
