export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  clientType: string;
  roles?: string[];
  expiresInSeconds?: number;
};

const LEGACY_AUTH_STORAGE_KEY = "lims.auth.session";
const LEGACY_ACCESS_TOKEN_KEY = "accessToken";

type AuthStorageScope = "internal" | "portal" | "mobile" | "default";

export function readStoredSessionForCurrentClient() {
  if (typeof window === "undefined") {
    return undefined;
  }

  const scope = currentScope();
  const raw = window.localStorage.getItem(sessionKey(scope)) ?? readLegacySessionForScope(scope);
  if (!raw) {
    return undefined;
  }

  try {
    return JSON.parse(raw) as StoredAuthSession;
  } catch {
    clearStoredSessionForCurrentClient();
    return undefined;
  }
}

export function writeStoredSessionForCurrentClient(session: StoredAuthSession) {
  if (typeof window === "undefined") {
    return;
  }

  const scope = scopeForClientType(session.clientType) ?? currentScope();
  window.localStorage.setItem(sessionKey(scope), JSON.stringify(session));
  window.localStorage.setItem(accessTokenKey(scope), session.accessToken);
  clearLegacySession();
}

export function clearStoredSessionForCurrentClient() {
  if (typeof window === "undefined") {
    return;
  }

  const scope = currentScope();
  window.localStorage.removeItem(sessionKey(scope));
  window.localStorage.removeItem(accessTokenKey(scope));
  clearLegacySession();
}

export function readStoredAccessTokenForCurrentClient() {
  if (typeof window === "undefined") {
    return undefined;
  }

  const scope = currentScope();
  const scopedToken = window.localStorage.getItem(accessTokenKey(scope));
  if (scopedToken) {
    return scopedToken;
  }
  return readStoredSessionForCurrentClient()?.accessToken;
}

function currentScope(): AuthStorageScope {
  if (typeof window === "undefined") {
    return "default";
  }
  const pathname = window.location.pathname;
  if (pathname === "/internal" || pathname.startsWith("/internal/")) {
    return "internal";
  }
  if (pathname === "/portal" || pathname.startsWith("/portal/")) {
    return "portal";
  }
  return "default";
}

function scopeForClientType(clientType: string): AuthStorageScope | undefined {
  if (clientType === "INTERNAL") {
    return "internal";
  }
  if (clientType === "PORTAL") {
    return "portal";
  }
  if (clientType === "MOBILE") {
    return "mobile";
  }
  return undefined;
}

function sessionKey(scope: AuthStorageScope) {
  return `lims.${scope}.auth.session`;
}

function accessTokenKey(scope: AuthStorageScope) {
  return `lims.${scope}.accessToken`;
}

function readLegacySessionForScope(scope: AuthStorageScope) {
  const raw = window.localStorage.getItem(LEGACY_AUTH_STORAGE_KEY);
  if (!raw) {
    return undefined;
  }
  try {
    const session = JSON.parse(raw) as StoredAuthSession;
    if (scopeForClientType(session.clientType) === scope) {
      return raw;
    }
  } catch {
    clearLegacySession();
  }
  return undefined;
}

function clearLegacySession() {
  window.localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
  window.localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
}
