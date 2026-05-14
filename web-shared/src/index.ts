export type ClientType = "INTERNAL" | "PORTAL" | "MOBILE";

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  clientType: ClientType;
  roles: string[];
};

export { API_BASE_PATH } from "./api/config";
export { http } from "./api/http";
export type { components, operations, paths } from "./generated/api-types";
