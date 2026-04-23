export type Dictionary = Record<string, any>

export interface LaunchReadinessResult extends Dictionary {
  overallReady?: boolean
  gates?: Dictionary
  recommendedActions?: string[]
}

export interface LaunchChecklistItem extends Dictionary {
  id: string
  label: string
  ready: boolean
  severity?: string
  detail?: string
  action?: string
}

export interface LaunchRecord extends Dictionary {
  id?: string
  name?: string
  recordType?: string
  environment?: string
  operator?: string
  passed?: boolean
  status?: string
  summary?: string
  createdAt?: string
  updatedAt?: string
}

export interface LaunchFinalSummary extends Dictionary {
  finalReady?: boolean
  gates?: Dictionary
  readiness?: LaunchReadinessResult
  latestLoadTest?: LaunchRecord
  latestDrill?: LaunchRecord
  latestChecklistSnapshot?: LaunchRecord
  recommendedActions?: string[]
}

export interface LaunchRunbookBundle extends Dictionary {
  finalSummary?: LaunchFinalSummary
  documents?: Record<string, { title?: string; path?: string; content?: string }>
}

export interface LaunchHandoffSummary extends Dictionary {
  finalReady?: boolean
  ownerActions?: string[]
  latestArtifacts?: Dictionary
  documents?: Record<string, { title?: string; path?: string; content?: string }>
  summary?: LaunchFinalSummary
}

export interface LaunchTimelineItem extends Dictionary {
  type?: string
  id?: string | number
  title?: string
  summary?: string
  status?: string
  timestamp?: string
  record?: Dictionary
}

export interface LaunchSignoff extends Dictionary {
  id?: string
  signoffRole?: string
  operator?: string
  approved?: boolean
  status?: string
  notes?: string
  updatedAt?: string
}

export interface LaunchDependencyCheck extends Dictionary {
  id?: string
  dependencyName?: string
  operator?: string
  ready?: boolean
  status?: string
  notes?: string
  updatedAt?: string
}

export interface LaunchWindow extends Dictionary {
  id?: string
  windowName?: string
  operator?: string
  startAt?: string
  endAt?: string
  status?: string
  notes?: string
  updatedAt?: string
}

export interface LaunchCloseout extends Dictionary {
  id?: string
  name?: string
  operator?: string
  environment?: string
  finalReady?: boolean
  status?: string
  summary?: string
  updatedAt?: string
}

export interface LaunchGatewayConfig extends Dictionary {
  enabled?: boolean
  gatewayEnabled?: boolean
  mockGatewayEnabled?: boolean
  gatewayBaseUrl?: string
  gatewayQueryPath?: string
  gatewayConnectTimeoutMs?: number
  gatewayReadTimeoutMs?: number
  gatewayRetryCount?: number
  cacheTtlSeconds?: number
  circuitFailureThreshold?: number
  circuitOpenSeconds?: number
}

export interface LaunchPackage extends Dictionary {
  readiness?: LaunchReadinessResult
  checklist?: LaunchChecklistItem[]
  finalSummary?: LaunchFinalSummary
  handoffSummary?: LaunchHandoffSummary
  runbookBundle?: LaunchRunbookBundle
  timeline?: LaunchTimelineItem[]
}

interface Resp<T> {
  code: number
  message: string
  data: T
}

const API_BASE = ((import.meta as any)?.env?.VITE_API_BASE as string | undefined) ?? ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
      ...init,
    })
  } catch (error) {
    const original = error instanceof Error ? error.message : 'network error'
    throw new Error(`上线准备后端不可访问，请确认服务已启动。原始错误：${original}`)
  }

  const payload = (await response.json()) as Resp<T>
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || `请求失败: ${response.status}`)
  }
  return payload.data
}

