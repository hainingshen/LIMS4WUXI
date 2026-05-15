import { http } from "./api/http";
import type { components } from "./generated/api-types";
import { DICTIONARY_ITEMS, type DictionaryCode } from "./generated/dictionaries";

export type FormSchema = components["schemas"]["FormSchema"];
export type Submission = components["schemas"]["Submission"];
export type Project = components["schemas"]["Project"];
export type Quotation = components["schemas"]["Quotation"];
export type Contract = components["schemas"]["Contract"];
export type StudyPlan = components["schemas"]["StudyPlan"];
export type WorkOrder = components["schemas"]["WorkOrder"];
export type RawRecord = components["schemas"]["RawRecord"];
export type Report = components["schemas"]["Report"];
export type PaymentReceipt = components["schemas"]["PaymentReceipt"];
export type ReportTemplate = components["schemas"]["ReportTemplate"];
export type SampleRetention = components["schemas"]["SampleRetention"];
export type GreenChannelRequest = components["schemas"]["GreenChannelRequest"];
export type ReportRegistryItem = components["schemas"]["ReportRegistryItem"];
export type ReportRegistryVersion = components["schemas"]["ReportRegistryVersion"];
export type ReportDeliveryLog = components["schemas"]["ReportDeliveryLog"];
export type ImportExportTask = components["schemas"]["ImportExportTask"];
export type Device = components["schemas"]["Device"];
export type DeviceFile = components["schemas"]["DeviceFile"];
export type InventoryBatch = components["schemas"]["InventoryBatch"];
export type InventoryTransaction = components["schemas"]["InventoryTransaction"];
export type ArchiveItem = components["schemas"]["ArchiveItem"];
export type SubmissionPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Submission[];
};
export type ProjectPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Project[];
};
export type ContractPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Contract[];
};
export type StudyPlanPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: StudyPlan[];
};
export type WorkOrderPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: WorkOrder[];
};
export type RawRecordPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: RawRecord[];
};
export type ReportPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Report[];
};
export type PaymentReceiptPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: PaymentReceipt[];
};
export type ReportTemplatePage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: ReportTemplate[];
};
export type SampleRetentionPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: SampleRetention[];
};
export type GreenChannelRequestPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: GreenChannelRequest[];
};
export type ReportRegistryPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: ReportRegistryItem[];
};
export type ReportDeliveryLogPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: ReportDeliveryLog[];
};
export type DevicePage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Device[];
};
export type InventoryBatchPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: InventoryBatch[];
};
export type InventoryTransactionPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: InventoryTransaction[];
};
export type ImportExportTaskPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: ImportExportTask[];
};
export type ArchiveItemPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: ArchiveItem[];
};
export type SubmissionCreateRequest = components["schemas"]["SubmissionCreateRequest"];
export type SubmissionSplitProjectsRequest = components["schemas"]["SubmissionSplitProjectsRequest"];
export type ApprovalRequest = components["schemas"]["ApprovalRequest"];
export type QuotationCreateRequest = components["schemas"]["QuotationCreateRequest"];
export type ContractCreateRequest = components["schemas"]["ContractCreateRequest"];
export type ContractMarkSignedRequest = components["schemas"]["ContractMarkSignedRequest"];
export type StudyPlanCreateRequest = components["schemas"]["StudyPlanCreateRequest"];
export type RawRecordSubmitRequest = components["schemas"]["RawRecordSubmitRequest"];
export type ReportGenerateDraftRequest = components["schemas"]["ReportGenerateDraftRequest"];
export type PaymentReceiptCreateRequest = components["schemas"]["PaymentReceiptCreateRequest"];
export type ReportTemplateCreateRequest = components["schemas"]["ReportTemplateCreateRequest"];
export type SampleRetentionCreateRequest = components["schemas"]["SampleRetentionCreateRequest"];
export type SampleRetentionExtendRequest = components["schemas"]["SampleRetentionExtendRequest"];
export type SampleRetentionDisposeRequest = components["schemas"]["SampleRetentionDisposeRequest"];
export type GreenChannelCreateRequest = components["schemas"]["GreenChannelCreateRequest"];
export type ReportRegistryBatchDownloadRequest = components["schemas"]["ReportRegistryBatchDownloadRequest"];
export type DeviceFileUploadRequest = components["schemas"]["DeviceFileUploadRequest"];
export type InventoryBatchUpsertRequest = components["schemas"]["InventoryBatchUpsertRequest"];
export type InventoryTransactionCreateRequest = components["schemas"]["InventoryTransactionCreateRequest"];
export type ImportExportTaskCreateRequest = components["schemas"]["ImportExportTaskCreateRequest"];
export type ArchiveItemCreateRequest = components["schemas"]["ArchiveItemCreateRequest"];
export type SubmissionFormCode = SubmissionCreateRequest["formCode"];
export type RuntimeDictionaryItem = components["schemas"]["DictionaryItem"];

