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
  createPortalUser,
  listCustomerOrgs,
  listPortalUsers,
  upsertCustomerOrg,
  usePermission,
  type CustomerOrg,
  type CustomerOrgUpsertRequest,
  type PortalUser,
  type PortalUserCreateRequest,
} from "@lims4wuxi/shared";

const PORTAL_ROLES = ["CUSTOMER_ORG_ADMIN", "CUSTOMER_APPLICANT", "CUSTOMER_VIEWER"];

type CustomerOrgFormValues = {
  nameZh: string;
  nameEn?: string;
  addressZh: string;
  addressEn?: string;
  taxNo?: string;
  invoiceTitle?: string;
};

type PortalUserFormValues = {
  customerOrgId: string;
  username: string;
  displayName: string;
  email?: string;
  roleCodes: string[];
};

export function CustomerManagementPage() {
  const canEdit = usePermission("MASTER_DATA_EDIT");
  const [orgForm] = Form.useForm<CustomerOrgFormValues>();
  const [userForm] = Form.useForm<PortalUserFormValues>();
  const [keyword, setKeyword] = useState("");
  const [orgs, setOrgs] = useState<CustomerOrg[]>([]);
  const [portalUsers, setPortalUsers] = useState<PortalUser[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<CustomerOrg>();
  const [loading, setLoading] = useState(true);
  const [usersLoading, setUsersLoading] = useState(false);
  const [submittingOrg, setSubmittingOrg] = useState(false);
  const [submittingUser, setSubmittingUser] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load(nextKeyword = keyword, preferredOrgId = selectedOrg?.id) {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listCustomerOrgs({
        page: 1,
        pageSize: 100,
        keyword: emptyToUndefined(nextKeyword),
      });
      const nextOrgs = page.items ?? [];
      setOrgs(nextOrgs);
      const nextSelected = nextOrgs.find((item) => item.id === preferredOrgId) ?? nextOrgs[0];
      setSelectedOrg(nextSelected);
      if (nextSelected) {
        userForm.setFieldsValue({ customerOrgId: nextSelected.id });
        await loadPortalUsers(nextSelected.id);
      } else {
        setPortalUsers([]);
      }
    } catch {
      setError("客户单位加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadPortalUsers(customerOrgId?: string) {
    setUsersLoading(true);
    try {
      const page = await listPortalUsers({ customerOrgId, page: 1, pageSize: 100 });
      setPortalUsers(page.items ?? []);
    } catch {
      setError("门户账号加载失败");
    } finally {
      setUsersLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function selectOrg(org: CustomerOrg) {
    setSelectedOrg(org);
    userForm.setFieldsValue({ customerOrgId: org.id });
    await loadPortalUsers(org.id);
  }

  async function submitOrg(values: CustomerOrgFormValues) {
    setSubmittingOrg(true);
    setError(undefined);
    setFeedback(undefined);
    const request: CustomerOrgUpsertRequest = {
      nameZh: values.nameZh.trim(),
      nameEn: emptyToUndefined(values.nameEn),
      addressZh: values.addressZh.trim(),
      addressEn: emptyToUndefined(values.addressEn),
      taxNo: emptyToUndefined(values.taxNo),
      invoiceTitle: emptyToUndefined(values.invoiceTitle),
    };
    try {
      const saved = await upsertCustomerOrg(request);
      setFeedback(`客户单位 ${saved.nameZh} 已保存`);
      orgForm.resetFields();
      await load(keyword, saved.id);
    } catch {
      setError("客户单位保存失败，请确认必填项和权限");
    } finally {
      setSubmittingOrg(false);
    }
  }

  async function submitPortalUser(values: PortalUserFormValues) {
    setSubmittingUser(true);
    setError(undefined);
    setFeedback(undefined);
    const request: PortalUserCreateRequest = {
      customerOrgId: values.customerOrgId,
      username: values.username.trim(),
      displayName: values.displayName.trim(),
      email: emptyToUndefined(values.email),
      roleCodes: values.roleCodes,
    };
    try {
      const created = await createPortalUser(request);
      setFeedback(`门户账号 ${created.username} 已创建`);
      userForm.resetFields(["username", "displayName", "email", "roleCodes"]);
      await loadPortalUsers(values.customerOrgId);
    } catch {
      setError("门户账号创建失败，请确认用户名唯一、客户单位和角色");
    } finally {
      setSubmittingUser(false);
    }
  }

  const orgColumns = useMemo<TableColumnsType<CustomerOrg>>(() => [
    {
      title: "客户单位",
      dataIndex: "nameZh",
      key: "nameZh",
      width: 220,
      fixed: "left",
    },
    {
      title: "英文名",
      dataIndex: "nameEn",
      key: "nameEn",
      width: 180,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: CustomerOrg["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
    {
      title: "税号",
      dataIndex: "taxNo",
      key: "taxNo",
      width: 160,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "发票抬头",
      dataIndex: "invoiceTitle",
      key: "invoiceTitle",
      width: 220,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "地址",
      dataIndex: "addressZh",
      key: "addressZh",
      width: 260,
    },
  ], []);

  const userColumns = useMemo<TableColumnsType<PortalUser>>(() => [
    {
      title: "账号",
      dataIndex: "username",
      key: "username",
      width: 180,
      fixed: "left",
    },
    {
      title: "姓名",
      dataIndex: "displayName",
      key: "displayName",
      width: 160,
    },
    {
      title: "邮箱",
      dataIndex: "email",
      key: "email",
      width: 220,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "角色",
      dataIndex: "roleCodes",
      key: "roleCodes",
      width: 300,
      render: (roleCodes: string[]) => (
        <Space size={4} wrap>
          {roleCodes.map((roleCode) => <Tag key={roleCode} color="blue">{roleCode}</Tag>)}
        </Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: PortalUser["status"]) => <Tag color={status === "ACTIVE" ? "green" : "default"}>{status}</Tag>,
    },
  ], []);

  return (
    <section className="customerManagementPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">CUSTOMER MASTER DATA</Typography.Text>
          <Typography.Title level={3}>客户主数据</Typography.Title>
          <Typography.Text>维护委托单位、发票信息和客户门户账号。</Typography.Text>
        </div>
        <Space wrap>
          <Input.Search
            className="customerSearchInput"
            allowClear
            placeholder="客户单位 / 税号"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) => {
              setKeyword(value);
              void load(value);
            }}
          />
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <section className="customerManagementGrid">
        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>客户单位</Typography.Title>
            <Tag color="blue">{orgs.length} 个单位</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={orgColumns}
            dataSource={orgs}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1150 }}
            locale={{ emptyText: "暂无客户单位" }}
            rowClassName={(record) => record.id === selectedOrg?.id ? "selectedRow" : ""}
            onRow={(record) => ({ onClick: () => void selectOrg(record) })}
          />
        </section>

        <section className="deskPanel">
          <div className="deskPanelHeader">
            <Typography.Title level={5}>门户账号</Typography.Title>
            <Tag>{selectedOrg?.nameZh ?? "未选择"}</Tag>
          </div>
          <Table
            rowKey="id"
            size="small"
            loading={usersLoading}
            columns={userColumns}
            dataSource={portalUsers}
            pagination={{ pageSize: 6 }}
            scroll={{ x: 970 }}
            locale={{ emptyText: "暂无门户账号" }}
          />
        </section>
      </section>

      {canEdit ? (
        <section className="customerManagementFormGrid">
          <section className="deskPanel">
            <div className="deskPanelHeader">
              <Typography.Title level={5}>新增客户单位</Typography.Title>
            </div>
            <Form form={orgForm} layout="vertical" onFinish={(values) => void submitOrg(values)}>
              <Form.Item name="nameZh" label="客户单位" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="nameEn" label="英文名">
                <Input />
              </Form.Item>
              <Form.Item name="addressZh" label="中文地址" rules={[{ required: true }]}>
                <Input.TextArea rows={2} />
              </Form.Item>
              <Form.Item name="addressEn" label="英文地址">
                <Input.TextArea rows={2} />
              </Form.Item>
              <Form.Item name="taxNo" label="税号">
                <Input />
              </Form.Item>
              <Form.Item name="invoiceTitle" label="发票抬头">
                <Input />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={submittingOrg} block>
                保存客户单位
              </Button>
            </Form>
          </section>

          <section className="deskPanel">
            <div className="deskPanelHeader">
              <Typography.Title level={5}>新增门户账号</Typography.Title>
            </div>
            <Form
              form={userForm}
              layout="vertical"
              initialValues={{ roleCodes: ["CUSTOMER_APPLICANT"] }}
              onFinish={(values) => void submitPortalUser(values)}
            >
              <Form.Item name="customerOrgId" label="客户单位" rules={[{ required: true }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  options={orgs.map((org) => ({ value: org.id, label: org.nameZh }))}
                />
              </Form.Item>
              <Form.Item name="username" label="账号" rules={[{ required: true }]}>
                <Input autoComplete="off" />
              </Form.Item>
              <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="email" label="邮箱">
                <Input />
              </Form.Item>
              <Form.Item name="roleCodes" label="角色" rules={[{ required: true }]}>
                <Select
                  mode="multiple"
                  options={PORTAL_ROLES.map((role) => ({ value: role, label: role }))}
                />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={submittingUser} block>
                创建门户账号
              </Button>
            </Form>
          </section>
        </section>
      ) : null}
    </section>
  );
}

function emptyToUndefined(value?: string | null) {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  return value.trim();
}
