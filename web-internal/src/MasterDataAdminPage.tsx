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
  Tabs,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  listHolidays,
  listInternalUsers,
  listLegacyStatusMappings,
  listOrganizationUnits,
  listRoles,
  upsertHoliday,
  upsertLegacyStatusMapping,
  usePermission,
  type Holiday,
  type HolidayUpsertRequest,
  type InternalUser,
  type LegacyStatusMapping,
  type LegacyStatusMappingUpsertRequest,
  type OrganizationUnit,
  type Role,
} from "@lims4wuxi/shared";

const HOLIDAY_TYPES: Holiday["type"][] = ["HOLIDAY", "WORKDAY"];
const MIGRATION_MODES: LegacyStatusMapping["migrationMode"][] = ["ACTIVE_FLOW", "READONLY_ARCHIVE", "MANUAL_REVIEW"];

type HolidayFormValues = {
  date: string;
  type: Holiday["type"];
  name: string;
};

type LegacyMappingFormValues = {
  legacyEntityType: string;
  legacyStatusText: string;
  targetEntityType: string;
  targetStatus: string;
  migrationMode: LegacyStatusMapping["migrationMode"];
  qualityScoreDelta?: number;
  note?: string;
};

export function MasterDataAdminPage() {
  const canEditHoliday = usePermission("HOLIDAY_EDIT");
  const canEditMasterData = usePermission("MASTER_DATA_EDIT");
  const [holidayForm] = Form.useForm<HolidayFormValues>();
  const [mappingForm] = Form.useForm<LegacyMappingFormValues>();
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [organizationUnits, setOrganizationUnits] = useState<OrganizationUnit[]>([]);
  const [internalUsers, setInternalUsers] = useState<InternalUser[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [mappings, setMappings] = useState<LegacyStatusMapping[]>([]);
  const [loading, setLoading] = useState(true);
  const [submittingHoliday, setSubmittingHoliday] = useState(false);
  const [submittingMapping, setSubmittingMapping] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [nextHolidays, nextUnits, nextUsers, nextRoles, nextMappings] = await Promise.all([
        listHolidays(),
        listOrganizationUnits(),
        listInternalUsers({ page: 1, pageSize: 100 }),
        listRoles(),
        listLegacyStatusMappings(),
      ]);
      setHolidays(nextHolidays);
      setOrganizationUnits(nextUnits);
      setInternalUsers(nextUsers.items ?? []);
      setRoles(nextRoles);
      setMappings(nextMappings);
    } catch {
      setError("主数据加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function submitHoliday(values: HolidayFormValues) {
    setSubmittingHoliday(true);
    setError(undefined);
    setFeedback(undefined);
    const request: HolidayUpsertRequest = {
      id: crypto.randomUUID(),
      date: values.date,
      type: values.type,
      name: values.name.trim(),
    };
    try {
      const saved = await upsertHoliday(request);
      setFeedback(`节假日 ${saved.date} 已保存`);
      holidayForm.resetFields();
      await load();
    } catch {
      setError("节假日保存失败，请确认日期、类型和权限");
    } finally {
      setSubmittingHoliday(false);
    }
  }

  async function submitMapping(values: LegacyMappingFormValues) {
    setSubmittingMapping(true);
    setError(undefined);
    setFeedback(undefined);
    const request: LegacyStatusMappingUpsertRequest = {
      legacyEntityType: values.legacyEntityType.trim(),
      legacyStatusText: values.legacyStatusText.trim(),
      targetEntityType: values.targetEntityType.trim(),
      targetStatus: values.targetStatus.trim(),
      migrationMode: values.migrationMode,
      qualityScoreDelta: values.qualityScoreDelta,
      note: emptyToUndefined(values.note),
    };
    try {
      const saved = await upsertLegacyStatusMapping(request);
      setFeedback(`旧状态映射 ${saved.legacyEntityType}/${saved.legacyStatusText} 已保存`);
      mappingForm.resetFields();
      await load();
    } catch {
      setError("旧状态映射保存失败，请确认迁移模式、状态和权限");
    } finally {
      setSubmittingMapping(false);
    }
  }

  const holidayColumns = useMemo<TableColumnsType<Holiday>>(() => [
    { title: "日期", dataIndex: "date", key: "date", width: 140, fixed: "left" },
    {
      title: "类型",
      dataIndex: "type",
      key: "type",
      width: 130,
      render: (type: Holiday["type"]) => <Tag color={type === "HOLIDAY" ? "red" : "blue"}>{type}</Tag>,
    },
    { title: "名称", dataIndex: "name", key: "name", width: 180 },
  ], []);

  const unitColumns = useMemo<TableColumnsType<OrganizationUnit>>(() => [
    { title: "组织", dataIndex: "name", key: "name", width: 220, fixed: "left" },
    { title: "父级", dataIndex: "parentId", key: "parentId", width: 260, render: (value?: string | null) => value ?? "-" },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: OrganizationUnit["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
  ], []);

  const userColumns = useMemo<TableColumnsType<InternalUser>>(() => [
    { title: "账号", dataIndex: "username", key: "username", width: 180, fixed: "left" },
    { title: "姓名", dataIndex: "displayName", key: "displayName", width: 180 },
    {
      title: "角色",
      dataIndex: "roleCodes",
      key: "roleCodes",
      width: 360,
      render: (roleCodes: string[]) => (
        <Space size={4} wrap>{roleCodes.map((role) => <Tag key={role} color="blue">{role}</Tag>)}</Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: InternalUser["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
  ], []);

  const roleColumns = useMemo<TableColumnsType<Role>>(() => [
    { title: "角色编码", dataIndex: "roleCode", key: "roleCode", width: 240, fixed: "left" },
    { title: "角色名称", dataIndex: "roleName", key: "roleName", width: 180 },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: Role["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
  ], []);

  const mappingColumns = useMemo<TableColumnsType<LegacyStatusMapping>>(() => [
    { title: "旧实体", dataIndex: "legacyEntityType", key: "legacyEntityType", width: 150, fixed: "left" },
    { title: "旧状态", dataIndex: "legacyStatusText", key: "legacyStatusText", width: 170 },
    { title: "目标实体", dataIndex: "targetEntityType", key: "targetEntityType", width: 150 },
    { title: "目标状态", dataIndex: "targetStatus", key: "targetStatus", width: 210 },
    {
      title: "迁移模式",
      dataIndex: "migrationMode",
      key: "migrationMode",
      width: 190,
      render: (mode: LegacyStatusMapping["migrationMode"]) => <Tag color="blue">{mode}</Tag>,
    },
    { title: "质量分", dataIndex: "qualityScoreDelta", key: "qualityScoreDelta", width: 100, render: (value?: number | null) => value ?? 0 },
    { title: "备注", dataIndex: "note", key: "note", width: 240, render: (value?: string | null) => value ?? "-" },
  ], []);

  return (
    <section className="masterDataAdminPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">MASTER DATA</Typography.Text>
          <Typography.Title level={3}>主数据维护</Typography.Title>
          <Typography.Text>维护组织、用户角色、节假日和旧状态迁移映射。</Typography.Text>
        </div>
        <Button onClick={() => void load()}>刷新</Button>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <Tabs
        items={[
          {
            key: "calendar",
            label: "节假日",
            children: (
              <section className="masterDataAdminGrid">
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>节假日列表</Typography.Title>
                    <Tag color="blue">{holidays.length} 天</Tag>
                  </div>
                  <Table rowKey="id" size="small" loading={loading} columns={holidayColumns} dataSource={holidays} pagination={{ pageSize: 8 }} />
                </section>
                {canEditHoliday ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>保存节假日</Typography.Title>
                    </div>
                    <Form form={holidayForm} layout="vertical" initialValues={{ type: "HOLIDAY" }} onFinish={(values) => void submitHoliday(values)}>
                      <Form.Item name="date" label="日期" rules={[{ required: true }]}>
                        <Input type="date" />
                      </Form.Item>
                      <Form.Item name="type" label="类型" rules={[{ required: true }]}>
                        <Select options={HOLIDAY_TYPES.map((type) => ({ value: type, label: type }))} />
                      </Form.Item>
                      <Form.Item name="name" label="名称" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingHoliday} block>保存节假日</Button>
                    </Form>
                  </section>
                ) : null}
              </section>
            ),
          },
          {
            key: "org",
            label: "组织用户",
            children: (
              <section className="masterDataAdminGrid">
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>组织架构</Typography.Title>
                    <Tag color="blue">{organizationUnits.length} 个组织</Tag>
                  </div>
                  <Table rowKey="id" size="small" loading={loading} columns={unitColumns} dataSource={organizationUnits} pagination={{ pageSize: 8 }} scroll={{ x: 590 }} />
                </section>
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>内部用户</Typography.Title>
                    <Tag color="blue">{internalUsers.length} 个用户</Tag>
                  </div>
                  <Table rowKey="id" size="small" loading={loading} columns={userColumns} dataSource={internalUsers} pagination={{ pageSize: 8 }} scroll={{ x: 830 }} />
                </section>
                <section className="deskPanel masterDataWidePanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>角色</Typography.Title>
                    <Tag color="blue">{roles.length} 个角色</Tag>
                  </div>
                  <Table rowKey="roleCode" size="small" loading={loading} columns={roleColumns} dataSource={roles} pagination={{ pageSize: 8 }} scroll={{ x: 530 }} />
                </section>
              </section>
            ),
          },
          {
            key: "legacy",
            label: "旧状态映射",
            children: (
              <section className="masterDataAdminGrid">
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>映射列表</Typography.Title>
                    <Tag color="blue">{mappings.length} 条</Tag>
                  </div>
                  <Table
                    rowKey={(record) => `${record.legacyEntityType}:${record.legacyStatusText}`}
                    size="small"
                    loading={loading}
                    columns={mappingColumns}
                    dataSource={mappings}
                    pagination={{ pageSize: 8 }}
                    scroll={{ x: 1210 }}
                  />
                </section>
                {canEditMasterData ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>保存映射</Typography.Title>
                    </div>
                    <Form
                      form={mappingForm}
                      layout="vertical"
                      initialValues={{ migrationMode: "MANUAL_REVIEW", qualityScoreDelta: 0 }}
                      onFinish={(values) => void submitMapping(values)}
                    >
                      <Form.Item name="legacyEntityType" label="旧实体" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="legacyStatusText" label="旧状态" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="targetEntityType" label="目标实体" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="targetStatus" label="目标状态" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="migrationMode" label="迁移模式" rules={[{ required: true }]}>
                        <Select options={MIGRATION_MODES.map((mode) => ({ value: mode, label: mode }))} />
                      </Form.Item>
                      <Form.Item name="qualityScoreDelta" label="质量分">
                        <InputNumber className="fullWidthInput" />
                      </Form.Item>
                      <Form.Item name="note" label="备注">
                        <Input.TextArea rows={2} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingMapping} block>保存映射</Button>
                    </Form>
                  </section>
                ) : null}
              </section>
            ),
          },
        ]}
      />
    </section>
  );
}

function emptyToUndefined(value?: string | null) {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  return value.trim();
}
