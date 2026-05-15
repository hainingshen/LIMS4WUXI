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
  createInternalUser,
  listHolidays,
  listInternalUsers,
  listLegacyStatusMappings,
  listOrganizationUnits,
  listRoles,
  replaceInternalUserRoles,
  resetInternalUserPassword,
  updateInternalUserStatus,
  updateRoleStatus,
  upsertHoliday,
  upsertLegacyStatusMapping,
  usePermission,
  type Holiday,
  type HolidayUpsertRequest,
  type InternalUserCreateRequest,
  type InternalUser,
  type InternalUserRolesReplaceRequest,
  type InternalUserPasswordResetRequest,
  type LegacyStatusMapping,
  type LegacyStatusMappingUpsertRequest,
  type OrganizationUnit,
  type Role,
  type StatusUpdateRequest,
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

type UserCreateFormValues = {
  username: string;
  displayName: string;
  email?: string;
  organizationUnitId?: string;
  password: string;
  roleCodes: string[];
};

type UserActionFormValues = {
  userId: string;
  roleCodes?: string[];
  password?: string;
  status?: StatusUpdateRequest["status"];
};

type RoleActionFormValues = {
  roleCode: string;
  status: StatusUpdateRequest["status"];
};

export function MasterDataAdminPage() {
  const canEditHoliday = usePermission("HOLIDAY_EDIT");
  const canEditMasterData = usePermission("MASTER_DATA_EDIT");
  const [holidayForm] = Form.useForm<HolidayFormValues>();
  const [mappingForm] = Form.useForm<LegacyMappingFormValues>();
  const [userCreateForm] = Form.useForm<UserCreateFormValues>();
  const [userActionForm] = Form.useForm<UserActionFormValues>();
  const [roleActionForm] = Form.useForm<RoleActionFormValues>();
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [organizationUnits, setOrganizationUnits] = useState<OrganizationUnit[]>([]);
  const [internalUsers, setInternalUsers] = useState<InternalUser[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [mappings, setMappings] = useState<LegacyStatusMapping[]>([]);
  const [loading, setLoading] = useState(true);
  const [submittingHoliday, setSubmittingHoliday] = useState(false);
  const [submittingMapping, setSubmittingMapping] = useState(false);
  const [submittingUserCreate, setSubmittingUserCreate] = useState(false);
  const [submittingUserAction, setSubmittingUserAction] = useState(false);
  const [submittingRoleAction, setSubmittingRoleAction] = useState(false);
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

  async function submitUserCreate(values: UserCreateFormValues) {
    setSubmittingUserCreate(true);
    setError(undefined);
    setFeedback(undefined);
    const request: InternalUserCreateRequest = {
      username: values.username.trim(),
      displayName: values.displayName.trim(),
      email: emptyToUndefined(values.email),
      organizationUnitId: emptyToUndefined(values.organizationUnitId),
      password: values.password,
      roleCodes: values.roleCodes,
    };
    try {
      const saved = await createInternalUser(request);
      setFeedback(`内部用户 ${saved.username} 已创建`);
      userCreateForm.resetFields();
      await load();
    } catch {
      setError("内部用户创建失败，请确认用户名、组织、角色和权限");
    } finally {
      setSubmittingUserCreate(false);
    }
  }

  async function submitUserRoles() {
    setSubmittingUserAction(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const values = await userActionForm.validateFields(["userId", "roleCodes"]);
      const request: InternalUserRolesReplaceRequest = { roleCodes: values.roleCodes ?? [] };
      const saved = await replaceInternalUserRoles(values.userId, request);
      setFeedback(`内部用户 ${saved.username} 角色已更新`);
      await load();
    } catch (err) {
      if (!isFormValidationError(err)) {
        setError("角色更新失败，请确认用户、角色和权限");
      }
    } finally {
      setSubmittingUserAction(false);
    }
  }

  async function submitUserPasswordReset() {
    setSubmittingUserAction(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const values = await userActionForm.validateFields(["userId", "password"]);
      const request: InternalUserPasswordResetRequest = { password: values.password ?? "" };
      const saved = await resetInternalUserPassword(values.userId, request);
      setFeedback(`内部用户 ${saved.username} 密码已重置`);
      userActionForm.setFieldValue("password", undefined);
      await load();
    } catch (err) {
      if (!isFormValidationError(err)) {
        setError("密码重置失败，请确认用户、密码长度和权限");
      }
    } finally {
      setSubmittingUserAction(false);
    }
  }

  async function submitUserStatus() {
    setSubmittingUserAction(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const values = await userActionForm.validateFields(["userId", "status"]);
      const request: StatusUpdateRequest = { status: values.status ?? "ACTIVE" };
      const saved = await updateInternalUserStatus(values.userId, request);
      setFeedback(`内部用户 ${saved.username} 状态已更新为 ${saved.status}`);
      await load();
    } catch (err) {
      if (!isFormValidationError(err)) {
        setError("用户状态更新失败，请确认用户、状态和权限");
      }
    } finally {
      setSubmittingUserAction(false);
    }
  }

  async function submitRoleStatus(values: RoleActionFormValues) {
    setSubmittingRoleAction(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const saved = await updateRoleStatus(values.roleCode, { status: values.status });
      setFeedback(`角色 ${saved.roleCode} 状态已更新为 ${saved.status}`);
      await load();
    } catch {
      setError("角色状态更新失败，请确认角色、状态和权限");
    } finally {
      setSubmittingRoleAction(false);
    }
  }

  const activeUnitOptions = useMemo(() => organizationUnits
      .filter((unit) => unit.status === "ACTIVE")
      .map((unit) => ({ value: unit.id, label: unit.name })),
  [organizationUnits]);

  const activeInternalRoleOptions = useMemo(() => roles
      .filter((role) => role.side === "INTERNAL" && role.status === "ACTIVE")
      .map((role) => ({ value: role.roleCode, label: `${role.roleCode} ${role.roleName}` })),
  [roles]);

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
    { title: "邮箱", dataIndex: "email", key: "email", width: 220, render: (value?: string | null) => value ?? "-" },
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
    {
      title: "操作",
      key: "action",
      width: 120,
      render: (_, record) => canEditMasterData ? (
        <Button
          size="small"
          onClick={() => userActionForm.setFieldsValue({
            userId: record.id,
            roleCodes: record.roleCodes,
            status: record.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
          })}
        >
          配置
        </Button>
      ) : null,
    },
  ], [canEditMasterData, userActionForm]);

  const roleColumns = useMemo<TableColumnsType<Role>>(() => [
    { title: "角色编码", dataIndex: "roleCode", key: "roleCode", width: 240, fixed: "left" },
    { title: "角色名称", dataIndex: "roleName", key: "roleName", width: 180 },
    {
      title: "侧别",
      dataIndex: "side",
      key: "side",
      width: 110,
      render: (side: Role["side"]) => <Tag color={side === "INTERNAL" ? "blue" : "cyan"}>{side}</Tag>,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: Role["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 120,
      render: (_, record) => canEditMasterData ? (
        <Button
          size="small"
          onClick={() => roleActionForm.setFieldsValue({
            roleCode: record.roleCode,
            status: record.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
          })}
        >
          配置
        </Button>
      ) : null,
    },
  ], [canEditMasterData, roleActionForm]);

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
                  <Table rowKey="id" size="small" loading={loading} columns={userColumns} dataSource={internalUsers} pagination={{ pageSize: 8 }} scroll={{ x: 990 }} />
                </section>
                {canEditMasterData ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>创建内部用户</Typography.Title>
                    </div>
                    <Form
                      form={userCreateForm}
                      layout="vertical"
                      initialValues={{ roleCodes: ["INSPECTOR"] }}
                      onFinish={(values) => void submitUserCreate(values)}
                    >
                      <Form.Item name="username" label="账号" rules={[{ required: true }]}>
                        <Input autoComplete="off" />
                      </Form.Item>
                      <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="email" label="邮箱">
                        <Input type="email" />
                      </Form.Item>
                      <Form.Item name="organizationUnitId" label="组织">
                        <Select allowClear options={activeUnitOptions} />
                      </Form.Item>
                      <Form.Item name="roleCodes" label="角色" rules={[{ required: true }]}>
                        <Select mode="multiple" options={activeInternalRoleOptions} />
                      </Form.Item>
                      <Form.Item name="password" label="初始密码" rules={[{ required: true, min: 8 }]}>
                        <Input.Password autoComplete="new-password" />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingUserCreate} block>创建用户</Button>
                    </Form>
                  </section>
                ) : null}
                {canEditMasterData ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>用户操作</Typography.Title>
                    </div>
                    <Form form={userActionForm} layout="vertical" initialValues={{ status: "ACTIVE" }}>
                      <Form.Item name="userId" label="用户" rules={[{ required: true }]}>
                        <Select
                          showSearch
                          optionFilterProp="label"
                          options={internalUsers.map((user) => ({
                            value: user.id,
                            label: `${user.username} ${user.displayName}`,
                          }))}
                        />
                      </Form.Item>
                      <Form.Item name="roleCodes" label="角色" rules={[{ required: true }]}>
                        <Select mode="multiple" options={activeInternalRoleOptions} />
                      </Form.Item>
                      <Space.Compact block>
                        <Button onClick={() => void submitUserRoles()} loading={submittingUserAction}>更新角色</Button>
                      </Space.Compact>
                      <Form.Item name="status" label="状态" rules={[{ required: true }]} className="masterDataActionField">
                        <Select options={["ACTIVE", "INACTIVE"].map((status) => ({ value: status, label: status }))} />
                      </Form.Item>
                      <Button onClick={() => void submitUserStatus()} loading={submittingUserAction} block>更新状态</Button>
                      <Form.Item name="password" label="新密码" rules={[{ required: true }, { min: 8 }]} className="masterDataActionField">
                        <Input.Password autoComplete="new-password" />
                      </Form.Item>
                      <Button onClick={() => void submitUserPasswordReset()} loading={submittingUserAction} block>重置密码</Button>
                    </Form>
                  </section>
                ) : null}
                <section className="deskPanel masterDataWidePanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>角色</Typography.Title>
                    <Tag color="blue">{roles.length} 个角色</Tag>
                  </div>
                  <Table rowKey="roleCode" size="small" loading={loading} columns={roleColumns} dataSource={roles} pagination={{ pageSize: 8 }} scroll={{ x: 760 }} />
                </section>
                {canEditMasterData ? (
                  <section className="deskPanel masterDataWidePanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>角色状态</Typography.Title>
                    </div>
                    <Form
                      form={roleActionForm}
                      layout="inline"
                      initialValues={{ status: "ACTIVE" }}
                      onFinish={(values) => void submitRoleStatus(values)}
                    >
                      <Form.Item name="roleCode" label="角色" rules={[{ required: true }]}>
                        <Select
                          className="masterDataInlineSelect"
                          showSearch
                          optionFilterProp="label"
                          options={roles.map((role) => ({
                            value: role.roleCode,
                            label: `${role.roleCode} ${role.roleName}`,
                          }))}
                        />
                      </Form.Item>
                      <Form.Item name="status" label="状态" rules={[{ required: true }]}>
                        <Select className="masterDataInlineSelect" options={["ACTIVE", "INACTIVE"].map((status) => ({ value: status, label: status }))} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingRoleAction}>保存角色状态</Button>
                    </Form>
                  </section>
                ) : null}
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

function isFormValidationError(error: unknown) {
  return typeof error === "object" && error != null && "errorFields" in error;
}
