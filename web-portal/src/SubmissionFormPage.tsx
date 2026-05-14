import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from "antd";
import {
  collectDictionaryCodes,
  createSubmissionDraft,
  getSubmissionFormFields,
  loadDictionaryLookup,
  loadPublishedFormSchema,
  type DictionaryLookup,
  type FormSchema,
  type Submission,
  type SubmissionFormField,
  type SubmissionFormCode,
} from "@lims4wuxi/shared";

const SUBMISSION_FORM_CODE = "SUBMISSION_VER15" satisfies SubmissionFormCode;

type FieldValue = boolean | number | string | unknown[] | undefined;
type FormValues = Record<string, FieldValue>;

export function SubmissionFormPage() {
  const { schema, loading, error, reload } = useFormSchema(SUBMISSION_FORM_CODE);
  const fields = useMemo(
    () => (schema ? getSubmissionFormFields(schema.schemaJson) : []),
    [schema],
  );
  const dictionaries = useDictionaries(fields);

  if (loading) {
    return (
      <div className="pageState">
        <Spin />
      </div>
    );
  }

  if (error || !schema) {
    return (
      <div className="pageState">
        <Alert
          type="error"
          showIcon
          message="送检表单加载失败"
          action={<Button onClick={reload}>重试</Button>}
        />
      </div>
    );
  }

  return (
    <section className="formWorkspace">
      <header className="pageHeader">
        <div>
          <Typography.Title level={3}>新建送检单</Typography.Title>
          <Typography.Text type="secondary">{schema.formCode}</Typography.Text>
        </div>
        <Tag color="blue">v{schema.versionNo}</Tag>
      </header>
      <SchemaForm
        fields={fields}
        dictionaries={dictionaries}
        onSubmit={(values) => createSubmissionDraft(SUBMISSION_FORM_CODE, schema.versionNo, values)}
      />
    </section>
  );
}