function toQuery(params: Record<string, string | number | boolean | null | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export function fetchLaunchReadiness(detailLimit = 10, taskLimit = 10, maxDegradedRate = 0.2, minEvalPassRate = 1, maxRunAgeHours = 24) {
  return request<LaunchReadinessResult>(
    `/api/admin/launch/readiness/overview${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      minEvalPassRate,
      maxRunAgeHours,
    })}`,
  )
}

export function fetchLaunchChecklist(maxDegradedRate = 0.2, minEvalPassRate = 1, maxRunAgeHours = 24) {
  return request<LaunchChecklistItem[]>(
    `/api/admin/launch/checklist${toQuery({ maxDegradedRate, minEvalPassRate, maxRunAgeHours })}`,
  )
}

export function runLaunchSmoke(requestBody: Dictionary) {
  return request<Dictionary>('/api/admin/launch/smoke/run', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function forceLaunchRealtimeFallback(requestBody: Dictionary) {
  return request<Dictionary>('/api/admin/launch/chaos/realtime/force-fallback', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function recoverLaunchRealtimeFallback() {
  return request<Dictionary>('/api/admin/launch/chaos/realtime/recover', {
    method: 'POST',
  })
}

export function listLaunchLoadTests(limit = 20) {
  return request<LaunchRecord[]>(`/api/admin/launch/load-tests/list${toQuery({ limit })}`)
}

export function recordLaunchLoadTest(requestBody: Dictionary) {
  return request<LaunchRecord>('/api/admin/launch/load-tests/run', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchDrills(limit = 20) {
  return request<LaunchRecord[]>(`/api/admin/launch/drills/list${toQuery({ limit })}`)
}

export function recordLaunchDrill(requestBody: Dictionary) {
  return request<LaunchRecord>('/api/admin/launch/drills/run', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchCloseouts(limit = 20) {
  return request<LaunchCloseout[]>(`/api/admin/launch/closeouts/list${toQuery({ limit })}`)
}

export function createLaunchCloseout(requestBody: Dictionary) {
  return request<LaunchCloseout>('/api/admin/launch/closeouts/create', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchChecklistSnapshots(limit = 20) {
  return request<LaunchRecord[]>(`/api/admin/launch/checklist-snapshots/list${toQuery({ limit })}`)
}

export function createLaunchChecklistSnapshot(requestBody: Dictionary) {
  return request<LaunchRecord>('/api/admin/launch/checklist-snapshots/create', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchSignoffs(limit = 20) {
  return request<LaunchSignoff[]>(`/api/admin/launch/signoffs/list${toQuery({ limit })}`)
}

export function recordLaunchSignoff(requestBody: Dictionary) {
  return request<LaunchSignoff>('/api/admin/launch/signoffs/record', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchDependencyChecks(limit = 20) {
  return request<LaunchDependencyCheck[]>(`/api/admin/launch/dependencies/list${toQuery({ limit })}`)
}

export function recordLaunchDependencyCheck(requestBody: Dictionary) {
  return request<LaunchDependencyCheck>('/api/admin/launch/dependencies/record', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function listLaunchWindows(limit = 20) {
  return request<LaunchWindow[]>(`/api/admin/launch/windows/list${toQuery({ limit })}`)
}

export function createLaunchWindow(requestBody: Dictionary) {
  return request<LaunchWindow>('/api/admin/launch/windows/create', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function closeLaunchWindow(requestBody: Dictionary) {
  return request<LaunchWindow>('/api/admin/launch/windows/close', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchLaunchFinalSummary(
  detailLimit = 10,
  taskLimit = 10,
  maxDegradedRate = 0.2,
  minEvalPassRate = 1,
  maxRunAgeHours = 24,
  loadTestFreshnessHours = 72,
  drillFreshnessHours = 72,
  checklistFreshnessHours = 24,
) {
  return request<LaunchFinalSummary>(
    `/api/admin/launch/final-summary${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      minEvalPassRate,
      maxRunAgeHours,
      loadTestFreshnessHours,
      drillFreshnessHours,
      checklistFreshnessHours,
    })}`,
  )
}

export function fetchLaunchRunbookBundle(
  detailLimit = 10,
  taskLimit = 10,
  maxDegradedRate = 0.2,
  minEvalPassRate = 1,
  maxRunAgeHours = 24,
) {
  return request<LaunchRunbookBundle>(
    `/api/admin/launch/runbook/bundle${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      minEvalPassRate,
      maxRunAgeHours,
    })}`,
  )
}

export function fetchLaunchHandoffSummary(
  detailLimit = 10,
  taskLimit = 10,
  maxDegradedRate = 0.2,
  minEvalPassRate = 1,
  maxRunAgeHours = 24,
) {
  return request<LaunchHandoffSummary>(
    `/api/admin/launch/handoff-summary${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      minEvalPassRate,
      maxRunAgeHours,
    })}`,
  )
}

export function fetchLaunchTimeline(limit = 20) {
  return request<LaunchTimelineItem[]>(`/api/admin/launch/timeline${toQuery({ limit })}`)
}

export function fetchLaunchGatewayConfig() {
  return request<LaunchGatewayConfig>('/api/admin/launch/realtime/gateway-config')
}

export function fetchLaunchPackage(
  detailLimit = 10,
  taskLimit = 10,
  maxDegradedRate = 0.2,
  minEvalPassRate = 1,
  maxRunAgeHours = 24,
  timelineLimit = 20,
) {
  return request<LaunchPackage>(
    `/api/admin/launch/export/package${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      minEvalPassRate,
      maxRunAgeHours,
      timelineLimit,
    })}`,
  )
}
