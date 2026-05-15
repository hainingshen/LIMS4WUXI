import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createInventoryTransaction,
  listInventoryBatches,
  listInventoryTransactions,
  upsertInventoryBatch,
  useAuthStore,
  type InventoryBatch,
  type InventoryBatchUpsertRequest,
  type InventoryTransaction,
  type InventoryTransactionCreateRequest,
} from "@lims4wuxi/shared";

const STORAGE_CONDITIONS = ["ROOM_TEMPERATURE", "REFRIGERATED", "FROZEN", "OTHER"];
const TXN_TYPES: InventoryTransactionCreateRequest["txnType"][] = ["IN", "OUT", "USE", "RETURN", "SCRAP", "ADJUST"];

type BatchFormValues = {
  batchNo: string;
  name: string;
  casNo?: string;
  specification?: string;
  manufacturer?: string;
  receivedAt?: string;
  expiryDate?: string;
  quantity: number;
  unit: string;
  storageCondition: string;
  storageLocation?: string;
};

type TransactionFormValues = {
  txnType: InventoryTransactionCreateRequest["txnType"];
  quantity: number;
  reason?: string;
};

export function InventoryPage() {
  const roles = useAuthStore((state) => state.roles);
  const [batchForm] = Form.useForm<BatchFormValues>();
  const [transactionForm] = Form.useForm<TransactionFormValues>();
  const [keyword, setKeyword] = useState("");
  const [batches, setBatches] = useState<InventoryBatch[]>([]);
  const [transactions, setTransactions] = useState<InventoryTransaction[]>([]);
  const [selectedBatch, setSelectedBatch] = useState<InventoryBatch>();
  const [loading, setLoading] = useState(true);
  const [transactionLoading, setTransactionLoading] = useState(false);
  const [submittingBatch, setSubmittingBatch] = useState(false);
  const [submittingTransaction, setSubmittingTransaction] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextKeyword = keyword, preferredBatchId = selectedBatch?.id) {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listInventoryBatches({
        page: 1,
        pageSize: 100,
        keyword: emptyToUndefined(nextKeyword),
      });
      const nextBatches = page.items ?? [];
      setBatches(nextBatches);
      const nextSelected = nextBatches.find((item) => item.id === preferredBatchId) ?? nextBatches[0];
      setSelectedBatch(nextSelected);
      if (nextSelected) {
        await loadTransactions(nextSelected.id);
      } else {
        setTransactions([]);
      }
    } catch {
      setError("库存批次加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadTransactions(batchId: string) {
    setTransactionLoading(true);
    try {
      const page = await listInventoryTransactions({ inventoryBatchId: batchId, page: 1, pageSize: 100 });
      setTransactions(page.items ?? []);
    } catch {
      setError("库存流水加载失败");
    } finally {
      setTransactionLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function selectBatch(batch: InventoryBatch) {
    setSelectedBatch(batch);
    await loadTransactions(batch.id);
  }

  async function submitBatch(values: BatchFormValues) {
    setSubmittingBatch(true);
    setError(undefined);
    setFeedback(undefined);
    const request: InventoryBatchUpsertRequest = {
      batchNo: values.batchNo.trim(),
      name: values.name.trim(),
      casNo: emptyToUndefined(values.casNo),
      specification: emptyToUndefined(values.specification),
      manufacturer: emptyToUndefined(values.manufacturer),
      receivedAt: emptyToUndefined(values.receivedAt),
      expiryDate: emptyToUndefined(values.expiryDate),
      quantity: values.quantity,
      unit: values.unit.trim(),
      storageCondition: values.storageCondition.trim(),
      storageLocation: emptyToUndefined(values.storageLocation),
    };
    try {
      const saved = await upsertInventoryBatch(request);
      setFeedback(`库存批次 ${saved.batchNo} 已保存`);
      batchForm.resetFields();
      await load(keyword, saved.id);
    } catch {
      setError("库存批次保存失败，请确认批号唯一、数量和权限");
    } finally {
      setSubmittingBatch(false);
    }
  }

  async function submitTransaction(values: TransactionFormValues) {
    if (!selectedBatch) {
      setError("请先选择库存批次");
      return;
    }
    setSubmittingTransaction(true);
    setError(undefined);
    setFeedback(undefined);
    const request: InventoryTransactionCreateRequest = {
      inventoryBatchId: selectedBatch.id,
      txnType: values.txnType,
      quantity: values.quantity,
      reason: emptyToUndefined(values.reason),
    };
    try {
      const saved = await createInventoryTransaction(request);
      setFeedback(`库存流水 ${saved.txnType} 已记录`);
      transactionForm.resetFields();
      await load(keyword, selectedBatch.id);
    } catch {
      setError("库存流水登记失败，请确认库存充足、数量和权限");
    } finally {
      setSubmittingTransaction(false);
    }
  }

  const batchColumns = useMemo<TableColumnsType<InventoryBatch>>(() => [
    {
      title: "批号",
      dataIndex: "batchNo",
      key: "batchNo",
      width: 160,
      fixed: "left",
    },
    {
      title: "名称",
      dataIndex: "name",
      key: "name",
      width: 190,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 140,
      render: (status: InventoryBatch["status"]) => <Tag color={batchStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "数量",
      key: "quantity",
      width: 130,
      render: (_, batch) => `${batch.quantity} ${batch.unit}`,
    },
    {
      title: "有效期",
      dataIndex: "expiryDate",
      key: "expiryDate",
      width: 130,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "库位",
      dataIndex: "storageLocation",
      key: "storageLocation",
      width: 160,
      render: (value?: string | null) => value ?? "-",
    },
  ], []);

  const transactionColumns = useMemo<TableColumnsType<InventoryTransaction>>(() => [
    {
      title: "类型",
      dataIndex: "txnType",
      key: "txnType",
      width: 110,
      render: (value: InventoryTransaction["txnType"]) => <Tag color={transactionTypeColor(value)}>{value}</Tag>,
    },
    {
      title: "数量",
      dataIndex: "quantity",
      key: "quantity",
      width: 110,
    },
    {
      title: "原因",
      dataIndex: "reason",
      key: "reason",
      width: 240,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "时间",
      dataIndex: "operatedAt",
      key: "operatedAt",
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
  ], []);

  const canManageInventory = canManage(roles);

  return (
    <section className="inventoryPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">INVENTORY</Typography.Text>
          <Typography.Title level={3}>库存管理</Typography.Title>
          <Typography.Text>维护标准品、试剂和耗材台账，记录出入库、领用、退回和报废。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="inventoryGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>库存批次</Typography.Title>
            <Space wrap>
              <Input
                allowClear
                placeholder="批号 / 名称 / CAS"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                onPressEnter={() => void load(keyword)}
                className="inventorySearchInput"
              />
              <Button type="primary" onClick={() => void load(keyword)}>查询</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={batchColumns}
            dataSource={batches}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 910 }}
            rowClassName={(item) => item.id === selectedBatch?.id ? "selectedRow" : ""}
            onRow={(item) => ({ onClick: () => void selectBatch(item) })}
            locale={{ emptyText: "暂无库存批次" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>批次登记</Typography.Title>
          </div>
          <Form
            form={batchForm}
            layout="vertical"
            initialValues={{ quantity: 1, unit: "bottle", storageCondition: "ROOM_TEMPERATURE" }}
            onFinish={(values) => void submitBatch(values)}
          >
            <div className="inventoryTxnGrid">
              <Form.Item name="batchNo" label="批号" rules={[{ required: true }]}>
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="name" label="名称" rules={[{ required: true }]}>
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="casNo" label="CAS">
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="specification" label="规格">
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="manufacturer" label="厂家">
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="receivedAt" label="接收日期">
                <Input type="date" disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="expiryDate" label="有效期">
                <Input type="date" disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
                <InputNumber min={0} precision={3} className="fullWidthInput" disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="unit" label="单位" rules={[{ required: true }]}>
                <Input disabled={!canManageInventory} />
              </Form.Item>
              <Form.Item name="storageCondition" label="保存条件" rules={[{ required: true }]}>
                <Select
                  options={STORAGE_CONDITIONS.map((value) => ({ label: value, value }))}
                  disabled={!canManageInventory}
                />
              </Form.Item>
            </div>
            <Form.Item name="storageLocation" label="库位">
              <Input disabled={!canManageInventory} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submittingBatch} disabled={!canManageInventory}>
              保存批次
            </Button>
          </Form>
        </section>
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <div>
            <Typography.Title level={5}>库存流水</Typography.Title>
            <Typography.Text>
              {selectedBatch ? `${selectedBatch.batchNo} ${selectedBatch.name}` : "请选择库存批次"}
            </Typography.Text>
          </div>
          <Tag color={selectedBatch ? batchStatusColor(selectedBatch.status) : "default"}>
            {selectedBatch ? `${selectedBatch.quantity} ${selectedBatch.unit}` : "未选择"}
          </Tag>
        </div>
        <Form
          form={transactionForm}
          layout="vertical"
          initialValues={{ txnType: "USE", quantity: 1 }}
          onFinish={(values) => void submitTransaction(values)}
        >
          <div className="inventoryTxnGrid">
            <Form.Item name="txnType" label="流水类型" rules={[{ required: true }]}>
              <Select
                options={TXN_TYPES.map((value) => ({ label: value, value }))}
                disabled={!canManageInventory}
              />
            </Form.Item>
            <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
              <InputNumber min={0.001} precision={3} className="fullWidthInput" disabled={!canManageInventory} />
            </Form.Item>
            <Form.Item name="reason" label="原因">
              <Input disabled={!canManageInventory} />
            </Form.Item>
          </div>
          <Button
            type="primary"
            htmlType="submit"
            loading={submittingTransaction}
            disabled={!selectedBatch || !canManageInventory}
          >
            记录流水
          </Button>
        </Form>
        <Table
          rowKey="id"
          size="small"
          loading={transactionLoading}
          columns={transactionColumns}
          dataSource={transactions}
          pagination={{ pageSize: 6 }}
          scroll={{ x: 630 }}
          locale={{ emptyText: "请选择批次或登记流水" }}
        />
      </section>
    </section>
  );
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function canManage(roles: readonly string[]) {
  return roles.includes("INVENTORY_MANAGER");
}

function batchStatusColor(status: InventoryBatch["status"]) {
  if (status === "IN_STOCK" || status === "OPENED") {
    return "green";
  }
  if (status === "LOW_STOCK" || status === "PENDING_ACCEPTANCE") {
    return "orange";
  }
  if (status === "EXPIRED" || status === "DEPLETED" || status === "SCRAPPED" || status === "REJECTED") {
    return "red";
  }
  return "default";
}

function transactionTypeColor(type: InventoryTransaction["txnType"]) {
  if (type === "IN" || type === "RETURN") {
    return "green";
  }
  if (type === "ADJUST") {
    return "blue";
  }
  if (type === "SCRAP") {
    return "red";
  }
  return "orange";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}
