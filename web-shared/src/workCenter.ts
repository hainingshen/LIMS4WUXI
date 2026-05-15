import { http } from "./api/http";
import type { components } from "./generated/api-types";

export type WorkCenterSummary = components["schemas"]["WorkCenterSummary"];
export type WorkCenterProject = components["schemas"]["WorkCenterProject"];
export type WorkCenterBlocker = components["schemas"]["WorkCenterBlocker"];
export type WorkCenterResourceLoad = components["schemas"]["WorkCenterResourceLoad"];
export type WorkCenterTrend = components["schemas"]["WorkCenterTrend"];
export type WorkCenterBlockerCreateRequest = components["schemas"]["WorkCenterBlockerCreateRequest"];
export type WorkCenterBlockerUpdateRequest = components["schemas"]["WorkCenterBlockerUpdateRequest"];
export type WorkCenterBlockerCloseRequest = components["schemas"]["WorkCenterBlockerCloseRequest"];

export type WorkCenterProjectPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: WorkCenterProject[];
};

export type WorkCenterBlockerPage = Omit<components["schemas"]["PageResponse"], "items"> & {
  items: WorkCenterBlocker[];
};

export type WorkCenterProjectFilter = "normal" | "warning" | "risk" | "overdue";

export async function getWorkCenterSummary() {
  const response = await http.get<WorkCenterSummary>("/work-center/summary");
  return response.data;
}

export async function listWorkCenterProjects(params: {
  filter?: WorkCenterProjectFilter;
  page?: number;
  pageSize?: number;
}) {
  const response = await http.get<WorkCenterProjectPage>("/work-center/projects", { params });
  return response.data;
}

export async function listWorkCenterBlockers(params: { page?: number; pageSize?: number }) {
  const response = await http.get<WorkCenterBlockerPage>("/work-center/blockers", { params });
  return response.data;
}

export async function createWorkCenterBlocker(request: WorkCenterBlockerCreateRequest) {
  const response = await http.post<WorkCenterBlocker>("/work-center/blockers", request);
  return response.data;
}

export async function updateWorkCenterBlocker(id: string, request: WorkCenterBlockerUpdateRequest) {
  const response = await http.patch<WorkCenterBlocker>(`/work-center/blockers/${id}`, request);
  return response.data;
}

export async function closeWorkCenterBlocker(id: string, request: WorkCenterBlockerCloseRequest) {
  const response = await http.post<WorkCenterBlocker>(`/work-center/blockers/${id}/close`, request);
  return response.data;
}

export async function getWorkCenterResourceLoad() {
  const response = await http.get<WorkCenterResourceLoad[]>("/work-center/resource-load");
  return response.data;
}

export async function getWorkCenterTrends() {
  const response = await http.get<WorkCenterTrend[]>("/work-center/trends");
  return response.data;
}
