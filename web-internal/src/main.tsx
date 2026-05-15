import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Alert,
  Button,
  Card,
  ConfigProvider,
  Form,
  Grid,
  Input,
  Layout,
  Menu,
  Space,
  Tag,
  Typography,
} from "antd";
import "antd/dist/reset.css";
import { Button as MobileButton, Form as MobileForm, Input as MobileInput, List, NavBar, NoticeBar } from "antd-mobile";
import "antd-mobile/es/global";
import {
  hasAnyRole,
  loginForClient,
  roleLabel,
  useAuthStore,
  usePermission,
  type AuthSession,
  limsAntdTheme,
} from "@lims4wuxi/shared";
import "@lims4wuxi/shared/theme/limsTheme.css";

import { ArchiveItemPage } from "./ArchiveItemPage";
import { ContractListPage } from "./ContractListPage";
import { CustomerManagementPage } from "./CustomerManagementPage";
import { DeviceBookingPage } from "./DeviceBookingPage";
import { DeviceFilePage } from "./DeviceFilePage";
import { GreenChannelPage } from "./GreenChannelPage";
import { ImportExportTaskPage } from "./ImportExportTaskPage";
import { InventoryPage } from "./InventoryPage";
import { MasterDataAdminPage } from "./MasterDataAdminPage";
import { ProjectListPage } from "./ProjectListPage";
import { ReportRegistryPage } from "./ReportRegistryPage";
import { ReportWorkbenchPage } from "./ReportWorkbenchPage";
import { SampleRetentionPage } from "./SampleRetentionPage";
import { SampleReceivingDesk } from "./SampleReceivingDesk";
import { StudyPlanPage } from "./StudyPlanPage";
import { SubmissionAcceptanceDesk } from "./SubmissionAcceptanceDesk";
import { SystemGovernancePage } from "./SystemGovernancePage";
import { WorkOrderExecutionPage } from "./WorkOrderExecutionPage";
import { WorkCenterPage } from "./WorkCenterPage";
import "./styles.css";

type InternalRoute = {
  key: string;
  label: string;
  detail: string;
  roles: readonly string[];
  actionCode?: string;
  path: string;
};

