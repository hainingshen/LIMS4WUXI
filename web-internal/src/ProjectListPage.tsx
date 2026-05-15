import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Space, Table, Tag, Typography, type TableColumnsType } from "antd";
import { listProjects, type Project } from "@lims4wuxi/shared";

export function ProjectListPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listProjects({ page: 1, pageSize: 50 });
      setProjects(page.items ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setError("项目列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const columns = useMemo<TableColumnsType<Project>>(() => [
    {
      title: "项目编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "项目状态",
      dataIndex: "status",
      key: "status",
      width: 150,
      render: (status: Project["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    {
      title: "项目类型",
      dataIndex: "projectType",
      key: "projectType",
      width: 170,
      render: (value: Project["projectType"]) => projectTypeLabel(value),
    },
    {
      title: "资质",
      dataIndex: "qualificationType",
      key: "qualificationType",
      width: 100,
      render: (value: Project["qualificationType"]) => value ?? "-",
    },
    {
      title: "GLP",
      dataIndex: "requiresGlp",
      key: "requiresGlp",
      width: 86,
      render: (value: boolean) => value ? <Tag color="green">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "动物试验",
      dataIndex: "requiresAnimalTest",
      key: "requiresAnimalTest",
      width: 104,
      render: (value: boolean) => value ? <Tag color="orange">YES</Tag> : <Tag>NO</Tag>,
    },
    {
      title: "付款状态",
      dataIndex: "paymentStatus",
      key: "paymentStatus",
      width: 120,
      render: (value: Project["paymentStatus"]) => value ?? "-",
    },
    {
      title: "送检单 ID",
      dataIndex: "submissionId",
      key: "submissionId",
      ellipsis: true,
    },
  ], []);

  return (
    <section className="projectListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">PROJECT LIST</Typography.Text>
          <Typography.Title level={3}>项目详情</Typography.Title>
          <Typography.Text type="secondary">
            查看已拆分项目的类型、资质、GLP 和付款状态，后续承接评审、报价、合同、样品与报告流程。
          </Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{total} 项目</Tag>
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      {error ? <Alert className="deskFeedback" type="error" showIcon message={error} /> : null}

      <Card className="workCenterPanel">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={projects}
          loading={loading}
          pagination={false}
          scroll={{ x: 980 }}
          locale={{ emptyText: "暂无项目" }}
        />
      </Card>
    </section>
  );
}

function projectTypeLabel(projectType: Project["projectType"]) {
  if (projectType === "STD_ACCREDITED") {
    return "标准方法 + 有资质";
  }
  if (projectType === "STD_NON_ACCREDITED") {
    return "标准方法 + 无资质";
  }
  return "非标方法";
}

function statusColor(status: Project["status"]) {
  if (status === "ACCEPTED" || status === "QUOTABLE") {
    return "green";
  }
  if (status === "NONSTANDARD_REVIEW" || status === "PAYMENT_HOLD") {
    return "gold";
  }
  if (status === "ARCHIVED") {
    return "blue";
  }
  return "geekblue";
}
