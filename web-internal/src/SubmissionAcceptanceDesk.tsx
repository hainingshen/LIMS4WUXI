import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Space, Table, Tag, Typography, type TableColumnsType } from "antd";
import {
  acceptSubmission,
  listSubmissions,
  type Submission,
} from "@lims4wuxi/shared";

type LoadingAction = {
  submissionId: string;
  action: "accept";
};

export function SubmissionAcceptanceDesk() {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<LoadingAction>();
  const [error, setError] = useState<string>();
  const [acceptedSubmission, setAcceptedSubmission] = useState<Submission>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listSubmissions({ page: 1, pageSize: 50 });
      setSubmissions(page.items ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setError("待受理送检单加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function accept(record: Submission) {
    setAction({ submissionId: record.id, action: "accept" });
    setError(undefined);
    setAcceptedSubmission(undefined);
    try {
      const accepted = await acceptSubmission(record.id);
      setAcceptedSubmission(accepted);
      await load();
    } catch {
      setError("受理失败，请确认送检单状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  const columns = useMemo<TableColumnsType<Submission>>(() => [
    {
      title: "送检单号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 160,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: Submission["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    {
      title: "GLP",
      dataIndex: "requiresGlp",
      key: "requiresGlp",
      width: 92,
      render: (value: boolean) => value ? <Tag color="green">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "报告语言",
      dataIndex: "reportLanguage",
      key: "reportLanguage",
      width: 110,
    },
    {
      title: "协议语言",
      dataIndex: "protocolLanguage",
      key: "protocolLanguage",
      width: 110,
    },
    {
      title: "提交数量",
      dataIndex: "submittedQuantityText",
      key: "submittedQuantityText",
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 120,
      fixed: "right",
      render: (_, record) => (
        <Button
          type="primary"
          size="small"
          disabled={record.status !== "SUBMITTED"}
          loading={action?.submissionId === record.id && action.action === "accept"}
          onClick={() => accept(record)}
        >
          受理
        </Button>
      ),
    },
  ], [action]);

  return (
    <section className="submissionDesk">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">SUBMISSION ACCEPTANCE</Typography.Text>
          <Typography.Title level={3}>送检受理</Typography.Title>
          <Typography.Text type="secondary">
            查看客户已提交的送检单，完成内部受理并进入项目拆分准备。
          </Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{total} 待处理</Tag>
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      <Space direction="vertical" size={12} className="deskFeedback">
        {error ? <Alert type="error" showIcon message={error} /> : null}
        {acceptedSubmission ? (
          <Alert type="success" showIcon message={`送检单 ${acceptedSubmission.bizNo} 已受理`} />
        ) : null}
      </Space>

      <Card className="workCenterPanel">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={submissions}
          loading={loading}
          pagination={false}
          scroll={{ x: 920 }}
          locale={{ emptyText: "暂无待受理送检单" }}
        />
      </Card>
    </section>
  );
}

function statusColor(status: Submission["status"]) {
  if (status === "SUBMITTED") {
    return "gold";
  }
  if (status === "ACCEPTED") {
    return "green";
  }
  if (status === "CANCELLED") {
    return "red";
  }
  return "blue";
}
