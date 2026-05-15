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
  enterWorkOrderResult,
  listRawRecords,
  listWorkOrders,
  pickWorkOrderSample,
  reviewRawRecord,
  startWorkOrder,
  submitWorkOrderRawRecord,
  useAuthStore,
  type RawRecord,
  type WorkOrder,
} from "@lims4wuxi/shared";

type WorkOrderAction = {
  id: string;
  action: "pick" | "start" | "result" | "submit" | "approve" | "reject";
};

type RawRecordFormValues = {
  templateCode: string;
  testItem: string;
  testBasis: string;
  testTime?: string;
  testLocation?: string;
  temperatureCelsius?: string;
  humidityPercent?: string;
  method?: string;
};

export function WorkOrderExecutionPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<RawRecordFormValues>();
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [rawRecords, setRawRecords] = useState<RawRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<WorkOrderAction>();
  const [submitTarget, setSubmitTarget] = useState<WorkOrder>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [workOrderPage, rawRecordPage] = await Promise.all([
        listWorkOrders({ page: 1, pageSize: 50 }),
        listRawRecords({ page: 1, pageSize: 50 }),
      ]);
      setWorkOrders(workOrderPage.items ?? []);
      setRawRecords(rawRecordPage.items ?? []);
    } catch {
      setError("工单或原始记录列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function runWorkOrderAction(
    workOrder: WorkOrder,
    nextAction: WorkOrderAction["action"],
    execute: () => Promise<WorkOrder>,
  ) {
    setAction({ id: workOrder.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = await execute();
      setFeedback(`工单 ${updated.bizNo} 已更新为 ${updated.status}`);
      await load();
    } catch {
      setError("工单动作执行失败，请确认工单状态、负责人和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  function openRawRecord(workOrder: WorkOrder) {
    setError(undefined);
    setFeedback(undefined);
    form.setFieldsValue({
      templateCode: "RAW_RECORD_STERILITY",
      testItem: "STERILITY",
      testBasis: "ISO 11737",
      testTime: new Date().toISOString().slice(0, 10),
      testLocation: "Lab A",
      temperatureCelsius: "22",
      humidityPercent: "45",
      method: "DIRECT_INOCULATION",
    });
    setSubmitTarget(workOrder);
  }

  async function submitRawRecord() {
    if (!submitTarget) {
      return;
    }
    setAction({ id: submitTarget.id, action: "submit" });
    setError(undefined);
    try {
      const values = await form.validateFields();
      const rawRecord = await submitWorkOrderRawRecord(submitTarget.id, {
        templateCode: values.templateCode,
        recordData: {
          testItem: values.testItem,
          testBasis: values.testBasis,
          testTime: values.testTime,
          testLocation: values.testLocation,
          temperatureCelsius: values.temperatureCelsius,
          humidityPercent: values.humidityPercent,
          method: values.method,
        },
        attachmentFileIds: [],
      });
      setFeedback(`原始记录 ${rawRecord.bizNo} 已提交`);
      setSubmitTarget(undefined);
      await load();
    } catch {
      setError("原始记录提交失败，请确认工单已录入结果且当前账号为工单负责人");
    } finally {
      setAction(undefined);
    }
  }

  async function review(record: RawRecord, outcome: "APPROVED" | "REJECTED") {
    const nextAction = outcome === "APPROVED" ? "approve" : "reject";
    setAction({ id: record.id, action: nextAction });
    setError(undefined);
    setFeedback(undefined);
    try {
      const reviewed = await reviewRawRecord(record.id, {
        stage: "QC",
        outcome,
        comment: outcome === "APPROVED" ? "记录完整" : "退回补充",
      });
      setFeedback(`原始记录 ${reviewed.bizNo} 已${outcome === "APPROVED" ? "通过" : "退回"}`);
      await load();
    } catch {
      setError("原始记录审核失败，请确认记录处于审核中且当前账号有 QC 权限");
    } finally {
      setAction(undefined);
    }
  }

  const workOrderColumns = useMemo<TableColumnsType<WorkOrder>>(() => [
    {
      title: "工单编号",
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
      render: (status: WorkOrder["status"]) => <Tag color={workOrderStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "进度",
      dataIndex: "progressPercent",
      key: "progressPercent",
      width: 90,
      render: (value: number) => `${value}%`,
    },
    {
      title: "项目 ID",
      dataIndex: "projectId",
      key: "projectId",
      ellipsis: true,
    },
    {
      title: "样品 ID",
      dataIndex: "sampleId",
      key: "sampleId",
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 260,
      fixed: "right",
      render: (_, workOrder) => (
        <Space size={8} wrap>
          {canExecute(roles) && workOrder.status === "PENDING_EXECUTION" ? (
            <Button
              size="small"
              loading={isLoading(action, workOrder.id, "pick")}
              onClick={() => void runWorkOrderAction(workOrder, "pick", () => pickWorkOrderSample(workOrder.id))}
            >
              领样
            </Button>
          ) : null}
          {canExecute(roles) && workOrder.status === "SAMPLE_PICKED" ? (
            <Button
              size="small"
              loading={isLoading(action, workOrder.id, "start")}
              onClick={() => void runWorkOrderAction(workOrder, "start", () => startWorkOrder(workOrder.id))}
            >
              开始
            </Button>
          ) : null}
          {canExecute(roles) && workOrder.status === "IN_PROGRESS" ? (
            <Button
              size="small"
              loading={isLoading(action, workOrder.id, "result")}
              onClick={() => void runWorkOrderAction(workOrder, "result", () => enterWorkOrderResult(workOrder.id))}
            >
              录入结果
            </Button>
          ) : null}
          {canExecute(roles) && workOrder.status === "RESULT_ENTERED" ? (
            <Button
              size="small"
              type="primary"
              loading={isLoading(action, workOrder.id, "submit")}
              onClick={() => openRawRecord(workOrder)}
            >
              提交记录
            </Button>
          ) : null}
        </Space>
      ),
    },
  ], [action, roles]);

  const rawRecordColumns = useMemo<TableColumnsType<RawRecord>>(() => [
    {
      title: "记录编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: RawRecord["status"]) => <Tag color={rawRecordStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "检测项",
      dataIndex: "testItem",
      key: "testItem",
      width: 140,
      render: (value?: string) => value ?? "-",
    },
    {
      title: "工单 ID",
      dataIndex: "workOrderId",
      key: "workOrderId",
      ellipsis: true,
    },
    {
      title: "模板",
      dataIndex: "templateCode",
      key: "templateCode",
      width: 180,
    },
    {
      title: "操作",
      key: "action",
      width: 180,
      fixed: "right",
      render: (_, record) => (
        canReview(roles) && record.status === "REVIEWING" ? (
          <Space size={8} wrap>
            <Button
              size="small"
              loading={isLoading(action, record.id, "reject")}
              onClick={() => void review(record, "REJECTED")}
            >
              退回
            </Button>
            <Button
              size="small"
              type="primary"
              loading={isLoading(action, record.id, "approve")}
              onClick={() => void review(record, "APPROVED")}
            >
              通过
            </Button>
          </Space>
        ) : null
      ),
    },
  ], [action, roles]);

  return (
    <section className="workOrderPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">WORK ORDER EXECUTION</Typography.Text>
          <Typography.Title level={3}>试验执行</Typography.Title>
          <Typography.Text>工单执行、结果录入和原始记录审核。</Typography.Text>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

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
          scroll={{ x: 1060 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无工单" }}
        />
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>原始记录</Typography.Title>
          <Tag color="blue">{rawRecords.length} 份</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={rawRecordColumns}
          dataSource={rawRecords}
          scroll={{ x: 960 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无原始记录" }}
        />
      </section>

      <Modal
        title="提交原始记录"
        open={Boolean(submitTarget)}
        onCancel={() => setSubmitTarget(undefined)}
        onOk={() => void submitRawRecord()}
        confirmLoading={action?.id === submitTarget?.id && action?.action === "submit"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="工单编号">
            <Input value={submitTarget?.bizNo ?? ""} disabled />
          </Form.Item>
          <Form.Item name="templateCode" label="记录模板" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="testItem" label="检测项" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="testBasis" label="检测依据" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="testTime" label="检测日期">
            <Input type="date" />
          </Form.Item>
          <Form.Item name="testLocation" label="检测地点">
            <Input />
          </Form.Item>
          <Space.Compact block>
            <Form.Item name="temperatureCelsius" label="温度" className="compactFormItem">
              <Input suffix="C" />
            </Form.Item>
            <Form.Item name="humidityPercent" label="湿度" className="compactFormItem">
              <Input suffix="%" />
            </Form.Item>
          </Space.Compact>
          <Form.Item name="method" label="方法">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}

function canExecute(roles: readonly string[]) {
  return roles.includes("INSPECTOR");
}

function canReview(roles: readonly string[]) {
  return roles.includes("QC");
}

function isLoading(action: WorkOrderAction | undefined, id: string, name: WorkOrderAction["action"]) {
  return action?.id === id && action.action === name;
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

function rawRecordStatusColor(status: RawRecord["status"]) {
  if (status === "APPROVED") {
    return "green";
  }
  if (status === "REVIEWING" || status === "SUBMITTED") {
    return "blue";
  }
  if (status === "REJECTED") {
    return "red";
  }
  return "default";
}
