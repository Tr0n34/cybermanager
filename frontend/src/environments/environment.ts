function browserOrigin(): string {
  if (typeof window === 'undefined') {
    return '';
  }
  return window.location.origin;
}

type RuntimeConfig = {
  authApiUrl?: string;
  usersApiUrl?: string;
};

function runtimeConfig(): RuntimeConfig {
  if (typeof window === 'undefined') {
    return {};
  }
  return (window as Window & { __CM_CONFIG__?: RuntimeConfig }).__CM_CONFIG__ ?? {};
}

function readOverride(key: string): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(key);
}

function normalizeApiUrl(url: string): string {
  return url.trim().replace(/^https:\/\/https:\/\//, 'https://').replace(/\/+$/, '');
}

const defaultAuthApiUrl = 'http://localhost:8081/api';
const defaultUsersApiUrl = 'http://localhost:8082/api';

export const environment = {
  authApiUrl: normalizeApiUrl(readOverride('cm_auth_api_url') ?? runtimeConfig().authApiUrl ?? defaultAuthApiUrl),
  usersApiUrl: normalizeApiUrl(readOverride('cm_users_api_url') ?? runtimeConfig().usersApiUrl ?? defaultUsersApiUrl),
};
