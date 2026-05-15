import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
  type TableColumnsType,
} from "antd";
import {
  listAuditLogs,
  listBusinessRules,
  listSystemConfigs,
  upsertBusinessRule,
  upsertSystemConfig,
  usePermission,
  type AuditLog,
  type BusinessRule,
  type BusinessRuleUpsertRequest,
  type SystemConfig,
  type SystemConfigUpsertRequest,
} from "@lims4wuxi/shared";

const VALUE_TYPES: SystemConfigUpsertRequest["valueType"][] = ["INT", "DECIMAL", "STRING", "BOOLEAN", "JSON"];

type ConfigFormValues = {
  configKey: string;
  configValue: string;
  valueType: SystemConfigUpsertRequest["valueType"];
  category: string;
  editableByRole: string;
  descriptionZh?: string;
};

type RuleFormValues = {
  ruleCode: string;
  nameZh: string;
  triggerType: string;
  conditionExpr: string;
  actionCode: string;
  actionPayloadJson?: string;
  priority: number;
  enabled: boolean;
  note?: string;
};

export function SystemGovernancePage() {
  const canEdit = usePermission("SYSTEM_CONFIG");
  const canReadAudit = usePermission("AUDIT_LOG_READ");
  const [configForm] = Form.useForm<ConfigFormValues>();
  const [ruleForm] = Form.useForm<RuleFormValues>();
  const [configs, setConfigs] = useState<SystemConfig[]>([]);
  const [rules, setRules] = useState<BusinessRule[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [auditLoading, setAuditLoading] = useState(false);
  const [submittingConfig, setSubmittingConfig] = useState(false);
  const [submittingRule, setSubmittingRule] = useState(false);
  const [feedback, setFeedback] = useState<string>();
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const [nextConfigs, nextRules] = await Promise.all([listSystemConfigs(), listBusinessRules()]);
      setConfigs(nextConfigs);
      setRules(nextRules);
      if (canReadAudit) {
        await loadAuditLogs();
      }
    } catch {
      setError("系统治理数据加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadAuditLogs() {
    setAuditLoading(true);
    try {
      const page = await listAuditLogs({ page: 1, pageSize: 100 });
      setAuditLogs(page.items ?? []);
    } catch {
      setError("审计日志加载失败");
    } finally {
      setAuditLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function submitConfig(values: ConfigFormValues) {
    setSubmittingConfig(true);
    setError(undefined);
    setFeedback(undefined);
    const request: SystemConfigUpsertRequest = {
      configKey: values.configKey.trim(),
      configValue: values.configValue.trim(),
      valueType: values.valueType,
      category: values.category.trim(),
      editableByRole: values.editableByRole.trim(),
      descriptionZh: emptyToUndefined(values.descriptionZh),
    };
    try {
      const saved = await upsertSystemConfig(request);
      setFeedback(`系统配置 ${saved.configKey} 已保存`);
      configForm.resetFields();
      await load();
    } catch {
      setError("系统配置保存失败，请确认值类型、权限和必填项");
    } finally {
      setSubmittingConfig(false);
    }
  }

  async function submitRule(values: RuleFormValues) {
    setSubmittingRule(true);
    setError(undefined);
    setFeedback(undefined);
    try {
      const request: BusinessRuleUpsertRequest = {
        ruleCode: values.ruleCode.trim(),
        nameZh: values.nameZh.trim(),
        triggerType: values.triggerType.trim(),
        conditionExpr: values.conditionExpr.trim(),
        actionCode: values.actionCode.trim(),
        actionPayload: parsePayload(values.actionPayloadJson),
        priority: values.priority,
        enabled: values.enabled,
        note: emptyToUndefined(values.note),
      };
      const saved = await upsertBusinessRule(request);
      setFeedback(`业务规则 ${saved.ruleCode} 已保存`);
      ruleForm.resetFields();
      await load();
    } catch {
      setError("业务规则保存失败，请确认 JSON、表达式和权限");
    } finally {
      setSubmittingRule(false);
    }
  }

  const configColumns = useMemo<TableColumnsType<SystemConfig>>(() => [
    {
      title: "键",
      dataIndex: "configKey",
      key: "configKey",
      width: 260,
      fixed: "left",
    },
    {
      title: "值",
      dataIndex: "configValue",
      key: "configValue",
      width: 220,
      ellipsis: true,
    },
    {
      title: "类型",
      dataIndex: "valueType",
      key: "valueType",
      width: 110,
      render: (value: SystemConfig["valueType"]) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: "分类",
      dataIndex: "category",
      key: "category",
      width: 160,
    },
    {
      title: "可编辑角色",
      dataIndex: "editableByRole",
      key: "editableByRole",
      width: 180,
    },
    {
      title: "说明",
      dataIndex: "descriptionZh",
      key: "descriptionZh",
      width: 260,
      render: (value?: string | null) => value ?? "-",
    },
  ], []);

  const ruleColumns = useMemo<TableColumnsType<BusinessRule>>(() => [
    {
      title: "规则编码",
      dataIndex: "ruleCode",
      key: "ruleCode",
      width: 220,
      fixed: "left",
    },
    {
      title: "名称",
      dataIndex: "nameZh",
      key: "nameZh",
      width: 220,
    },
    {
      title: "触发点",
      dataIndex: "triggerType",
      key: "triggerType",
      width: 180,
    },
    {
      title: "表达式",
      dataIndex: "conditionExpr",
      key: "conditionExpr",
      width: 260,
      ellipsis: true,
    },
    {
      title: "动作",
      dataIndex: "actionCode",
      key: "actionCode",
      width: 190,
    },
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 100,
    },
    {
      title: "状态",
      dataIndex: "enabled",
      key: "enabled",
      width: 100,
      render: (enabled: boolean) => <Tag color={enabled ? "green" : "default"}>{enabled ? "ENABLED" : "DISABLED"}</Tag>,
    },
  ], []);

  const auditColumns = useMemo<TableColumnsType<AuditLog>>(() => [
    {
      title: "时间",
      dataIndex: "occurredAt",
      key: "occurredAt",
      width: 180,
      fixed: "left",
      render: (value: string) => formatDateTime(value),
    },
    {
      title: "结果",
      dataIndex: "result",
      key: "result",
      width: 110,
      render: (value: AuditLog["result"]) => <Tag color={value === "SUCCESS" ? "green" : "red"}>{value}</Tag>,
    },
    {
      title: "动作",
      dataIndex: "actionCode",
      key: "actionCode",
      width: 220,
    },
    {
      title: "端",
      dataIndex: "clientType",
      key: "clientType",
      width: 120,
    },
    {
      title: "实体",
      key: "entity",
      width: 260,
      render: (_, log) => log.entityType ? `${log.entityType}:${log.entityId ?? "-"}` : "-",
    },
    {
      title: "失败原因",
      dataIndex: "failureReason",
      key: "failureReason",
      width: 260,
      render: (value?: string | null) => value ?? "-",
    },
    {
      title: "追踪",
      dataIndex: "traceId",
      key: "traceId",
      width: 180,
      render: (value?: string | null) => value ?? "-",
    },
  ], []);

  return (
    <section className="systemGovernancePage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SYSTEM GOVERNANCE</Typography.Text>
          <Typography.Title level={3}>系统治理</Typography.Title>
          <Typography.Text>维护系统配置、业务规则并查看写操作审计。</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={() => void load()}>刷新</Button>
        </Space>
      </header>

      {feedback ? <Alert type="success" message={feedback} showIcon className="deskFeedback" /> : null}
      {error ? <Alert type="error" message={error} showIcon className="deskFeedback" /> : null}

      <Tabs
        items={[
          {
            key: "configs",
            label: "系统配置",
            children: (
              <section className="systemGovernanceGrid">
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>配置列表</Typography.Title>
                    <Tag color="blue">{configs.length} 项</Tag>
                  </div>
                  <Table
                    rowKey="id"
                    size="small"
                    loading={loading}
                    columns={configColumns}
                    dataSource={configs}
                    pagination={{ pageSize: 8 }}
                    scroll={{ x: 1210 }}
                    locale={{ emptyText: "暂无系统配置" }}
                  />
                </section>
                {canEdit ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>保存配置</Typography.Title>
                    </div>
                    <Form
                      form={configForm}
                      layout="vertical"
                      initialValues={{ valueType: "STRING", category: "SYSTEM", editableByRole: "SYSTEM_ADMIN" }}
                      onFinish={(values) => void submitConfig(values)}
                    >
                      <Form.Item name="configKey" label="配置键" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="configValue" label="配置值" rules={[{ required: true }]}>
                        <Input.TextArea rows={2} />
                      </Form.Item>
                      <Form.Item name="valueType" label="值类型" rules={[{ required: true }]}>
                        <Select options={VALUE_TYPES.map((value) => ({ value, label: value }))} />
                      </Form.Item>
                      <Form.Item name="category" label="分类" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="editableByRole" label="可编辑角色" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="descriptionZh" label="说明">
                        <Input.TextArea rows={2} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingConfig} block>
                        保存配置
                      </Button>
                    </Form>
                  </section>
                ) : null}
              </section>
            ),
          },
          {
            key: "rules",
            label: "业务规则",
            children: (
              <section className="systemGovernanceGrid">
                <section className="deskPanel">
                  <div className="deskPanelHeader">
                    <Typography.Title level={5}>规则列表</Typography.Title>
                    <Tag color="blue">{rules.length} 条</Tag>
                  </div>
                  <Table
                    rowKey="id"
                    size="small"
                    loading={loading}
                    columns={ruleColumns}
                    dataSource={rules}
                    pagination={{ pageSize: 8 }}
                    scroll={{ x: 1270 }}
                    locale={{ emptyText: "暂无业务规则" }}
                  />
                </section>
                {canEdit ? (
                  <section className="deskPanel">
                    <div className="deskPanelHeader">
                      <Typography.Title level={5}>保存规则</Typography.Title>
                    </div>
                    <Form
                      form={ruleForm}
                      layout="vertical"
                      initialValues={{ priority: 100, enabled: true, actionPayloadJson: "{}" }}
                      onFinish={(values) => void submitRule(values)}
                    >
                      <Form.Item name="ruleCode" label="规则编码" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="nameZh" label="名称" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="triggerType" label="触发点" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="conditionExpr" label="条件表达式" rules={[{ required: true }]}>
                        <Input.TextArea rows={2} />
                      </Form.Item>
                      <Form.Item name="actionCode" label="动作编码" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="actionPayloadJson" label="动作参数 JSON">
                        <Input.TextArea rows={3} />
                      </Form.Item>
                      <Form.Item name="priority" label="优先级" rules={[{ required: true }]}>
                        <InputNumber className="fullWidthInput" min={0} />
                      </Form.Item>
                      <Form.Item name="enabled" label="启用" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                      <Form.Item name="note" label="备注">
                        <Input.TextArea rows={2} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={submittingRule} block>
                        保存规则
                      </Button>
                    </Form>
                  </section>
                ) : null}
              </section>
            ),
          },
          {
            key: "audit",
            label: "审计日志",
            children: canReadAudit ? (
              <section className="deskPanel">
                <div className="deskPanelHeader">
                  <Typography.Title level={5}>审计日志</Typography.Title>
                  <Button size="small" onClick={() => void loadAuditLogs()}>刷新日志</Button>
                </div>
                <Table
                  rowKey="id"
                  size="small"
                  loading={auditLoading}
                  columns={auditColumns}
                  dataSource={auditLogs}
                  pagination={{ pageSize: 10 }}
                  scroll={{ x: 1330 }}
                  locale={{ emptyText: "暂无审计日志" }}
                />
              </section>
            ) : null,
          },
        ]}
      />
    </section>
  );
}

function parsePayload(value?: string) {
  if (!value || value.trim() === "") {
    return undefined;
  }
  return JSON.parse(value) as Record<string, unknown>;
}

function emptyToUndefined(value?: string | null) {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  return value.trim();
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
