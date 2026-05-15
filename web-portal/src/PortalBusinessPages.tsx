import { useEffect, useState } from "react";
import { Alert, Button, Descriptions, Space, Spin, Tag, Typography } from "antd";
import {
  downloadOfficialReportPdf,
  getCurrentUser,
  getSubmission,
  listContracts,
  listReports,
  listSubmissions,
  roleLabel,
  type Contract,
  type CurrentUser,
  type Report,
  type Submission,
} from "@lims4wuxi/shared";

import { contractStatusColor, contractTypeLabel } from "./contractPresentation";
import { isPaymentGateLocked, paymentGateColor, reportStatusColor, triggerPdfDownload } from "./reportPresentation";

type PortalSubmissionListPageProps = {
  onNewSubmission?: () => void;
  onOpenSubmission?: (submissionId: string) => void;
};

type PortalContractListPageProps = {
  onOpenContract?: (contractId: string) => void;
};

type PortalReportCenterPageProps = {
  onPreviewReport?: (reportId: string) => void;
};

export function PortalSubmissionListPage({
  onNewSubmission,
  onOpenSubmission,
}: PortalSubmissionListPageProps) {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listSubmissions({ page: 1, pageSize: 100 });
      setSubmissions(page.items ?? []);
    } catch {
      setError("送检单加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <section className="portalListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SUBMISSIONS</Typography.Text>
          <Typography.Title level={3}>我的送检单</Typography.Title>
        </div>
        <Space wrap>
          {onNewSubmission ? <Button type="primary" onClick={onNewSubmission}>新建送检单</Button> : null}
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {loading ? (
        <PageLoading />
      ) : submissions.length ? (
        <div className="portalDataList">
          {submissions.map((submission) => (
            <article key={submission.id} className="portalListItem">
              <div>
                <Typography.Title level={5}>{submission.bizNo}</Typography.Title>
                <Typography.Text type="secondary">{submission.submittedQuantityText ?? "未填写样品数量"}</Typography.Text>
              </div>
              <Space wrap>
                <Tag color={submission.status === "DRAFT" ? "default" : "blue"}>{submission.status}</Tag>
                <Tag>{submission.requiresGlp ? "GLP" : "Non-GLP"}</Tag>
                <Tag>{submission.reportLanguage}</Tag>
              </Space>
              {onOpenSubmission ? <Button onClick={() => onOpenSubmission(submission.id)}>查看详情</Button> : null}
            </article>
          ))}
        </div>
      ) : (
        <EmptyBlock title="暂无送检单" description="新建送检单后可在这里跟踪草稿和提交状态。" />
      )}
    </section>
  );
}

