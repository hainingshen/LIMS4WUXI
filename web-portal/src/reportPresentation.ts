import type { Report } from "@lims4wuxi/shared";

export function reportStatusColor(status: Report["status"]) {
  if (status === "RELEASED" || status === "GREEN_RELEASED") {
    return "green";
  }
  if (status === "ISSUED_PAYMENT_LOCKED") {
    return "orange";
  }
  if (status.endsWith("REVIEWING") || status === "PENDING_ISSUE") {
    return "blue";
  }
  return "default";
}

export function paymentGateColor(status: Report["paymentGateStatus"]) {
  if (status === "UNLOCKED" || status === "GREEN_UNLOCKED") {
    return "green";
  }
  return "orange";
}

export function previewWatermark(report: Report) {
  if (report.watermarkType === "NONE" && report.paymentGateStatus !== "LOCKED") {
    return "OFFICIAL";
  }
  return report.watermarkType ?? "DRAFT";
}

export function isPaymentGateLocked(error: unknown) {
  return (error as { response?: { status?: number } }).response?.status === 423;
}

export function triggerPdfDownload(pdf: Blob, fileName: string) {
  const url = URL.createObjectURL(pdf);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
