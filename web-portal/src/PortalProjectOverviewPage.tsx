import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Progress, Space, Spin, Tag, Typography } from "antd";
import {
  downloadOfficialReportPdf,
  listContracts,
  listProjects,
  listReports,
  listSubmissions,
  type Contract,
  type Project,
  type Report,
  type Submission,
} from "@lims4wuxi/shared";

import { contractStatusColor, contractTypeLabel } from "./contractPresentation";
import { isPaymentGateLocked, paymentGateColor, reportStatusColor, triggerPdfDownload } from "./reportPresentation";

type PortalProjectOverviewPageProps = {
  mode: "dashboard" | "projects";
  selectedProjectId?: string;
  onNewSubmission?: () => void;
  onOpenSubmission?: (submissionId: string) => void;
  onOpenProjects?: () => void;
  onOpenProject?: (projectId: string) => void;
  onOpenContract?: (contractId: string) => void;
  onPreviewReport?: (reportId: string) => void;
};

const PROJECT_PHASES = ["受理", "样品", "试验", "报告", "发放"] as const;

export function PortalProjectOverviewPage({
  mode,
  selectedProjectId,
  onNewSubmission,
  onOpenSubmission,
  onOpenProjects,
  onOpenProject,
  onOpenContract,
  onPreviewReport,
}: PortalProjectOverviewPageProps) {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [downloadFeedback, setDownloadFeedback] = useState<string>();
  const [downloadingReportId, setDownloadingReportId] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [submissionPage, projectPage, contractPage, reportPage] = await Promise.all([
        listSubmissions({ page: 1, pageSize: 50 }),
        listProjects({ page: 1, pageSize: 50 }),
        listContracts({ page: 1, pageSize: 100 }),
        listReports({ page: 1, pageSize: 100 }),
      ]);
      setSubmissions(submissionPage.items ?? []);
      setProjects(projectPage.items ?? []);
      setContracts(contractPage.items ?? []);
      setReports(reportPage.items ?? []);
    } catch {
      setError("客户进度加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function downloadReport(report: Report) {
    setDownloadFeedback(undefined);
    setDownloadingReportId(report.id);
    try {
      const pdf = await downloadOfficialReportPdf(report.id);
      triggerPdfDownload(pdf, `${report.bizNo}.pdf`);
      setDownloadFeedback("正式报告已开始下载，发放日志会记录本次下载。");
    } catch (err) {
      setDownloadFeedback(
        isPaymentGateLocked(err)
          ? "未付款或绿色通道未解锁，正式报告暂不可下载；当前仅支持在线预览。"
          : "正式报告下载失败，请稍后重试。",
      );
    } finally {
      setDownloadingReportId(undefined);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId),
    [projects, selectedProjectId],
  );
  const submissionById = useMemo(
    () => new Map(submissions.map((submission) => [submission.id, submission])),
    [submissions],
  );
  const reportByProjectId = useMemo(() => {
    const byProject = new Map<string, Report>();
    for (const report of reports) {
      if (!byProject.has(report.projectId)) {
        byProject.set(report.projectId, report);
      }
    }
    return byProject;
  }, [reports]);
  const contractByProjectId = useMemo(() => {
    const byProject = new Map<string, Contract>();
    for (const contract of contracts) {
      if (!byProject.has(contract.projectId)) {
        byProject.set(contract.projectId, contract);
      }
    }
    return byProject;
  }, [contracts]);
  const recentProjects = mode === "dashboard" ? projects.slice(0, 5) : projects;
  const submittedCount = submissions.filter((submission) => submission.status !== "DRAFT").length;
  const releasedCount = projects.filter((project) => project.status === "RELEASED" || project.status === "ARCHIVED").length;
  const pendingContracts = contracts.filter((contract) => contract.status === "PENDING_SIGN");
  const lockedReports = reports.filter((report) => report.paymentGateStatus === "LOCKED");
  const pendingSubmissions = submissions.filter((submission) => submission.status === "SUBMITTED");
  const messages = [
    ...pendingContracts.slice(0, 2).map((contract) => ({
      key: `contract:${contract.id}`,
      title: "协议待确认",
      detail: contract.bizNo,
      action: () => onOpenContract?.(contract.id),
    })),
    ...lockedReports.slice(0, 2).map((report) => ({
      key: `report:${report.id}`,
      title: "报告付款门禁未解锁",
      detail: report.bizNo,
      action: () => onPreviewReport?.(report.id),
    })),
    ...pendingSubmissions.slice(0, 2).map((submission) => ({
      key: `submission:${submission.id}`,
      title: "送检单已提交待受理",
      detail: submission.bizNo,
      action: () => onOpenSubmission?.(submission.id),
    })),
  ];

  if (loading) {
    return (
      <div className="pageState">
        <Spin />
      </div>
    );
  }

  return (
    <section className="portalProjectPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">CUSTOMER PROJECTS</Typography.Text>
          <Typography.Title level={3}>{mode === "dashboard" ? "客户首页" : "我的项目"}</Typography.Title>
        </div>
        <Space wrap>
          {onNewSubmission ? <Button type="primary" onClick={onNewSubmission}>新建送检单</Button> : null}
          {mode === "dashboard" && onOpenProjects ? <Button onClick={onOpenProjects}>查看全部</Button> : null}
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      {error ? <Alert type="error" showIcon message={error} className="portalFeedback" /> : null}
      {downloadFeedback ? <Alert type="info" showIcon message={downloadFeedback} className="portalFeedback" /> : null}
      {selectedProjectId && !selectedProject ? (
        <Alert type="warning" showIcon message="当前项目不在你的授权范围内" className="portalFeedback" />
      ) : null}

      <div className="portalStats">
        <div className="portalStat">
          <span>{projects.length}</span>
          <Typography.Text>项目</Typography.Text>
        </div>
        <div className="portalStat">
          <span>{submittedCount}</span>
          <Typography.Text>已提交送检</Typography.Text>
        </div>
        <div className="portalStat">
          <span>{reports.length}</span>
          <Typography.Text>报告</Typography.Text>
        </div>
        <div className="portalStat">
          <span>{releasedCount}</span>
          <Typography.Text>已发放</Typography.Text>
        </div>
      </div>

      {messages.length ? (
        <div className="portalMessageList" aria-label="客户待处理消息">
          {messages.map((message) => (
            <button key={message.key} type="button" onClick={message.action}>
              <span>{message.title}</span>
              <strong>{message.detail}</strong>
            </button>
          ))}
        </div>
      ) : null}

      {recentProjects.length ? (
        <div className="portalProjectList">
          {recentProjects.map((project) => {
            const contract = contractByProjectId.get(project.id);
            const report = reportByProjectId.get(project.id);
            return (
              <article
                key={project.id}
                className={project.id === selectedProjectId ? "portalProjectItem isSelected" : "portalProjectItem"}
              >
                <div className="portalProjectTopline">
                  <div>
                    <Typography.Title level={5}>{project.bizNo}</Typography.Title>
                    <Typography.Text type="secondary">
                      {submissionById.get(project.submissionId)?.bizNo ?? project.submissionId}
                    </Typography.Text>
                  </div>
                  <Space wrap>
                    <Tag color={projectStatusColor(project.status)}>{project.status}</Tag>
                    <Tag>{project.paymentStatus ?? "UNPAID"}</Tag>
                  </Space>
                </div>
                <div className="portalProjectMeta">
                  <span>{projectTypeLabel(project.projectType)}</span>
                  <span>{project.qualificationType ?? "NONE"}</span>
                  <span>{project.requiresGlp ? "GLP" : "Non-GLP"}</span>
                </div>
                <Progress
                  percent={phasePercent(project.status)}
                  showInfo={false}
                  strokeColor="var(--lims-primary)"
                  trailColor="var(--lims-line-soft)"
                />
                <div className="portalPhaseRow" aria-label="项目进度">
                  {PROJECT_PHASES.map((phase, index) => (
                    <span key={phase} className={index <= phaseIndex(project.status) ? "isDone" : undefined}>
                      {phase}
                    </span>
                  ))}
                </div>
                {onOpenProject ? (
                  <div className="portalProjectActions">
                    <Button onClick={() => onOpenProject(project.id)}>查看项目</Button>
                  </div>
                ) : null}
                {contract ? (
                  <div className="portalContractRow">
                    <div>
                      <Typography.Text strong>{contract.bizNo}</Typography.Text>
                      <Space wrap className="portalReportTags">
                        <Tag color={contractStatusColor(contract.status)}>{contract.status}</Tag>
                        <Tag>{contractTypeLabel(contract.contractType)}</Tag>
                        <Tag>{contract.templateCode ?? "-"}</Tag>
                      </Space>
                    </div>
                    {onOpenContract ? (
                      <Button onClick={() => onOpenContract(contract.id)}>
                        {contract.status === "PENDING_SIGN" ? "确认协议" : "查看协议"}
                      </Button>
                    ) : null}
                  </div>
                ) : null}
                {report ? (
                  <div className="portalReportRow">
                    <div>
                      <Typography.Text strong>{report.bizNo}</Typography.Text>
                      <Space wrap className="portalReportTags">
                        <Tag color={reportStatusColor(report.status)}>{report.status}</Tag>
                        <Tag color={paymentGateColor(report.paymentGateStatus)}>{report.paymentGateStatus}</Tag>
                        <Tag>{report.watermarkType ?? "NONE"}</Tag>
                      </Space>
                    </div>
                    <Space wrap>
                      {onPreviewReport ? (
                        <Button onClick={() => onPreviewReport(report.id)}>预览报告</Button>
                      ) : null}
                      <Button
                        type="primary"
                        ghost={report.paymentGateStatus === "LOCKED"}
                        loading={downloadingReportId === report.id}
                        onClick={() => void downloadReport(report)}
                      >
                        下载正式报告
                      </Button>
                    </Space>
                  </div>
                ) : null}
              </article>
            );
          })}
        </div>
      ) : (
        <div className="portalEmptyState">
          <Typography.Title level={5}>暂无项目</Typography.Title>
          <Typography.Text type="secondary">
            {submissions.length ? "送检单已保存，内部受理后会生成项目。" : "当前没有送检记录。"}
          </Typography.Text>
        </div>
      )}

      {!projects.length && submissions.length ? (
        <div className="portalSubmissionList">
          {submissions.slice(0, 6).map((submission) => (
            <article key={submission.id} className="portalSubmissionItem">
              <Typography.Text strong>{submission.bizNo}</Typography.Text>
              <Tag color={submission.status === "DRAFT" ? "default" : "blue"}>{submission.status}</Tag>
              {onOpenSubmission ? (
                <Button onClick={() => onOpenSubmission(submission.id)}>查看详情</Button>
              ) : (
                <Typography.Text type="secondary">{submission.submittedQuantityText ?? "-"}</Typography.Text>
              )}
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

function phaseIndex(status: Project["status"]) {
  if (status === "RELEASED" || status === "ARCHIVED") {
    return 4;
  }
  if (status === "REPORTING" || status === "PAYMENT_HOLD") {
    return 3;
  }
  if (status === "PLAN_READY" || status === "IN_TESTING") {
    return 2;
  }
  if (status === "SAMPLE_RECEIVED" || status === "WAITING_SAMPLE") {
    return 1;
  }
  return 0;
}

function phasePercent(status: Project["status"]) {
  return [8, 32, 56, 78, 100][phaseIndex(status)];
}

function projectTypeLabel(value: Project["projectType"]) {
  if (value === "STD_ACCREDITED") {
    return "标准有资质";
  }
  if (value === "STD_NON_ACCREDITED") {
    return "标准无资质";
  }
  return "非标";
}

function projectStatusColor(status: Project["status"]) {
  if (status === "RELEASED" || status === "ARCHIVED") {
    return "green";
  }
  if (status === "PAYMENT_HOLD") {
    return "orange";
  }
  if (status === "NONSTANDARD_REVIEW" || status === "IN_TESTING" || status === "REPORTING") {
    return "blue";
  }
  return "default";
}
