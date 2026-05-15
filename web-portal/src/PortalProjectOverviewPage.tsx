import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Progress, Space, Spin, Tag, Typography } from "antd";
import {
  listProjects,
  listSubmissions,
  type Project,
  type Submission,
} from "@lims4wuxi/shared";

type PortalProjectOverviewPageProps = {
  mode: "dashboard" | "projects";
  selectedProjectId?: string;
  onNewSubmission?: () => void;
  onOpenProjects?: () => void;
};

const PROJECT_PHASES = ["受理", "样品", "试验", "报告", "发放"] as const;

export function PortalProjectOverviewPage({
  mode,
  selectedProjectId,
  onNewSubmission,
  onOpenProjects,
}: PortalProjectOverviewPageProps) {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [submissionPage, projectPage] = await Promise.all([
        listSubmissions({ page: 1, pageSize: 50 }),
        listProjects({ page: 1, pageSize: 50 }),
      ]);
      setSubmissions(submissionPage.items ?? []);
      setProjects(projectPage.items ?? []);
    } catch {
      setError("客户进度加载失败");
    } finally {
      setLoading(false);
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
  const recentProjects = mode === "dashboard" ? projects.slice(0, 5) : projects;
  const draftCount = submissions.filter((submission) => submission.status === "DRAFT").length;
  const submittedCount = submissions.filter((submission) => submission.status !== "DRAFT").length;
  const releasedCount = projects.filter((project) => project.status === "RELEASED" || project.status === "ARCHIVED").length;

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
          <span>{draftCount}</span>
          <Typography.Text>草稿</Typography.Text>
        </div>
        <div className="portalStat">
          <span>{releasedCount}</span>
          <Typography.Text>已发放</Typography.Text>
        </div>
      </div>

      {recentProjects.length ? (
        <div className="portalProjectList">
          {recentProjects.map((project) => (
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
            </article>
          ))}
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
              <Typography.Text type="secondary">{submission.submittedQuantityText ?? "-"}</Typography.Text>
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