function SchemaForm({
  fields,
  dictionaries,
  onSubmit,
}: {
  fields: SubmissionFormField[];
  dictionaries: DictionaryLookup;
  onSubmit: (values: Record<string, unknown>) => Promise<Submission>;
}) {
  const [form] = Form.useForm<FormValues>();
  const [saving, setSaving] = useState(false);
  const [submitError, setSubmitError] = useState<string>();
  const [createdSubmission, setCreatedSubmission] = useState<Submission>();
  const groups = useMemo(() => groupByPart(fields), [fields]);

  async function submit(values: FormValues) {
    setSaving(true);
    setSubmitError(undefined);
    setCreatedSubmission(undefined);
    try {
      const created = await onSubmit(normalizePayload(values, fields));
      setCreatedSubmission(created);
    } catch {
      setSubmitError("送检单保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card className="schemaFormSurface">
      <Form form={form} layout="vertical" requiredMark={false} onFinish={submit}>
        {groups.map((group) => (
          <section key={group.part} className="formPart">
            <div className="formPartHeader">
              <Typography.Title level={5}>{group.part}</Typography.Title>
              <Typography.Text type="secondary">{group.fields.length}</Typography.Text>
            </div>
            <Row gutter={[16, 4]}>
              {group.fields.map((field) => (
                <Col key={field.fieldKey} xs={24} md={columnSpan(field)} xl={columnSpan(field)}>
                  <Form.Item
                    name={field.fieldKey}
                    label={field.labelZh}
                    rules={field.required ? [{ required: true, message: `${field.labelZh}必填` }] : undefined}
                  >
                    {renderFieldControl(field, dictionaries)}
                  </Form.Item>
                </Col>
              ))}
            </Row>
          </section>
        ))}
        <Space direction="vertical" size={12} className="formFeedback">
          {submitError ? <Alert type="error" showIcon message={submitError} /> : null}
          {createdSubmission ? (
            <Alert type="success" showIcon message={`草稿 ${createdSubmission.bizNo} 已保存`} />
          ) : null}
        </Space>
        <div className="formActions">
          <Button onClick={() => form.resetFields()}>重置</Button>
          <Button type="primary" htmlType="submit" loading={saving}>
            保存草稿
          </Button>
        </div>
      </Form>
    </Card>
  );
}

function useFormSchema(formCode: SubmissionFormCode) {
  const [schema, setSchema] = useState<FormSchema>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>();
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(undefined);
    loadPublishedFormSchema(formCode)
      .then((result) => {
        if (active) {
          setSchema(result);
        }
      })
      .catch((caught) => {
        if (active) {
          setError(caught);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [formCode, reloadToken]);

  return {
    schema,
    loading,
    error,
    reload: () => setReloadToken((value) => value + 1),
  };
}

function useDictionaries(fields: readonly SubmissionFormField[]) {
  const dictionaryCodes = useMemo(() => collectDictionaryCodes(fields), [fields]);
  const dictionaryKey = dictionaryCodes.join("|");
  const [dictionaries, setDictionaries] = useState<DictionaryLookup>({});

  useEffect(() => {
    let active = true;
    if (dictionaryCodes.length === 0) {
      setDictionaries({});
      return () => {
        active = false;
      };
    }
    loadDictionaryLookup(dictionaryCodes).then((lookup) => {
      if (active) {
        setDictionaries(lookup);
      }
    });
    return () => {
      active = false;
    };
  }, [dictionaryKey]);

  return dictionaries;
}

function renderFieldControl(field: SubmissionFormField, dictionaries: DictionaryLookup) {
  if (field.type === "boolean") {
    const yesNoOptions = dictionaries[field.dictionary ?? "YES_NO"] ?? [];
    const yesLabel = yesNoOptions.find((item) => item.itemCode === "YES")?.labelZh ?? "是";
    const noLabel = yesNoOptions.find((item) => item.itemCode === "NO")?.labelZh ?? "否";
    return (
      <Select
        options={[
          { value: "true", label: yesLabel },
          { value: "false", label: noLabel },
        ]}
      />
    );
  }

  if (field.type === "enum") {
    return (
      <Select
        showSearch
        optionFilterProp="label"
        options={(dictionaries[field.dictionary ?? ""] ?? []).map((item) => ({
          value: item.itemCode,
          label: item.labelZh,
        }))}
      />
    );
  }

  if (field.type === "text" || field.type === "array") {
    return <Input.TextArea rows={field.type === "array" ? 4 : 3} />;
  }

  if (field.type === "date") {
    return <Input type="date" />;
  }

  if (field.type === "datetime") {
    return <Input type="datetime-local" />;
  }

  return <Input />;
}

function normalizePayload(values: FormValues, fields: readonly SubmissionFormField[]) {
  const payload: Record<string, unknown> = {};
  for (const field of fields) {
    const value = normalizeFieldValue(field, values[field.fieldKey]);
    if (value !== undefined) {
      payload[field.fieldKey] = value;
    }
  }
  return payload;
}

function normalizeFieldValue(field: SubmissionFormField, value: FieldValue) {
  if (value === undefined || value === "" || value === null) {
    return undefined;
  }
  if (field.type === "boolean") {
    return value === true || value === "true" || value === "YES";
  }
  if (field.type === "array") {
    return normalizeArrayValue(value);
  }
  return value;
}

function normalizeArrayValue(value: FieldValue) {
  if (Array.isArray(value)) {
    return value;
  }
  if (typeof value !== "string") {
    return value;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  try {
    const parsed = JSON.parse(trimmed) as unknown;
    return Array.isArray(parsed) ? parsed : [parsed];
  } catch {
    return trimmed
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => ({ value: line }));
  }
}

function groupByPart(fields: readonly SubmissionFormField[]) {
  const groups: { part: string; fields: SubmissionFormField[] }[] = [];
  for (const field of fields) {
    const group = groups.find((item) => item.part === field.part);
    if (group) {
      group.fields.push(field);
    } else {
      groups.push({ part: field.part, fields: [field] });
    }
  }
  return groups;
}

function columnSpan(field: SubmissionFormField) {
  return field.type === "text" || field.type === "array" ? 24 : 12;
}
