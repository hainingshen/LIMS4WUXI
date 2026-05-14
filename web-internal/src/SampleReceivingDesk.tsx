import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
  type TableProps,
} from "antd";
import {
  getCurrentUser,
  listSamples,
  receiveSample,
  stockInSample,
  uploadSamplePhoto,
  type CurrentUser,
  type Sample,
} from "@lims4wuxi/shared";

type SampleAction = {
  type: "exception" | "stock";
  sample: Sample;
};

type ActionForm = {
  exceptionNote?: string;
  inventoryLocation?: string;
};

const STATUS_LABELS: Record<Sample["status"], string> = {
  WAITING_DELIVERY: "待到达",
  ARRIVED_PENDING_CHECK: "待核验",
  CHECK_EXCEPTION: "核验异常",
  PRE_IN_STOCK: "预入库",
  IN_STOCK: "已入库",
  CONDITIONING: "置样中",
  READY_FOR_TEST: "待试验",
  IN_TESTING: "试验中",
  TEST_COMPLETED: "试验完成",
  PENDING_RETURN: "待退还",
  PENDING_DISPOSAL: "待处置",
  RETURNED: "已退还",
  DISPOSED: "已处置",
  ARCHIVED: "已归档",
};

export function SampleReceivingDesk() {
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [samples, setSamples] = useState<Sample[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [currentUser, setCurrentUser] = useState<CurrentUser>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const [photoFile, setPhotoFile] = useState<File>();
  const [photoNote, setPhotoNote] = useState("");
  const [action, setAction] = useState<SampleAction>();
  const [form] = Form.useForm<ActionForm>();

  const selectedSample = useMemo(
    () => samples.find((sample) => sample.id === selectedId) ?? samples[0],
    [samples, selectedId],
  );

  const loadSamples = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listSamples({
        keyword: query.trim() || undefined,
        page: 1,
        pageSize: 50,
      });
      setSamples(page.items);
      setSelectedId((current) => {
        if (current && page.items.some((sample) => sample.id === current)) {
          return current;
        }
        return page.items[0]?.id;
      });
    } catch {
      setError("样品列表加载失败");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    void getCurrentUser().then(setCurrentUser).catch(() => setError("当前用户信息读取失败"));
  }, []);

  useEffect(() => {
    void loadSamples();
  }, [loadSamples]);

  const columns: TableProps<Sample>["columns"] = [
    {
      title: "样品编号",
      dataIndex: "bizNo",
      width: 150,
      render: (value, sample) => (
        <Button type="link" onClick={() => setSelectedId(sample.id)}>
          {value}
        </Button>
      ),
    },
    {
      title: "样品名称",
      dataIndex: "sampleNameZh",
      render: (_, sample) => (
        <Space direction="vertical" size={0}>
          <span>{sample.sampleNameZh}</span>
          <Typography.Text type="secondary">{sample.sampleNameEn}</Typography.Text>
        </Space>
      ),
    },
    { title: "型号", dataIndex: "modelRef", width: 140 },
    { title: "批号", dataIndex: "lotBatch", width: 140 },
    {
      title: "数量",
      width: 110,
      render: (_, sample) => formatQuantity(sample),
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 120,
      render: (status: Sample["status"]) => <StatusTag status={status} />,
    },
  ];

  async function markReceived(sample: Sample) {
    if (!currentUser) {
      setError("当前用户信息读取失败");
      return;
    }
    setSaving(true);
    try {
      await receiveSample(sample.id, {
        receivedAt: new Date().toISOString(),
        receivedBy: currentUser.id,
        checkPassed: true,
      });
      message.success("已登记到达并预入库");
      await loadSamples();
    } catch {
      setError("登记到达失败");
    } finally {
      setSaving(false);
    }
  }

  async function submitAction(values: ActionForm) {
    if (!action || !currentUser) {
      return;
    }
    setSaving(true);
    setError(undefined);
    try {
      if (action.type === "exception") {
        await receiveSample(action.sample.id, {
          receivedAt: new Date().toISOString(),
          receivedBy: currentUser.id,
          checkPassed: false,
          exceptionNote: values.exceptionNote,
        });
        message.success("已记录核验异常");
      } else {
        await stockInSample(action.sample.id, {
          inventoryLocation: values.inventoryLocation,
          stockedAt: new Date().toISOString(),
        });
        message.success("已正式入库");
      }
      closeAction();
      await loadSamples();
    } catch {
      setError(action.type === "exception" ? "异常记录失败" : "正式入库失败");
    } finally {
      setSaving(false);
    }
  }

  async function uploadPhoto() {
    if (!selectedSample || !photoFile) {
      return;
    }
    setSaving(true);
    setError(undefined);
    try {
      await uploadSamplePhoto(selectedSample.id, photoFile, photoNote.trim() || undefined);
      message.success("样品照片已上传");
      setPhotoFile(undefined);
      setPhotoNote("");
    } catch {
      setError("样品照片上传失败");
    } finally {
      setSaving(false);
    }
  }

  function openAction(nextAction: SampleAction) {
    setAction(nextAction);
    form.setFieldsValue({
      exceptionNote: "",
      inventoryLocation: nextAction.sample.inventoryLocation ?? "",
    });
  }

  function closeAction() {
    setAction(undefined);
    form.resetFields();
  }

  return (
    <section className="sampleDesk">
      <header className="sampleDeskHeader">
        <div>
          <Typography.Title level={3}>样品收样台</Typography.Title>
        </div>
        <Input.Search
          className="sampleDeskSearch"
          placeholder="送检单 / 项目 / 样品 / 批号"
          value={keyword}
          allowClear
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={setQuery}
          enterButton="搜索"
        />
      </header>

      {error ? <Alert type="error" message={error} showIcon closable onClose={() => setError(undefined)} /> : null}

      <div className="sampleDeskGrid">
        <div className="deskPanel">
          <Table
            rowKey="id"
            size="middle"
            loading={loading}
            columns={columns}
            dataSource={samples}
            pagination={false}
            rowClassName={(sample) => sample.id === selectedSample?.id ? "selectedRow" : ""}
            onRow={(sample) => ({ onClick: () => setSelectedId(sample.id) })}
            scroll={{ x: 760 }}
          />
        </div>

        <aside className="deskPanel sampleDetailPanel">
          {selectedSample ? (
            <>
              <div className="detailTitle">
                <Space direction="vertical" size={2}>
                  <Typography.Text strong>{selectedSample.bizNo}</Typography.Text>
                  <Typography.Text>{selectedSample.sampleNameZh}</Typography.Text>
                </Space>
                <StatusTag status={selectedSample.status} />
              </div>

              <Descriptions size="small" column={1} bordered>
                <Descriptions.Item label="英文名">{selectedSample.sampleNameEn}</Descriptions.Item>
                <Descriptions.Item label="型号">{selectedSample.modelRef ?? "-"}</Descriptions.Item>
                <Descriptions.Item label="规格">{selectedSample.sizeSpec ?? "-"}</Descriptions.Item>
                <Descriptions.Item label="批号">{selectedSample.lotBatch}</Descriptions.Item>
                <Descriptions.Item label="数量">{formatQuantity(selectedSample)}</Descriptions.Item>
                <Descriptions.Item label="材料">{selectedSample.rawMaterial ?? "-"}</Descriptions.Item>
                <Descriptions.Item label="包装">{selectedSample.packagingMaterial ?? "-"}</Descriptions.Item>
                <Descriptions.Item label="储存">{selectedSample.storageCondition ?? "-"}</Descriptions.Item>
                <Descriptions.Item label="入库位置">{selectedSample.inventoryLocation ?? "-"}</Descriptions.Item>
              </Descriptions>

              <Space wrap className="sampleActions">
                <Button
                  type="primary"
                  disabled={!canReceive(selectedSample)}
                  loading={saving}
                  onClick={() => void markReceived(selectedSample)}
                >
                  登记到达
                </Button>
                <Button
                  disabled={!canReceive(selectedSample)}
                  loading={saving}
                  onClick={() => openAction({ type: "exception", sample: selectedSample })}
                >
                  核验异常
                </Button>
                <Button
                  disabled={selectedSample.status !== "PRE_IN_STOCK"}
                  loading={saving}
                  onClick={() => openAction({ type: "stock", sample: selectedSample })}
                >
                  正式入库
                </Button>
              </Space>

              <div className="photoUpload">
                <Upload.Dragger
                  accept="image/*"
                  maxCount={1}
                  beforeUpload={(file) => {
                    setPhotoFile(file);
                    return false;
                  }}
                  onRemove={() => {
                    setPhotoFile(undefined);
                    return true;
                  }}
                >
                  <p className="ant-upload-text">上传样品照片</p>
                </Upload.Dragger>
                <Input
                  placeholder="照片备注"
                  value={photoNote}
                  onChange={(event) => setPhotoNote(event.target.value)}
                />
                <Button type="primary" disabled={!photoFile} loading={saving} onClick={() => void uploadPhoto()}>
                  保存照片
                </Button>
              </div>
            </>
          ) : (
            <Typography.Text type="secondary">暂无样品</Typography.Text>
          )}
        </aside>
      </div>

      <Modal
        open={Boolean(action)}
        title={action?.type === "exception" ? "核验异常" : "正式入库"}
        confirmLoading={saving}
        onOk={() => form.submit()}
        onCancel={closeAction}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={submitAction}>
          {action?.type === "exception" ? (
            <Form.Item name="exceptionNote" label="异常说明" rules={[{ required: true }]}>
              <Input.TextArea rows={4} />
            </Form.Item>
          ) : (
            <Form.Item name="inventoryLocation" label="入库位置" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </section>
  );
}

function StatusTag({ status }: { status: Sample["status"] }) {
  const color = status === "CHECK_EXCEPTION" ? "red" : status === "IN_STOCK" ? "green" : "blue";
  return <Tag color={color}>{STATUS_LABELS[status]}</Tag>;
}

function canReceive(sample: Sample) {
  return ["WAITING_DELIVERY", "ARRIVED_PENDING_CHECK", "CHECK_EXCEPTION"].includes(sample.status);
}

function formatQuantity(sample: Sample) {
  if (sample.quantity === undefined || sample.quantity === null) {
    return "-";
  }
  return `${sample.quantity} ${sample.quantityUnit ?? ""}`.trim();
}
