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
  Spin,
  Tag,
  Typography,
} from "antd";
import "antd/dist/reset.css";
import { Button as MobileButton, Form as MobileForm, Input as MobileInput, List, NavBar, NoticeBar } from "antd-mobile";
import "antd-mobile/es/global";
import {
  hasAnyRole,
  downloadOfficialReportPdf,
  getContract,
  getReport,
  loginForClient,
  limsAntdTheme,
  markContractSigned,
  roleLabel,
  useAuthStore,
  type AuthSession,
  type Contract,
  type Report,
} from "@lims4wuxi/shared";
import "@lims4wuxi/shared/theme/limsTheme.css";

import { PortalProjectOverviewPage } from "./PortalProjectOverviewPage";
import {
  PortalAccountPage,
  PortalContractListPage,
  PortalReportCenterPage,
  PortalSubmissionDetailPage,
  PortalSubmissionListPage,
} from "./PortalBusinessPages";
import { SubmissionFormPage } from "./SubmissionFormPage";
import { contractStatusColor, contractTypeLabel } from "./contractPresentation";
import {
  isPaymentGateLocked,
  paymentGateColor,
  previewWatermark,
  reportStatusColor,
  triggerPdfDownload,
} from "./reportPresentation";
import "./styles.css";

type PortalRouteKey =
  | "dashboard"
  | "submissions"
  | "new-submission"
  | "submission-detail"
  | "projects"
  | "contracts"
  | "contract-confirm"
  | "reports"
  | "report-preview"
  | "account";

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
    key: "submissions",
    label: "我的送检单",
    detail: "草稿、提交和受理状态",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/submissions",
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
    key: "contracts",
    label: "合同协议",
    detail: "待确认协议和已签约记录",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/contracts",
    menu: true,
  },
  {
    key: "reports",
    label: "报告中心",
    detail: "预览、付款门禁和正式下载",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/reports",
    menu: true,
  },
  {
    key: "account",
    label: "账户信息",
    detail: "账号、角色和客户单位",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/account",
    menu: true,
  },
  {
    key: "submission-detail",
    label: "送检单详情",
    detail: "查看送检单详情",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/submissions/:id",
    menu: false,
  },
  {
    key: "report-preview",
    label: "报告预览",
    detail: "查看草稿和发放状态",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/reports/:id/preview",
    menu: false,
  },
  {
    key: "contract-confirm",
    label: "协议确认",
    detail: "查看并确认合同协议",
    roles: ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"],
    path: "/portal/contracts/:id/confirm",
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

  function navigate(key: PortalRouteKey, path?: string) {
    setSelectedKey(key);
    const nextPath = path ?? portalPathForKey(key);
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
  selectRoute: (key: PortalRouteKey, path?: string) => void,
  routeState: PortalRouteState,
) {
  if (activeKey === "new-submission") {
    return <SubmissionFormPage />;
  }

  if (activeKey === "submissions") {
    return (
      <PortalSubmissionListPage
        onNewSubmission={() => selectRoute("new-submission")}
        onOpenSubmission={(submissionId) => selectRoute("submission-detail", `/portal/submissions/${submissionId}`)}
      />
    );
  }

  if (activeKey === "submission-detail") {
    return (
      <PortalSubmissionDetailPage
        submissionId={routeState.submissionId}
        onBack={() => selectRoute("submissions")}
      />
    );
  }

  if (activeKey === "dashboard") {
    return (
      <PortalProjectOverviewPage
        mode="dashboard"
        onNewSubmission={() => selectRoute("new-submission")}
        onOpenSubmission={(submissionId) => selectRoute("submission-detail", `/portal/submissions/${submissionId}`)}
        onOpenProjects={() => selectRoute("projects")}
        onOpenProject={(projectId) => selectRoute("projects", `/portal/projects/${projectId}`)}
        onOpenContract={(contractId) => selectRoute("contract-confirm", `/portal/contracts/${contractId}/confirm`)}
        onPreviewReport={(reportId) => selectRoute("report-preview", `/portal/reports/${reportId}/preview`)}
      />
    );
  }

  if (activeKey === "projects") {
    return (
      <PortalProjectOverviewPage
        mode="projects"
        selectedProjectId={routeState.projectId}
        onNewSubmission={() => selectRoute("new-submission")}
        onOpenSubmission={(submissionId) => selectRoute("submission-detail", `/portal/submissions/${submissionId}`)}
        onOpenProject={(projectId) => selectRoute("projects", `/portal/projects/${projectId}`)}
        onOpenContract={(contractId) => selectRoute("contract-confirm", `/portal/contracts/${contractId}/confirm`)}
        onPreviewReport={(reportId) => selectRoute("report-preview", `/portal/reports/${reportId}/preview`)}
      />
    );
  }

  if (activeKey === "contracts") {
    return <PortalContractListPage onOpenContract={(contractId) => selectRoute("contract-confirm", `/portal/contracts/${contractId}/confirm`)} />;
  }

  if (activeKey === "contract-confirm") {
    return <PortalContractConfirm contractId={routeState.contractId} onBack={() => selectRoute("contracts")} />;
  }

  if (activeKey === "reports") {
    return <PortalReportCenterPage onPreviewReport={(reportId) => selectRoute("report-preview", `/portal/reports/${reportId}/preview`)} />;
  }

  if (activeKey === "report-preview") {
    return <PortalReportPreview reportId={routeState.reportId} onBack={() => selectRoute("reports")} />;
  }

  if (activeKey === "account") {
    return <PortalAccountPage />;
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
  contractId?: string;
  projectId?: string;
  reportId?: string;
  submissionId?: string;
};

function portalRouteState(key: PortalRouteKey, pathname: string): PortalRouteState {
  if (key === "submission-detail") {
    const match = pathname.match(/^\/portal\/submissions\/([^/]+)$/);
    return { submissionId: match?.[1] };
  }
  if (key === "projects") {
    const match = pathname.match(/^\/portal\/projects\/([^/]+)$/);
    return { projectId: match?.[1] };
  }
  if (key === "report-preview") {
    const match = pathname.match(/^\/portal\/reports\/([^/]+)\/preview$/);
    return { reportId: match?.[1] };
  }
  if (key === "contract-confirm") {
    const match = pathname.match(/^\/portal\/contracts\/([^/]+)\/confirm$/);
    return { contractId: match?.[1] };
  }
  return {};
}

function portalKeyFromPath(pathname: string): PortalRouteKey {
  if (pathname === "/portal/submissions/new") {
    return "new-submission";
  }
  if (pathname === "/portal/submissions") {
    return "submissions";
  }
  if (/^\/portal\/submissions\/[^/]+$/.test(pathname)) {
    return "submission-detail";
  }
  if (pathname === "/portal/projects" || pathname.startsWith("/portal/projects/")) {
    return "projects";
  }
  if (pathname === "/portal/contracts") {
    return "contracts";
  }
  if (/^\/portal\/reports\/[^/]+\/preview$/.test(pathname)) {
    return "report-preview";
  }
  if (pathname === "/portal/reports") {
    return "reports";
  }
  if (/^\/portal\/contracts\/[^/]+\/confirm$/.test(pathname)) {
    return "contract-confirm";
  }
  if (pathname === "/portal/account") {
    return "account";
  }
  return "dashboard";
}

function portalPathForKey(key: PortalRouteKey) {
  return PORTAL_ROUTES.find((route) => route.key === key)?.path.replace("/:id", "") ?? "/portal/";
}

function PortalContractConfirm({ contractId, onBack }: { contractId?: string; onBack?: () => void }) {
  const roles = useAuthStore((state) => state.roles);
  const [contract, setContract] = useState<Contract>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();
  const [confirming, setConfirming] = useState(false);
  const canConfirm = contract?.status === "PENDING_SIGN"
    && hasAnyRole(roles, ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT"]);

  useEffect(() => {
    let active = true;
    async function load() {
      if (!contractId) {
        setError("未选择协议");
        setLoading(false);
        return;
      }
      setLoading(true);
      setError(undefined);
      try {
        const detail = await getContract(contractId);
        if (active) {
          setContract(detail);
        }
      } catch {
        if (active) {
          setError("协议不存在或不在你的授权范围内");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, [contractId]);

  async function confirmContract() {
    if (!contract || !canConfirm) {
      return;
    }
    setFeedback(undefined);
    setConfirming(true);
    try {
      const signed = await markContractSigned(contract.id, { signedAt: new Date().toISOString() });
      setContract(signed);
      setFeedback("协议已确认签署，项目进入已签约状态。");
    } catch {
      setFeedback("协议确认失败，请稍后重试。");
    } finally {
      setConfirming(false);
    }
  }

  if (loading) {
    return (
      <div className="pageState">
        <Spin />
      </div>
    );
  }

  return (
    <section className="portalContractConfirmPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">CONTRACT CONFIRM</Typography.Text>
          <Typography.Title level={3}>协议确认</Typography.Title>
        </div>
        <Space wrap>
          {onBack ? <Button onClick={onBack}>返回项目</Button> : null}
          <Button type="primary" loading={confirming} disabled={!canConfirm} onClick={() => void confirmContract()}>
            确认签署
          </Button>
        </Space>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {feedback ? <Alert type="info" showIcon message={feedback} className="portalFeedback" /> : null}
      {!contractId ? <Alert type="warning" showIcon message="未选择协议" className="portalFeedback" /> : null}
      {contractId && !contract ? (
        <Alert type="warning" showIcon message="当前协议不在你的授权范围内" className="portalFeedback" />
      ) : null}
      {contract ? (
        <div className="portalContractSheet">
          <div className="portalContractHeading">
            <Typography.Text className="pageEyebrow">NJU XSBI LIMS</Typography.Text>
            <Typography.Title level={4}>{contractTypeLabel(contract.contractType)}</Typography.Title>
            <Typography.Text>{contract.bizNo}</Typography.Text>
          </div>
          <dl className="portalReportMetaGrid">
            <div>
              <dt>协议状态</dt>
              <dd>
                <Tag color={contractStatusColor(contract.status)}>{contract.status}</Tag>
              </dd>
            </div>
            <div>
              <dt>协议类型</dt>
              <dd>{contractTypeLabel(contract.contractType)}</dd>
            </div>
            <div>
              <dt>模板</dt>
              <dd>{contract.templateCode ?? "-"}</dd>
            </div>
            <div>
              <dt>项目 ID</dt>
              <dd>{contract.projectId}</dd>
            </div>
            <div>
              <dt>签署时间</dt>
              <dd>{contract.signedAt ?? "-"}</dd>
            </div>
            <div>
              <dt>签署文件</dt>
              <dd>{contract.signedFileId ?? "-"}</dd>
            </div>
          </dl>
          <div className="portalContractBody">
            <Typography.Paragraph>
              委托检测范围、双方责任、报告交付和费用条款以正式盖章合同或协议文件为准。
            </Typography.Paragraph>
            <Typography.Paragraph>
              {contract.status === "PENDING_SIGN"
                ? "当前协议待客户确认签署。"
                : "当前协议已完成确认。"}
            </Typography.Paragraph>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function PortalReportPreview({ reportId, onBack }: { reportId?: string; onBack?: () => void }) {
  const [report, setReport] = useState<Report>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [downloadFeedback, setDownloadFeedback] = useState<string>();
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let active = true;
    async function load() {
      if (!reportId) {
        setError("未选择报告");
        setLoading(false);
        return;
      }
      setLoading(true);
      setError(undefined);
      try {
        const detail = await getReport(reportId);
        if (active) {
          setReport(detail);
        }
      } catch {
        if (active) {
          setError("报告不存在或不在你的授权范围内");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, [reportId]);

  async function downloadReport() {
    if (!report) {
      return;
    }
    setDownloadFeedback(undefined);
    setDownloading(true);
    try {
      const pdf = await downloadOfficialReportPdf(report.id);
      triggerPdfDownload(pdf, `${report.bizNo}.pdf`);
      setDownloadFeedback("正式报告已开始下载，发放日志会记录本次下载。");
    } catch (err) {
      setDownloadFeedback(
        isPaymentGateLocked(err)
          ? "未付款或绿色通道未解锁，正式报告暂不可下载；当前预览保留水印。"
          : "正式报告下载失败，请稍后重试。",
      );
    } finally {
      setDownloading(false);
    }
  }

  if (loading) {
    return (
      <div className="pageState">
        <Spin />
      </div>
    );
  }

  return (
    <section className="portalReportPreviewPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">REPORT PREVIEW</Typography.Text>
          <Typography.Title level={3}>报告预览</Typography.Title>
        </div>
        <Space wrap>
          {onBack ? <Button onClick={onBack}>返回项目</Button> : null}
          <Button type="primary" loading={downloading} disabled={!report} onClick={() => void downloadReport()}>
            下载正式报告
          </Button>
        </Space>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {downloadFeedback ? <Alert type="info" showIcon message={downloadFeedback} className="portalFeedback" /> : null}
      {!reportId ? <Alert type="warning" showIcon message="未选择报告" className="portalFeedback" /> : null}
      {reportId && !report ? (
        <Alert type="warning" showIcon message="当前报告不在你的授权范围内" className="portalFeedback" />
      ) : null}
      {report ? (
        <div className="portalReportSheet">
          <div className="portalReportWatermark">{previewWatermark(report)}</div>
          <div className="portalReportHeading">
            <Typography.Text className="pageEyebrow">NJU XSBI LIMS</Typography.Text>
            <Typography.Title level={4}>检验检测报告</Typography.Title>
            <Typography.Text>{report.bizNo}</Typography.Text>
          </div>
          <dl className="portalReportMetaGrid">
            <div>
              <dt>报告状态</dt>
              <dd>
                <Tag color={reportStatusColor(report.status)}>{report.status}</Tag>
              </dd>
            </div>
            <div>
              <dt>付款门禁</dt>
              <dd>
                <Tag color={paymentGateColor(report.paymentGateStatus)}>{report.paymentGateStatus}</Tag>
              </dd>
            </div>
            <div>
              <dt>模板</dt>
              <dd>{report.templateCode}</dd>
            </div>
            <div>
              <dt>模板版本</dt>
              <dd>{report.templateVersion ?? "-"}</dd>
            </div>
            <div>
              <dt>项目 ID</dt>
              <dd>{report.projectId}</dd>
            </div>
            <div>
              <dt>水印</dt>
              <dd>{report.watermarkType ?? "NONE"}</dd>
            </div>
          </dl>
          <div className="portalReportBody">
            <Typography.Paragraph>
              委托单位、样品信息、检测项目、原始记录结论和授权签发信息以正式 PDF 为准。
            </Typography.Paragraph>
            <Typography.Paragraph>
              {report.paymentGateStatus === "LOCKED"
                ? "付款门禁未解锁，当前预览件保留水印，正式报告暂不可下载。"
                : "报告已满足正式发放条件，正式 PDF 可下载并进入发放记录。"}
            </Typography.Paragraph>
          </div>
        </div>
      ) : null}
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
