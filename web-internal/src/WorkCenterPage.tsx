import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Card,
  Empty,
  Progress,
  Segmented,
  Space,
  Table,
  Tag,
  Typography,
  type TableProps,
} from "antd";
import {
  getWorkCenterResourceLoad,
  getWorkCenterSummary,
  getWorkCenterTrends,
  listWorkCenterBlockers,
  listWorkCenterProjects,
  type WorkCenterBlocker,
  type WorkCenterProject,
  type WorkCenterProjectFilter,
  type WorkCenterResourceLoad,
  type WorkCenterSummary,
  type WorkCenterTrend,
} from "@lims4wuxi/shared";

const FILTER_OPTIONS: Array<{ label: string; value: WorkCenterProjectFilter }> = [
  { label: "风险", value: "risk" },
  { label: "预警", value: "warning" },
  { label: "超期", value: "overdue" },
  { label: "正常", value: "normal" },
];

const HEALTH_LABELS: Record<WorkCenterProject["healthStatus"], string> = {
  NORMAL: "正常",
  WARNING: "预警",
  RISK: "风险",
};

const BLOCKER_STATUS_LABELS: Record<WorkCenterBlocker["status"], string> = {
  OPEN: "未处理",
  IN_PROGRESS: "处理中",
  RESOLVED: "已解决",
  CLOSED: "已关闭",
};

const TREND_LABELS: Record<string, string> = {
  plannedTasks: "计划任务",
  completedTasks: "完成任务",
  overdueTasks: "超期任务",
  openBlockers: "未关阻塞",
  riskProjects: "风险项目",
  resourceLoadRate: "资源负荷",
};