const INTERNAL_ROUTES: readonly InternalRoute[] = [
  {
    key: "work-center",
    label: "工作中心",
    detail: "待办、风险和负荷",
    roles: ["SALES_REP", "BUSINESS_OWNER", "TECHNICAL_LEADER", "SD", "QA", "QC", "QUALITY_MANAGER", "DEPT_MANAGER", "GENERAL_MANAGER"],
    actionCode: "WORK_CENTER_READ",
    path: "/internal/work-center",
  },
  {
    key: "customer-management",
    label: "客户主数据",
    detail: "委托单位、发票信息和门户账号",
    roles: ["SYSTEM_ADMIN", "QUALITY_MANAGER"],
    actionCode: "MASTER_DATA_READ",
    path: "/internal/customer-management",
  },
  {
    key: "system-governance",
    label: "系统治理",
    detail: "系统配置、规则和审计日志",
    roles: ["SYSTEM_ADMIN", "QUALITY_MANAGER"],
    actionCode: "MASTER_DATA_READ",
    path: "/internal/system",
  },
  {
    key: "master-data-admin",
    label: "主数据维护",
    detail: "组织、用户、节假日和旧状态",
    roles: ["SYSTEM_ADMIN", "QUALITY_MANAGER"],
    actionCode: "MASTER_DATA_READ",
    path: "/internal/master-data",
  },
  {
    key: "submissions",
    label: "送检受理",
    detail: "提交审核、受理和拆分",
    roles: ["SALES_REP", "SAMPLE_MANAGER", "TECHNICAL_LEADER"],
    actionCode: "SUBMISSION_ACCEPT",
    path: "/internal/submissions",
  },
  {
    key: "projects",
    label: "项目详情",
    detail: "评审、方案和报告进度",
    roles: ["SALES_REP", "BUSINESS_OWNER", "TECHNICAL_LEADER", "SD", "QA", "QC", "SAMPLE_MANAGER", "REPORT_PREPARER"],
    path: "/internal/projects",
  },
  {
    key: "study-plans",
    label: "方案编制",
    detail: "审批、下发和工单",
    roles: ["TECHNICAL_LEADER", "SD"],
    path: "/internal/study-plans",
  },
  {
    key: "work-orders",
    label: "试验执行",
    detail: "领样、结果和原始记录",
    roles: ["INSPECTOR", "QC"],
    path: "/internal/work-orders",
  },
  {
    key: "reports",
    label: "报告管理",
    detail: "草稿、审核、签发和收款",
    roles: ["REPORT_PREPARER", "QC", "QA", "SD", "AUTHORIZED_SIGNATORY", "FINANCE_CASHIER"],
    path: "/internal/reports",
  },
  {
    key: "report-registry",
    label: "报告注册中心",
    detail: "版本、发放和批量下载",
    roles: ["REPORT_PREPARER", "QC", "QA", "SD", "AUTHORIZED_SIGNATORY", "ARCHIVE_MANAGER", "QUALITY_MANAGER"],
    actionCode: "REPORT_REGISTRY_READ",
    path: "/internal/report-registry",
  },
  {
    key: "device-files",
    label: "设备文件",
    detail: "台账、证书和 SOP 有效性",
    roles: ["INSPECTOR", "EQUIPMENT_MANAGER", "QUALITY_MANAGER"],
    actionCode: "DEVICE_FILE_READ",
    path: "/internal/devices/files",
  },
  {
    key: "device-bookings",
    label: "设备预约",
    detail: "日历、冲突和覆盖重约",
    roles: ["EQUIPMENT_MANAGER", "QUALITY_MANAGER"],
    actionCode: "DEVICE_BOOKING_READ_ALL",
    path: "/internal/devices/calendar",
  },
  {
    key: "import-export-tasks",
    label: "导入导出任务",
    detail: "预校验、执行和结果文件",
    roles: ["SYSTEM_ADMIN", "QUALITY_MANAGER", "ARCHIVE_MANAGER"],
    actionCode: "IMPORT_EXPORT_TASK_CREATE",
    path: "/internal/import-export-tasks",
  },
  {
    key: "archive-items",
    label: "档案归档",
    detail: "项目归档件和长期保存",
    roles: ["ARCHIVE_MANAGER", "QUALITY_MANAGER"],
    actionCode: "ARCHIVE_READ",
    path: "/internal/archive",
  },
  {
    key: "inventory",
    label: "库存管理",
    detail: "标准品、耗材和流水",
    roles: ["INVENTORY_MANAGER", "INSPECTOR", "QUALITY_MANAGER"],
    actionCode: "INVENTORY_READ",
    path: "/internal/inventory",
  },
  {
    key: "green-channels",
    label: "绿色通道",
    detail: "申请、部门审批和管理审批",
    roles: ["SALES_REP", "DEPT_MANAGER", "GENERAL_MANAGER", "FINANCE_MANAGER"],
    path: "/internal/green-channel-requests",
  },
  {
    key: "sample-retentions",
    label: "留样管理",
    detail: "登记、延期和处置",
    roles: ["SAMPLE_MANAGER", "QUALITY_MANAGER", "ARCHIVE_MANAGER"],
    path: "/internal/sample-retentions",
  },
  {
    key: "contracts",
    label: "合同协议",
    detail: "签署确认和字段锁定",
    roles: ["SALES_REP", "BUSINESS_OWNER"],
    path: "/internal/contracts",
  },
  {
    key: "samples-receiving",
    label: "样品收样台",
    detail: "到达核验、拍照和入库",
    roles: ["SAMPLE_MANAGER"],
    actionCode: "SAMPLE_RECEIVE",
    path: "/internal/samples/receiving",
  },
];

function InternalApp() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const clientType = useAuthStore((state) => state.clientType);
  if (!accessToken || clientType !== "INTERNAL") {
    return <LoginScreen />;
  }
  return <InternalShell />;
}

function LoginScreen() {
  const screens = Grid.useBreakpoint();
  return screens.xs ? <MobileLogin /> : <DesktopLogin />;
}

function DesktopLogin() {
  const [error, setError] = useState<string>();
  const setSession = useAuthStore((state) => state.setSession);

  async function submit(values: { username: string; password: string }) {
    setError(undefined);
    try {
      const token = await loginForClient("INTERNAL", values);
      setSession(toSession(token));
    } catch {
      setError("登录失败");
    }
  }

  return (
    <main className="loginSurface">
      <section className="loginHero">
        <div className="brandMark">NJU</div>
        <Typography.Title level={2}>无锡锡山 LIMS 内部工作台</Typography.Title>
        <Typography.Text>
          面向业务、样品、检测、质量和管理角色的实验室全流程工作台。
        </Typography.Text>
        <div className="credentialRow">
          <span>CNAS</span>
          <span>CMA</span>
          <span>GLP</span>
        </div>
      </section>
      <Card className="loginPanel" title="内部工作台">
        <Form layout="vertical" initialValues={{ username: "internal.sales", password: "Sales123!" }} onFinish={submit}>
          <Form.Item name="username" label="账号" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          {error ? <Alert type="error" message={error} showIcon className="formMessage" /> : null}
          <Button type="primary" htmlType="submit" block>
            登录
          </Button>
        </Form>
      </Card>
    </main>
  );
}

