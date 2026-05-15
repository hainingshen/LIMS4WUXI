import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createArchiveItem,
  listArchiveItems,
  useAuthStore,
  type ArchiveItem,
  type ArchiveItemCreateRequest,
} from "@lims4wuxi/shared";

type ArchiveFormValues = {
  projectId: string;
  entityType?: string;
  entityId?: string;
  archiveType: string;
  fileId: string;
};

export function ArchiveItemPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<ArchiveFormValues>();
  const [keyword, setKeyword] = useState("");
  const [items, setItems] = useState<ArchiveItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listArchiveItems({
        page: 1,
        pageSize: 100,
      });
      setItems(page.items ?? []);
    } catch {
      setError("档案项加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function submit(values: ArchiveFormValues) {
    setSubmitting(true);
    setError(undefined);
    setFeedback(undefined);
    const request: ArchiveItemCreateRequest = {
      projectId: values.projectId.trim(),
      entityType: emptyToUndefined(values.entityType),
      entityId: emptyToUndefined(values.entityId),
      archiveType: values.archiveType.trim(),
      fileId: values.fileId.trim(),
    };
    try {
      const created = await createArchiveItem(request);
      setFeedback(`档案项 ${archiveTypeLabel(created.archiveType)} 已归档`);
      form.resetFields();
      await load();
    } catch {
      setError("档案登记失败，请确认项目、文件 ID 和档案管理员权限");
    } finally {
      setSubmitting(false);
    }
  }

  const filteredItems = useMemo(() => filterArchiveItems(items, keyword), [items, keyword]);

  const columns = useMemo<TableColumnsType<ArchiveItem>>(() => [
    {
      title: "项目",
      key: "project",
      width: 230,
      fixed: "left",
      render: (_, item) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{item.projectBizNo ?? shortId(item.projectId)}</Typography.Text>
          <Typography.Text type="secondary">{item.customerOrgName ?? "-"}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "档案类型",
      dataIndex: "archiveType",
      key: "archiveType",
      width: 170,
      render: (value: string) => <Tag color="blue">{archiveTypeLabel(value)}</Tag>,
    },
    {
      title: "对象",
      key: "entity",
      width: 190,
      render: (_, item) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{item.entityBizNo ?? item.entityType ?? "-"}</Typography.Text>
          <Typography.Text type="secondary">{entityTypeLabel(item.entityType)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "文件",
      key: "fileId",
      width: 250,
      render: (_, item) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{item.fileName ?? shortId(item.fileId)}</Typography.Text>
          <Typography.Text type="secondary">{fileMeta(item)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: ArchiveItem["status"]) => <Tag color={statusColor(status)}>{archiveStatusLabel(status)}</Tag>,
    },
    {
      title: "归档时间",
      dataIndex: "archivedAt",
      key: "archivedAt",
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
  ], []);

  return (
    <section className="archivePage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">ARCHIVE CENTER</Typography.Text>
          <Typography.Title level={3}>档案归档</Typography.Title>
          <Typography.Text>按项目保存送检、合同、报告、付款、绿色通道和留样处置归档件。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="archiveGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>档案列表</Typography.Title>
            <Space wrap>
              <Input
                allowClear
                placeholder="项目号 / 客户 / 报告号 / 文件名"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                onPressEnter={() => void load()}
                className="archiveSearchInput"
              />
              <Button type="primary" onClick={() => void load()}>查询</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={filteredItems}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1190 }}
            locale={{ emptyText: "暂无档案项" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>归档登记</Typography.Title>
          </div>
          <Form
            form={form}
            layout="vertical"
            initialValues={{ entityType: "REPORT", archiveType: "REPORT_OFFICIAL_PDF" }}
            onFinish={(values) => void submit(values)}
          >
            <Form.Item name="projectId" label="项目 ID（从项目详情复制）" rules={[{ required: true }]}>
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="entityType" label="对象类型">
              <Input placeholder="REPORT / CONTRACT / SAMPLE_RETENTION" disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="entityId" label="对象 ID（可选）">
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="archiveType" label="档案类型" rules={[{ required: true }]}>
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="fileId" label="文件 ID（由上传服务返回）" rules={[{ required: true }]}>
              <Input placeholder="文件上传后自动生成的 ID" disabled={!canManage(roles)} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!canManage(roles)}>
              登记归档
            </Button>
          </Form>
        </section>
      </section>
    </section>
  );
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function canManage(roles: readonly string[]) {
  return roles.includes("ARCHIVE_MANAGER");
}

function statusColor(status: ArchiveItem["status"]) {
  if (status === "ARCHIVED") {
    return "green";
  }
  if (status === "VOIDED") {
    return "red";
  }
  return "orange";
}

function archiveStatusLabel(status: ArchiveItem["status"]) {
  const labels: Record<ArchiveItem["status"], string> = {
    ARCHIVED: "已归档",
    SUPERSEDED: "已替换",
    VOIDED: "已作废",
  };
  return labels[status] ?? status;
}

function archiveTypeLabel(type: string) {
  const labels: Record<string, string> = {
    REPORT_OFFICIAL_PDF: "正式报告 PDF",
    CONTRACT_SIGNED: "已签署合同",
    SUBMISSION_FORM: "送检单",
    RAW_RECORD: "原始记录",
    SAMPLE_PHOTO: "样品照片",
    PAYMENT_RECEIPT: "付款凭证",
    DELIVERY_LOG: "发放日志",
  };
  return labels[type] ?? type;
}

function entityTypeLabel(type?: string) {
  if (!type) {
    return "-";
  }
  const labels: Record<string, string> = {
    REPORT: "报告",
    CONTRACT: "合同",
    SUBMISSION: "送检单",
    SAMPLE_RETENTION: "留样",
    RAW_RECORD: "原始记录",
    PAYMENT: "付款",
  };
  return labels[type] ?? type;
}

function fileMeta(item: ArchiveItem) {
  const parts = [item.contentType, item.sizeBytes ? formatBytes(item.sizeBytes) : undefined].filter(Boolean);
  return parts.length ? parts.join(" / ") : shortId(item.fileId);
}

function filterArchiveItems(items: readonly ArchiveItem[], keyword: string) {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return items;
  }
  return items.filter((item) => [
    item.projectBizNo,
    item.customerOrgName,
    item.entityBizNo,
    item.archiveType,
    item.fileName,
    item.status,
  ].some((value) => value?.toLowerCase().includes(normalized)));
}

function shortId(id?: string | null) {
  return id ? id.slice(0, 8) : "-";
}

function formatBytes(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${size} B`;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}
