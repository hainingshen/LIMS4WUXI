import { http } from "./api/http";
import type { components } from "./generated/api-types";
import { DICTIONARY_ITEMS, type DictionaryCode } from "./generated/dictionaries";

export type FormSchema = components["schemas"]["FormSchema"];
export type Submission = components["schemas"]["Submission"];
export type SubmissionCreateRequest = components["schemas"]["SubmissionCreateRequest"];
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
