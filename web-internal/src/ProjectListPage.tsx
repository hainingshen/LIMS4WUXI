import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  approveNonstandardReview,
  createContract,
  createProjectQuotation,
  listProjects,
  startNonstandardReview,
  useAuthStore,
  type Project,
  type Quotation,
} from "@lims4wuxi/shared";

type ProjectAction = {
  projectId: string;
  action: "start-review" | "tech-approve" | "business-approve" | "quote" | "contract";
};

type QuotationFormValues = {
  testName: string;
  unitPrice: number;
  quantity: number;
  discountAmount?: number;
  note?: string;
};

export function ProjectListPage() {
  const roles = useAuthStore((state) => state.roles);
  const [quotationForm] = Form.useForm<QuotationFormValues>();
  const [projects, setProjects] = useState<Project[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<ProjectAction>();
  const [quoteTarget, setQuoteTarget] = useState<Project>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listProjects({ page: 1, pageSize: 50 });
      setProjects(page.items ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setError("项目列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function runProjectAction(project: Project, nextAction: ProjectAction["action"]) {
    setAction({ projectId: project.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = await executeProjectAction(project.id, nextAction);
      setFeedback(`项目 ${updated.bizNo} 已更新为 ${updated.status}`);
      await load();
    } catch {
      setError("项目动作执行失败，请确认项目状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  function openQuotation(project: Project) {
    setError(undefined);
    setFeedback(undefined);
    quotationForm.setFieldsValue({
      testName: "检测服务",
      unitPrice: 100,
      quantity: 1,
      discountAmount: 0,
    });
    setQuoteTarget(project);
  }

  async function createQuotation() {
    if (!quoteTarget) {
      return;
    }
    setAction({ projectId: quoteTarget.id, action: "quote" });
    setError(undefined);
    try {
      const values = await quotationForm.validateFields();
      const quotation = await createProjectQuotation(quoteTarget.id, {
        items: [
          {
            testName: values.testName,
            unitPrice: values.unitPrice,
            quantity: values.quantity,
          },
        ],
        discountAmount: values.discountAmount ?? 0,
        note: values.note,
      });
      setFeedback(`报价 ${quotation.bizNo} 已创建，总额 ${formatAmount(quotation)}`);
      setQuoteTarget(undefined);
      await load();
    } catch {
      setError("报价创建失败，请确认项目已通过评审并达到可报价状态");
    } finally {
      setAction(undefined);
    }
  }

  async function createContractForProject(project: Project) {
    setAction({ projectId: project.id, action: "contract" });
    setError(undefined);
    setFeedback(undefined);
    try {
      const contract = await createContract({
        projectId: project.id,
        contractType: "TEST_AGREEMENT",
        templateCode: "TEST_AGREEMENT_VER1",
      });
      setFeedback(`合同 ${contract.bizNo} 已创建，等待签署确认`);
      await load();
    } catch {
      setError("合同创建失败，请确认项目已完成报价并达到可签约状态");
    } finally {
      setAction(undefined);
    }
  }

  const columns = useMemo<TableColumnsType<Project>>(() => [
    {
      title: "项目编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "项目状态",
      dataIndex: "status",
      key: "status",
      width: 150,
      render: (status: Project["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    {
      title: "项目类型",
      dataIndex: "projectType",
      key: "projectType",
      width: 170,
      render: (value: Project["projectType"]) => projectTypeLabel(value),
    },
    {
      title: "资质",
      dataIndex: "qualificationType",
      key: "qualificationType",
      width: 100,
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
      title: "付款状态",
      dataIndex: "paymentStatus",
      key: "paymentStatus",
      width: 120,
      render: (value: Project["paymentStatus"]) => value ?? "-",
    },
    {
      title: "送检单 ID",
      dataIndex: "submissionId",
      key: "submissionId",
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 220,
      fixed: "right",
      render: (_, project) => (
        <Space size={8} wrap>
          {canStartReview(project, roles) ? (
            <Button
              size="small"
              loading={isLoading(action, project.id, "start-review")}
              onClick={() => runProjectAction(project, "start-review")}
            >
              发起评审
            </Button>
          ) : null}
          {canApproveTech(project, roles) ? (
            <Button
              size="small"
              loading={isLoading(action, project.id, "tech-approve")}
              onClick={() => runProjectAction(project, "tech-approve")}
            >
              技术通过
            </Button>
          ) : null}
          {canApproveBusiness(project, roles) ? (
            <Button
              size="small"
              loading={isLoading(action, project.id, "business-approve")}
              onClick={() => runProjectAction(project, "business-approve")}
            >
              业务通过
            </Button>
          ) : null}
          {canQuote(project, roles) ? (
            <Button
              type="primary"
              size="small"
              loading={isLoading(action, project.id, "quote")}
              onClick={() => openQuotation(project)}
            >
              创建报价
            </Button>
          ) : null}
          {canCreateContract(project, roles) ? (
            <Button
              type="primary"
              size="small"
              loading={isLoading(action, project.id, "contract")}
              onClick={() => createContractForProject(project)}
            >
              创建合同
            </Button>
          ) : null}
        </Space>
      ),
    },
  ], [action, roles]);

  return (
    <section className="projectListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">PROJECT LIST</Typography.Text>
          <Typography.Title level={3}>项目详情</Typography.Title>
          <Typography.Text type="secondary">
            查看已拆分项目的类型、资质、GLP 和付款状态，后续承接评审、报价、合同、样品与报告流程。
          </Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{total} 项目</Tag>
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      <Space direction="vertical" size={12} className="deskFeedback">
        {error ? <Alert type="error" showIcon message={error} /> : null}
        {feedback ? <Alert type="success" showIcon message={feedback} /> : null}
      </Space>

      <Card className="workCenterPanel">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={projects}
          loading={loading}
          pagination={false}
          scroll={{ x: 1120 }}
          locale={{ emptyText: "暂无项目" }}
        />
      </Card>

      <Modal
        title="创建报价"
        open={Boolean(quoteTarget)}
        okText="确认创建"
        cancelText="取消"
        confirmLoading={action?.projectId === quoteTarget?.id && action?.action === "quote"}
        onCancel={() => setQuoteTarget(undefined)}
        onOk={createQuotation}
        destroyOnClose
      >
        <Form form={quotationForm} layout="vertical" preserve={false}>
          <Form.Item label="项目编号">
            <Typography.Text>{quoteTarget?.bizNo ?? "-"}</Typography.Text>
          </Form.Item>
          <Form.Item name="testName" label="检测项目" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Space size={16} wrap>
            <Form.Item name="unitPrice" label="单价" rules={[{ required: true }]}>
              <InputNumber min={0} precision={2} />
            </Form.Item>
            <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
              <InputNumber min={0.01} precision={2} />
            </Form.Item>
            <Form.Item name="discountAmount" label="折扣金额">
              <InputNumber min={0} precision={2} />
            </Form.Item>
          </Space>
          <Form.Item name="note" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function canStartReview(project: Project, roles: readonly string[]) {
  return roles.includes("SALES_REP")
    && project.status === "ACCEPTED"
    && project.requiresNonstandardReview === true;
}

function canApproveTech(project: Project, roles: readonly string[]) {
  return roles.includes("TECHNICAL_LEADER") && project.status === "NONSTANDARD_REVIEW";
}

function canApproveBusiness(project: Project, roles: readonly string[]) {
  return roles.includes("BUSINESS_OWNER") && project.status === "NONSTANDARD_REVIEW";
}

function canQuote(project: Project, roles: readonly string[]) {
  return roles.includes("SALES_REP") && project.status === "QUOTABLE";
}

function canCreateContract(project: Project, roles: readonly string[]) {
  return roles.includes("SALES_REP") && project.status === "QUOTED";
}

async function executeProjectAction(projectId: string, action: ProjectAction["action"]) {
  if (action === "start-review") {
    return startNonstandardReview(projectId);
  }
  if (action === "tech-approve") {
    return approveNonstandardReview(projectId, {
      stage: "TECH",
      outcome: "APPROVED",
      comment: "技术通过",
    });
  }
  if (action === "business-approve") {
    return approveNonstandardReview(projectId, {
      stage: "BUSINESS",
      outcome: "APPROVED",
      comment: "业务通过",
    });
  }
  throw new Error(`Unsupported project action: ${action}`);
}

function isLoading(action: ProjectAction | undefined, projectId: string, actionName: ProjectAction["action"]) {
  return action?.projectId === projectId && action.action === actionName;
}

function formatAmount(quotation: Quotation) {
  return `${quotation.currency} ${quotation.totalAmount.toFixed(2)}`;
}

function projectTypeLabel(projectType: Project["projectType"]) {
  if (projectType === "STD_ACCREDITED") {
    return "标准方法 + 有资质";
  }
  if (projectType === "STD_NON_ACCREDITED") {
    return "标准方法 + 无资质";
  }
  return "非标方法";
}

function statusColor(status: Project["status"]) {
  if (status === "ACCEPTED" || status === "QUOTABLE") {
    return "green";
  }
  if (status === "NONSTANDARD_REVIEW" || status === "PAYMENT_HOLD") {
    return "gold";
  }
  if (status === "ARCHIVED") {
    return "blue";
  }
  return "geekblue";
}
