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

test("portal submission form exposes separate draft and submit actions", async ({ page }) => {
  await page.goto("/portal/");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("新建送检单").first().click();
  await expect(page.getByText("SUBMISSION VER.15")).toBeVisible();
  await expect(page.getByRole("button", { name: "保存草稿" })).toBeVisible();
  await expect(page.getByRole("button", { name: "提交送检单" })).toBeVisible();
});

test("internal sales can open submission acceptance desk", async ({ page }) => {
  await page.goto("/internal/");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("送检受理").first().click();
  await expect(page.getByText("SUBMISSION ACCEPTANCE")).toBeVisible();
  await expect(page.getByRole("button", { name: /刷\s*新/ })).toBeVisible();
});

test("internal sales can open project list after split flow", async ({ page }) => {
  await page.goto("/internal/");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("项目详情").first().click();
  await expect(page.getByText("PROJECT LIST")).toBeVisible();
  await expect(page.getByRole("button", { name: /刷\s*新/ })).toBeVisible();
  await expect(page.getByText(/P-\d{4}-/).first()).toBeVisible();
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
