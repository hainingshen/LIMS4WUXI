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
  const [projectId, setProjectId] = useState("");
  const [items, setItems] = useState<ArchiveItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextProjectId = projectId) {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listArchiveItems({
        page: 1,
        pageSize: 100,
        projectId: emptyToUndefined(nextProjectId),
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
      setFeedback(`档案项 ${created.archiveType} 已归档`);
      form.resetFields();
      await load(projectId);
    } catch {
      setError("档案登记失败，请确认项目、文件 ID 和档案管理员权限");
    } finally {
      setSubmitting(false);
    }
  }

  const columns = useMemo<TableColumnsType<ArchiveItem>>(() => [
    {
      title: "项目",
      dataIndex: "projectId",
      key: "projectId",
      width: 250,
      fixed: "left",
    },
    {
      title: "档案类型",
      dataIndex: "archiveType",
      key: "archiveType",
      width: 170,
      render: (value: string) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: "对象",
      key: "entity",
      width: 220,
      render: (_, item) => item.entityType ? `${item.entityType} ${item.entityId ?? ""}`.trim() : "-",
    },
    {
      title: "文件",
      dataIndex: "fileId",
      key: "fileId",
      width: 250,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: ArchiveItem["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
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
                placeholder="项目 UUID"
                value={projectId}
                onChange={(event) => setProjectId(event.target.value)}
                onPressEnter={() => void load(projectId)}
                className="archiveSearchInput"
              />
              <Button type="primary" onClick={() => void load(projectId)}>查询</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={items}
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
            <Form.Item name="projectId" label="项目 ID" rules={[{ required: true }]}>
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="entityType" label="对象类型">
              <Input placeholder="REPORT / CONTRACT / SAMPLE_RETENTION" disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="entityId" label="对象 ID">
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="archiveType" label="档案类型" rules={[{ required: true }]}>
              <Input disabled={!canManage(roles)} />
            </Form.Item>
            <Form.Item name="fileId" label="文件 ID" rules={[{ required: true }]}>
              <Input placeholder="file_metadata UUID" disabled={!canManage(roles)} />
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

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}
