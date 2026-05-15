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
  createSampleRetention,
  disposeSampleRetention,
  extendSampleRetention,
  listReports,
  listSampleRetentions,
  listSamples,
  registerSampleRetention,
  useAuthStore,
  type Report,
  type Sample,
  type SampleRetention,
  type SampleRetentionCreateRequest,
} from "@lims4wuxi/shared";

type RetentionAction = {
  id: string;
  action: "create" | "register" | "extend" | "dispose";
};

type RetentionFormValues = {
  retainedQuantity: number;
  quantityUnit: string;
  storageCondition: string;
  storageLocation: string;
  retainUntil: string;
  dispositionPlan: SampleRetentionCreateRequest["dispositionPlan"];
};

export function SampleRetentionPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<RetentionFormValues>();
  const [samples, setSamples] = useState<Sample[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [retentions, setRetentions] = useState<SampleRetention[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<RetentionAction>();
  const [createTarget, setCreateTarget] = useState<Sample>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [samplePage, reportPage, retentionPage] = await Promise.all([
        listSamples({ page: 1, pageSize: 100 }),
        listReports({ page: 1, pageSize: 100 }),
        listSampleRetentions({ page: 1, pageSize: 100 }),
      ]);
      setSamples(samplePage.items ?? []);
      setReports(reportPage.items ?? []);
      setRetentions(retentionPage.items ?? []);
    } catch {
      setError("样品、报告或留样列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const retainedSampleIds = useMemo(() => new Set(retentions.map((retention) => retention.sampleId)), [retentions]);
  const releasedReportByProject = useMemo(() => {
    return new Map(
      reports
        .filter((report) => report.status === "RELEASED")
        .map((report) => [report.projectId, report]),
    );
  }, [reports]);
  const candidateSamples = useMemo(() => {
    return samples.filter((sample) => sample.status === "IN_STOCK" && !retainedSampleIds.has(sample.id));
  }, [retainedSampleIds, samples]);

  function openCreate(sample: Sample) {
    setError(undefined);
    setFeedback(undefined);
    form.setFieldsValue({
      retainedQuantity: sample.quantity ?? 1,
      quantityUnit: sample.quantityUnit ?? "pcs",
      storageCondition: sample.storageCondition ?? "ROOM_TEMPERATURE",
      storageLocation: "RET-A-01",
      retainUntil: nextDate(90),
      dispositionPlan: "DESTROY",
    });
    setCreateTarget(sample);
  }

  async function createRetention() {
    if (!createTarget) {
      return;
    }
    const report = releasedReportByProject.get(createTarget.projectId);
    if (!report) {
      setError("留样登记需要已释放报告");
      return;
    }
    setAction({ id: createTarget.id, action: "create" });
    setError(undefined);
    try {
      const values = await form.validateFields();
      const retention = await createSampleRetention({
        sampleId: createTarget.id,
        projectId: createTarget.projectId,
        reportId: report.id,
        retainedQuantity: values.retainedQuantity,
        quantityUnit: values.quantityUnit,
        storageCondition: values.storageCondition,
        storageLocation: values.storageLocation,
        retainUntil: values.retainUntil,
        dispositionPlan: values.dispositionPlan,
      });
      setFeedback(`留样 ${retention.bizNo} 已创建`);
      setCreateTarget(undefined);
      await load();
    } catch {
      setError("留样创建失败，请确认样品、报告和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  async function register(retention: SampleRetention) {
    await runRetentionAction(retention, "register", async () => registerSampleRetention(retention.id));
  }

  async function extend(retention: SampleRetention) {
    await runRetentionAction(retention, "extend", async () => extendSampleRetention(retention.id, {
      retainUntil: nextDate(150),
      reason: "延期留样",
    }));
  }

  async function dispose(retention: SampleRetention) {
    await runRetentionAction(retention, "dispose", async () => disposeSampleRetention(retention.id, {
      disposition: "DESTROY",
      reason: "留样到期销毁",
    }));
  }

  async function runRetentionAction(
    retention: SampleRetention,
    nextAction: RetentionAction["action"],
    execute: () => Promise<SampleRetention>,
  ) {
    setAction({ id: retention.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = await execute();
      setFeedback(`留样 ${updated.bizNo} 已更新为 ${updated.status}`);
      await load();
    } catch {
      setError("留样动作执行失败，请确认状态、付款门禁和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  const sampleColumns = useMemo<TableColumnsType<Sample>>(() => [
    {
      title: "样品编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "样品名称",
      dataIndex: "sampleNameZh",
      key: "sampleNameZh",
      width: 180,
    },
    {
      title: "批号",
      dataIndex: "lotBatch",
      key: "lotBatch",
      width: 140,
    },
    {
      title: "数量",
      key: "quantity",
      width: 100,
      render: (_, sample) => formatQuantity(sample),
    },
    {
      title: "报告",
      key: "report",
      width: 120,
      render: (_, sample) => releasedReportByProject.has(sample.projectId) ? <Tag color="green">RELEASED</Tag> : <Tag>NONE</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 130,
      fixed: "right",
      render: (_, sample) => (
        <Button
          size="small"
          type="primary"
          disabled={!canCreateOrRegister(roles) || !releasedReportByProject.has(sample.projectId)}
          loading={action?.id === sample.id && action.action === "create"}
          onClick={() => openCreate(sample)}
        >
          创建留样
        </Button>
      ),
    },
  ], [action, releasedReportByProject, roles]);

  const retentionColumns = useMemo<TableColumnsType<SampleRetention>>(() => [
    {
      title: "留样编号",
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
      render: (status: SampleRetention["status"]) => <Tag color={retentionStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "数量",
      key: "quantity",
      width: 110,
      render: (_, retention) => `${retention.retainedQuantity} ${retention.quantityUnit}`.trim(),
    },
    {
      title: "位置",
      dataIndex: "storageLocation",
      key: "storageLocation",
      width: 130,
    },
    {
      title: "留样至",
      dataIndex: "retainUntil",
      key: "retainUntil",
      width: 120,
    },
    {
      title: "处置计划",
      dataIndex: "dispositionPlan",
      key: "dispositionPlan",
      width: 120,
      render: (value?: SampleRetention["dispositionPlan"]) => value ?? "-",
    },
    {
      title: "操作",
      key: "action",
      width: 230,
      fixed: "right",
      render: (_, retention) => (
        <Space size={8} wrap>
          {canCreateOrRegister(roles) && retention.status === "PENDING_REGISTER" ? (
            <Button size="small" loading={isLoading(action, retention.id, "register")} onClick={() => void register(retention)}>登记</Button>
          ) : null}
          {canExtend(roles) && ["RETAINED", "EXPIRING"].includes(retention.status) ? (
            <Button size="small" loading={isLoading(action, retention.id, "extend")} onClick={() => void extend(retention)}>延期</Button>
          ) : null}
          {canDispose(roles) && ["RETAINED", "EXPIRING", "PENDING_DISPOSAL"].includes(retention.status) ? (
            <Button
              size="small"
              type="primary"
              loading={isLoading(action, retention.id, "dispose")}
              onClick={() => void dispose(retention)}
            >
              销毁
            </Button>
          ) : null}
        </Space>
      ),
    },
  ], [action, roles]);

  return (
    <section className="sampleRetentionPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SAMPLE RETENTION</Typography.Text>
          <Typography.Title level={3}>留样管理</Typography.Title>
          <Typography.Text>留样登记、延期和处置。</Typography.Text>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>可留样样品</Typography.Title>
          <Tag color="blue">{candidateSamples.length} 件</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={sampleColumns}
          dataSource={candidateSamples}
          scroll={{ x: 820 }}
          pagination={{ pageSize: 6 }}
          locale={{ emptyText: "暂无可留样样品" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>留样记录</Typography.Title>
          <Tag color="blue">{retentions.length} 条</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={retentionColumns}
          dataSource={retentions}
          scroll={{ x: 980 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无留样记录" }}
        />
      </section>

      <Modal
        title="创建留样"
        open={Boolean(createTarget)}
        onCancel={() => setCreateTarget(undefined)}
        onOk={() => void createRetention()}
        confirmLoading={action?.id === createTarget?.id && action?.action === "create"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="样品编号">
            <Input value={createTarget?.bizNo ?? ""} disabled />
          </Form.Item>
          <Form.Item name="retainedQuantity" label="留样数量" rules={[{ required: true }]}>
            <InputNumber min={0.001} precision={3} className="fullWidthInput" />
          </Form.Item>
          <Form.Item name="quantityUnit" label="单位" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="storageCondition" label="储存条件" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="storageLocation" label="储存位置" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="retainUntil" label="留样至" rules={[{ required: true }]}>
            <Input type="date" />
          </Form.Item>
          <Form.Item name="dispositionPlan" label="处置计划" rules={[{ required: true }]}>
            <Select
              options={[
                { label: "销毁", value: "DESTROY" },
                { label: "退还", value: "RETURN" },
                { label: "归档", value: "ARCHIVE" },
                { label: "延期", value: "EXTEND" },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function canCreateOrRegister(roles: readonly string[]) {
  return roles.includes("SAMPLE_MANAGER");
}

function canExtend(roles: readonly string[]) {
  return roles.includes("SAMPLE_MANAGER") || roles.includes("QUALITY_MANAGER");
}

function canDispose(roles: readonly string[]) {
  return roles.includes("SAMPLE_MANAGER") || roles.includes("QUALITY_MANAGER");
}

function isLoading(action: RetentionAction | undefined, id: string, name: RetentionAction["action"]) {
  return action?.id === id && action.action === name;
}

function nextDate(days: number) {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

function formatQuantity(sample: Sample) {
  if (sample.quantity === undefined || sample.quantity === null) {
    return "-";
  }
  return `${sample.quantity} ${sample.quantityUnit ?? ""}`.trim();
}

function retentionStatusColor(status: SampleRetention["status"]) {
  if (status === "RETAINED") {
    return "blue";
  }
  if (status === "DESTROYED" || status === "RETURNED" || status === "ARCHIVED") {
    return "green";
  }
  if (status === "EXPIRING" || status === "PENDING_DISPOSAL") {
    return "orange";
  }
  return "default";
}
