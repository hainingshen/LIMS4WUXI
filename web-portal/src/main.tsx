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
  limsAntdTheme,
  roleLabel,
  useAuthStore,
  type AuthSession,
} from "@lims4wuxi/shared";
import "@lims4wuxi/shared/theme/limsTheme.css";

import { PortalProjectOverviewPage } from "./PortalProjectOverviewPage";
import { SubmissionFormPage } from "./SubmissionFormPage";
import "./styles.css";

type PortalRouteKey = "dashboard" | "new-submission" | "projects" | "report-preview";

type PortalRoute = {
  key: PortalRouteKey;
  label: string;
  detail: string;
  roles: readonly string[];
  path: string;
  menu: boolean;
};

const PORTAL_ROUTES: readonly PortalRoute[] = [
  {
    key: "dashboard",
    label: "客户首页",
    detail: "送检进度和报告动态",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/",
    menu: true,
  },
  {
    key: "new-submission",
    label: "新建送检单",
    detail: "填写并提交委托信息",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT"],
    path: "/portal/submissions/new",
    menu: true,
  },
  {
    key: "projects",
    label: "我的项目",
    detail: "查看项目、样品和报告",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/projects",
    menu: true,
  },
  {
    key: "report-preview",
    label: "报告预览",
    detail: "查看草稿和发放状态",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/reports/:id/preview",
    menu: false,
  },
] as const;

