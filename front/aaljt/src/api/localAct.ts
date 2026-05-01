import type { ApiResp, LocalActEnrollmentActionResponse, LocalActivityDetail } from '@/types/localAct';

const API_BASE = import.meta.env.VITE_API_BASE || '';

const buildUrl = (path: string, params?: Record<string, string | number | boolean | undefined | null>) => {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  });
  return API_BASE ? url.toString() : `${url.pathname}${url.search}`;
};

const request = async <T>(
  path: string,
  params?: Record<string, string | number | boolean | undefined | null>,
  init?: RequestInit
) => {
  const resp = await fetch(buildUrl(path, params), init);
  const data = (await resp.json().catch(() => null)) as ApiResp<T> | null;
  if (!resp.ok || !data || data.code !== 200) {
    throw new Error(data?.message || `请求失败：${resp.status}`);
  }
  return data.data;
};

export const fetchLocalActivityDetail = (id: string | number, username?: string) =>
  request<LocalActivityDetail>(`/api/local-act/activities/${id}`, { username });

export const enrollLocalActivity = (id: string | number, username: string) =>
  request<LocalActEnrollmentActionResponse>(
    `/api/local-act/activities/${id}/enroll`,
    undefined,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    }
  );

export const cancelLocalActivityEnrollment = (id: string | number, username: string, reason?: string) =>
  request<LocalActEnrollmentActionResponse>(
    `/api/local-act/activities/${id}/cancel-enrollment`,
    undefined,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, reason })
    }
  );
