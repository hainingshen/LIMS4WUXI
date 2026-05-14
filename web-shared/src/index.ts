export type ClientType = "INTERNAL" | "PORTAL" | "MOBILE";

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  clientType: ClientType;
  roles: string[];
};

export const API_BASE_PATH = "/api/v1";
