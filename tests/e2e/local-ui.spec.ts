import { mkdirSync } from "node:fs";

import { expect, test } from "@playwright/test";

const screenshotDir = "test-results/ui-screenshots";

test.beforeAll(() => {
  mkdirSync(screenshotDir, { recursive: true });
});

async function loginPortalDemo(page: import("@playwright/test").Page) {
  await page.locator("#username").fill("portal.demo.seed");
  await page.locator("#password").fill("Applicant123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
}

async function loginPortalViewer(page: import("@playwright/test").Page) {
  await page.locator("#username").fill("portal.viewer");
  await page.locator("#password").fill("Viewer123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
}

test("internal desktop aligns to LIMS workbench shell", async ({ page }) => {
  await page.goto("/internal/");
  await expect(page.getByText("无锡锡山 LIMS 内部工作台")).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/internal-login-desktop.png`, fullPage: true });

  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("工作中心").first()).toBeVisible();
  await expect(page.getByText("PROJECT WORK CENTER")).toBeVisible();
  await page.locator(".ant-segmented-item", { hasText: "正常" }).click();
  const addBlocker = page.getByRole("button", { name: "新增阻塞" }).first();
  await expect(addBlocker).toBeVisible();
  await addBlocker.click();
  await expect(page.getByText("阻塞类型", { exact: true })).toBeVisible();
  await expect(page.getByText("预计恢复时间", { exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
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

test("portal deeplink opens submission form after login", async ({ page }) => {
  await page.goto("/portal/submissions/new");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("SUBMISSION VER.15")).toBeVisible();
  await expect(page).toHaveURL(/\/portal\/submissions\/new$/);
});

test("portal projects page shows customer progress", async ({ page }) => {
  await page.goto("/portal/projects");
  await loginPortalDemo(page);
  await expect(page.getByText("CUSTOMER PROJECTS")).toBeVisible();
  await expect(page.getByText(/DEMO-SEED-P-|P-\d{4}-/).first()).toBeVisible();
});

test("portal business pages expose submissions contracts reports and account", async ({ page }) => {
  await page.goto("/portal/");
  await page.getByRole("button", { name: /登\s*录/ }).click();

  await page.getByRole("menuitem", { name: "我的送检单" }).click();
  await expect(page.getByText("SUBMISSIONS")).toBeVisible();
  await expect(page.getByRole("button", { name: "查看详情" }).first()).toBeVisible();

  await page.getByRole("menuitem", { name: "合同协议" }).click();
  await expect(page.getByText("CONTRACTS")).toBeVisible();

  await page.getByRole("menuitem", { name: "报告中心" }).click();
  await expect(page.getByText("REPORT CENTER")).toBeVisible();

  await page.getByRole("menuitem", { name: "账户信息" }).click();
  await expect(page.getByText("ACCOUNT")).toBeVisible();
  await expect(page.getByText("portal.applicant")).toBeVisible();
});

test("portal report preview deeplink shows watermarked report state", async ({ page }) => {
  await page.goto("/portal/projects");
  await loginPortalDemo(page);
  await page.getByRole("button", { name: "预览报告" }).first().click();
  await expect(page.getByText("REPORT PREVIEW")).toBeVisible();
  await expect(page).toHaveURL(/\/portal\/reports\/[^/]+\/preview$/);
  await expect(page.getByText("报告状态")).toBeVisible();
  await expect(page.getByText(/DEMO-SEED-TR-|RPT-\d{4}-/).first()).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/portal-report-preview-desktop.png`, fullPage: true });
});

test("portal contract confirmation deeplink shows scoped agreement", async ({ page }) => {
  await page.goto("/portal/projects");
  await loginPortalDemo(page);
  await page.getByRole("button", { name: /确认协议|查看协议/ }).first().click();
  await expect(page.getByText("CONTRACT CONFIRM")).toBeVisible();
  await expect(page).toHaveURL(/\/portal\/contracts\/[^/]+\/confirm$/);
  await expect(page.getByText("协议状态")).toBeVisible();
  await expect(page.getByText(/DEMO-SEED-C-|CON-\d{4}-/).first()).toBeVisible();
  await page.screenshot({ path: `${screenshotDir}/portal-contract-confirm-desktop.png`, fullPage: true });
});

test("portal routes show explicit 403 and 404 states", async ({ page }) => {
  await page.goto("/portal/unknown-route");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("404")).toBeVisible();
  await expect(page.getByText("页面不存在")).toBeVisible();

  await page.evaluate(() => window.localStorage.clear());
  await page.goto("/portal/submissions/new");
  await loginPortalViewer(page);
  await expect(page.getByText("403")).toBeVisible();
  await expect(page.getByRole("heading", { name: "无权访问" })).toBeVisible();
});

