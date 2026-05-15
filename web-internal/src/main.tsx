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

import { ContractListPage } from "./ContractListPage";
import { ProjectListPage } from "./ProjectListPage";
import { SampleReceivingDesk } from "./SampleReceivingDesk";
import { StudyPlanPage } from "./StudyPlanPage";
import { SubmissionAcceptanceDesk } from "./SubmissionAcceptanceDesk";
import { WorkCenterPage } from "./WorkCenterPage";
import "./styles.css";

type InternalRoute = {
  key: string;
  label: string;
  detail: string;
  roles: readonly string[];
  actionCode?: string;
};

const INTERNAL_ROUTES: readonly InternalRoute[] = [
  {
    key: "work-center",
    label: "工作中心",
    detail: "待办、风险和负荷",
    roles: ["SALES_REP", "BUSINESS_OWNER", "TECHNICAL_LEADER", "SD", "QA", "QC", "QUALITY_MANAGER", "DEPT_MANAGER", "GENERAL_MANAGER"],
    actionCode: "WORK_CENTER_READ",
  },
  {
    key: "submissions",
    label: "送检受理",
    detail: "提交审核、受理和拆分",
    roles: ["SALES_REP", "SAMPLE_MANAGER", "TECHNICAL_LEADER"],
    actionCode: "SUBMISSION_ACCEPT",
  },
  {
    key: "projects",
    label: "项目详情",
    detail: "评审、方案和报告进度",
    roles: ["SALES_REP", "BUSINESS_OWNER", "TECHNICAL_LEADER", "SD", "QA", "QC", "SAMPLE_MANAGER", "REPORT_PREPARER"],
  },
  {
    key: "study-plans",
    label: "方案编制",
    detail: "审批、下发和工单",
    roles: ["TECHNICAL_LEADER", "SD"],
  },
  {
    key: "contracts",
    label: "合同协议",
    detail: "签署确认和字段锁定",
    roles: ["SALES_REP", "BUSINESS_OWNER"],
  },
  {
    key: "samples-receiving",
    label: "样品收样台",
    detail: "到达核验、拍照和入库",
    roles: ["SAMPLE_MANAGER"],
    actionCode: "SAMPLE_RECEIVE",
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
  const [activeKey, setActiveKey] = useState(entries[0]?.key);

  useEffect(() => {
    if (!entries.some((item) => item.key === activeKey)) {
      setActiveKey(entries[0]?.key);
    }
  }, [activeKey, entries]);

  const activeRoute = entries.find((item) => item.key === activeKey) ?? entries[0];

  if (screens.xs) {
    return (
      <main className="mobileSurface">
        <NavBar backArrow={false} right={<MobileButton size="mini" onClick={clearSession}>退出</MobileButton>}>
          内部工作台
        </NavBar>
        <List header="入口">
          {entries.map((item) => (
            <List.Item key={item.key} description={item.detail} onClick={() => setActiveKey(item.key)}>
              {item.actionCode ? (
                <RequireAction action={item.actionCode}>{item.label}</RequireAction>
              ) : item.label}
            </List.Item>
          ))}
        </List>
        <div className="mobileContent">{renderRoute(activeRoute, entries, setActiveKey)}</div>
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
          onClick={({ key }) => setActiveKey(String(key))}
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
        {renderRoute(activeRoute, entries, setActiveKey)}
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
