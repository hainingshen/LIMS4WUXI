import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  listDeviceBookings,
  listDevices,
  overrideDeviceBooking,
  useAuthStore,
  type Device,
  type DeviceBooking,
  type DeviceBookingOverrideRequest,
} from "@lims4wuxi/shared";

type OverrideValues = {
  reason: string;
  replacementStartAt: string;
  replacementEndAt: string;
};

export function DeviceBookingPage() {
  const roles = useAuthStore((state) => state.roles);
  const [form] = Form.useForm<OverrideValues>();
  const [devices, setDevices] = useState<Device[]>([]);
  const [deviceId, setDeviceId] = useState<string>();
  const [bookings, setBookings] = useState<DeviceBooking[]>([]);
  const [selectedBooking, setSelectedBooking] = useState<DeviceBooking>();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextDeviceId = deviceId) {
    setLoading(true);
    setError(undefined);
    try {
      const [devicePage, bookingPage] = await Promise.all([
        listDevices({ page: 1, pageSize: 100 }),
        listDeviceBookings({ page: 1, pageSize: 100, deviceId: nextDeviceId }),
      ]);
      const nextDevices = devicePage.items ?? [];
      const nextBookings = bookingPage.items ?? [];
      setDevices(nextDevices);
      setBookings(nextBookings);
      setSelectedBooking((current) => nextBookings.find((item) => item.id === current?.id) ?? nextBookings[0]);
    } catch {
      setError("设备预约加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function changeDevice(nextDeviceId?: string) {
    setDeviceId(nextDeviceId);
    await load(nextDeviceId);
  }

  async function submit(values: OverrideValues) {
    if (!selectedBooking) {
      setError("请先选择设备预约");
      return;
    }
    setSubmitting(true);
    setError(undefined);
    setFeedback(undefined);
    const request: DeviceBookingOverrideRequest = {
      reason: values.reason.trim(),
      replacementStartAt: toIso(values.replacementStartAt),
      replacementEndAt: toIso(values.replacementEndAt),
    };
    try {
      const updated = await overrideDeviceBooking(selectedBooking.id, request);
      setFeedback(`预约 ${updated.bizNo} 已标记待重约`);
      form.resetFields();
      await load(deviceId);
    } catch {
      setError("覆盖预约失败，请确认预约类型、时间和权限");
    } finally {
      setSubmitting(false);
    }
  }

  const columns = useMemo<TableColumnsType<DeviceBooking>>(() => [
    {
      title: "预约号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "设备",
      dataIndex: "deviceId",
      key: "deviceId",
      width: 230,
      render: (value: string) => deviceName(devices, value),
    },
    {
      title: "预约人",
      dataIndex: "bookerType",
      key: "bookerType",
      width: 130,
      render: (value: DeviceBooking["bookerType"]) => <Tag color={value === "EXTERNAL_USER" ? "orange" : "blue"}>{value}</Tag>,
    },
    {
      title: "时间",
      key: "time",
      width: 300,
      render: (_, booking) => `${formatDateTime(booking.startAt)} - ${formatDateTime(booking.endAt)}`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 140,
      render: (status: DeviceBooking["status"]) => <Tag color={bookingStatusColor(status)}>{status}</Tag>,
    },
    {
      title: "用途",
      dataIndex: "purpose",
      key: "purpose",
      width: 220,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "覆盖原因",
      dataIndex: "overrideReason",
      key: "overrideReason",
      width: 220,
      render: (value?: string | null) => value ?? "-",
    },
  ], [devices]);

  const overrideEnabled = canOverride(roles)
      && selectedBooking?.bookerType === "EXTERNAL_USER"
      && selectedBooking.status === "BOOKED";

  return (
    <section className="deviceBookingPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">DEVICE BOOKINGS</Typography.Text>
          <Typography.Title level={3}>设备预约</Typography.Title>
          <Typography.Text>按设备查看半小时粒度预约，覆盖外部预约时记录原因并置为待重约。</Typography.Text>
        </div>
        <Space wrap>
          <Select
            allowClear
            placeholder="全部设备"
            value={deviceId}
            onChange={(value) => void changeDevice(value)}
            className="deviceBookingDeviceSelect"
            options={devices.map((device) => ({ label: `${device.deviceNo} ${device.name}`, value: device.id }))}
          />
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="deviceBookingGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>预约列表</Typography.Title>
            <Tag color="blue">{bookings.length} 条</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={bookings}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1390 }}
            rowClassName={(item) => item.id === selectedBooking?.id ? "selectedRow" : ""}
            onRow={(item) => ({ onClick: () => setSelectedBooking(item) })}
            locale={{ emptyText: "暂无设备预约" }}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <div>
              <Typography.Title level={5}>覆盖外部预约</Typography.Title>
              <Typography.Text>
                {selectedBooking ? `${selectedBooking.bizNo} ${selectedBooking.status}` : "请选择外部预约"}
              </Typography.Text>
            </div>
          </div>
          <Form form={form} layout="vertical" onFinish={(values) => void submit(values)}>
            <Form.Item name="reason" label="覆盖原因" rules={[{ required: true }]}>
              <Input disabled={!overrideEnabled} />
            </Form.Item>
            <Form.Item name="replacementStartAt" label="替代开始时间" rules={[{ required: true }]}>
              <Input type="datetime-local" disabled={!overrideEnabled} />
            </Form.Item>
            <Form.Item name="replacementEndAt" label="替代结束时间" rules={[{ required: true }]}>
              <Input type="datetime-local" disabled={!overrideEnabled} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!overrideEnabled}>
              覆盖并通知重约
            </Button>
          </Form>
        </section>
      </section>
    </section>
  );
}

function deviceName(devices: readonly Device[], id: string) {
  const device = devices.find((item) => item.id === id);
  return device ? `${device.deviceNo} ${device.name}` : id;
}

function canOverride(roles: readonly string[]) {
  return roles.includes("EQUIPMENT_MANAGER") || roles.includes("TECHNICAL_LEADER");
}

function bookingStatusColor(status: DeviceBooking["status"]) {
  if (status === "BOOKED") {
    return "green";
  }
  if (status === "NEEDS_RESCHEDULE" || status === "OVERRIDDEN") {
    return "orange";
  }
  if (status === "CANCELLED") {
    return "red";
  }
  return "default";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}

function toIso(value: string) {
  return new Date(value).toISOString();
}
