import { http } from "./api/http";
import type { components } from "./generated/api-types";

export type Sample = components["schemas"]["Sample"];
export type SamplePhoto = components["schemas"]["SamplePhoto"];
export type SampleReceiveRequest = components["schemas"]["SampleReceiveRequest"];
export type SampleStockInRequest = components["schemas"]["SampleStockInRequest"];

export type SamplePage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: Sample[];
};

export type SampleListParams = {
  keyword?: string;
  status?: Sample["status"];
  page?: number;
  pageSize?: number;
};

export async function listSamples(params: SampleListParams) {
  const response = await http.get<SamplePage>("/samples", { params });
  return response.data;
}

export async function receiveSample(sampleId: string, request: SampleReceiveRequest) {
  const response = await http.post<Sample>(`/samples/${sampleId}/receive`, request);
  return response.data;
}

export async function stockInSample(sampleId: string, request: SampleStockInRequest) {
  const response = await http.post<Sample>(`/samples/${sampleId}/stock-in`, request);
  return response.data;
}

export async function uploadSamplePhoto(sampleId: string, file: File, note?: string) {
  const form = new FormData();
  form.append("file", file);
  const response = await http.post<SamplePhoto>(`/samples/${sampleId}/photos`, form, {
    params: note ? { note } : undefined,
  });
  return response.data;
}