test("internal deeplink opens master data page after login", async ({ page }) => {
  await page.goto("/internal/master-data");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("MASTER DATA", { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/internal\/master-data$/);
});

test("internal routes show explicit 403 and 404 states", async ({ page }) => {
  await page.goto("/internal/unknown-route");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("404")).toBeVisible();
  await expect(page.getByText("页面不存在")).toBeVisible();

  await page.evaluate(() => window.localStorage.clear());
  await page.goto("/internal/system");
  await page.locator("#username").fill("internal.sample");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page.getByText("403")).toBeVisible();
  await expect(page.getByRole("heading", { name: "无权访问" })).toBeVisible();
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

test("internal sales can open contract list after signing flow", async ({ page }) => {
  await page.goto("/internal/");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("合同协议").first().click();
  await expect(page.getByText("CONTRACT LIST")).toBeVisible();
  await expect(page.getByRole("button", { name: /刷\s*新/ })).toBeVisible();
  await expect(page.getByText(/CON-\d{4}-/).first()).toBeVisible();
});

test("sample manager can open sample receiving desk", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.sample");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("样品收样台").first().click();
  await expect(page.getByText("SAMPLE RECEIVING DESK")).toBeVisible();
  await expect(page.getByPlaceholder("送检单 / 项目 / 样品 / 批号")).toBeVisible();
});

test("SD can open study plan dispatch page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.sd");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("方案编制").first().click();
  await expect(page.getByText("STUDY PLAN DISPATCH")).toBeVisible();
  await expect(page.getByText("试验工单")).toBeVisible();
});

test("inspector can open work order execution page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.inspector");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("试验执行").first().click();
  await expect(page.getByText("WORK ORDER EXECUTION")).toBeVisible();
  await expect(page.getByRole("heading", { name: "原始记录" })).toBeVisible();
});

test("report preparer can open report workbench", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.report_preparer");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("报告管理").first().click();
  await expect(page.getByText("REPORT WORKBENCH")).toBeVisible();
  await expect(page.getByRole("heading", { name: "报告列表" })).toBeVisible();
});

test("quality manager can open report registry page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("报告注册中心").first().click();
  await expect(page.getByText("REPORT REGISTRY")).toBeVisible();
  await expect(page.getByRole("heading", { name: "发放日志" })).toBeVisible();
});

test("equipment manager can open device file page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.equipment_manager");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("设备文件").first().click();
  await expect(page.getByText("DEVICE FILES")).toBeVisible();
  await expect(page.getByRole("heading", { name: "文件登记" })).toBeVisible();
});

test("equipment manager can open device booking page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.equipment_manager");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("设备预约").first().click();
  await expect(page.getByText("DEVICE BOOKINGS")).toBeVisible();
  await expect(page.getByRole("heading", { name: "覆盖外部预约" })).toBeVisible();
});

test("system admin can open import export task page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("导入导出任务").first().click();
  await expect(page.getByText("IMPORT / EXPORT")).toBeVisible();
  await expect(page.getByRole("heading", { name: "创建任务" })).toBeVisible();
});

test("system admin can open customer master data page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("客户主数据").first().click();
  await expect(page.getByText("CUSTOMER MASTER DATA")).toBeVisible();
  await expect(page.getByRole("heading", { name: "新增门户账号" })).toBeVisible();
});

test("system admin can open system governance page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("系统治理").first().click();
  await expect(page.getByText("SYSTEM GOVERNANCE")).toBeVisible();
  await expect(page.getByRole("tab", { name: "审计日志" })).toBeVisible();
});

test("system admin can open master data admin page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.admin");
  await page.locator("#password").fill("Internal123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("主数据维护").first().click();
  await expect(page.getByText("MASTER DATA", { exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "组织用户" }).click();
  await expect(page.getByRole("heading", { name: "创建内部用户" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "用户操作" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "角色状态" })).toBeVisible();
  await expect(page.getByRole("tab", { name: "旧状态映射" })).toBeVisible();
});

test("archive manager can open archive page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.archive");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("档案归档").first().click();
  await expect(page.getByText("ARCHIVE CENTER")).toBeVisible();
  await expect(page.getByRole("heading", { name: "归档登记" })).toBeVisible();
});

test("inventory manager can open inventory page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.inventory");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("库存管理").first().click();
  await expect(page.getByText("INVENTORY", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "库存流水" })).toBeVisible();
});

test("sample manager can open sample retention page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.sample");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("留样管理").first().click();
  await expect(page.getByText("SAMPLE RETENTION")).toBeVisible();
  await expect(page.getByRole("heading", { name: "留样记录" })).toBeVisible();
});

test("department manager can open green channel page", async ({ page }) => {
  await page.goto("/internal/");
  await page.locator("#username").fill("internal.dept_manager");
  await page.locator("#password").fill("Sales123!");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByText("绿色通道").first().click();
  await expect(page.getByText("GREEN CHANNEL")).toBeVisible();
  await expect(page.getByRole("heading", { name: "绿色通道申请" })).toBeVisible();
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