export type SubmissionFormField = {
  part: string;
  fieldKey: string;
  labelZh: string;
  labelEn?: string;
  type: "array" | "boolean" | "date" | "datetime" | "decimal_or_text" | "enum" | "string" | "text" | string;
  required: boolean;
  dictionary?: string;
  entity?: string;
  notes?: string;
  lockPolicy?: string;
};

export type DictionaryLookup = Record<string, RuntimeDictionaryItem[]>;

type JsonObject = Record<string, unknown>;

export async function loadPublishedFormSchema(formCode: SubmissionFormCode) {
  const response = await http.get<FormSchema[]>("/form-schemas", {
    params: { formCode, status: "PUBLISHED" },
  });
  const schema = response.data[0];
  if (!schema) {
    throw new Error(`Published form schema not found: ${formCode}`);
  }
  return schema;
}

export async function createSubmissionDraft(
  formCode: SubmissionFormCode,
  schemaVersion: number,
  data: Record<string, unknown>,
) {
  const request: SubmissionCreateRequest = { formCode, schemaVersion, data };
  const response = await http.post<Submission>("/submissions", request);
  return response.data;
}

export async function updateSubmissionDraft(
  id: string,
  formCode: SubmissionFormCode,
  schemaVersion: number,
  data: Record<string, unknown>,
) {
  const request: SubmissionCreateRequest = { formCode, schemaVersion, data };
  const response = await http.patch<Submission>(`/submissions/${id}`, request);
  return response.data;
}

export async function submitSubmission(id: string) {
  const response = await http.post<Submission>(`/submissions/${id}/submit`);
  return response.data;
}

export async function acceptSubmission(id: string) {
  const response = await http.post<Submission>(`/submissions/${id}/accept`);
  return response.data;
}

export async function splitSubmissionProjects(id: string, request?: SubmissionSplitProjectsRequest) {
  const response = await http.post<Project[]>(`/submissions/${id}/split-projects`, request);
  return response.data;
}

export async function listSubmissions(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<SubmissionPage>("/submissions", { params });
  return response.data;
}

export async function listProjects(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<ProjectPage>("/projects", { params });
  return response.data;
}

export async function startNonstandardReview(id: string) {
  const response = await http.post<Project>(`/projects/${id}/nonstandard-review/start`);
  return response.data;
}

export async function approveNonstandardReview(id: string, request: ApprovalRequest) {
  const response = await http.post<Project>(`/projects/${id}/nonstandard-review/approve`, request);
  return response.data;
}

export async function createProjectQuotation(id: string, request: QuotationCreateRequest) {
  const response = await http.post<Quotation>(`/projects/${id}/quotations`, request);
  return response.data;
}

export async function listContracts(params: { page?: number; pageSize?: number; projectId?: string } = {}) {
  const response = await http.get<ContractPage>("/contracts", { params });
  return response.data;
}

export async function createContract(request: ContractCreateRequest) {
  const response = await http.post<Contract>("/contracts", request);
  return response.data;
}

export async function markContractSigned(id: string, request: ContractMarkSignedRequest = {}) {
  const response = await http.post<Contract>(`/contracts/${id}/mark-signed`, request);
  return response.data;
}

