import { useEffect, useMemo, useState, type Key } from "react";
import {
  Alert,
  Button,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  createReportRegistryBatchDownload,
  listReportDeliveryLogs,
  searchReportRegistry,
  useAuthStore,
  type ImportExportTask,
  type ReportDeliveryLog,
  type ReportRegistryItem,
  type ReportRegistryVersion,
} from "@lims4wuxi/shared";

const REPORT_STATUSES: ReportRegistryItem["status"][] = [
  "ISSUED_PAYMENT_LOCKED",
  "GREEN_RELEASED",
  "RELEASED",
  "ARCHIVED",
  "VOIDED",
];

const REPORT_STATUS_LABELS: Partial<Record<ReportRegistryItem["status"], string>> = {
  DRAFT: "草稿",
  PENDING_GENERATE: "待生成",
  QC_REVIEWING: "QC 审核",
  QA_REVIEWING: "QA 审核",
  SD_REVIEWING: "SD 审核",
  PENDING_ISSUE: "待签发",
  ISSUED_PAYMENT_LOCKED: "已签发待付款",
  GREEN_RELEASED: "绿色通道放行",
  RELEASED: "已发放",
  ARCHIVED: "已归档",
  REVISION_DRAFT: "重签草稿",
  VOIDED: "已作废",
};

const GATE_LABELS: Record<ReportRegistryItem["paymentGateStatus"], string> = {
  LOCKED: "付款锁定",
  GREEN_UNLOCKED: "绿色通道",
  UNLOCKED: "已解锁",
};

const DELIVERY_LABELS: Record<ReportDeliveryLog["deliveryType"], string> = {
  PREVIEW: "预览",
  OFFICIAL_DOWNLOAD: "正式下载",
  INTERNAL_DOWNLOAD: "内部下载",
  EMAIL: "邮件发送",
  GREEN_CHANNEL_RELEASE: "绿色放行",
  PAYMENT_UNLOCK_RELEASE: "付款解锁",
  BATCH_DOWNLOAD: "批量下载",
};

const VERSION_STATUS_LABELS: Record<ReportRegistryVersion["status"], string> = {
  DRAFT: "草稿",
  ISSUED: "已签发",
  VOIDED: "已作废",
  SUPERSEDED: "已替换",
};

type RegistryFilters = {
  keyword?: string;
  status?: ReportRegistryItem["status"];
};

