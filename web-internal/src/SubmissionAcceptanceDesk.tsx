import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Form,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  acceptSubmission,
  listSubmissions,
  splitSubmissionProjects,
  type Project,
  type Submission,
} from "@lims4wuxi/shared";

type LoadingAction = {
  submissionId: string;
  action: "accept" | "split";
};

type SplitFormValues = {
  projectType: Project["projectType"];
  qualificationType: NonNullable<Project["qualificationType"]>;
  serviceType: "NORMAL" | "URGENT";
  requiresAnimalTest: boolean;
  urgentExtraFeePercent?: number;
  urgentTatDays?: number;
};

export function SubmissionAcceptanceDesk() {
  const [splitForm] = Form.useForm<SplitFormValues>();
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<LoadingAction>();
  const [error, setError] = useState<string>();
  const [acceptedSubmission, setAcceptedSubmission] = useState<Submission>();
  const [splitTarget, setSplitTarget] = useState<Submission>();
  const [createdProjects, setCreatedProjects] = useState<Project[]>([]);

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listSubmissions({ page: 1, pageSize: 50 });
      setSubmissions(page.items ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setError("待受理送检单加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function accept(record: Submission) {
    setAction({ submissionId: record.id, action: "accept" });
    setError(undefined);
    setAcceptedSubmission(undefined);
    setCreatedProjects([]);
    try {
      const accepted = await acceptSubmission(record.id);
      setAcceptedSubmission(accepted);
      await load();
    } catch {
      setError("受理失败，请确认送检单状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  function openSplit(record: Submission) {
    setError(undefined);
    setAcceptedSubmission(undefined);
    setCreatedProjects([]);
    splitForm.setFieldsValue({
      projectType: "STD_ACCREDITED",
      qualificationType: "CMA",
      serviceType: "NORMAL",
      requiresAnimalTest: false,
    });
    setSplitTarget(record);
  }

  async function splitProject() {
    if (!splitTarget) {
      return;
    }
    setAction({ submissionId: splitTarget.id, action: "split" });
    setError(undefined);
    try {
      const values = await splitForm.validateFields();
      const projects = await splitSubmissionProjects(splitTarget.id, {
        splitStrategy: "MANUAL",
        projectDrafts: [values],
      });
      setCreatedProjects(projects);
      setSplitTarget(undefined);
      await load();
    } catch {
      setError("项目拆分失败，请确认送检单已受理且当前账号有项目类型判断权限");
    } finally {
      setAction(undefined);
    }
  }

  const columns = useMemo<TableColumnsType<Submission>>(() => [
    {
      title: "送检单号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 160,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: Submission["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    {
      title: "GLP",
      dataIndex: "requiresGlp",
      key: "requiresGlp",
      width: 92,
      render: (value: boolean) => value ? <Tag color="green">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "报告语言",
      dataIndex: "reportLanguage",
      key: "reportLanguage",
      width: 110,
    },
    {
      title: "协议语言",
      dataIndex: "protocolLanguage",
      key: "protocolLanguage",
      width: 110,
    },
    {
      title: "提交数量",
      dataIndex: "submittedQuantityText",
      key: "submittedQuantityText",
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 136,
      fixed: "right",
      render: (_, record) => {
        if (record.status === "ACCEPTED") {
          return (
            <Button
              type="primary"
              size="small"
              loading={action?.submissionId === record.id && action.action === "split"}
              onClick={() => openSplit(record)}
            >
              拆分项目
            </Button>
          );
        }
        return (
          <Button
            type="primary"
            size="small"
            disabled={record.status !== "SUBMITTED"}
            loading={action?.submissionId === record.id && action.action === "accept"}
            onClick={() => accept(record)}
          >
            受理
          </Button>
        );
      },
    },
  ], [action]);

  return (
    <section className="submissionDesk">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SUBMISSION ACCEPTANCE</Typography.Text>
          <Typography.Title level={3}>送检受理</Typography.Title>
          <Typography.Text type="secondary">
            查看客户已提交的送检单，完成内部受理并进入项目拆分准备。
          </Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{total} 待处理</Tag>
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      <Space direction="vertical" size={12} className="deskFeedback">
        {error ? <Alert type="error" showIcon message={error} /> : null}
        {acceptedSubmission ? (
          <Alert type="success" showIcon message={`送检单 ${acceptedSubmission.bizNo} 已受理`} />
        ) : null}
        {createdProjects.length ? (
          <Alert
            type="success"
            showIcon
            message={`已拆分项目：${createdProjects.map((project) => project.bizNo).join("、")}`}
          />
        ) : null}
      </Space>

      <Card className="workCenterPanel">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={submissions}
          loading={loading}
          pagination={false}
          scroll={{ x: 960 }}
          locale={{ emptyText: "暂无待受理或待拆分送检单" }}
        />
      </Card>

      <Modal
        title="拆分项目"
        open={Boolean(splitTarget)}
        okText="确认拆分"
        cancelText="取消"
        confirmLoading={action?.submissionId === splitTarget?.id && action?.action === "split"}
        onCancel={() => setSplitTarget(undefined)}
        onOk={splitProject}
        destroyOnClose
      >
        <Form form={splitForm} layout="vertical" preserve={false}>
          <Form.Item label="送检单号">
            <Typography.Text>{splitTarget?.bizNo ?? "-"}</Typography.Text>
          </Form.Item>
          <Form.Item name="projectType" label="项目类型" rules={[{ required: true }]}>
            <Select
              options={[
                { value: "STD_ACCREDITED", label: "标准方法 + 有资质" },
                { value: "STD_NON_ACCREDITED", label: "标准方法 + 无资质" },
                { value: "NON_STANDARD", label: "非标方法" },
              ]}
            />
          </Form.Item>
          <Form.Item name="qualificationType" label="资质类型" rules={[{ required: true }]}>
            <Select
              options={[
                { value: "CMA", label: "CMA" },
                { value: "CNAS", label: "CNAS" },
                { value: "CMA_CNAS", label: "CMA + CNAS" },
                { value: "NONE", label: "无" },
              ]}
            />
          </Form.Item>
          <Form.Item name="serviceType" label="服务类型" rules={[{ required: true }]}>
            <Select
              options={[
                { value: "NORMAL", label: "常规" },
                { value: "URGENT", label: "加急" },
              ]}
            />
          </Form.Item>
          <Space size={16} wrap>
            <Form.Item name="requiresAnimalTest" label="动物试验" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="urgentTatDays" label="加急 TAT 天数">
              <InputNumber min={1} precision={0} />
            </Form.Item>
            <Form.Item name="urgentExtraFeePercent" label="加急费 %">
              <InputNumber min={0} max={1000} precision={2} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </section>
  );
}

function statusColor(status: Submission["status"]) {
  if (status === "SUBMITTED") {
    return "gold";
  }
  if (status === "ACCEPTED") {
    return "green";
  }
  if (status === "PROJECT_SPLIT") {
    return "purple";
  }
  if (status === "CANCELLED") {
    return "red";
  }
  return "blue";
}
