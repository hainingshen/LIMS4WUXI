import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Space, Table, Tag, Typography, type TableColumnsType } from "antd";
import { listContracts, markContractSigned, useAuthStore, type Contract } from "@lims4wuxi/shared";

type ContractAction = {
  contractId: string;
  action: "sign";
};

export function ContractListPage() {
  const roles = useAuthStore((state) => state.roles);
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<ContractAction>();
  const [error, setError] = useState<string>();
  const [feedback, setFeedback] = useState<string>();

  async function load() {
    setLoading(true);
    setError(undefined);
    try {
      const page = await listContracts({ page: 1, pageSize: 50 });
      setContracts(page.items ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setError("合同列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function markSigned(contract: Contract) {
    setAction({ contractId: contract.id, action: "sign" });
    setError(undefined);
    setFeedback(undefined);
    try {
      const signed = await markContractSigned(contract.id, { signedAt: new Date().toISOString() });
      setFeedback(`合同 ${signed.bizNo} 已确认签署`);
      await load();
    } catch {
      setError("签署确认失败，请确认合同状态和当前账号权限");
    } finally {
      setAction(undefined);
    }
  }

  const columns = useMemo<TableColumnsType<Contract>>(() => [
    {
      title: "合同编号",
      dataIndex: "bizNo",
      key: "bizNo",
      width: 150,
      fixed: "left",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 130,
      render: (status: Contract["status"]) => <Tag color={statusColor(status)}>{status}</Tag>,
    },
    {
      title: "合同类型",
      dataIndex: "contractType",
      key: "contractType",
      width: 190,
      render: (value: Contract["contractType"]) => contractTypeLabel(value),
    },
    {
      title: "模板",
      dataIndex: "templateCode",
      key: "templateCode",
      width: 180,
      render: (value: string | undefined) => value ?? "-",
    },
    {
      title: "签署时间",
      dataIndex: "signedAt",
      key: "signedAt",
      width: 210,
      render: (value: string | null | undefined) => value ?? "-",
    },
    {
      title: "项目 ID",
      dataIndex: "projectId",
      key: "projectId",
      ellipsis: true,
    },
    {
      title: "操作",
      key: "action",
      width: 130,
      fixed: "right",
      render: (_, contract) => (
        <Button
          type="primary"
          size="small"
          disabled={!canMarkSigned(contract, roles)}
          loading={action?.contractId === contract.id && action.action === "sign"}
          onClick={() => markSigned(contract)}
        >
          确认签署
        </Button>
      ),
    },
  ], [action, roles]);

  return (
    <section className="contractListPage">
      <header className="pageHeader">
        <div>
          <Typography.Text className="pageEyebrow">CONTRACT LIST</Typography.Text>
          <Typography.Title level={3}>合同协议</Typography.Title>
          <Typography.Text type="secondary">
            查看报价后的合同协议，登记签署后推进项目进入已签约并锁定送检核心字段。
          </Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{total} 合同</Tag>
          <Button onClick={load}>刷新</Button>
        </Space>
      </header>

      <Space direction="vertical" size={12} className="deskFeedback">
        {error ? <Alert type="error" showIcon message={error} /> : null}
        {feedback ? <Alert type="success" showIcon message={feedback} /> : null}
      </Space>

      <Card className="workCenterPanel">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={contracts}
          loading={loading}
          pagination={false}
          scroll={{ x: 1040 }}
          locale={{ emptyText: "暂无合同" }}
        />
      </Card>
    </section>
  );
}

function canMarkSigned(contract: Contract, roles: readonly string[]) {
  return roles.includes("SALES_REP") && contract.status === "PENDING_SIGN";
}

function contractTypeLabel(contractType: Contract["contractType"]) {
  if (contractType === "TEST_AGREEMENT") {
    return "检测协议";
  }
  if (contractType === "TECHNICAL_COOPERATION") {
    return "技术合作";
  }
  if (contractType === "SUPPLEMENTAL") {
    return "补充协议";
  }
  return "分包合同";
}

function statusColor(status: Contract["status"]) {
  if (status === "SIGNED") {
    return "green";
  }
  if (status === "PENDING_SIGN") {
    return "gold";
  }
  if (status === "CANCELLED" || status === "SUPERSEDED") {
    return "red";
  }
  return "blue";
}
