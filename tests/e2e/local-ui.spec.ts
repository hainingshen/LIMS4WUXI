import { mkdirSync } from "node:fs";

import { expect, test } from "@playwright/test";

const screenshotDir = "test-results/ui-screenshots";

test.beforeAll(() => {
  mkdirSync(screenshotDir, { recursive: true });
});

test("internal desktop aligns to LIMS workbench shell", async ({ page }) => {
  await page.goto("/internal/");
  await expect(page.getByText("无锡锡山 LIMS 内部工作台")).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/internal-login-desktop.png`, fullPage: true });

  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("工作中心").first()).toBeVisible();
  await expect(page.getByText("PROJECT WORK CENTER")).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/internal-work-center-desktop.png`, fullPage: true });
});

test("portal desktop aligns to customer portal shell", async ({ page }) => {
  await page.goto("/portal/");
  await expect(page.getByText("南京大学锡山应用生物技术研究所")).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/portal-login-desktop.png`, fullPage: true });

  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("客户首页").first()).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/portal-dashboard-desktop.png`, fullPage: true });
});

test("mobile portal keeps 390px workflow usable", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });

  await page.goto("/portal/");
  await expect(page.getByText("客户门户").first()).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/portal-login-mobile.png`, fullPage: true });

  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("入口")).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(hasHorizontalOverflow).toBe(false);
  await page.screenshot({ path: `${screenshotDir}/portal-dashboard-mobile.png`, fullPage: true });
});
