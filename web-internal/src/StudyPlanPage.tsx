import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createStudyPlan,
  dispatchStudyPlan,
  listProjects,
  listStudyPlans,
  listWorkOrders,
  submitStudyPlanReview,
  useAuthStore,
  type Project,
  type StudyPlan,
  type WorkOrder,
} from "@lims4wuxi/shared";

type PlanAction = {
  id: string;
  action: "create" | "review" | "dispatch";
};

type StudyPlanFormValues = {
  requiresConditioning: boolean;
  plannedStartAt?: string;
  plannedFinishAt?: string;
};

const ELIGIBLE_PROJECT_STATUSES: Array<Project["status"]> = ["CONTRACTED", "SAMPLE_RECEIVED", "PLAN_READY"];

export function StudyPlanPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<StudyPlanFormValues>();
  const [projects, setProjects] = useState<Project[]>([]);
  const [plans, setPlans] = useState<StudyPlan[]>([]);
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<PlanAction>();
  const [createTarget, setCreateTarget] = useState<Project>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [projectPage, planPage, workOrderPage] = await Promise.all([
        listProjects({ page: 1, pageSize: 50 }),
        listStudyPlans({ page: 1, pageSize: 50 }),
        listWorkOrders({ page: 1, pageSize: 50 }),
      ]);
      setProjects(projectPage.items ?? []);
      setPlans(planPage.items ?? []);
      setWorkOrders(workOrderPage.items ?? []);
    } catch {
      setError("方案、项目或工单列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const planProjectIds = useMemo(() => new Set(plans.map((plan) => plan.projectId)), [plans]);
  const planProjectBizNo = useMemo(() => {
    return Object.fromEntries(projects.map((project) => [project.id, project.bizNo]));
  }, [projects]);
  const eligibleProjects = useMemo(() => {
    return projects.filter((project) => ELIGIBLE_PROJECT_STATUSES.includes(project.status) && !planProjectIds.has(project.id));
  }, [planProjectIds, projects]);

  function openCreate(project: Project) {
    setError(undefined);
    setFeedback(undefined);
    form.setFieldsValue({
      requiresConditioning: false,
      plannedStartAt: toLocalDateTimeInputValue(daysFromNow(1)),
      plannedFinishAt: toLocalDateTimeInputValue(daysFromNow(10)),
    });
    setCreateTarget(project);
  }

  async function createPlan() {
    if (!createTarget) {
      return;
    }
    setAction({ id: createTarget.id, action: "create" });
    setError(undefined);
    try {
      const values = await form.validateFields();
      const plan = await createStudyPlan({
        projectId: createTarget.id,
        requiresConditioning: values.requiresConditioning,
        plannedStartAt: toIsoOrUndefined(values.plannedStartAt),
        plannedFinishAt: toIsoOrUndefined(values.plannedFinishAt),
      });
      setFeedback(`方案 ${plan.bizNo} 已创建`);
      setCreateTarget(undefined);
      await load();
    } catch {
      setError("方案创建失败，请确认项目已签约且当前账号有方案编制权限");
    } finally {
      setAction(undefined);
    }
  }

  async function reviewPlan(plan: StudyPlan) {
    await runPlanAction(plan, "review", async () => {
      const reviewed = await submitStudyPlanReview(plan.id);
      setFeedback(`方案 ${reviewed.bizNo} 已审批为 ${reviewed.status}`);
    });
  }

  async function dispatchPlan(plan: StudyPlan) {
    await runPlanAction(plan, "dispatch", async () => {
      const dispatched = await dispatchStudyPlan(plan.id);
      setFeedback(`方案 ${dispatched.bizNo} 已下发`);
    });
  }

  async function runPlanAction(plan: StudyPlan, nextAction: PlanAction["action"], execute: () => Promise<void>) {
    setAction({ id: plan.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      await execute();
      await load();
    } catch {
      setError("方案动作执行失败，请确认合同、样品、审批和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  const projectColumns = useMemo<TableColumnsType<Project>>(() => [
    {
      title: "项目编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 150,
      render: (status: Project["status"]) => <Tag color={projectStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "资质",
      dataIndex: "qualificationType",
      key: "qualificationType",
      width: 110,
      render: (value: Project["qualificationType"]) => value ?? "-",
    },
    {
      title: "GLP",
      dataIndex: "requiresGlp",
      key: "requiresGlp",
      width: 86,
      render: (value: boolean) => value ? <Tag color="green">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "动物试验",
      dataIndex: "requiresAnimalTest",
      key: "requiresAnimalTest",
      width: 104,
      render: (value: boolean) => value ? <Tag color="orange">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 120,
      fixed: "right",
      render: (_, project) => (
        <Button
          size="small"
          disabled={!roles.includes("TECHNICAL_LEADER")}
          loading={action?.id === project.id && action.action === "create"}
          onClick={() => openCreate(project)}
        >
          创建方案
        </Button>
      ),
    },
  ], [action, roles]);

  const planColumns = useMemo<TableColumnsType<StudyPlan>>(() => [
    {
      title: "方案编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "项目编号",
      dataIndex: "projectId",
      key: "projectId",
      width: 150,
      render: (projectId: string) => planProjectBizNo[projectId] ?? projectId,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 160,
      render: (status: StudyPlan["status"]) => <Tag color={planStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "置样",
      dataIndex: "requiresConditioning",
      key: "requiresConditioning",
      width: 86,
      render: (value: boolean) => value ? <Tag color="orange">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "GLP",
      dataIndex: "requiresGlp",
      key: "requiresGlp",
      width: 86,
      render: (value: boolean) => value ? <Tag color="green">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "开始",
      dataIndex: "plannedStartAt",
      key: "plannedStartAt",
      width: 180,
      render: formatDateTime,
    },
    {
      title: "完成",
      dataIndex: "plannedFinishAt",
      key: "plannedFinishAt",
      width: 180,
      render: formatDateTime,
    },
    {
      title: "操作",
      key: "action",
      width: 190,
      fixed: "right",
      render: (_, plan) => (
        <Space size={8} wrap>
          {canReview(plan, roles) ? (
            <Button
              size="small"
              loading={action?.id === plan.id && action.action === "review"}
              onClick={() => void reviewPlan(plan)}
            >
              SD审批
            </Button>
          ) : null}
          {canDispatch(plan, roles) ? (
            <Button
              size="small"
              type="primary"
              loading={action?.id === plan.id && action.action === "dispatch"}
              onClick={() => void dispatchPlan(plan)}
            >
              下发
            </Button>
          ) : null}
        </Space>
      ),
    },
  ], [action, planProjectBizNo, roles]);

  const workOrderColumns = useMemo<TableColumnsType<WorkOrder>>(() => [
    {
      title: "工单编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "项目编号",
      dataIndex: "projectId",
      key: "projectId",
      width: 150,
      render: (projectId: string) => planProjectBizNo[projectId] ?? projectId,
    },
    {
      title: "样品 ID",
      dataIndex: "sampleId",
      key: "sampleId",
      ellipsis: true,
    },
    {
      title: "负责人",
      dataIndex: "ownerUserId",
      key: "ownerUserId",
      width: 240,
      render: (value?: string) => value ?? "-",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 150,
      render: (status: WorkOrder["status"]) => <Tag color={workOrderStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "进度",
      dataIndex: "progressPercent",
      key: "progressPercent",
      width: 90,
      render: (value: number) => `${value}%`,
    },
  ], [planProjectBizNo]);

  return (
    <section className="studyPlanPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">STUDY PLAN DISPATCH</Typography.Text>
          <Typography.Title level={3}>方案编制</Typography.Title>
          <Typography.Text>方案审批、下发和工单生成。</Typography.Text>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>待编制项目</Typography.Title>
          <Tag color="blue">{eligibleProjects.length} 项</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={projectColumns}
          dataSource={eligibleProjects}
          scroll={{ x: 760 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无待编制项目" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>试验方案</Typography.Title>
          <Tag color="blue">{plans.length} 份</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={planColumns}
          dataSource={plans}
          scroll={{ x: 1100 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无方案" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>试验工单</Typography.Title>
          <Tag color="blue">{workOrders.length} 条</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={workOrderColumns}
          dataSource={workOrders}
          scroll={{ x: 980 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无工单" }}
        />
      </section>

      <Modal
        title="创建方案"
        open={Boolean(createTarget)}
        onCancel={() => setCreateTarget(undefined)}
        onOk={() => void createPlan()}
        confirmLoading={action?.id === createTarget?.id && action?.action === "create"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="项目编号">
            <Input value={createTarget?.bizNo ?? ""} disabled />
          </Form.Item>
          <Form.Item name="requiresConditioning" label="需要置样" valuePropName="checked">
            <Switch checkedChildren="YES" unCheckedChildren="NO" />
          </Form.Item>
          <Form.Item name="plannedStartAt" label="计划开始">
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item name="plannedFinishAt" label="计划完成">
            <Input type="datetime-local" />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function canReview(plan: StudyPlan, roles: readonly string[]) {
  return roles.includes("SD") && ["DRAFT", "REVIEWING"].includes(plan.status);
}

function canDispatch(plan: StudyPlan, roles: readonly string[]) {
  return roles.includes("SD") && ["READY_TO_DISPATCH", "PENDING_DISPATCH"].includes(plan.status);
}

function toIsoOrUndefined(value?: string) {
  return value ? new Date(value).toISOString() : undefined;
}

function daysFromNow(days: number) {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
}

function toLocalDateTimeInputValue(value: Date) {
  const offsetMs = value.getTimezoneOffset() * 60 * 1000;
  return new Date(value.getTime() - offsetMs).toISOString().slice(0, 16);
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function planStatusColor(status: StudyPlan["status"]) {
  if (status === "DISPATCHED" || status === "COMPLETED") {
    return "green";
  }
  if (status === "READY_TO_DISPATCH") {
    return "blue";
  }
  if (status === "PENDING_DISPATCH") {
    return "orange";
  }
  return "default";
}

function workOrderStatusColor(status: WorkOrder["status"]) {
  if (status === "COMPLETED") {
    return "green";
  }
  if (status === "IN_PROGRESS" || status === "RESULT_ENTERED" || status === "RAW_REVIEWING") {
    return "blue";
  }
  if (status === "PAUSED") {
    return "orange";
  }
  return "default";
}

function projectStatusColor(status: Project["status"]) {
  if (status === "SAMPLE_RECEIVED" || status === "PLAN_READY" || status === "IN_TESTING") {
    return "green";
  }
  if (status === "CONTRACTED") {
    return "blue";
  }
  return "default";
}
