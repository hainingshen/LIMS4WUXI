import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createImportExportTask,
  downloadImportExportTaskResultFile,
  listImportExportTasks,
  runImportExportTask,
  uploadFile,
  validateImportExportTask,
  useAuthStore,
  type ImportExportTask,
  type ImportExportTaskCreateRequest,
} from "@lims4wuxi/shared";

const TASK_TYPES: ImportExportTaskCreateRequest["taskType"][] = [
  "MASTER_IMPORT",
  "MIGRATION_IMPORT",
  "LIST_EXPORT",
  "REPORT_BATCH_DOWNLOAD",
  "AUDIT_EXPORT",
];

type TaskFormValues = {
  taskType: ImportExportTaskCreateRequest["taskType"];
  entityType: string;
  keyword?: string;
  includeDeliveryLog?: boolean;
};

const TASK_TYPE_LABELS: Record<ImportExportTaskCreateRequest["taskType"], string> = {
  MASTER_IMPORT: "主数据导入",
  MIGRATION_IMPORT: "迁移导入",
  LIST_EXPORT: "列表导出",
  REPORT_BATCH_DOWNLOAD: "报告批量下载",
  AUDIT_EXPORT: "审计导出",
};

const TASK_STATUS_LABELS: Record<ImportExportTask["status"], string> = {
  PENDING: "待执行",
  VALIDATING: "校验中",
  RUNNING: "执行中",
  SUCCESS: "成功",
  FAILED: "失败",
  EXPIRED: "已过期",
};

