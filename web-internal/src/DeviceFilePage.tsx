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
  disableDeviceFile,
  listDeviceFiles,
  listDevices,
  uploadDeviceFile,
  useAuthStore,
  type Device,
  type DeviceFile,
  type DeviceFileUploadRequest,
} from "@lims4wuxi/shared";

const FILE_TYPES: DeviceFile["fileType"][] = [
  "MANUAL",
  "CALIBRATION_CERT",
  "VERIFICATION_CERT",
  "MAINTENANCE_RECORD",
  "SOP",
  "TRAINING_RECORD",
  "OTHER",
];

type UploadValues = {
  fileType: DeviceFileUploadRequest["fileType"];
  fileId: string;
  versionNo: string;
  validFrom?: string;
  validUntil?: string;
  isCurrent?: boolean;
};

export function DeviceFilePage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<UploadValues>();
  const [keyword, setKeyword] = useState("");
  const [devices, setDevices] = useState<Device[]>([]);
  const [files, setFiles] = useState<DeviceFile[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<Device>();
  const [loading, setLoading] = useState(true);
  const [fileLoading, setFileLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextKeyword = keyword) {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listDevices({
        page: 1,
        pageSize: 100,
        keyword: emptyToUndefined(nextKeyword),
      });
      const nextDevices = page.items ?? [];
      setDevices(nextDevices);
      const nextSelected = nextDevices.find((item) => item.id === selectedDevice?.id) ?? nextDevices[0];
      setSelectedDevice(nextSelected);
      if (nextSelected) {
        await loadFiles(nextSelected.id);
      } else {
        setFiles([]);
      }
    } catch {
      setError("设备台账加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadFiles(deviceId: string) {
    setFileLoading(true);
    try {
      const nextFiles = await listDeviceFiles(deviceId);
      setFiles(nextFiles ?? []);
    } catch {
      setError("设备文件加载失败");
    } finally {
      setFileLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function selectDevice(device: Device) {
    setSelectedDevice(device);
    await loadFiles(device.id);
  }

  async function submit(values: UploadValues) {
    if (!selectedDevice) {
      setError("请先选择设备");
      return;
    }
    setSubmitting(true);
    setError(undefined);
    setFeedback(undefined);
    const request: DeviceFileUploadRequest = {
      fileType: values.fileType,
      fileId: values.fileId.trim(),
      versionNo: values.versionNo.trim(),
      validFrom: emptyToUndefined(values.validFrom),
      validUntil: emptyToUndefined(values.validUntil),
      isCurrent: values.isCurrent ?? true,
    };
    try {
      const created = await uploadDeviceFile(selectedDevice.id, request);
      setFeedback(`设备文件 ${created.versionNo} 已登记`);
      form.resetFields();
      await loadFiles(selectedDevice.id);
    } catch {
      setError("设备文件登记失败，请确认文件 ID、类型和权限");
    } finally {
      setSubmitting(false);
    }
  }

  async function disableFile(file: DeviceFile) {
    setError(undefined);
    setFeedback(undefined);
    try {
      const updated = await disableDeviceFile(file.id);
      setFeedback(`设备文件 ${updated.versionNo} 已作废`);
      if (selectedDevice) {
        await loadFiles(selectedDevice.id);
      }
    } catch {
      setError("设备文件作废失败，请确认当前账号权限");
    }
  }

  const deviceColumns = useMemo<TableColumnsType<Device>>(() => [
    {
      title: "设备编号",
      dataIndex: "deviceNo",
      key: "deviceNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "设备名称",
      dataIndex: "name",
      key: "name",
      width: 180,
    },
    {
      title: "型号",
      dataIndex: "model",
      key: "model",
      width: 130,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "位置",
      dataIndex: "location",
      key: "location",
      width: 160,
    },
    {
      title: "计量",
      dataIndex: "metrologyRequired",
      key: "metrologyRequired",
      width: 90,
      render: (value: boolean) => value ? <Tag color="orange">需要</Tag> : <Tag>无需</Tag>,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: Device["status"]) => <Tag color={deviceStatusColor(status)}>{status}</Tag>,
    },
  ], []);

  const fileColumns = useMemo<TableColumnsType<DeviceFile>>(() => [
    {
      title: "类型",
      dataIndex: "fileType",
      key: "fileType",
      width: 170,
      render: (value: DeviceFile["fileType"]) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: "版本",
      dataIndex: "versionNo",
      key: "versionNo",
      width: 120,
    },
    {
      title: "当前",
      dataIndex: "isCurrent",
      key: "isCurrent",
      width: 90,
      render: (value: boolean) => value ? <Tag color="green">当前</Tag> : <Tag>历史</Tag>,
    },
    {
      title: "有效期",
      key: "validRange",
      width: 210,
      render: (_, file) => `${file.validFrom ?? "-"} 至 ${file.validUntil ?? "-"}`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status: DeviceFile["status"]) => <Tag color={fileStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "上传时间",
      dataIndex: "uploadedAt",
      key: "uploadedAt",
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      fixed: "right",
      render: (_, file) => {
        const reason = disableReason(file, roles);
        return (
          <Tooltip title={reason}>
            <span>
              <Button
                danger
                size="small"
                disabled={Boolean(reason)}
                onClick={() => void disableFile(file)}
              >
                作废
              </Button>
            </span>
          </Tooltip>
        );
      },
    },
  ], [roles]);

  return (
    <section className="deviceFilePage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">DEVICE FILES</Typography.Text>
          <Typography.Title level={3}>设备文件</Typography.Title>
          <Typography.Text>维护设备说明书、校准证书、SOP 和当前有效文件。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deviceFileGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>设备台账</Typography.Title>
            <Space wrap>
              <Input
                allowClear
                placeholder="设备编号 / 名称"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                onPressEnter={() => void load(keyword)}
                className="deviceSearchInput"
              />
              <Button type="primary" onClick={() => void load(keyword)}>查询</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={deviceColumns}
            dataSource={devices}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 820 }}
            rowClassName={(item) => item.id === selectedDevice?.id ? "selectedRow" : ""}
            onRow={(item) => ({ onClick: () => void selectDevice(item) })}
            locale={{ emptyText: "暂无设备" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <div>
              <Typography.Title level={5}>文件登记</Typography.Title>
              <Typography.Text>{selectedDevice ? `${selectedDevice.deviceNo} ${selectedDevice.name}` : "请选择设备"}</Typography.Text>
            </div>
            <Tag color="blue">{files.length} 份文件</Tag>
          </div>
          <Form
            form={form}
            layout="vertical"
            initialValues={{ fileType: "CALIBRATION_CERT", isCurrent: true }}
            onFinish={(values) => void submit(values)}
          >
            <div className="deviceFileFormGrid">
              <Form.Item name="fileType" label="文件类型" rules={[{ required: true }]}>
                <Select options={FILE_TYPES.map((value) => ({ label: value, value }))} disabled={!canUpload(roles)} />
              </Form.Item>
              <Form.Item name="versionNo" label="版本号" rules={[{ required: true }]}>
                <Input placeholder="v1.0" disabled={!canUpload(roles)} />
              </Form.Item>
              <Form.Item name="fileId" label="文件 ID" rules={[{ required: true }]}>
                <Input placeholder="file_metadata UUID" disabled={!canUpload(roles)} />
              </Form.Item>
              <Form.Item name="validFrom" label="有效开始">
                <Input type="date" disabled={!canUpload(roles)} />
              </Form.Item>
              <Form.Item name="validUntil" label="有效截止">
                <Input type="date" disabled={!canUpload(roles)} />
              </Form.Item>
              <Form.Item name="isCurrent" valuePropName="checked" className="deviceCurrentItem">
                <Checkbox disabled={!canUpload(roles)}>设为当前有效</Checkbox>
              </Form.Item>
            </div>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!selectedDevice || !canUpload(roles)}>
              登记文件
            </Button>
          </Form>
        </section>
      </section>

      <section className="deskPanel">
        <div className="deskPanelHeader">
          <Typography.Title level={5}>设备文件列表</Typography.Title>
          <Tag color={selectedDevice ? "green" : "default"}>{selectedDevice?.status ?? "未选择"}</Tag>
        </div>
        <Table
          rowKey="id"
          size="small"
          loading={fileLoading}
          columns={fileColumns}
          dataSource={files}
          pagination={{ pageSize: 6 }}
          scroll={{ x: 960 }}
          locale={{ emptyText: "请选择设备或登记文件" }}
        />
      </section>
    </section>
  );
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function canUpload(roles: readonly string[]) {
  return roles.includes("EQUIPMENT_MANAGER");
}

function canDisable(roles: readonly string[]) {
  return roles.includes("EQUIPMENT_MANAGER") || roles.includes("QUALITY_MANAGER");
}

function disableReason(file: DeviceFile, roles: readonly string[]) {
  if (!canDisable(roles)) {
    return "当前账号没有设备文件作废权限";
  }
  if (file.status === "DISABLED") {
    return "设备文件已作废，不能重复作废";
  }
  return undefined;
}

function deviceStatusColor(status: Device["status"]) {
  if (status === "ACTIVE") {
    return "green";
  }
  if (status === "MAINTENANCE") {
    return "orange";
  }
  if (status === "DISABLED") {
    return "red";
  }
  return "default";
}

function fileStatusColor(status?: DeviceFile["status"]) {
  return status === "DISABLED" ? "red" : "green";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}