export async function listStudyPlans(params: { page?: number; pageSize?: number; projectId?: string } = {}) {
  const response = await http.get<StudyPlanPage>("/study-plans", { params });
  return response.data;
}

export async function createStudyPlan(request: StudyPlanCreateRequest) {
  const response = await http.post<StudyPlan>("/study-plans", request);
  return response.data;
}

export async function submitStudyPlanReview(id: string) {
  const response = await http.post<StudyPlan>(`/study-plans/${id}/submit-review`);
  return response.data;
}

export async function dispatchStudyPlan(id: string) {
  const response = await http.post<StudyPlan>(`/study-plans/${id}/dispatch`);
  return response.data;
}

export async function listWorkOrders(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<WorkOrderPage>("/work-orders", { params });
  return response.data;
}

export async function pickWorkOrderSample(id: string) {
  const response = await http.post<WorkOrder>(`/work-orders/${id}/pick-sample`);
  return response.data;
}

export async function startWorkOrder(id: string) {
  const response = await http.post<WorkOrder>(`/work-orders/${id}/start`);
  return response.data;
}

export async function enterWorkOrderResult(id: string) {
  const response = await http.post<WorkOrder>(`/work-orders/${id}/enter-result`);
  return response.data;
}

export async function submitWorkOrderRawRecord(id: string, request: RawRecordSubmitRequest) {
  const response = await http.post<RawRecord>(`/work-orders/${id}/submit-raw-record`, request);
  return response.data;
}

export async function listRawRecords(params: { page?: number; pageSize?: number; workOrderId?: string } = {}) {
  const response = await http.get<RawRecordPage>("/raw-records", { params });
  return response.data;
}

export async function reviewRawRecord(id: string, request: ApprovalRequest) {
  const response = await http.post<RawRecord>(`/raw-records/${id}/review`, request);
  return response.data;
}

export async function listReports(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<ReportPage>("/reports", { params });
  return response.data;
}

export async function generateDraftReport(request: ReportGenerateDraftRequest) {
  const response = await http.post<Report>("/reports/generate-draft", request);
  return response.data;
}

export async function submitReportQc(id: string) {
  const response = await http.post<Report>(`/reports/${id}/submit-qc`);
  return response.data;
}

export async function reviewReport(id: string, request: ApprovalRequest) {
  const response = await http.post<Report>(`/reports/${id}/review`, request);
  return response.data;
}

export async function issueReport(id: string) {
  const response = await http.post<Report>(`/reports/${id}/issue`);
  return response.data;
}

export async function listPaymentReceipts(params: { page?: number; pageSize?: number; projectId?: string } = {}) {
  const response = await http.get<PaymentReceiptPage>("/payment-receipts", { params });
  return response.data;
}

export async function createPaymentReceipt(request: PaymentReceiptCreateRequest) {
  const response = await http.post<PaymentReceipt>("/payment-receipts", request);
  return response.data;
}

export async function listReportTemplates(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<ReportTemplatePage>("/report-templates", { params });
  return response.data;
}

export async function createReportTemplate(request: ReportTemplateCreateRequest) {
  const response = await http.post<ReportTemplate>("/report-templates", request);
  return response.data;
}

export async function publishReportTemplate(id: string) {
  const response = await http.post<ReportTemplate>(`/report-templates/${id}/publish`);
  return response.data;
}

export async function listSampleRetentions(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<SampleRetentionPage>("/sample-retentions", { params });
  return response.data;
}

export async function createSampleRetention(request: SampleRetentionCreateRequest) {
  const response = await http.post<SampleRetention>("/sample-retentions", request);
  return response.data;
}

export async function registerSampleRetention(id: string) {
  const response = await http.post<SampleRetention>(`/sample-retentions/${id}/register`);
  return response.data;
}

export async function extendSampleRetention(id: string, request: SampleRetentionExtendRequest) {
  const response = await http.post<SampleRetention>(`/sample-retentions/${id}/extend`, request);
  return response.data;
}

