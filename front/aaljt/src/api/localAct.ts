import type {
  ApiResp,
  LocalActCreateResponse,
  LocalActEnrollmentActionResponse,
  LocalActMediaUploadResponse,
  LocalActivityDetail,
  LocalActivityListItem
} from '@/types/localAct';

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

export const fetchLocalActivities = (params?: Record<string, string | number | boolean | undefined | null>) =>
  request<LocalActivityListItem[]>('/api/local-act/activities/list', params);

export const fetchMyLocalActivities = (params: Record<string, string | number | boolean | undefined | null>) =>
  request<LocalActivityListItem[]>('/api/local-act/my-activities', params);

export const fetchFavoriteLocalActivities = (username: string, page = 1, size = 20) =>
  request<LocalActivityListItem[]>('/api/local-act/favorites', { username, page, size });

export const createLocalActivity = (payload: Record<string, unknown>) =>
  request<LocalActCreateResponse>('/api/local-act/activities', undefined, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

export const uploadLocalActMedia = (file: File, scene: 'activity' | 'story' = 'activity') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('scene', scene);
  return request<LocalActMediaUploadResponse>('/api/local-act/media/upload', undefined, {
    method: 'POST',
    body: formData
  });
};

export const createLocalActStory = (payload: Record<string, unknown>) =>
  request<number>('/api/local-act/stories', undefined, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

export const fetchAdminLocalActReviews = (params?: Record<string, string | number | boolean | undefined | null>) =>
  request<LocalActivityListItem[]>('/api/admin/local-act/reviews', params);

export const approveLocalActReview = (id: string | number, note?: string) =>
  request<number>(`/api/admin/local-act/reviews/${id}/approve`, { note }, {
    method: 'POST'
  });

export const rejectLocalActReview = (id: string | number, note?: string) =>
  request<void>(`/api/admin/local-act/reviews/${id}/reject`, { note }, {
    method: 'POST'
  });

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

export const favoriteLocalActivity = (id: string | number, username: string) =>
  request<void>(`/api/local-act/activities/${id}/favorite`, undefined, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username })
  });

export const unfavoriteLocalActivity = (id: string | number, username: string) =>
  request<void>(`/api/local-act/activities/${id}/unfavorite`, undefined, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username })
  });
