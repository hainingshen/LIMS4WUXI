import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  approveGreenChannelRequest,
  createGreenChannelRequest,
  listGreenChannelRequests,
  listProjects,
  listReports,
  useAuthStore,
  type ApprovalRequest,
  type GreenChannelRequest,
  type Project,
  type Report,
} from "@lims4wuxi/shared";

type GreenChannelAction = {
  id: string;
  action: "create" | "dept-approve" | "dept-reject" | "management-approve" | "management-reject";
};

type GreenChannelFormValues = {
  reason: string;
};

export function GreenChannelPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<GreenChannelFormValues>();
  const [reports, setReports] = useState<Report[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [requests, setRequests] = useState<GreenChannelRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<GreenChannelAction>();
  const [createTarget, setCreateTarget] = useState<Report>();
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [reportPage, projectPage, requestPage] = await Promise.all([
        listReports({ page: 1, pageSize: 100 }),
        listProjects({ page: 1, pageSize: 100 }),
        listGreenChannelRequests({ page: 1, pageSize: 100 }),
      ]);
      setReports(reportPage.items ?? []);
      setProjects(projectPage.items ?? []);
      setRequests(requestPage.items ?? []);
    } catch {
      setError("绿色通道、报告或项目列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const projectById = useMemo(() => new Map(projects.map((project) => [project.id, project])), [projects]);
  const reportById = useMemo(() => new Map(reports.map((report) => [report.id, report])), [reports]);
  const activeRequestReportIds = useMemo(() => {
    return new Set(
      requests
        .filter((request) => !["REJECTED", "CLOSED"].includes(request.status))
        .map((request) => request.reportId),
    );
  }, [requests]);
  const candidateReports = useMemo(() => {
    return reports.filter((report) => (
      report.status === "ISSUED_PAYMENT_LOCKED"
      && report.paymentGateStatus === "LOCKED"
      && !activeRequestReportIds.has(report.id)
    ));
  }, [activeRequestReportIds, reports]);

  function openCreate(report: Report) {
    setError(undefined);
    setFeedback(undefined);
    form.setFieldsValue({
      reason: "客户需在付款结算前获取电子正式报告",
    });
    setCreateTarget(report);
  }

  async function createRequest() {
    if (!createTarget) {
      return;
    }
    setAction({ id: createTarget.id, action: "create" });
    setError(undefined);
    try {
      const values = await form.validateFields();
      const request = await createGreenChannelRequest({
        projectId: createTarget.projectId,
        reportId: createTarget.id,
        reason: values.reason,
      });
      setFeedback(`绿色通道 ${request.bizNo} 已提交部门审批`);
      setCreateTarget(undefined);
      await load();
    } catch {
      setError("绿色通道申请失败，请确认报告仍处于付款锁定且当前账号有权限");
    } finally {
      setAction(undefined);
    }
  }

  async function approve(request: GreenChannelRequest, stage: ApprovalRequest["stage"], outcome: ApprovalRequest["outcome"]) {
    const nextAction = actionName(stage, outcome);
    setAction({ id: request.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      await approveGreenChannelRequest(request.id, {
        stage,
        outcome,
        comment: outcome === "APPROVED" ? "同意绿色通道" : "驳回绿色通道",
      });
      setFeedback(`绿色通道 ${request.bizNo} 已${outcome === "APPROVED" ? "通过" : "驳回"}`);
      await load();
    } catch {
      setError("绿色通道审批失败，请确认审批阶段、状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  const reportColumns = useMemo<TableColumnsType<Report>>(() => [
    {
      title: "报告编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 170,
      fixed: "left",
    },
    {
      title: "项目编号",
      key: "project",
      width: 170,
      render: (_, report) => projectById.get(report.projectId)?.bizNo ?? shortId(report.projectId),
    },
    {
      title: "报告状态",
      dataIndex: "status",
      key: "status",
      width: 170,
      render: (status: Report["status"]) => <Tag color={reportStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "门禁",
      dataIndex: "paymentGateStatus",
      key: "paymentGateStatus",
      width: 150,
      render: (status: Report["paymentGateStatus"]) => <Tag color={gateStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "项目付款",
      key: "paymentStatus",
      width: 130,
      render: (_, report) => projectById.get(report.projectId)?.paymentStatus ?? "UNPAID",
    },
    {
      title: "操作",
      key: "action",
      width: 130,
      fixed: "right",
      render: (_, report) => (
        <Button
          size="small"
          type="primary"
          disabled={!canCreate(roles)}
          loading={isLoading(action, report.id, "create")}
          onClick={() => openCreate(report)}
        >
          申请
        </Button>
      ),
    },
  ], [action, projectById, roles]);

  const requestColumns = useMemo<TableColumnsType<GreenChannelRequest>>(() => [
    {
      title: "申请编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 160,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 180,
      render: (status: GreenChannelRequest["status"]) => <Tag color={requestStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "报告",
      key: "report",
      width: 170,
      render: (_, request) => reportById.get(request.reportId)?.bizNo ?? shortId(request.reportId),
    },
    {
      title: "项目",
      key: "project",
      width: 170,
      render: (_, request) => projectById.get(request.projectId)?.bizNo ?? shortId(request.projectId),
    },
    {
      title: "范围",
      dataIndex: "unlockScope",
      key: "unlockScope",
      width: 180,
    },
    {
      title: "原因",
      dataIndex: "reason",
      key: "reason",
      width: 260,
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 280,
      fixed: "right",
      render: (_, request) => (
        <Space size={8} wrap>
          {canDeptApprove(roles) && request.status === "DEPT_REVIEWING" ? (
            <>
              <Button
                size="small"
                loading={isLoading(action, request.id, "dept-approve")}
                onClick={() => void approve(request, "DEPT", "APPROVED")}
              >
                部门通过
              </Button>
              <Button
                size="small"
                danger
                loading={isLoading(action, request.id, "dept-reject")}
                onClick={() => void approve(request, "DEPT", "REJECTED")}
              >
                部门驳回
              </Button>
            </>
          ) : null}
          {canManagementApprove(roles) && request.status === "MANAGEMENT_REVIEWING" ? (
            <>
              <Button
                size="small"
                loading={isLoading(action, request.id, "management-approve")}
                onClick={() => void approve(request, "MANAGEMENT", "APPROVED")}
              >
                管理通过
              </Button>
              <Button
                size="small"
                danger
                loading={isLoading(action, request.id, "management-reject")}
                onClick={() => void approve(request, "MANAGEMENT", "REJECTED")}
              >
                管理驳回
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ], [action, projectById, reportById, roles]);

  return (
    <section className="greenChannelPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">GREEN CHANNEL</Typography.Text>
          <Typography.Title level={3}>绿色通道</Typography.Title>
          <Typography.Text>未到账电子正式报告的临时解锁申请和两级审批。</Typography.Text>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>可申请报告</Typography.Title>
          <Tag color="blue">{candidateReports.length} 份</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={reportColumns}
          dataSource={candidateReports}
          scroll={{ x: 920 }}
          pagination={{ pageSize: 6 }}
          locale={{ emptyText: "暂无付款锁定报告" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>绿色通道申请</Typography.Title>
          <Tag color="blue">{requests.length} 条</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={requestColumns}
          dataSource={requests}
          scroll={{ x: 1220 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无绿色通道申请" }}
        />
      </section>

      <Modal
        title="发起绿色通道"
        open={Boolean(createTarget)}
        onCancel={() => setCreateTarget(undefined)}
        onOk={() => void createRequest()}
        confirmLoading={action?.id === createTarget?.id && action?.action === "create"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="报告编号">
            <Input value={createTarget?.bizNo ?? ""} disabled />
          </Form.Item>
          <Form.Item name="reason" label="申请原因" rules={[{ required: true }]}>
            <Input.TextArea rows={4} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function actionName(stage: ApprovalRequest["stage"], outcome: ApprovalRequest["outcome"]): GreenChannelAction["action"] {
  if (stage === "DEPT") {
    return outcome === "APPROVED" ? "dept-approve" : "dept-reject";
  }
  return outcome === "APPROVED" ? "management-approve" : "management-reject";
}

function canCreate(roles: readonly string[]) {
  return roles.includes("SALES_REP");
}

function canDeptApprove(roles: readonly string[]) {
  return roles.includes("DEPT_MANAGER");
}

function canManagementApprove(roles: readonly string[]) {
  return roles.includes("GENERAL_MANAGER");
}

function isLoading(action: GreenChannelAction | undefined, id: string, name: GreenChannelAction["action"]) {
  return action?.id === id && action.action === name;
}

function shortId(id: string) {
  return id.slice(0, 8);
}

function reportStatusColor(status: Report["status"]) {
  if (status === "ISSUED_PAYMENT_LOCKED") {
    return "orange";
  }
  if (status === "GREEN_RELEASED") {
    return "cyan";
  }
  if (status === "RELEASED") {
    return "green";
  }
  return "default";
}

function gateStatusColor(status: Report["paymentGateStatus"]) {
  if (status === "LOCKED") {
    return "red";
  }
  if (status === "GREEN_UNLOCKED") {
    return "cyan";
  }
  return "green";
}

function requestStatusColor(status: GreenChannelRequest["status"]) {
  if (status === "APPROVED" || status === "UNLOCKED") {
    return "green";
  }
  if (status === "REJECTED" || status === "CLOSED") {
    return "red";
  }
  return "orange";
}