export function ImportExportTaskPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<TaskFormValues>();
  const [items, setItems] = useState<ImportExportTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<string>();
  const [sourceFile, setSourceFile] = useState<File>();
  const [sourceFileInputKey, setSourceFileInputKey] = useState(0);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();
  const taskType = (Form.useWatch("taskType", form) ?? "LIST_EXPORT") as ImportExportTaskCreateRequest["taskType"];

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listImportExportTasks({ page: 1, pageSize: 100 });
      setItems(page.items ?? []);
    } catch {
      setError("导入导出任务加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    if (!isImportTask(taskType)) {
      setSourceFile(undefined);
      setSourceFileInputKey((value) => value + 1);
    }
  }, [taskType]);

  async function submit(values: TaskFormValues) {
    if (isImportTask(values.taskType) && !sourceFile) {
      setError("导入任务需要先选择源文件");
      return;
    }
    setSubmitting(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const uploaded = sourceFile ? await uploadFile("IMPORT_EXPORT", sourceFile) : undefined;
      const request: ImportExportTaskCreateRequest = {
        taskType: values.taskType,
        entityType: values.entityType.trim(),
        sourceFileId: uploaded?.fileId,
        parameters: buildTaskParameters(values),
      };
      const created = await createImportExportTask(request);
      setFeedback(`任务 ${created.bizNo} 已创建`);
      form.resetFields();
      setSourceFile(undefined);
      setSourceFileInputKey((value) => value + 1);
      await load();
    } catch {
      setError("任务创建失败，请确认文件格式、任务参数和权限");
    } finally {
      setSubmitting(false);
    }
  }

  async function runAction(task: ImportExportTask, action: "validate" | "run") {
    setActionLoadingId(`${task.id}:${action}`);
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = action === "validate"
        ? await validateImportExportTask(task.id)
        : await runImportExportTask(task.id);
      setFeedback(`任务 ${updated.bizNo} 状态 ${updated.status}`);
      await load();
    } catch {
      setError(action === "validate" ? "任务预校验失败" : "任务执行失败");
    } finally {
      setActionLoadingId(undefined);
    }
  }

  async function downloadResult(task: ImportExportTask) {
    setActionLoadingId(`${task.id}:download`);
    setError(undefined);
    try {
      const blob = await downloadImportExportTaskResultFile(task.id);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${task.bizNo}-result.json`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      setError("结果文件尚未生成或当前账号无下载权限");
    } finally {
      setActionLoadingId(undefined);
    }
  }

  const columns = useMemo<TableColumnsType<ImportExportTask>>(() => [
    {
      title: "任务编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 170,
      fixed: "left",
    },
    {
      title: "任务类型",
      dataIndex: "taskType",
      key: "taskType",
      width: 180,
      render: (value: ImportExportTask["taskType"]) => <Tag color="blue">{TASK_TYPE_LABELS[value] ?? value}</Tag>,
    },
    {
      title: "对象",
      dataIndex: "entityType",
      key: "entityType",
      width: 130,
      render: (value: string) => entityTypeLabel(value),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: ImportExportTask["status"]) => (
        <Tag color={statusColor(status)}>{TASK_STATUS_LABELS[status] ?? status}</Tag>
      ),
    },
    {
      title: "源文件",
      dataIndex: "sourceFileId",
      key: "sourceFileId",
      width: 110,
      render: (value?: string | null) => value ? <Tag color="blue">有</Tag> : <Tag>无</Tag>,
    },
    {
      title: "结果",
      dataIndex: "resultFileId",
      key: "resultFileId",
      width: 110,
      render: (value?: string | null) => value ? <Tag color="green">已生成</Tag> : <Tag>未生成</Tag>,
    },
    {
      title: "摘要",
      dataIndex: "errorSummary",
      key: "errorSummary",
      width: 260,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "请求时间",
      dataIndex: "requestedAt",
      key: "requestedAt",
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: "操作",
      key: "action",
      width: 260,
      fixed: "right",
      render: (_, task) => {
        const validateReason = taskActionReason(task, "validate", roles);
        const runReason = taskActionReason(task, "run", roles);
        const downloadReason = taskActionReason(task, "download", roles);
        return (
          <Space>
            <Tooltip title={validateReason}>
              <span>
                <Button
                  size="small"
                  disabled={Boolean(validateReason)}
                  loading={actionLoadingId === `${task.id}:validate`}
                  onClick={() => void runAction(task, "validate")}
                >
                  预校验
                </Button>
              </span>
            </Tooltip>
            <Tooltip title={runReason}>
              <span>
                <Button
                  size="small"
                  type="primary"
                  disabled={Boolean(runReason)}
                  loading={actionLoadingId === `${task.id}:run`}
                  onClick={() => void runAction(task, "run")}
                >
                  执行
                </Button>
              </span>
            </Tooltip>
            <Tooltip title={downloadReason}>
              <span>
                <Button
                  size="small"
                  disabled={Boolean(downloadReason)}
                  loading={actionLoadingId === `${task.id}:download`}
                  onClick={() => void downloadResult(task)}
                >
                  下载
                </Button>
              </span>
            </Tooltip>
          </Space>
        );
      },
    },
  ], [actionLoadingId, roles]);

  return (
    <section className="importExportPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">IMPORT / EXPORT</Typography.Text>
          <Typography.Title level={3}>导入导出任务</Typography.Title>
          <Typography.Text>管理主数据导入、清单导出、报告批量下载和结果文件。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="importExportGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>任务列表</Typography.Title>
            <Tag color="blue">{items.length} 个任务</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={items}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1430 }}
            locale={{ emptyText: "暂无导入导出任务" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>创建任务</Typography.Title>
          </div>
          <Form
            form={form}
            layout="vertical"
            initialValues={{ taskType: "LIST_EXPORT", entityType: "PROJECT", includeDeliveryLog: true }}
            onFinish={(values) => void submit(values)}
          >
            <Form.Item name="taskType" label="任务类型" rules={[{ required: true }]}>
              <Select options={TASK_TYPES.map((value) => ({ label: TASK_TYPE_LABELS[value] ?? value, value }))} disabled={!canCreate(roles)} />
            </Form.Item>
            <Form.Item name="entityType" label="对象类型" rules={[{ required: true }]}>
              <Select
                disabled={!canCreate(roles)}
                options={[
                  { label: "项目", value: "PROJECT" },
                  { label: "报告", value: "REPORT" },
                  { label: "客户", value: "CUSTOMER_ORG" },
                  { label: "审计日志", value: "AUDIT_LOG" },
                ]}
              />
            </Form.Item>
            <Form.Item label="源文件" required={isImportTask(taskType)}>
              <Input
                key={sourceFileInputKey}
                type="file"
                accept=".pdf,.docx,.xlsx,.csv,.json,.png,.jpg,.jpeg"
                disabled={!canCreate(roles) || !isImportTask(taskType)}
                onChange={(event) => setSourceFile(event.target.files?.[0])}
              />
              {sourceFile ? <Typography.Text type="secondary">{sourceFile.name}</Typography.Text> : null}
            </Form.Item>
            <Form.Item name="keyword" label="筛选关键字">
              <Input placeholder="项目号 / 报告号 / 客户关键字" disabled={!canCreate(roles)} />
            </Form.Item>
            <Form.Item name="includeDeliveryLog" valuePropName="checked">
              <Checkbox disabled={!canCreate(roles) || taskType !== "REPORT_BATCH_DOWNLOAD"}>包含报告发放日志</Checkbox>
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!canCreate(roles)}>
              创建任务
            </Button>
          </Form>
        </section>
      </section>
    </section>
  );
}

function buildTaskParameters(values: TaskFormValues): ImportExportTaskCreateRequest["parameters"] {
  const parameters: Record<string, unknown> = {};
  const keyword = values.keyword?.trim();
  if (keyword) {
    parameters.keyword = keyword;
  }
  if (values.taskType === "REPORT_BATCH_DOWNLOAD") {
    parameters.includeDeliveryLog = values.includeDeliveryLog ?? true;
  }
  return parameters;
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function canCreate(roles: readonly string[]) {
  return roles.includes("SYSTEM_ADMIN") || roles.includes("QUALITY_MANAGER") || roles.includes("ARCHIVE_MANAGER");
}

function canValidateOrRun(roles: readonly string[]) {
  return roles.includes("SYSTEM_ADMIN") || roles.includes("QUALITY_MANAGER");
}

function canDownload(roles: readonly string[]) {
  return roles.includes("SYSTEM_ADMIN") || roles.includes("QUALITY_MANAGER") || roles.includes("ARCHIVE_MANAGER");
}

function isImportTask(taskType: ImportExportTaskCreateRequest["taskType"]) {
  return taskType === "MASTER_IMPORT" || taskType === "MIGRATION_IMPORT";
}

function entityTypeLabel(value: string) {
  const labels: Record<string, string> = {
    PROJECT: "项目",
    REPORT: "报告",
    CUSTOMER_ORG: "客户",
    AUDIT_LOG: "审计日志",
  };
  return labels[value] ?? value;
}

function taskActionReason(task: ImportExportTask, action: "validate" | "run" | "download", roles: readonly string[]) {
  if (action === "download") {
    if (!canDownload(roles)) {
      return "当前账号没有导入导出结果下载权限";
    }
    if (!task.resultFileId) {
      return "任务结果文件尚未生成";
    }
    return undefined;
  }
  if (!canValidateOrRun(roles)) {
    return "当前账号没有导入导出任务执行权限";
  }
  if (task.status !== "PENDING") {
    return task.status === "SUCCESS"
      ? "任务已成功，只能下载或查看结果，不能重复执行"
      : `当前状态 ${task.status} 不允许执行`;
  }
  return undefined;
}

function statusColor(status: ImportExportTask["status"]) {
  if (status === "SUCCESS") {
    return "green";
  }
  if (status === "FAILED" || status === "EXPIRED") {
    return "red";
  }
  if (status === "RUNNING" || status === "VALIDATING") {
    return "blue";
  }
  return "orange";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}
