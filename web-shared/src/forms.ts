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
export type SubmissionCreateRequest = components["schemas"]["SubmissionCreateRequest"];
export type SubmissionSplitProjectsRequest = components["schemas"]["SubmissionSplitProjectsRequest"];
export type ApprovalRequest = components["schemas"]["ApprovalRequest"];
export type QuotationCreateRequest = components["schemas"]["QuotationCreateRequest"];
export type ContractCreateRequest = components["schemas"]["ContractCreateRequest"];
export type ContractMarkSignedRequest = components["schemas"]["ContractMarkSignedRequest"];
export type StudyPlanCreateRequest = components["schemas"]["StudyPlanCreateRequest"];
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