export function ReportRegistryPage() {
  const roles = useAuthStore((state) => state.roles);
  const [filters, setFilters] = useState<RegistryFilters>({});
  const [items, setItems] = useState<ReportRegistryItem[]>([]);
  const [logs, setLogs] = useState<ReportDeliveryLog[]>([]);
  const [selectedReport, setSelectedReport] = useState<ReportRegistryItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [loading, setLoading] = useState(true);
  const [logLoading, setLogLoading] = useState(false);
  const [batchLoading, setBatchLoading] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextFilters: RegistryFilters = filters) {
    setLoading(true);
    setError(undefined);
    try {
      const page = await searchReportRegistry({
        page: 1,
        pageSize: 100,
        keyword: emptyToUndefined(nextFilters.keyword),
        status: nextFilters.status,
      });
      const nextItems = page.items ?? [];
      setItems(nextItems);
      setSelectedRowKeys((current) => current.filter((key) => nextItems.some((item) => item.reportId === key)));
      const nextSelected = nextItems.find((item) => item.reportId === selectedReport?.reportId) ?? nextItems[0];
      setSelectedReport(nextSelected);
      if (nextSelected) {
        await loadLogs(nextSelected.reportId);
      } else {
        setLogs([]);
      }
    } catch {
      setError("报告注册中心加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadLogs(reportId: string) {
    setLogLoading(true);
    try {
      const page = await listReportDeliveryLogs({ reportId, page: 1, pageSize: 100 });
      setLogs(page.items ?? []);
    } catch {
      setError("发放日志加载失败");
    } finally {
      setLogLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function selectReport(item: ReportRegistryItem) {
    setSelectedReport(item);
    await loadLogs(item.reportId);
  }

  async function runBatchDownload() {
    if (!selectedRowKeys.length) {
      setError("请先选择报告");
      return;
    }
    setBatchLoading(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const task: ImportExportTask = await createReportRegistryBatchDownload({
        reportIds: selectedRowKeys.map(String),
        includeDeliveryLog: true,
      });
      setFeedback(`批量下载任务 ${task.bizNo} 已创建，状态 ${task.status}`);
    } catch {
      setError("批量下载任务创建失败，请确认当前账号权限");
    } finally {
      setBatchLoading(false);
    }
  }

  const itemColumns = useMemo<TableColumnsType<ReportRegistryItem>>(() => [
    {
      title: "报告编号",
      dataIndex: "reportBizNo",
      key: "reportBizNo",
      width: 170,
      fixed: "left",
    },
    {
      title: "项目编号",
      dataIndex: "projectBizNo",
      key: "projectBizNo",
      width: 170,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 170,
      render: (status: ReportRegistryItem["status"]) => <Tag color={statusColor(status)}>{REPORT_STATUS_LABELS[status] ?? status}</Tag>,
    },
    {
      title: "门禁",
      dataIndex: "paymentGateStatus",
      key: "paymentGateStatus",
      width: 150,
      render: (status: ReportRegistryItem["paymentGateStatus"]) => <Tag color={gateColor(status)}>{GATE_LABELS[status] ?? status}</Tag>,
    },
    {
      title: "模板",
      key: "template",
      width: 190,
      render: (_, item) => `${item.templateCode ?? "-"} ${item.templateVersion ?? ""}`.trim(),
    },
    {
      title: "当前版本",
      dataIndex: "currentVersionNo",
      key: "currentVersionNo",
      width: 110,
      render: (value: number) => `V${value}`,
    },
    {
      title: "发放",
      dataIndex: "deliveryCount",
      key: "deliveryCount",
      width: 90,
      render: (value?: number) => value ?? 0,
    },
    {
      title: "签发时间",
      dataIndex: "issuedAt",
      key: "issuedAt",
      width: 170,
      render: (value?: string | null) => formatDateTime(value),
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      fixed: "right",
      render: (_, item) => (
        <Button size="small" onClick={() => void selectReport(item)}>
          查看
        </Button>
      ),
    },
  ], []);

  const versionColumns = useMemo<TableColumnsType<ReportRegistryVersion>>(() => [
    {
      title: "版本",
      dataIndex: "versionNo",
      key: "versionNo",
      width: 90,
      render: (value: number) => `V${value}`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 130,
      render: (status: ReportRegistryVersion["status"]) => <Tag>{VERSION_STATUS_LABELS[status] ?? status}</Tag>,
    },
    {
      title: "DOCX",
      dataIndex: "docxFileId",
      key: "docxFileId",
      width: 120,
      render: (value?: string | null) => value ? <Tag color="blue">已生成</Tag> : <Tag>无</Tag>,
    },
    {
      title: "PDF",
      dataIndex: "pdfFileId",
      key: "pdfFileId",
      width: 120,
      render: (value?: string | null) => value ? <Tag color="green">已生成</Tag> : <Tag>无</Tag>,
    },
    {
      title: "签发时间",
      dataIndex: "issuedAt",
      key: "issuedAt",
      width: 170,
      render: (value?: string | null) => formatDateTime(value),
    },
  ], []);

  const logColumns = useMemo<TableColumnsType<ReportDeliveryLog>>(() => [
    {
      title: "类型",
      dataIndex: "deliveryType",
      key: "deliveryType",
      width: 150,
      render: (value: ReportDeliveryLog["deliveryType"]) => <Tag color={deliveryColor(value)}>{DELIVERY_LABELS[value] ?? value}</Tag>,
    },
    {
      title: "客户端",
      dataIndex: "clientType",
      key: "clientType",
      width: 110,
      render: (value?: ReportDeliveryLog["clientType"]) => clientTypeLabel(value),
    },
    {
      title: "门禁",
      dataIndex: "paymentGateStatus",
      key: "paymentGateStatus",
      width: 150,
      render: (value?: ReportDeliveryLog["paymentGateStatus"]) => value ? <Tag color={gateColor(value)}>{GATE_LABELS[value] ?? value}</Tag> : "-",
    },
    {
      title: "文件",
      dataIndex: "fileId",
      key: "fileId",
      width: 120,
      render: (value?: string | null) => value ? <Tag color="green">有</Tag> : <Tag>无</Tag>,
    },
    {
      title: "发放时间",
      dataIndex: "deliveredAt",
      key: "deliveredAt",
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
  ], []);

  return (
    <section className="reportRegistryPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">REPORT REGISTRY</Typography.Text>
          <Typography.Title level={3}>报告注册中心</Typography.Title>
          <Typography.Text>检索正式报告、版本链和发放日志。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
          <Button
            type="primary"
            disabled={!canBatchDownload(roles)}
            loading={batchLoading}
            onClick={() => void runBatchDownload()}
          >
            批量下载
          </Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>报告检索</Typography.Title>
          <Space wrap>
            <Input
              allowClear
              placeholder="报告 / 项目 / 模板"
              value={filters.keyword}
              onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value }))}
              onPressEnter={() => void load(filters)}
              className="registrySearchInput"
            />
            <Select
              allowClear
              placeholder="状态"
              value={filters.status}
              onChange={(status) => setFilters((current) => ({ ...current, status }))}
              options={REPORT_STATUSES.map((status) => ({ label: REPORT_STATUS_LABELS[status] ?? status, value: status }))}
              className="registryStatusSelect"
            />
            <Button type="primary" onClick={() => void load(filters)}>查询</Button>
          </Space>
        </div>
        <Table
          rowKey="reportId"
          size="small"
          loading={loading}
          columns={itemColumns}
          dataSource={items}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
            getCheckboxProps: () => ({ disabled: !canBatchDownload(roles) }),
          }}
          onRow={(item) => ({ onClick: () => void selectReport(item) })}
          rowClassName={(item) => item.reportId === selectedReport?.reportId ? "selectedRow" : ""}
          scroll={{ x: 1320 }}
          pagination={{ pageSize: 8 }}
          locale={{ emptyText: "暂无报告注册记录" }}
        />
      </section>

      <section className="reportRegistryDetailGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>版本链</Typography.Title>
            <Tag color="blue">{selectedReport?.versions?.length ?? 0} 个版本</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            columns={versionColumns}
            dataSource={selectedReport?.versions ?? []}
            pagination={false}
            locale={{ emptyText: "请选择报告" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>发放日志</Typography.Title>
            <Tag color="blue">{logs.length} 条</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={logLoading}
            columns={logColumns}
            dataSource={logs}
            pagination={{ pageSize: 5 }}
            scroll={{ x: 700 }}
            locale={{ emptyText: "暂无发放日志" }}
          />
        </section>
      </section>
    </section>
  );
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function canBatchDownload(roles: readonly string[]) {
  return roles.includes("ARCHIVE_MANAGER") || roles.includes("QUALITY_MANAGER");
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}

function statusColor(status: ReportRegistryItem["status"]) {
  if (status === "RELEASED") {
    return "green";
  }
  if (status === "GREEN_RELEASED") {
    return "cyan";
  }
  if (status === "ISSUED_PAYMENT_LOCKED") {
    return "orange";
  }
  if (status === "VOIDED") {
    return "red";
  }
  return "default";
}

function gateColor(status: ReportRegistryItem["paymentGateStatus"] | ReportDeliveryLog["paymentGateStatus"]) {
  if (status === "LOCKED") {
    return "red";
  }
  if (status === "GREEN_UNLOCKED") {
    return "cyan";
  }
  return "green";
}

function deliveryColor(value: ReportDeliveryLog["deliveryType"]) {
  if (value === "GREEN_CHANNEL_RELEASE") {
    return "cyan";
  }
  if (value === "OFFICIAL_DOWNLOAD" || value === "INTERNAL_DOWNLOAD" || value === "BATCH_DOWNLOAD") {
    return "green";
  }
  if (value === "PAYMENT_UNLOCK_RELEASE") {
    return "gold";
  }
  return "blue";
}

function clientTypeLabel(value?: ReportDeliveryLog["clientType"]) {
  if (!value) {
    return "-";
  }
  const labels: Record<NonNullable<ReportDeliveryLog["clientType"]>, string> = {
    INTERNAL: "内部端",
    PORTAL: "客户门户",
    MOBILE: "移动端",
    SYSTEM: "系统",
  };
  return labels[value] ?? value;
}
