import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  Modal,
  Progress,
  Segmented,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  type TableProps,
} from "antd";
import {
  closeWorkCenterBlocker,
  createWorkCenterBlocker,
  getCurrentUser,
  getWorkCenterResourceLoad,
  getWorkCenterSummary,
  getWorkCenterTasks,
  getWorkCenterTrends,
  listWorkCenterBlockers,
  listWorkCenterProjects,
  updateWorkCenterBlocker,
  type CurrentUser,
  type WorkCenterBlocker,
  type WorkCenterProject,
  type WorkCenterProjectFilter,
  type WorkCenterResourceLoad,
  type WorkCenterSummary,
  type WorkCenterTask,
  type WorkCenterTrend,
} from "@lims4wuxi/shared";

const FILTER_OPTIONS: Array<{ label: string; value: WorkCenterProjectFilter }> = [
  { label: "风险", value: "risk" },
  { label: "预警", value: "warning" },
  { label: "超期", value: "overdue" },
  { label: "正常", value: "normal" },
];

const BLOCKER_TYPE_OPTIONS = [
  { label: "待审批", value: "PENDING_APPROVAL" },
  { label: "缺样", value: "MISSING_SAMPLE" },
  { label: "缺资料", value: "MISSING_MATERIAL" },
  { label: "设备冲突", value: "DEVICE_CONFLICT" },
  { label: "人员不可用", value: "STAFF_UNAVAILABLE" },
  { label: "技术风险", value: "TECHNICAL_RISK" },
  { label: "客户确认", value: "CUSTOMER_CONFIRMATION" },
  { label: "分包延迟", value: "SUBCONTRACT_DELAY" },
  { label: "付款阻塞", value: "PAYMENT_BLOCKED" },
  { label: "报告返工", value: "REPORT_REWORK" },
  { label: "其他", value: "OTHER" },
];