function MobileLogin() {
  const [username, setUsername] = useState("internal.sales");
  const [password, setPassword] = useState("Sales123!");
  const [error, setError] = useState<string>();
  const setSession = useAuthStore((state) => state.setSession);

  async function submit() {
    setError(undefined);
    try {
      const token = await loginForClient("INTERNAL", { username, password });
      setSession(toSession(token));
    } catch {
      setError("登录失败");
    }
  }

  return (
    <main className="mobileSurface">
      <NavBar backArrow={false}>内部工作台</NavBar>
      {error ? <NoticeBar color="alert" content={error} /> : null}
      <MobileForm layout="vertical" footer={<MobileButton color="primary" block onClick={submit}>登录</MobileButton>}>
        <MobileForm.Item label="账号">
          <MobileInput value={username} onChange={setUsername} />
        </MobileForm.Item>
        <MobileForm.Item label="密码">
          <MobileInput type="password" value={password} onChange={setPassword} />
        </MobileForm.Item>
      </MobileForm>
    </main>
  );
}

function InternalShell() {
  const screens = Grid.useBreakpoint();
  const roles = useAuthStore((state) => state.roles);
  const clearSession = useAuthStore((state) => state.clearSession);
  const entries = useMemo(() => INTERNAL_ROUTES.filter((item) => hasAnyRole(roles, item.roles)), [roles]);
  const [activeKey, setActiveKey] = useState(() => internalKeyFromPath(window.location.pathname));

  useEffect(() => {
    if (!entries.some((item) => item.key === activeKey)) {
      setActiveKey(entries[0]?.key);
    }
  }, [activeKey, entries]);

  useEffect(() => {
    const onPopState = () => setActiveKey(internalKeyFromPath(window.location.pathname));
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(key: string | undefined) {
    if (!key) {
      return;
    }
    setActiveKey(key);
    const nextPath = internalPathForKey(key);
    if (nextPath && nextPath !== window.location.pathname) {
      window.history.pushState({}, "", nextPath);
    }
  }

  const activeRoute = entries.find((item) => item.key === activeKey) ?? entries[0];

  if (screens.xs) {
    return (
      <main className="mobileSurface">
        <NavBar backArrow={false} right={<MobileButton size="mini" onClick={clearSession}>退出</MobileButton>}>
          内部工作台
        </NavBar>
        <List header="入口">
          {entries.map((item) => (
            <List.Item key={item.key} description={item.detail} onClick={() => navigate(item.key)}>
              {item.actionCode ? (
                <RequireAction action={item.actionCode}>{item.label}</RequireAction>
              ) : item.label}
            </List.Item>
          ))}
        </List>
        <div className="mobileContent">{renderRoute(activeRoute, entries, navigate)}</div>
      </main>
    );
  }

  return (
    <Layout className="appShell">
      <Layout.Sider width={232} theme="light" className="sidebar">
        <div className="sideBrand">
          <div className="sideLogo">NJU</div>
          <div>
            <Typography.Title level={4}>LIMS4WUXI</Typography.Title>
            <Typography.Text>Internal Workspace</Typography.Text>
          </div>
        </div>
        <Menu
          mode="inline"
          selectedKeys={activeRoute ? [activeRoute.key] : []}
          onClick={({ key }) => navigate(String(key))}
          items={entries.map((item) => ({ key: item.key, label: item.label }))}
        />
      </Layout.Sider>
      <Layout.Content className="content">
        <header className="topbar">
          <Space wrap>
            {roles.map((role) => <Tag key={role}>{roleLabel(role)}</Tag>)}
          </Space>
          <Button onClick={clearSession}>退出</Button>
        </header>
        {renderRoute(activeRoute, entries, navigate)}
      </Layout.Content>
    </Layout>
  );
}

function renderRoute(
  activeRoute: InternalRoute | undefined,
  entries: readonly InternalRoute[],
  setActiveKey: (key: string) => void,
) {
  if (activeRoute?.key === "samples-receiving") {
    return (
      <RequireAction action="SAMPLE_RECEIVE">
        <SampleReceivingDesk />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "customer-management") {
    return (
      <RequireAction action="MASTER_DATA_READ">
        <CustomerManagementPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "system-governance") {
    return (
      <RequireAction action="MASTER_DATA_READ">
        <SystemGovernancePage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "master-data-admin") {
    return (
      <RequireAction action="MASTER_DATA_READ">
        <MasterDataAdminPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "submissions") {
    return (
      <RequireAction action="SUBMISSION_ACCEPT">
        <SubmissionAcceptanceDesk />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "projects") {
    return <ProjectListPage />;
  }
  if (activeRoute?.key === "study-plans") {
    return <StudyPlanPage />;
  }
  if (activeRoute?.key === "work-orders") {
    return <WorkOrderExecutionPage />;
  }
  if (activeRoute?.key === "reports") {
    return <ReportWorkbenchPage />;
  }
  if (activeRoute?.key === "report-registry") {
    return (
      <RequireAction action="REPORT_REGISTRY_READ">
        <ReportRegistryPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "device-files") {
    return (
      <RequireAction action="DEVICE_FILE_READ">
        <DeviceFilePage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "device-bookings") {
    return (
      <RequireAction action="DEVICE_BOOKING_READ_ALL">
        <DeviceBookingPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "import-export-tasks") {
    return (
      <RequireAction action="IMPORT_EXPORT_TASK_CREATE">
        <ImportExportTaskPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "archive-items") {
    return (
      <RequireAction action="ARCHIVE_READ">
        <ArchiveItemPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "inventory") {
    return (
      <RequireAction action="INVENTORY_READ">
        <InventoryPage />
      </RequireAction>
    );
  }
  if (activeRoute?.key === "green-channels") {
    return <GreenChannelPage />;
  }
  if (activeRoute?.key === "sample-retentions") {
    return <SampleRetentionPage />;
  }
  if (activeRoute?.key === "contracts") {
    return <ContractListPage />;
  }
  if (activeRoute?.key === "work-center") {
    return (
      <RequireAction action="WORK_CENTER_READ">
        <WorkCenterPage />
      </RequireAction>
    );
  }
  return (
    <section className="entryGrid">
      {entries.map((item) => (
        <Card key={item.key} title={item.label} className="entryCard">
          <Typography.Text>{item.detail}</Typography.Text>
          {item.actionCode ? (
            <div className="entryAction">
              <RequireAction action={item.actionCode}>
                <Button type="primary" onClick={() => setActiveKey(item.key)}>进入</Button>
              </RequireAction>
            </div>
          ) : null}
        </Card>
      ))}
    </section>
  );
}

function internalKeyFromPath(pathname: string) {
  if (pathname === "/internal/" || pathname === "/internal") {
    return "work-center";
  }
  if (pathname === "/internal/work-center") {
    return "work-center";
  }
  if (pathname === "/internal/submissions") {
    return "submissions";
  }
  if (pathname === "/internal/samples/receiving") {
    return "samples-receiving";
  }
  if (pathname === "/internal/master-data") {
    return "master-data-admin";
  }
  if (pathname === "/internal/system") {
    return "system-governance";
  }
  if (pathname === "/internal/customer-management") {
    return "customer-management";
  }
  if (pathname === "/internal/import-export-tasks") {
    return "import-export-tasks";
  }
  if (pathname === "/internal/inventory") {
    return "inventory";
  }
  if (pathname === "/internal/archive") {
    return "archive-items";
  }
  if (pathname === "/internal/report-registry") {
    return "report-registry";
  }
  if (pathname === "/internal/contracts") {
    return "contracts";
  }
  if (pathname === "/internal/sample-retentions") {
    return "sample-retentions";
  }
  if (pathname === "/internal/green-channel-requests") {
    return "green-channels";
  }
  if (pathname === "/internal/payments") {
    return "reports";
  }
  if (pathname.startsWith("/internal/projects")) {
    return "projects";
  }
  if (pathname.startsWith("/internal/study-plans")) {
    return "study-plans";
  }
  if (pathname.startsWith("/internal/work-orders")) {
    return "work-orders";
  }
  if (pathname.startsWith("/internal/reports")) {
    return "reports";
  }
  if (pathname.startsWith("/internal/devices") && pathname.endsWith("/files")) {
    return "device-files";
  }
  if (pathname.startsWith("/internal/devices") && pathname.endsWith("/calendar")) {
    return "device-bookings";
  }
  if (pathname === "/internal/devices/files") {
    return "device-files";
  }
  if (pathname === "/internal/devices/calendar") {
    return "device-bookings";
  }
  return "work-center";
}

function internalPathForKey(key: string) {
  return INTERNAL_ROUTES.find((route) => route.key === key)?.path;
}

function RequireAction({ action, children }: React.PropsWithChildren<{ action: string }>) {
  const allowed = usePermission(action);
  return allowed ? <>{children}</> : null;
}

function toSession(token: AuthSession): AuthSession {
  return token;
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider theme={limsAntdTheme}>
      <InternalApp />
    </ConfigProvider>
  </React.StrictMode>,
);
