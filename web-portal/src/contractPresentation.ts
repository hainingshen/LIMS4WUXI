import type { Contract } from "@lims4wuxi/shared";

export function contractStatusColor(status: Contract["status"]) {
  if (status === "SIGNED") {
    return "green";
  }
  if (status === "PENDING_SIGN") {
    return "orange";
  }
  if (status === "CANCELLED" || status === "SUPERSEDED") {
    return "red";
  }
  return "default";
}

export function contractTypeLabel(value: Contract["contractType"]) {
  if (value === "TEST_AGREEMENT") {
    return "检测协议";
  }
  if (value === "TECHNICAL_COOPERATION") {
    return "技术服务合同";
  }
  if (value === "SUPPLEMENTAL") {
    return "补充协议";
  }
  return "分包协议";
}