export function WorkCenterPage() {
  const [summary, setSummary] = useState<WorkCenterSummary>();
  const [projects, setProjects] = useState<WorkCenterProject[]>([]);
  const [blockers, setBlockers] = useState<WorkCenterBlocker[]>([]);
  const [resourceLoad, setResourceLoad] = useState<WorkCenterResourceLoad[]>([]);
  const [trends, setTrends] = useState<WorkCenterTrend[]>([]);
  const [filter, setFilter] = useState<WorkCenterProjectFilter>("risk");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const [nextSummary, nextProjects, nextBlockers, nextResourceLoad, nextTrends] = await Promise.all([
        getWorkCenterSummary(),
        listWorkCenterProjects({ filter, page: 1, pageSize: 20 }),
        listWorkCenterBlockers({ page: 1, pageSize: 20 }),
        getWorkCenterResourceLoad(),
        getWorkCenterTrends(),
      ]);
      setSummary(nextSummary);
      setProjects(nextProjects.items);
      setBlockers(nextBlockers.items);
      setResourceLoad(nextResourceLoad);
      setTrends(nextTrends);
    } catch {
      setError("工作中心数据加载失败");
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    void load();
  }, [load]);

  const projectColumns: TableProps<WorkCenterProject>["columns"] = [
    {
      title: "项目编号",
      dataIndex: "projectBizNo",
      width: 170,
      render: (value, project) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{value}</Typography.Text>
          <Typography.Text type="secondary">{project.customerOrgName ?? "-"}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 130,
    },
    {
      title: "健康度",
      width: 150,
      render: (_, project) => (
        <Space>
          <HealthTag status={project.healthStatus} />
          <Typography.Text>{project.healthScore}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "当前卡点",
      dataIndex: "mainBlocker",
      ellipsis: true,
      render: (value) => value || "-",
    },
  ];

  const blockerColumns: TableProps<WorkCenterBlocker>["columns"] = [
    {
      title: "类型",
      dataIndex: "blockerType",
      width: 150,
    },
    {
      title: "严重度",
      dataIndex: "severity",
      width: 100,
      render: (severity: WorkCenterBlocker["severity"]) => <SeverityTag severity={severity} />,
    },
    {
      title: "说明",
      dataIndex: "description",
      ellipsis: true,
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 110,
      render: (status: WorkCenterBlocker["status"]) => <Tag>{BLOCKER_STATUS_LABELS[status]}</Tag>,
    },
  ];

  const loadColumns: TableProps<WorkCenterResourceLoad>["columns"] = [
    { title: "人员", dataIndex: "displayName" },
    { title: "活跃工单", dataIndex: "activeWorkOrders", width: 100 },
    {
      title: "负荷",
      dataIndex: "loadPercent",
      width: 180,
      render: (value: number) => <Progress percent={value} size="small" status={value >= 90 ? "exception" : "active"} />,
    },
  ];

  const trendRows = useMemo(
    () => trends.map((trend) => ({ ...trend, label: TREND_LABELS[trend.metricCode] ?? trend.metricCode })),
    [trends],
  );

  return (
    <section className="workCenter">
      <header className="workCenterHeader">
        <div>
          <Typography.Title level={3}>工作中心</Typography.Title>
          <Typography.Text type="secondary">{summary?.generatedAt ? formatTime(summary.generatedAt) : ""}</Typography.Text>
        </div>
        <Segmented
          options={FILTER_OPTIONS}
          value={filter}
          onChange={(value) => setFilter(value as WorkCenterProjectFilter)}
        />
      </header>

      {error ? <Alert type="error" message={error} showIcon closable onClose={() => setError(undefined)} /> : null}

      <div className="workCenterSummary">
        <MetricCard label="正常项目" value={summary?.normalProjects ?? 0} tone="normal" />
        <MetricCard label="预警项目" value={summary?.warningProjects ?? 0} tone="warning" />
        <MetricCard label="风险项目" value={summary?.riskProjects ?? 0} tone="risk" />
        <MetricCard label="我的待办" value={summary?.myTodos ?? 0} tone="todo" />
        <MetricCard label="超期任务" value={summary?.overdueTasks ?? 0} tone="overdue" />
      </div>

      <div className="workCenterGrid">
        <Card title="项目健康度" className="workCenterPanel">
          <Table
            rowKey="projectId"
            size="middle"
            loading={loading}
            columns={projectColumns}
            dataSource={projects}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
            scroll={{ x: 760 }}
          />
        </Card>

        <Card title="当前阻塞" className="workCenterPanel">
          <Table
            rowKey="id"
            size="middle"
            loading={loading}
            columns={blockerColumns}
            dataSource={blockers}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
            scroll={{ x: 680 }}
          />
        </Card>

        <Card title="人员负荷" className="workCenterPanel">
          <Table
            rowKey="userId"
            size="middle"
            loading={loading}
            columns={loadColumns}
            dataSource={resourceLoad}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          />
        </Card>

        <Card title="趋势指标" className="workCenterPanel">
          <div className="trendGrid">
            {trendRows.map((trend) => (
              <div key={trend.metricCode} className="trendItem">
                <Typography.Text type="secondary">{trend.label}</Typography.Text>
                <Typography.Title level={4}>{formatMetricValue(trend)}</Typography.Title>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </section>
  );
}

function MetricCard({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <Card className={`metricCard metric-${tone}`}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Title level={3}>{value}</Typography.Title>
    </Card>
  );
}

function HealthTag({ status }: { status: WorkCenterProject["healthStatus"] }) {
  const color = status === "RISK" ? "red" : status === "WARNING" ? "gold" : "green";
  return <Tag color={color}>{HEALTH_LABELS[status]}</Tag>;
}

function SeverityTag({ severity }: { severity: WorkCenterBlocker["severity"] }) {
  const color = severity === "HIGH" ? "red" : severity === "MEDIUM" ? "gold" : "blue";
  return <Tag color={color}>{severity}</Tag>;
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatMetricValue(trend: WorkCenterTrend & { label: string }) {
  const value = Number(trend.value);
  return trend.metricCode === "resourceLoadRate" ? `${Math.round(value)}%` : Math.round(value).toString();
}