export function PortalSubmissionDetailPage({
  submissionId,
  onBack,
}: {
  submissionId?: string;
  onBack?: () => void;
}) {
  const [submission, setSubmission] = useState<Submission>();
  const [loading, setLoading] = useState(Boolean(submissionId));
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    async function load() {
      if (!submissionId) {
        setError("未选择送检单");
        setLoading(false);
        return;
      }
      setLoading(true);
      setError(undefined);
      try {
        const detail = await getSubmission(submissionId);
        if (active) {
          setSubmission(detail);
        }
      } catch (err) {
        if (active) {
          setError(resourceErrorMessage(err, "送检单"));
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
  }, [submissionId]);

  if (loading) {
    return <PageLoading />;
  }

  return (
    <section className="portalDetailPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SUBMISSION DETAIL</Typography.Text>
          <Typography.Title level={3}>送检单详情</Typography.Title>
        </div>
        {onBack ? <Button onClick={onBack}>返回送检单</Button> : null}
      </header>
      {error ? <Alert type="warning" showIcon message={error} className="portalFeedback" /> : null}
      {submission ? (
        <Descriptions bordered column={2} className="portalDetailGrid">
          <Descriptions.Item label="送检单号">{submission.bizNo}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={submission.status === "DRAFT" ? "default" : "blue"}>{submission.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="报告邮箱">{submission.officialReportEmail ?? "-"}</Descriptions.Item>
          <Descriptions.Item label="邮寄信息">{submission.shippingInfo ?? "-"}</Descriptions.Item>
          <Descriptions.Item label="GLP">{submission.requiresGlp ? "是" : "否"}</Descriptions.Item>
          <Descriptions.Item label="纸质报告">{submission.requiresHardCopyOfElectronicReport ? "需要" : "不需要"}</Descriptions.Item>
          <Descriptions.Item label="报告语言">{submission.reportLanguage}</Descriptions.Item>
          <Descriptions.Item label="协议语言">{submission.protocolLanguage}</Descriptions.Item>
          <Descriptions.Item label="样品数量">{submission.submittedQuantityText ?? "-"}</Descriptions.Item>
          <Descriptions.Item label="样品处置">{submission.sampleDisposition}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </section>
  );
}

export function PortalContractListPage({ onOpenContract }: PortalContractListPageProps) {
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listContracts({ page: 1, pageSize: 100 });
      setContracts(page.items ?? []);
    } catch {
      setError("协议列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <section className="portalListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">CONTRACTS</Typography.Text>
          <Typography.Title level={3}>合同协议</Typography.Title>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {loading ? (
        <PageLoading />
      ) : contracts.length ? (
        <div className="portalDataList">
          {contracts.map((contract) => (
            <article key={contract.id} className="portalListItem">
              <div>
                <Typography.Title level={5}>{contract.bizNo}</Typography.Title>
                <Typography.Text type="secondary">{contractTypeLabel(contract.contractType)}</Typography.Text>
              </div>
              <Space wrap>
                <Tag color={contractStatusColor(contract.status)}>{contract.status}</Tag>
                <Tag>{contract.templateCode ?? "-"}</Tag>
              </Space>
              {onOpenContract ? (
                <Button type={contract.status === "PENDING_SIGN" ? "primary" : "default"} onClick={() => onOpenContract(contract.id)}>
                  {contract.status === "PENDING_SIGN" ? "确认协议" : "查看协议"}
                </Button>
              ) : null}
            </article>
          ))}
        </div>
      ) : (
        <EmptyBlock title="暂无协议" description="项目报价和合同生成后会出现在这里。" />
      )}
    </section>
  );
}

export function PortalReportCenterPage({ onPreviewReport }: PortalReportCenterPageProps) {
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();
  const [downloadingReportId, setDownloadingReportId] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listReports({ page: 1, pageSize: 100 });
      setReports(page.items ?? []);
    } catch {
      setError("报告中心加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function downloadReport(report: Report) {
    setFeedback(undefined);
    setDownloadingReportId(report.id);
    try {
      const pdf = await downloadOfficialReportPdf(report.id);
      triggerPdfDownload(pdf, `${report.bizNo}.pdf`);
      setFeedback("正式报告已开始下载，发放日志会记录本次下载。");
    } catch (err) {
      setFeedback(
        isPaymentGateLocked(err)
          ? "付款门禁未解锁，正式报告暂不可下载。"
          : "正式报告下载失败，请稍后重试。",
      );
    } finally {
      setDownloadingReportId(undefined);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <section className="portalListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">REPORT CENTER</Typography.Text>
          <Typography.Title level={3}>报告中心</Typography.Title>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {feedback ? <Alert type="info" showIcon message={feedback} className="portalFeedback" /> : null}
      {loading ? (
        <PageLoading />
      ) : reports.length ? (
        <div className="portalDataList">
          {reports.map((report) => (
            <article key={report.id} className="portalListItem">
              <div>
                <Typography.Title level={5}>{report.bizNo}</Typography.Title>
                <Typography.Text type="secondary">{report.templateCode} / {report.templateVersion ?? "-"}</Typography.Text>
              </div>
              <Space wrap>
                <Tag color={reportStatusColor(report.status)}>{report.status}</Tag>
                <Tag color={paymentGateColor(report.paymentGateStatus)}>{report.paymentGateStatus}</Tag>
                <Tag>{report.watermarkType ?? "NONE"}</Tag>
              </Space>
              <Space wrap>
                {onPreviewReport ? <Button onClick={() => onPreviewReport(report.id)}>预览</Button> : null}
                <Button
                  type="primary"
                  ghost={report.paymentGateStatus === "LOCKED"}
                  loading={downloadingReportId === report.id}
                  onClick={() => void downloadReport(report)}
                >
                  下载正式报告
                </Button>
              </Space>
            </article>
          ))}
        </div>
      ) : (
        <EmptyBlock title="暂无报告" description="报告签发或草稿预览形成后会进入报告中心。" />
      )}
    </section>
  );
}

export function PortalAccountPage() {
  const [user, setUser] = useState<CurrentUser>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError(undefined);
      try {
        const current = await getCurrentUser();
        if (active) {
          setUser(current);
        }
      } catch {
        if (active) {
          setError("账户信息加载失败");
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
  }, []);

  if (loading) {
    return <PageLoading />;
  }

  return (
    <section className="portalDetailPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">ACCOUNT</Typography.Text>
          <Typography.Title level={3}>账户信息</Typography.Title>
        </div>
      </header>
      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {user ? (
        <Descriptions bordered column={2} className="portalDetailGrid">
          <Descriptions.Item label="账号">{user.username}</Descriptions.Item>
          <Descriptions.Item label="姓名">{user.displayName}</Descriptions.Item>
          <Descriptions.Item label="客户端">{user.clientType}</Descriptions.Item>
          <Descriptions.Item label="客户单位">{user.customerOrgId ?? "-"}</Descriptions.Item>
          <Descriptions.Item label="角色" span={2}>
            <Space wrap>
              {user.roleCodes.map((role) => <Tag key={role}>{roleLabel(role)}</Tag>)}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="数据范围" span={2}>
            <Space wrap>
              {user.dataScopes.length ? user.dataScopes.map((scope) => <Tag key={scope}>{scope}</Tag>) : <Tag>默认门户范围</Tag>}
            </Space>
          </Descriptions.Item>
        </Descriptions>
      ) : null}
    </section>
  );
}

function PageLoading() {
  return (
    <div className="pageState">
      <Spin />
    </div>
  );
}

function EmptyBlock({ title, description }: { title: string; description: string }) {
  return (
    <div className="portalEmptyState">
      <Typography.Title level={5}>{title}</Typography.Title>
      <Typography.Text type="secondary">{description}</Typography.Text>
    </div>
  );
}

function resourceErrorMessage(err: unknown, entityLabel: string) {
  const status = responseStatus(err);
  if (status === 403) {
    return `${entityLabel}不在你的授权范围内`;
  }
  if (status === 404) {
    return `${entityLabel}不存在或已删除`;
  }
  return `${entityLabel}加载失败`;
}

function responseStatus(err: unknown) {
  if (!err || typeof err !== "object" || !("response" in err)) {
    return undefined;
  }
  const response = (err as { response?: { status?: unknown } }).response;
  return typeof response?.status === "number" ? response.status : undefined;
}