export async function disposeSampleRetention(id: string, request: SampleRetentionDisposeRequest) {
  const response = await http.post<SampleRetention>(`/sample-retentions/${id}/dispose`, request);
  return response.data;
}

export async function listGreenChannelRequests(
  params: { page?: number; pageSize?: number; projectId?: string } = {},
) {
  const response = await http.get<GreenChannelRequestPage>("/green-channel-requests", { params });
  return response.data;
}

export async function createGreenChannelRequest(request: GreenChannelCreateRequest) {
  const response = await http.post<GreenChannelRequest>("/green-channel-requests", request);
  return response.data;
}

export async function approveGreenChannelRequest(id: string, request: ApprovalRequest) {
  await http.post<void>(`/green-channel-requests/${id}/approve`, request);
}

export async function searchReportRegistry(
  params: {
    page?: number;
    pageSize?: number;
    keyword?: string;
    customerOrgId?: string;
    projectId?: string;
    reportBizNo?: string;
    templateType?: string;
    status?: ReportRegistryItem["status"];
    issuedFrom?: string;
    issuedTo?: string;
  } = {},
) {
  const response = await http.get<ReportRegistryPage>("/report-registry/search", { params });
  return response.data;
}

export async function createReportRegistryBatchDownload(request: ReportRegistryBatchDownloadRequest) {
  const response = await http.post<ImportExportTask>("/report-registry/batch-download", request);
  return response.data;
}

export async function listReportDeliveryLogs(params: { page?: number; pageSize?: number; reportId?: string } = {}) {
  const response = await http.get<ReportDeliveryLogPage>("/report-delivery-logs", { params });
  return response.data;
}

export async function listDevices(params: { page?: number; pageSize?: number; keyword?: string } = {}) {
  const response = await http.get<DevicePage>("/devices", { params });
  return response.data;
}

export async function listDeviceFiles(deviceId: string) {
  const response = await http.get<DeviceFile[]>(`/devices/${deviceId}/files`);
  return response.data;
}

export async function uploadDeviceFile(deviceId: string, request: DeviceFileUploadRequest) {
  const response = await http.post<DeviceFile>(`/devices/${deviceId}/files`, request);
  return response.data;
}

export async function disableDeviceFile(id: string) {
  const response = await http.post<DeviceFile>(`/device-files/${id}/disable`);
  return response.data;
}

export async function listInventoryBatches(
  params: { page?: number; pageSize?: number; keyword?: string } = {},
) {
  const response = await http.get<InventoryBatchPage>("/inventory-batches", { params });
  return response.data;
}

export async function upsertInventoryBatch(request: InventoryBatchUpsertRequest) {
  const response = await http.post<InventoryBatch>("/inventory-batches", request);
  return response.data;
}

export async function listInventoryTransactions(
  params: { page?: number; pageSize?: number; inventoryBatchId?: string } = {},
) {
  const response = await http.get<InventoryTransactionPage>("/inventory-transactions", { params });
  return response.data;
}

export async function createInventoryTransaction(request: InventoryTransactionCreateRequest) {
  const response = await http.post<InventoryTransaction>("/inventory-transactions", request);
  return response.data;
}

export async function listImportExportTasks(params: { page?: number; pageSize?: number } = {}) {
  const response = await http.get<ImportExportTaskPage>("/import-export-tasks", { params });
  return response.data;
}

export async function createImportExportTask(request: ImportExportTaskCreateRequest) {
  const response = await http.post<ImportExportTask>("/import-export-tasks", request);
  return response.data;
}

export async function validateImportExportTask(id: string) {
  const response = await http.post<ImportExportTask>(`/import-export-tasks/${id}/validate`);
  return response.data;
}

export async function runImportExportTask(id: string) {
  const response = await http.post<ImportExportTask>(`/import-export-tasks/${id}/run`);
  return response.data;
}

export async function downloadImportExportTaskResultFile(id: string) {
  const response = await http.get<Blob>(`/import-export-tasks/${id}/result-file`, {
    responseType: "blob",
  });
  return response.data;
}

