import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createPaymentReceipt,
  generateDraftReport,
  issueReport,
  listPaymentReceipts,
  listProjects,
  listReportTemplates,
  listReports,
  listWorkOrders,
  reviewReport,
  submitReportQc,
  useAuthStore,
  type PaymentReceipt,
  type PaymentReceiptCreateRequest,
  type Project,
  type Report,
  type ReportTemplate,
  type WorkOrder,
} from "@lims4wuxi/shared";

type ReportAction = {
  id: string;
  action: "generate" | "submit-qc" | "review" | "issue" | "pay";
};

type PaymentFormValues = {
  amount: number;
  receivedAt: string;
  method: PaymentReceiptCreateRequest["method"];
  note?: string;
};

const TEMPLATE_CODE = "STERILITY_REPORT";

export function ReportWorkbenchPage() {
  const roles = useAuthStore((state) => state.roles);
  const [paymentForm] = Form.useForm<PaymentFormValues>();
  const [projects, setProjects] = useState<Project[]>([]);
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [templates, setTemplates] = useState<ReportTemplate[]>([]);
  const [payments, setPayments] = useState<PaymentReceipt[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<ReportAction>();
  const [paymentTarget, setPaymentTarget] = useState<Report>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [projectPage, workOrderPage, reportPage, templatePage, paymentPage] = await Promise.all([
        listProjects({ page: 1, pageSize: 100 }),
        listWorkOrders({ page: 1, pageSize: 100 }),
        listReports({ page: 1, pageSize: 100 }),
        listReportTemplates({ page: 1, pageSize: 100 }),
        listPaymentReceipts({ page: 1, pageSize: 100 }),
      ]);
      setProjects(projectPage.items ?? []);
      setWorkOrders(workOrderPage.items ?? []);
      setReports(reportPage.items ?? []);
      setTemplates(templatePage.items ?? []);
      setPayments(paymentPage.items ?? []);
    } catch {
      setError("报告、项目或收款列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const reportsByProject = useMemo(() => {
    return new Map(reports.map((report) => [report.projectId, report]));
  }, [reports]);
  const projectBizNo = useMemo(() => {
    return Object.fromEntries(projects.map((project) => [project.id, project.bizNo]));
  }, [projects]);
  const completedWorkOrdersByProject = useMemo(() => {
    const grouped = new Map<string, WorkOrder[]>();
    for (const workOrder of workOrders) {
      if (workOrder.status !== "COMPLETED") {
        continue;
      }
      grouped.set(workOrder.projectId, [...(grouped.get(workOrder.projectId) ?? []), workOrder]);
    }
    return grouped;
  }, [workOrders]);
  const draftCandidates = useMemo(() => {
    return projects.filter((project) => {
      return project.status === "REPORTING"
        && !reportsByProject.has(project.id)
        && (completedWorkOrdersByProject.get(project.id)?.length ?? 0) > 0;
    });
  }, [completedWorkOrdersByProject, projects, reportsByProject]);
  const publishedTemplate = templates.find(
    (template) => template.templateCode === TEMPLATE_CODE && template.status === "PUBLISHED",
  );

  async function generate(project: Project) {
    const projectWorkOrders = completedWorkOrdersByProject.get(project.id) ?? [];
    setAction({ id: project.id, action: "generate" });
    setError(undefined);
    setFeedback(undefined);
    try {
      const report = await generateDraftReport({
        projectId: project.id,
        templateCode: TEMPLATE_CODE,
        workOrderIds: projectWorkOrders.map((workOrder) => workOrder.id),
      });
      setFeedback(`报告 ${report.bizNo} 已生成草稿`);
      await load();
    } catch {
      setError("报告草稿生成失败，请确认工单已完成且试点模板已发布");
    } finally {
      setAction(undefined);
    }
  }

  async function runReportAction(
    report: Report,
    nextAction: ReportAction["action"],
    execute: () => Promise<Report>,
  ) {
    setAction({ id: report.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = await execute();
      setFeedback(`报告 ${updated.bizNo} 已更新为 ${updated.status}`);
      await load();
    } catch {
      setError("报告动作执行失败，请确认报告状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  async function review(report: Report, stage: "QC" | "QA" | "SD") {
    await runReportAction(report, "review", () => reviewReport(report.id, {
      stage,
      outcome: "APPROVED",
      comment: `${stage} approved`,
    }));
  }

  function openPayment(report: Report) {
    setError(undefined);
    setFeedback(undefined);
    paymentForm.setFieldsValue({
      amount: 1200,
      receivedAt: new Date().toISOString().slice(0, 16),
      method: "BANK_TRANSFER",
      note: "报告付款",
    });
    setPaymentTarget(report);
  }

  async function recordPayment() {
    if (!paymentTarget) {
      return;
    }
    setAction({ id: paymentTarget.id, action: "pay" });
    setError(undefined);
    try {
      const values = await paymentForm.validateFields();
      const payment = await createPaymentReceipt({
        projectId: paymentTarget.projectId,
        reportId: paymentTarget.id,
        amount: values.amount,
        receivedAt: new Date(values.receivedAt).toISOString(),
        method: values.method,
        note: values.note,
      });
      setFeedback(`收款 ${payment.bizNo} 已登记`);
      setPaymentTarget(undefined);
      await load();
    } catch {
      setError("收款登记失败，请确认当前账号有财务收款权限");
    } finally {
      setAction(undefined);
    }
  }

  const candidateColumns = useMemo<TableColumnsType<Project>>(() => [
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
      width: 130,
      render: (status: Project["status"]) => <Tag color={status === "REPORTING" ? "blue" : "default"}>{status}</Tag>,
    },
    {
      title: "完成工单",
      key: "workOrders",
      width: 100,
      render: (_, project) => completedWorkOrdersByProject.get(project.id)?.length ?? 0,
    },
    {
      title: "付款状态",
      dataIndex: "paymentStatus",
      key: "paymentStatus",
      width: 120,
      render: (value: Project["paymentStatus"]) => value ?? "-",
    },
    {
      title: "操作",
      key: "action",
      width: 130,
      fixed: "right",
      render: (_, project) => (
        <Button
          size="small"
          type="primary"
          disabled={!canGenerate(roles) || !publishedTemplate}
          loading={action?.id === project.id && action.action === "generate"}
          onClick={() => void generate(project)}
        >
          生成草稿
        </Button>
      ),
    },
  ], [action, completedWorkOrdersByProject, publishedTemplate, roles]);

  const reportColumns = useMemo<TableColumnsType<Report>>(() => [
    {
      title: "报告编号",
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
      render: (projectId: string) => projectBizNo[projectId] ?? projectId,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 180,
      render: (status: Report["status"]) => <Tag color={reportStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "付款门禁",
      dataIndex: "paymentGateStatus",
      key: "paymentGateStatus",
      width: 120,
      render: (status: Report["paymentGateStatus"]) => <Tag color={status === "UNLOCKED" ? "green" : "orange"}>{status}</Tag>,
    },
    {
      title: "模板",
      dataIndex: "templateCode",
      key: "templateCode",
      width: 170,
    },
    {
      title: "版本",
      dataIndex: "templateVersion",
      key: "templateVersion",
      width: 100,
      render: (value?: string) => value ?? "-",
    },
    {
      title: "操作",
      key: "action",
      width: 360,
      fixed: "right",
      render: (_, report) => (
        <Space size={8} wrap>
          {canSubmitQc(report, roles) ? (
            <Button
              size="small"
              loading={isLoading(action, report.id, "submit-qc")}
              onClick={() => void runReportAction(report, "submit-qc", () => submitReportQc(report.id))}
            >
              提交QC
            </Button>
          ) : null}
          {canReviewStage(report, roles, "QC") ? (
            <Button size="small" loading={isLoading(action, report.id, "review")} onClick={() => void review(report, "QC")}>QC通过</Button>
          ) : null}
          {canReviewStage(report, roles, "QA") ? (
            <Button size="small" loading={isLoading(action, report.id, "review")} onClick={() => void review(report, "QA")}>QA通过</Button>
          ) : null}
          {canReviewStage(report, roles, "SD") ? (
            <Button size="small" loading={isLoading(action, report.id, "review")} onClick={() => void review(report, "SD")}>SD通过</Button>
          ) : null}
          {canIssue(report, roles) ? (
            <Button
              size="small"
              type="primary"
              loading={isLoading(action, report.id, "issue")}
              onClick={() => void runReportAction(report, "issue", () => issueReport(report.id))}
            >
              签发
            </Button>
          ) : null}
          {canPay(report, roles) ? (
            <Button
              size="small"
              type="primary"
              loading={isLoading(action, report.id, "pay")}
              onClick={() => openPayment(report)}
            >
              登记收款
            </Button>
          ) : null}
        </Space>
      ),
    },
  ], [action, projectBizNo, roles]);

  const paymentColumns = useMemo<TableColumnsType<PaymentReceipt>>(() => [
    {
      title: "收款编号",
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
      render: (projectId: string) => projectBizNo[projectId] ?? projectId,
    },
    {
      title: "金额",
      dataIndex: "amount",
      key: "amount",
      width: 100,
      render: (value: number) => value.toFixed(2),
    },
    {
      title: "方式",
      dataIndex: "method",
      key: "method",
      width: 140,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: PaymentReceipt["status"]) => <Tag color={status === "RECORDED" ? "green" : "default"}>{status}</Tag>,
    },
  ], [projectBizNo]);

  return (
    <section className="reportWorkbenchPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">REPORT WORKBENCH</Typography.Text>
          <Typography.Title level={3}>报告管理</Typography.Title>
          <Typography.Text>报告草稿、审核、签发和收款解锁。</Typography.Text>
        </div>
        <Space wrap>
          <Tag color={publishedTemplate ? "green" : "orange"}>{publishedTemplate ? "模板已发布" : "缺少模板"}</Tag>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>待生成草稿</Typography.Title>
          <Tag color="blue">{draftCandidates.length} 项</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={candidateColumns}
          dataSource={draftCandidates}
          scroll={{ x: 700 }}
          pagination={{ pageSize: 6 }}
          locale={{ emptyText: "暂无待生成项目" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>报告列表</Typography.Title>
          <Tag color="blue">{reports.length} 份</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={reportColumns}
          dataSource={reports}
          scroll={{ x: 1240 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无报告" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>收款记录</Typography.Title>
          <Tag color="blue">{payments.length} 条</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={paymentColumns}
          dataSource={payments}
          scroll={{ x: 720 }}
          pagination={{ pageSize: 6 }}
          locale={{ emptyText: "暂无收款记录" }}
        />
      </section>

      <Modal
        title="登记收款"
        open={Boolean(paymentTarget)}
        onCancel={() => setPaymentTarget(undefined)}
        onOk={() => void recordPayment()}
        confirmLoading={action?.id === paymentTarget?.id && action?.action === "pay"}
        destroyOnHidden
      >
        <Form form={paymentForm} layout="vertical">
          <Form.Item label="报告编号">
            <Input value={paymentTarget?.bizNo ?? ""} disabled />
          </Form.Item>
          <Form.Item name="amount" label="金额" rules={[{ required: true }]}>
            <InputNumber min={0.01} precision={2} className="fullWidthInput" />
          </Form.Item>
          <Form.Item name="receivedAt" label="到账时间" rules={[{ required: true }]}>
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item name="method" label="方式" rules={[{ required: true }]}>
            <Select
              options={[
                { label: "银行转账", value: "BANK_TRANSFER" },
                { label: "现金", value: "CASH" },
                { label: "其他", value: "OTHER" },
              ]}
            />
          </Form.Item>
          <Form.Item name="note" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function canGenerate(roles: readonly string[]) {
  return roles.includes("REPORT_PREPARER");
}

function canSubmitQc(report: Report, roles: readonly string[]) {
  return roles.includes("REPORT_PREPARER") && report.status === "DRAFT";
}

function canReviewStage(report: Report, roles: readonly string[], stage: "QC" | "QA" | "SD") {
  if (stage === "QC") {
    return roles.includes("QC") && report.status === "QC_REVIEWING";
  }
  if (stage === "QA") {
    return roles.includes("QA") && report.status === "QA_REVIEWING";
  }
  return roles.includes("SD") && report.status === "SD_REVIEWING";
}

function canIssue(report: Report, roles: readonly string[]) {
  return roles.includes("AUTHORIZED_SIGNATORY") && report.status === "PENDING_ISSUE";
}

function canPay(report: Report, roles: readonly string[]) {
  return roles.includes("FINANCE_CASHIER") && report.status === "ISSUED_PAYMENT_LOCKED";
}

function isLoading(action: ReportAction | undefined, id: string, name: ReportAction["action"]) {
  return action?.id === id && action.action === name;
}

function reportStatusColor(status: Report["status"]) {
  if (status === "RELEASED" || status === "GREEN_RELEASED") {
    return "green";
  }
  if (status === "ISSUED_PAYMENT_LOCKED" || status === "PENDING_ISSUE") {
    return "orange";
  }
  if (status.endsWith("_REVIEWING")) {
    return "blue";
  }
  return "default";
}
