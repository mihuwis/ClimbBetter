import { test, expect } from '@playwright/test';
import { periodApi } from './period-summary.fixture';

const initial = '/statistics?report=summary&period=MONTH&year=2026&month=10&discipline=ALL';

test('summary shows period totals, successful achievements, and the two breakdowns', async ({ page }, testInfo) => {
  const { requests } = await periodApi(page);
  await page.goto(initial);
  await expect(page.getByRole('heading', { name: 'Training summary', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Training summary', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.locator('.period-summary-metric[aria-label="Sessions"] strong').first()).toHaveText('3');
  await expect(page.locator('.period-summary-metric[aria-label="Session time"] strong').first()).toHaveText('4 h');
  await expect(page.locator('.period-summary-metric[aria-label="Executed moves"] strong').first()).toHaveText('240');
  await expect(page.locator('.period-summary-metric[aria-label="Adjusted Load"] strong').first()).toHaveText('600');
  await expect(page.getByRole('heading', { name: 'Successful styles', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: /First light/ }).first()).toBeVisible();
  expect(requests.filter(url => url.pathname.endsWith('/statistics/summary'))).toHaveLength(1);
  await page.screenshot({ path: testInfo.outputPath('period-summary-desktop.png'), fullPage: true });
});

test('monthly and yearly navigation use exact calendar filters and preserve the discipline', async ({ page }) => {
  const { requests } = await periodApi(page);
  await page.goto(initial);
  await page.getByRole('combobox', { name: 'Discipline', exact: true }).selectOption('BOULDER');
  await expect(page.locator('.period-summary-metric[aria-label="Executed moves"] strong').first()).toHaveText('80');
  await expect(page.locator('.period-summary-metric[aria-label="Session time"] strong').first()).toHaveText('4 h');
  await expect(page.getByText(/time.*sessions.*(?:boulders|discipline)|sessions.*(?:boulders|discipline).*time/i).first()).toBeVisible();
  await page.getByRole('button', { name: 'Next period', exact: true }).click();
  await expect(page.getByRole('combobox', { name: 'Month', exact: true })).toHaveValue('11');
  await page.getByRole('combobox', { name: 'Period', exact: true }).selectOption('YEAR');
  await expect.poll(() => requests.some(url => url.searchParams.get('period') === 'YEAR' && !url.searchParams.has('month') && url.searchParams.get('discipline') === 'BOULDER')).toBe(true);
  await page.getByText('View chart data', { exact: true }).click();
  await page.getByRole('button', { name: 'Explore February', exact: true }).click();
  await expect(page.getByRole('combobox', { name: 'Period', exact: true })).toHaveValue('MONTH');
  await expect(page.getByRole('combobox', { name: 'Month', exact: true })).toHaveValue('2');
  await expect(page.getByRole('combobox', { name: 'Discipline', exact: true })).toHaveValue('BOULDER');
});

test('three load lines are enforced and chart choices survive reload without another API request on selection', async ({ page }) => {
  const { requests } = await periodApi(page);
  await page.goto(initial);
  async function toggle(name: string) {
    const checkbox = page.getByRole('checkbox', { name, exact: true });
    const checked = await checkbox.isChecked();
    await checkbox.click();
    if (checked) await expect(checkbox).not.toBeChecked();
    else await expect(checkbox).toBeChecked();
  }
  await toggle('Average Adjusted Load');
  await toggle('Max Adjusted Load');
  await expect(page.getByRole('checkbox', { name: 'Total Classic Load', exact: true })).toBeDisabled();
  await toggle('Total Adjusted Load');
  await toggle('Total Classic Load');
  await page.getByRole('combobox', { name: 'Bar metric', exact: true }).selectOption('durationMinutes');
  expect(requests.filter(url => url.pathname.endsWith('/statistics/summary'))).toHaveLength(1);
  await page.reload();
  await expect(page.getByRole('combobox', { name: 'Bar metric', exact: true })).toHaveValue('durationMinutes');
  for (const name of ['Average Adjusted Load', 'Max Adjusted Load', 'Total Classic Load']) {
    await expect(page.getByRole('checkbox', { name, exact: true })).toBeChecked();
  }
});

test('keyboard day drill-down opens its session and returns with filters and day preserved', async ({ page }) => {
  await periodApi(page);
  await page.goto(initial);
  await page.getByText('View chart data', { exact: true }).click();
  const day = page.getByRole('button', { name: 'Show sessions on 2 Oct', exact: true });
  await day.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: /Sessions on/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Short technique session', exact: true })).toBeVisible();
  const returnUrl = page.url();
  await page.getByRole('link', { name: 'Power and endurance', exact: true }).last().click();
  await expect(page.getByRole('heading', { name: 'Power and endurance', exact: true })).toBeVisible();
  await page.getByRole('link', { name: /Back to statistics/ }).click();
  await expect(page).toHaveURL(returnUrl);
  await expect(page.getByRole('heading', { name: /Sessions on/ })).toBeVisible();
});

test('failed reports can retry and an empty period has zero totals without invented achievements', async ({ page }) => {
  const { state } = await periodApi(page, { fail: true });
  await page.goto(initial);
  await expect(page.getByRole('alert')).toContainText('Period statistics unavailable.');
  state.fail = false;
  await page.getByRole('button', { name: 'Try again', exact: true }).click();
  await expect(page.locator('.period-summary-metric[aria-label="Sessions"] strong').first()).toHaveText('3');
  state.empty = true;
  await page.getByRole('button', { name: 'Refresh report', exact: true }).click();
  await expect(page.locator('.period-summary-metric[aria-label="Sessions"] strong').first()).toHaveText('0');
  await expect(page.getByRole('link', { name: /First light/ })).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'No training in this period', exact: true })).toBeVisible();
});

test('summary fits a phone with chart, selectors, and achievements available', async ({ page }, testInfo) => {
  await periodApi(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(initial);
  await expect(page.locator('.period-summary-metric[aria-label="Adjusted Load"] strong').first()).toHaveText('600');
  await expect(page.getByRole('group', { name: 'Training volume and load chart' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('period-summary-mobile.png'), fullPage: true });
});
