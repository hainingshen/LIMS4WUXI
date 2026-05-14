import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Alert,
  Button,
  Card,
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
  type AuthSession,
} from "@lims4wuxi/shared";

import "./styles.css";

const PORTAL_ROUTES = [
  {
    key: "dashboard",
    label: "客户首页",
    detail: "送检进度和报告动态",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
  },
  {
    key: "new-submission",
    label: "新建送检单",
    detail: "填写并提交委托信息",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT"],
  },
  {
    key: "projects",
    label: "我的项目",
    detail: "查看项目、样品和报告",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
  },
] as const;

function PortalApp() {
  const session = useAuthStore((state) => ({
    accessToken: state.accessToken,
    clientType: state.clientType,
  }));
  if (!session.accessToken || session.clientType !== "PORTAL") {
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
  const entries = useMemo(() => PORTAL_ROUTES.filter((item) => hasAnyRole(roles, item.roles)), [roles]);

  if (screens.xs) {
    return (
      <main className="mobileSurface">
        <NavBar backArrow={false} right={<MobileButton size="mini" onClick={clearSession}>退出</MobileButton>}>
          客户门户
        </NavBar>
        <List header="入口">
          {entries.map((item) => (
            <List.Item key={item.key} description={item.detail}>
              {item.label}
            </List.Item>
          ))}
        </List>
      </main>
    );
  }

  return (
    <Layout className="appShell">
      <Layout.Sider width={232} theme="light" className="sidebar">
        <Typography.Title level={4}>客户门户</Typography.Title>
        <Menu mode="inline" selectedKeys={entries[0] ? [entries[0].key] : []} items={entries.map((item) => ({ key: item.key, label: item.label }))} />
      </Layout.Sider>
      <Layout.Content className="content">
        <header className="topbar">
          <Space wrap>
            {roles.map((role) => <Tag key={role}>{roleLabel(role)}</Tag>)}
          </Space>
          <Button onClick={clearSession}>退出</Button>
        </header>
        <section className="entryGrid">
          {entries.map((item) => (
            <Card key={item.key} title={item.label} className="entryCard">
              <Typography.Text>{item.detail}</Typography.Text>
            </Card>
          ))}
        </section>
      </Layout.Content>
    </Layout>
  );
}

function toSession(token: AuthSession): AuthSession {
  return token;
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <PortalApp />
  </React.StrictMode>,
);