function PortalApp() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const clientType = useAuthStore((state) => state.clientType);
  if (!accessToken || clientType !== "PORTAL") {
    return <LoginScreen />;
  }
  return <PortalShell />;
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
      const token = await loginForClient("PORTAL", values);
      setSession(toSession(token));
    } catch {
      setError("登录失败");
    }
  }

  return (
    <main className="loginSurface">
      <section className="loginHero">
        <div className="brandMark">NJU</div>
        <Typography.Title level={2}>南京大学锡山应用生物技术研究所</Typography.Title>
        <Typography.Text>
          医疗器械检测客户门户，支持送检、进度跟踪、报告预览与发放。
        </Typography.Text>
        <div className="credentialRow">
          <span>CNAS</span>
          <span>CMA</span>
          <span>GLP</span>
        </div>
      </section>
      <Card className="loginPanel" title="客户门户">
        <Form layout="vertical" initialValues={{ username: "portal.applicant", password: "Applicant123!" }} onFinish={submit}>
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
  const [username, setUsername] = useState("portal.applicant");
  const [password, setPassword] = useState("Applicant123!");
  const [error, setError] = useState<string>();
  const setSession = useAuthStore((state) => state.setSession);

  async function submit() {
    setError(undefined);
    try {
      const token = await loginForClient("PORTAL", { username, password });
      setSession(toSession(token));
    } catch {
      setError("登录失败");
    }
  }

  return (
    <main className="mobileSurface">
      <NavBar backArrow={false}>客户门户</NavBar>
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

function PortalShell() {
  const screens = Grid.useBreakpoint();
  const roles = useAuthStore((state) => state.roles);
  const clearSession = useAuthStore((state) => state.clearSession);
  const allowedRoutes = useMemo(() => PORTAL_ROUTES.filter((item) => hasAnyRole(roles, item.roles)), [roles]);
  const entries = useMemo(() => allowedRoutes.filter((item) => item.menu), [allowedRoutes]);
  const [selectedKey, setSelectedKey] = useState<PortalRouteKey>(() => portalKeyFromPath(window.location.pathname));
  const routeState = portalRouteState(selectedKey, window.location.pathname);
  const selectedEntry = allowedRoutes.find((item) => item.key === selectedKey) ?? entries[0];
  const activeKey = selectedEntry?.key;
  const content = activeKey ? portalContent(activeKey, entries, navigate, routeState) : null;

  useEffect(() => {
    const onPopState = () => setSelectedKey(portalKeyFromPath(window.location.pathname));
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(key: PortalRouteKey) {
    setSelectedKey(key);
    const nextPath = portalPathForKey(key);
    if (nextPath !== window.location.pathname) {
      window.history.pushState({}, "", nextPath);
    }
  }

  if (screens.xs) {
    return (
      <main className="mobileSurface">
        <NavBar
          backArrow={activeKey !== "dashboard"}
          onBack={() => navigate("dashboard")}
          right={<MobileButton size="mini" onClick={clearSession}>退出</MobileButton>}
        >
          {selectedEntry?.label ?? "客户门户"}
        </NavBar>
        {activeKey === "dashboard" ? (
          <List header="入口">
            {entries.map((item) => (
              <List.Item key={item.key} description={item.detail} onClick={() => navigate(item.key)}>
                {item.label}
              </List.Item>
            ))}
          </List>
        ) : (
          <div className="mobileContent">{content}</div>
        )}
      </main>
    );
  }

  return (
    <Layout className="appShell">
      <Layout.Sider width={232} theme="light" className="sidebar">
        <div className="sideBrand">
          <div className="sideLogo">NJU</div>
          <div>
            <Typography.Title level={4}>客户门户</Typography.Title>
            <Typography.Text>Customer Portal</Typography.Text>
          </div>
        </div>
        <Menu
          mode="inline"
          selectedKeys={activeKey ? [activeKey] : []}
          items={entries.map((item) => ({ key: item.key, label: item.label }))}
          onClick={({ key }) => navigate(key as PortalRouteKey)}
        />
      </Layout.Sider>
      <Layout.Content className="content">
        <header className="topbar">
          <Space wrap>
            {roles.map((role) => <Tag key={role}>{roleLabel(role)}</Tag>)}
          </Space>
          <Button onClick={clearSession}>退出</Button>
        </header>
        {content}
      </Layout.Content>
    </Layout>
  );
}

function portalContent(
  activeKey: PortalRouteKey,
  entries: readonly PortalRoute[],
  selectRoute: (key: PortalRouteKey) => void,
  routeState: PortalRouteState,
) {
  if (activeKey === "new-submission") {
    return <SubmissionFormPage />;
  }

  if (activeKey === "dashboard") {
    return (
      <PortalProjectOverviewPage
        mode="dashboard"
        onNewSubmission={() => selectRoute("new-submission")}
        onOpenProjects={() => selectRoute("projects")}
      />
    );
  }

  if (activeKey === "projects") {
    return (
      <PortalProjectOverviewPage
        mode="projects"
        selectedProjectId={routeState.projectId}
        onNewSubmission={() => selectRoute("new-submission")}
      />
    );
  }

  if (activeKey === "report-preview") {
    return <PortalReportPreview reportId={routeState.reportId} />;
  }

  const entry = entries.find((item) => item.key === activeKey);
  return (
    <section className="routePanel">
      <Typography.Title level={3}>{entry?.label}</Typography.Title>
      <Typography.Text>{entry?.detail}</Typography.Text>
    </section>
  );
}

type PortalRouteState = {
  projectId?: string;
  reportId?: string;
};

function portalRouteState(key: PortalRouteKey, pathname: string): PortalRouteState {
  if (key === "projects") {
    const match = pathname.match(/^\/portal\/projects\/([^/]+)$/);
    return { projectId: match?.[1] };
  }
  if (key === "report-preview") {
    const match = pathname.match(/^\/portal\/reports\/([^/]+)\/preview$/);
    return { reportId: match?.[1] };
  }
  return {};
}

function portalKeyFromPath(pathname: string): PortalRouteKey {
  if (pathname === "/portal/submissions/new") {
    return "new-submission";
  }
  if (pathname === "/portal/projects" || pathname.startsWith("/portal/projects/")) {
    return "projects";
  }
  if (/^\/portal\/reports\/[^/]+\/preview$/.test(pathname)) {
    return "report-preview";
  }
  return "dashboard";
}

function portalPathForKey(key: PortalRouteKey) {
  return PORTAL_ROUTES.find((route) => route.key === key)?.path.replace("/:id", "") ?? "/portal/";
}

function PortalReportPreview({ reportId }: { reportId?: string }) {
  return (
    <section className="routePanel">
      <Typography.Text className="pageEyebrow">REPORT PREVIEW</Typography.Text>
      <Typography.Title level={3}>报告预览</Typography.Title>
      <Typography.Text>{reportId ?? "未选择报告"}</Typography.Text>
    </section>
  );
}

function toSession(token: AuthSession): AuthSession {
  return token;
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider theme={limsAntdTheme}>
      <PortalApp />
    </ConfigProvider>
  </React.StrictMode>,
);