const SEVERITY_OPTIONS = [
  { label: "高", value: "HIGH" },
  { label: "中", value: "MEDIUM" },
  { label: "低", value: "LOW" },
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

const ROLE_GROUP_LABELS: Record<string, string> = {
  SALES: "销售",
  TECHNICAL: "技术",
  BUSINESS: "业务",
  SAMPLE: "样品",
  INSPECTOR: "检测",
  QC: "QC",
  QA: "QA",
  SD: "SD",
  REPORT: "报告",
  SIGNATORY: "签发",
  FINANCE: "财务",
  GREEN_CHANNEL: "绿色通道",
  EQUIPMENT: "设备",
  INVENTORY: "库存",
  ARCHIVE: "档案",
};

const STATUS_LABELS: Record<string, string> = {
  SUBMITTED: "待受理",
  ACCEPTED: "已受理",
  NONSTANDARD_REVIEW: "非标评审",
  QUOTABLE: "待报价",
  QUOTED: "已报价",
  CONTRACTED: "已签约",
  ARRIVED_PENDING_CHECK: "待核验",
  PRE_IN_STOCK: "待入库",
  PENDING_RETURN: "待退还",
  PENDING_DISPOSAL: "待处置",
  PENDING_REGISTER: "待登记",
  EXPIRING: "临期",
  REVIEWING: "审核中",
  QC_REVIEWING: "QC 审核",
  QA_REVIEWING: "QA 审核",
  SD_REVIEWING: "SD 审核",
  PENDING_ISSUE: "待签发",
  REPORTING: "报告中",
  LOCKED: "锁定",
  GREEN_UNLOCKED: "绿色通道",
  UNLOCKED: "已解锁",
  DEPT_REVIEWING: "部门审批",
  MANAGEMENT_REVIEWING: "管理层审批",
  BOOKED: "已预约",
  NEEDS_RESCHEDULE: "待重约",
  ACTIVE: "有效",
  IN_STOCK: "在库",
  LOW_STOCK: "低库存",
  EXPIRED: "已过期",
  GREEN_RELEASED: "绿色放行",
  RELEASED: "已发放",
};

export function WorkCenterPage() {
  const [summary, setSummary] = useState<WorkCenterSummary>();
  const [currentUser, setCurrentUser] = useState<CurrentUser>();
  const [projects, setProjects] = useState<WorkCenterProject[]>([]);
  const [blockers, setBlockers] = useState<WorkCenterBlocker[]>([]);
  const [tasks, setTasks] = useState<WorkCenterTask[]>([]);
  const [resourceLoad, setResourceLoad] = useState<WorkCenterResourceLoad[]>([]);
  const [trends, setTrends] = useState<WorkCenterTrend[]>([]);
  const [filter, setFilter] = useState<WorkCenterProjectFilter>("risk");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [createOpen, setCreateOpen] = useState(false);
  const [createProject, setCreateProject] = useState<WorkCenterProject>();
  const [mutating, setMutating] = useState(false);
  const [closingBlocker, setClosingBlocker] = useState<WorkCenterBlocker>();
  const [createForm] = Form.useForm<BlockerFormValues>();
  const [closeForm] = Form.useForm<BlockerCloseValues>();

  const load = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const [nextUser, nextSummary, nextProjects, nextBlockers, nextTasks, nextResourceLoad, nextTrends] = await Promise.all([
        getCurrentUser(),
        getWorkCenterSummary(),
        listWorkCenterProjects({ filter, page: 1, pageSize: 20 }),
        listWorkCenterBlockers({ page: 1, pageSize: 20 }),
        getWorkCenterTasks({ limit: 40 }),
        getWorkCenterResourceLoad(),
        getWorkCenterTrends(),
      ]);
      setCurrentUser(nextUser);
      setSummary(nextSummary);
      setProjects(nextProjects.items);
      setBlockers(nextBlockers.items);
      setTasks(nextTasks);
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

  function openCreate(project: WorkCenterProject) {
    setCreateProject(project);
    createForm.setFieldsValue({
      blockerType: "CUSTOMER_CONFIRMATION",
      severity: "HIGH",
      description: "",
      impactScope: project.projectBizNo,
      expectedRecoveryAt: defaultRecoveryAt(),
      ownerUserId: currentUser?.id ?? "",
    });
    setCreateOpen(true);
  }

  async function submitCreate(values: BlockerFormValues) {
    if (!createProject) {
      return;
    }
    setMutating(true);
    setError(undefined);
    try {
      await createWorkCenterBlocker({
        projectId: createProject.projectId,
        blockerType: values.blockerType,
        severity: values.severity,
        description: values.description,
        impactScope: values.impactScope,
        expectedRecoveryAt: new Date(values.expectedRecoveryAt).toISOString(),
        ownerUserId: values.ownerUserId,
        collaboratorIds: [],
      });
      setCreateOpen(false);
      createForm.resetFields();
      await load();
    } catch {
      setError("新增阻塞失败");
    } finally {
      setMutating(false);
    }
  }

  async function changeBlockerStatus(blocker: WorkCenterBlocker, status: "IN_PROGRESS" | "RESOLVED") {
    setMutating(true);
    setError(undefined);
    try {
      await updateWorkCenterBlocker(blocker.id, { status });
      await load();
    } catch {
      setError("更新阻塞状态失败");
    } finally {
      setMutating(false);
    }
  }

  function openClose(blocker: WorkCenterBlocker) {
    setClosingBlocker(blocker);
    closeForm.setFieldsValue({ resolution: "" });
  }

  async function submitClose(values: BlockerCloseValues) {
    if (!closingBlocker) {
      return;
    }
    setMutating(true);
    setError(undefined);
    try {
      await closeWorkCenterBlocker(closingBlocker.id, values);
      setClosingBlocker(undefined);
      closeForm.resetFields();
      await load();
    } catch {
      setError("关闭阻塞失败");
    } finally {
      setMutating(false);
    }
  }

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
      render: (value: string) => statusLabel(value),
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
    {
      title: "操作",
      width: 190,
      render: (_, project) => (
        <Space wrap>
          <Button size="small" type="link" href="/internal/projects">查看项目</Button>
          <Button size="small" onClick={() => openCreate(project)}>新增阻塞</Button>
        </Space>
      ),
    },
  ];

  const taskColumns: TableProps<WorkCenterTask>["columns"] = [
    {
      title: "待办",
      dataIndex: "title",
      width: 220,
      render: (value, task) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{value}</Typography.Text>
          <Typography.Text type="secondary">{task.bizNo}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "岗位",
      dataIndex: "roleGroup",
      width: 110,
      render: (value: string) => <Tag color="blue">{ROLE_GROUP_LABELS[value] ?? value}</Tag>,
    },
    {
      title: "客户/对象",
      dataIndex: "customerOrgName",
      ellipsis: true,
      render: (value) => value || "-",
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 150,
      render: (value: string) => statusLabel(value),
    },
    {
      title: "时间",
      dataIndex: "dueAt",
      width: 140,
      render: (value?: string | null) => (value ? formatTime(value) : "-"),
    },
    {
      title: "操作",
      width: 90,
      render: (_, task) => (
        <Button size="small" type="link" href={task.actionPath} title={`${task.title}：${task.bizNo}`}>
          进入
        </Button>
      ),
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
    {
      title: "操作",
      width: 220,
      render: (_, blocker) => {
        const inProgressReason = blockerActionReason(blocker, "IN_PROGRESS", mutating);
        const resolveReason = blockerActionReason(blocker, "RESOLVED", mutating);
        const closeReason = blockerActionReason(blocker, "CLOSE", mutating);
        return (
          <Space wrap>
            <Tooltip title={inProgressReason}>
              <span>
                <Button
                  size="small"
                  disabled={Boolean(inProgressReason)}
                  onClick={() => void changeBlockerStatus(blocker, "IN_PROGRESS")}
                >
                  处理中
                </Button>
              </span>
            </Tooltip>
            <Tooltip title={resolveReason}>
              <span>
                <Button
                  size="small"
                  disabled={Boolean(resolveReason)}
                  onClick={() => void changeBlockerStatus(blocker, "RESOLVED")}
                >
                  解决
                </Button>
              </span>
            </Tooltip>
            <Tooltip title={closeReason}>
              <span>
                <Button
                  size="small"
                  disabled={Boolean(closeReason)}
                  onClick={() => openClose(blocker)}
                >
                  关闭
                </Button>
              </span>
            </Tooltip>
          </Space>
        );
      },
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
          <Typography.Text className="pageEyebrow">PROJECT WORK CENTER</Typography.Text>
          <Typography.Title level={3}>工作中心</Typography.Title>
          <Typography.Text type="secondary">
            健康度、阻塞、待办和人员负荷统一入口
            {summary?.generatedAt ? ` · ${formatTime(summary.generatedAt)}` : ""}
          </Typography.Text>
        </div>
        <Segmented
          options={FILTER_OPTIONS}
          value={filter}
          onChange={(value) => setFilter(value as WorkCenterProjectFilter)}
        />
      </header>

      {error ? <Alert type="error" message={error} showIcon closable onClose={() => setError(undefined)} /> : null}

      <div className="workCenterSummary">
        <MetricCard label="正常项目" helper="80-100" value={summary?.normalProjects ?? 0} tone="normal" onClick={() => setFilter("normal")} />
        <MetricCard label="预警项目" helper="60-79" value={summary?.warningProjects ?? 0} tone="warning" onClick={() => setFilter("warning")} />
        <MetricCard label="风险项目" helper="<60" value={summary?.riskProjects ?? 0} tone="risk" onClick={() => setFilter("risk")} />
        <MetricCard label="我的待办" helper="Today" value={summary?.myTodos ?? 0} tone="todo" />
        <MetricCard label="超期任务" helper="Overdue" value={summary?.overdueTasks ?? 0} tone="overdue" onClick={() => setFilter("overdue")} />
      </div>

      <div className="workCenterGrid">
        <Card title="岗位待办" className="workCenterPanel workCenterPanelWide">
          <Table
            rowKey="taskKey"
            size="middle"
            loading={loading}
            columns={taskColumns}
            dataSource={tasks}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
            scroll={{ x: 820 }}
          />
        </Card>

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

      <Modal
        title={createProject ? `新增阻塞 · ${createProject.projectBizNo}` : "新增阻塞"}
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => createForm.submit()}
        confirmLoading={mutating}
        destroyOnHidden
      >
        <Form form={createForm} layout="vertical" onFinish={(values) => void submitCreate(values)}>
          <Form.Item name="blockerType" label="阻塞类型" rules={[{ required: true }]}>
            <Select options={BLOCKER_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="severity" label="严重度" rules={[{ required: true }]}>
            <Select options={SEVERITY_OPTIONS} />
          </Form.Item>
          <Form.Item name="description" label="说明" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="impactScope" label="影响范围" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="expectedRecoveryAt" label="预计恢复时间" rules={[{ required: true }]}>
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item name="ownerUserId" label="负责人用户 ID" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="关闭阻塞"
        open={Boolean(closingBlocker)}
        onCancel={() => setClosingBlocker(undefined)}
        onOk={() => closeForm.submit()}
        confirmLoading={mutating}
        destroyOnHidden
      >
        <Form form={closeForm} layout="vertical" onFinish={(values) => void submitClose(values)}>
          <Form.Item name="resolution" label="解决说明" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

type BlockerFormValues = {
  blockerType: WorkCenterBlocker["blockerType"];
  severity: WorkCenterBlocker["severity"];
  description: string;
  impactScope: string;
  expectedRecoveryAt: string;
  ownerUserId: string;
};

type BlockerCloseValues = {
  resolution: string;
};

function MetricCard(
  { label, helper, value, tone, onClick }: {
    label: string;
    helper: string;
    value: number;
    tone: string;
    onClick?: () => void;
  },
) {
  return (
    <Card
      className={`metricCard metric-${tone}${onClick ? " metricCardAction" : ""}`}
      hoverable={Boolean(onClick)}
      onClick={onClick}
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={(event) => {
        if (onClick && (event.key === "Enter" || event.key === " ")) {
          onClick();
        }
      }}
    >
      <span className="metricAccent" />
      <div className="metricLabel">
        <Typography.Text type="secondary">{label}</Typography.Text>
        <Typography.Text type="secondary">{helper}</Typography.Text>
      </div>
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

function statusLabel(status: string) {
  return STATUS_LABELS[status] ?? status;
}

function blockerActionReason(
  blocker: WorkCenterBlocker,
  action: "IN_PROGRESS" | "RESOLVED" | "CLOSE",
  mutating: boolean,
) {
  if (mutating) {
    return "操作处理中，请稍候";
  }
  if (action === "IN_PROGRESS" && blocker.status !== "OPEN") {
    return "只有未处理阻塞可以转为处理中";
  }
  if (action === "RESOLVED" && (blocker.status === "RESOLVED" || blocker.status === "CLOSED")) {
    return blocker.status === "RESOLVED" ? "阻塞已解决，不能重复解决" : "阻塞已关闭，不能再解决";
  }
  if (action === "CLOSE" && blocker.status === "CLOSED") {
    return "阻塞已关闭，不能重复关闭";
  }
  return undefined;
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

function defaultRecoveryAt() {
  const value = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hour = String(value.getHours()).padStart(2, "0");
  const minute = String(value.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hour}:${minute}`;
}
