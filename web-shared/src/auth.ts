import { create } from "zustand";

import { http } from "./api/http";
import type { components } from "./generated/api-types";
import { PERMISSION_MATRIX } from "./generated/dictionaries";

export type ClientType = "INTERNAL" | "PORTAL" | "MOBILE";
export type AuthToken = components["schemas"]["AuthToken"];
export type CurrentUser = components["schemas"]["CurrentUser"];
export type LoginRequest = components["schemas"]["LoginRequest"];

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  clientType: ClientType;
  roles: string[];
  expiresInSeconds: number;
};

type AuthState = {
  accessToken?: string;
  refreshToken?: string;
  roles: string[];
  clientType?: ClientType;
  expiresInSeconds?: number;
  setSession: (session: AuthSession) => void;
  clearSession: () => void;
};

const AUTH_STORAGE_KEY = "lims.auth.session";

export const useAuthStore = create<AuthState>((set) => ({
  ...readStoredSession(),
  setSession: (session) => {
    writeStoredSession(session);
    set(session);
  },
  clearSession: () => {
    clearStoredSession();
    set({
      accessToken: undefined,
      refreshToken: undefined,
      roles: [],
      clientType: undefined,
      expiresInSeconds: undefined,
    });
  },
}));

export async function loginForClient(clientType: Exclude<ClientType, "MOBILE">, request: LoginRequest) {
  const path = clientType === "INTERNAL" ? "/auth/internal/login" : "/auth/portal/login";
  const response = await http.post<AuthToken>(path, request);
  return response.data;
}

export async function getCurrentUser() {
  const response = await http.get<CurrentUser>("/auth/me");
  return response.data;
}

export function hasAnyRole(userRoles: readonly string[], requiredRoles: readonly string[]) {
  return requiredRoles.some((role) => userRoles.includes(role));
}

export function canAccessAction(userRoles: readonly string[], actionCode: string) {
  return PERMISSION_MATRIX.some(
    (row) => row.actionCode === actionCode
      && userRoles.includes(row.roleCode)
      && String(row.access) !== "DENY",
  );
}

export function usePermission(actionCode: string) {
  const roles = useAuthStore((state) => state.roles);
  return canAccessAction(roles, actionCode);
}

export function roleLabel(role: string) {
  return role.replaceAll("_", " ");
}

function readStoredSession(): Pick<AuthState, "accessToken" | "refreshToken" | "roles" | "clientType" | "expiresInSeconds"> {
  if (typeof window === "undefined") {
    return { roles: [] };
  }
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return { roles: [] };
  }
  try {
    const session = JSON.parse(raw) as AuthSession;
    return {
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      roles: session.roles ?? [],
      clientType: session.clientType,
      expiresInSeconds: session.expiresInSeconds,
    };
  } catch {
    clearStoredSession();
    return { roles: [] };
  }
}

function writeStoredSession(session: AuthSession) {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  window.localStorage.setItem("accessToken", session.accessToken);
}

function clearStoredSession() {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  window.localStorage.removeItem("accessToken");
}