export async function listArchiveItems(params: { page?: number; pageSize?: number; projectId?: string } = {}) {
  const response = await http.get<ArchiveItemPage>("/archive-items", { params });
  return response.data;
}

export async function createArchiveItem(request: ArchiveItemCreateRequest) {
  const response = await http.post<ArchiveItem>("/archive-items", request);
  return response.data;
}

export async function listDictionaryItems(dictCode?: string) {
  const response = await http.get<RuntimeDictionaryItem[]>("/dictionaries", {
    params: dictCode ? { dictCode } : undefined,
  });
  return response.data
    .filter((item) => item.enabled !== false)
    .sort((left, right) => left.sortOrder - right.sortOrder);
}

export async function loadDictionaryLookup(dictionaryCodes: readonly string[]) {
  try {
    return buildDictionaryLookup(await listDictionaryItems(), dictionaryCodes);
  } catch {
    return buildSeedDictionaryLookup(dictionaryCodes);
  }
}

export function getSubmissionFormFields(schemaJson: JsonObject) {
  const fields = schemaJson.fields;
  if (!Array.isArray(fields)) {
    return [];
  }
  return fields
    .map((field) => normalizeFormField(field))
    .filter((field): field is SubmissionFormField => field !== undefined);
}

export function collectDictionaryCodes(fields: readonly SubmissionFormField[]) {
  return [...new Set(fields.map((field) => field.dictionary).filter((code): code is string => Boolean(code)))];
}

export function getSeedDictionaryItems(dictionaryCode: string) {
  if (!isDictionaryCode(dictionaryCode)) {
    return [];
  }
  return DICTIONARY_ITEMS[dictionaryCode]
    .map((item) => ({
      ...item,
      dictCode: dictionaryCode,
      enabled: true,
    }))
    .sort((left, right) => left.sortOrder - right.sortOrder);
}

function buildDictionaryLookup(
  items: readonly RuntimeDictionaryItem[],
  dictionaryCodes: readonly string[],
) {
  const requestedCodes = new Set(dictionaryCodes);
  const lookup: DictionaryLookup = {};
  for (const item of items) {
    if (requestedCodes.size > 0 && !requestedCodes.has(item.dictCode)) {
      continue;
    }
    if (item.enabled === false) {
      continue;
    }
    lookup[item.dictCode] = [...(lookup[item.dictCode] ?? []), item];
  }
  for (const code of dictionaryCodes) {
    if (!lookup[code]?.length) {
      lookup[code] = getSeedDictionaryItems(code);
    }
  }
  return sortDictionaryLookup(lookup);
}

function buildSeedDictionaryLookup(dictionaryCodes: readonly string[]) {
  const lookup: DictionaryLookup = {};
  for (const code of dictionaryCodes) {
    lookup[code] = getSeedDictionaryItems(code);
  }
  return lookup;
}

function sortDictionaryLookup(lookup: DictionaryLookup) {
  return Object.fromEntries(
    Object.entries(lookup).map(([code, items]) => [
      code,
      [...items].sort((left, right) => left.sortOrder - right.sortOrder),
    ]),
  );
}

function normalizeFormField(value: unknown): SubmissionFormField | undefined {
  if (!isRecord(value) || typeof value.fieldKey !== "string" || typeof value.labelZh !== "string") {
    return undefined;
  }
  return {
    part: text(value.part, "default"),
    fieldKey: value.fieldKey,
    labelZh: value.labelZh,
    labelEn: optionalText(value.labelEn),
    type: text(value.type, "string"),
    required: value.required === true,
    dictionary: optionalText(value.dictionary),
    entity: optionalText(value.entity),
    notes: optionalText(value.notes),
    lockPolicy: optionalText(value.lockPolicy),
  } satisfies SubmissionFormField;
}

function isDictionaryCode(code: string): code is DictionaryCode {
  return Object.prototype.hasOwnProperty.call(DICTIONARY_ITEMS, code);
}

function isRecord(value: unknown): value is JsonObject {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function text(value: unknown, fallback: string) {
  return typeof value === "string" && value.length > 0 ? value : fallback;
}

function optionalText(value: unknown) {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}
