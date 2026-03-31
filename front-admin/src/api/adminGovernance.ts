export type Dictionary = Record<string, any>

export type ReplayLookupMode = 'request' | 'trace' | 'session'

export interface ReplaySummary {
  requestId: string
  traceId: string
  sessionId: string
  userId?: string
  lastUserMessage?: string
  taskType?: string
  planType?: string
  answerType?: string
  errorCode?: string
  failedNode?: string
  degraded?: boolean
  durationMs?: number
  createdAt?: string
}

export interface ReplayCandidate extends ReplaySummary {
  existingQuery?: boolean
  suggestedRiskLevel?: string
  reason?: string
}

export interface ReplayCheckpoint {
  checkpointOrder: number
  nodeName: string
  stateSnapshot?: Dictionary
  createdAt?: string
}

export interface ReplayToolIo {
  stepOrder: number
  stepId: string
  toolName: string
  purpose?: string
  outputKey?: string
  optionalStep?: boolean
  executionStatus?: string
  inputPayload?: Dictionary
  outputPayload?: Dictionary
  createdAt?: string
}

export interface ReplayDetail {
  summary: ReplaySummary
  requestSnapshot?: Dictionary
  stateSnapshot?: Dictionary
  checkpoints: ReplayCheckpoint[]
  toolIos: ReplayToolIo[]
  sessionHistory: ReplaySummary[]
}

export interface EvalCaseStats {
  total: number
  enabled_total: number
  disabled_total: number
  by_bucket: Record<string, number>
  by_risk_level: Record<string, number>
}

export interface EvalCaseItem {
  id: number
  caseName?: string
  queryText?: string
  bucket?: string
  riskLevel?: string
  enabled?: number
  expectedTaskType?: string
  expectedPlanType?: string
  expectedAnswerType?: string
  tagsJson?: string
  notes?: string
  sourceType?: string
  createdAt?: string
  updatedAt?: string
}

export interface EvalCaseImportResult {
  total: number
  inserted: number
  updated: number
}

export interface EvalCaseVersion {
  id: number
  versionName: string
  bucket?: string
  totalCases?: number
  enabledTotal?: number
  notes?: string
  createdBy?: string
  createdAt?: string
}

export interface EvalCaseVersionItem {
  caseId?: number
  caseName?: string
  queryText?: string
  bucket?: string
  riskLevel?: string
  expectedTaskType?: string
  expectedPlanType?: string
  expectedAnswerType?: string
}

export interface EvalCaseVersionDetail {
  version: EvalCaseVersion
  items: EvalCaseVersionItem[]
  latestRun?: EvalRun | null
  recentRuns: EvalRun[]
}

export interface EvalCaseVersionComparisonItem {
  changeType: string
  caseId?: number
  caseName?: string
  queryText?: string
}

export interface EvalCaseVersionComparison {
  baseVersion: EvalCaseVersion
  targetVersion: EvalCaseVersion
  baseTotal?: number
  targetTotal?: number
  addedCount?: number
  removedCount?: number
  changedCount?: number
  unchangedCount?: number
  items: EvalCaseVersionComparisonItem[]
}

export interface EvalRun {
  id: number
  bucket?: string
  sourceType?: string
  versionId?: number
  regressionSetId?: number
  total?: number
  passedTotal?: number
  failedTotal?: number
  passRate?: number
  createdAt?: string
}

export interface EvalRunCaseResult {
  caseId?: number
  caseName?: string
  queryText?: string
  expectedTaskType?: string
  actualTaskType?: string
  taskTypeMatched?: boolean
  expectedPlanType?: string
  actualPlanType?: string
  planTypeMatched?: boolean
  expectedAnswerType?: string
  actualAnswerType?: string
  answerTypeMatched?: boolean
  degraded?: boolean
  passed?: boolean
  reply?: string
}

export interface EvalRunDetail {
  run: EvalRun
  results: EvalRunCaseResult[]
}

export interface EvalRunComparisonItem {
  caseId?: number
  caseName?: string
  queryText?: string
  changeType?: string
  basePassed?: boolean
  targetPassed?: boolean
  baseActualTaskType?: string
  targetActualTaskType?: string
  baseActualPlanType?: string
  targetActualPlanType?: string
  baseActualAnswerType?: string
  targetActualAnswerType?: string
}

export interface EvalRunComparison {
  baseRunId?: number
  targetRunId?: number
  baseTotal?: number
  targetTotal?: number
  basePassRate?: number
  targetPassRate?: number
  passRateDelta?: number
  improvedCount?: number
  regressedCount?: number
  unchangedCount?: number
  newCaseCount?: number
  removedCaseCount?: number
  items: EvalRunComparisonItem[]
}

export interface RegressionSet {
  id: number
  setName: string
  bucket?: string
  riskLevel?: string
  sourceVersionId?: number
  totalCases?: number
  enabledTotal?: number
  notes?: string
  createdBy?: string
  createdAt?: string
}

export interface RegressionSetDetail {
  regressionSet: RegressionSet
  items: Array<Dictionary>
  latestRun?: EvalRun | null
  recentRuns: EvalRun[]
}

export interface EvalRunResult {
  runId?: number
  bucket?: string
  sourceType?: string
  versionId?: number
  regressionSetId?: number
  total: number
  passedTotal: number
  failedTotal: number
  passRate?: number
  createdAt?: string
  results: EvalRunCaseResult[]
}

export interface EvalCaseVersionBatchRunItem {
  versionId: number
  runId?: number
  passRate?: number
  total?: number
  passedTotal?: number
  failedTotal?: number
  errorMessage?: string
}

export interface EvalCaseVersionBatchRunResult {
  totalVersions: number
  successCount: number
  failedCount: number
  items: EvalCaseVersionBatchRunItem[]
}

export interface GovernanceReleaseRecord {
  id: number
  releaseName: string
  targetScope?: string
  releaseStatus?: string
  evalCaseVersionId?: number
  regressionSetId?: number
  baselineEvalRunId?: number
  latestEvalRunId?: number
  grayStrategyJson?: string
  versionSnapshotJson?: string
  notes?: string
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface GovernanceGrayConfig {
  id: number
  configName: string
  queryBucket?: string
  trafficPercent?: number
  riskLevel?: string
  enabled?: number
  targetVersionJson?: string
  notes?: string
  createdAt?: string
  updatedAt?: string
}

export interface GovernanceReleaseEvent {
  id: number
  releaseRecordId: number
  eventType: string
  fromStatus?: string
  toStatus?: string
  operatorName?: string
  eventDetailJson?: string
  createdAt?: string
}

export interface GovernanceDashboard {
  overview?: Dictionary
  metrics_summary?: Dictionary
  metrics_trend?: Dictionary[]
  error_attribution_summary?: Dictionary
  error_attribution_trend?: Dictionary[]
  eval_case_stats?: Dictionary
  recent_releases?: GovernanceReleaseRecord[]
  recent_gray_configs?: GovernanceGrayConfig[]
  recent_eval_versions?: EvalCaseVersion[]
  recent_regression_sets?: RegressionSet[]
  recent_eval_runs?: EvalRun[]
  recent_replays?: ReplaySummary[]
}

export interface GovernanceMetricsDailyItem {
  date?: string
  replayTotal?: number
  degradedTotal?: number
  errorTotal?: number
  avgDurationMs?: number
}

export interface GovernanceErrorAttributionTrendItem {
  date?: string
  errorTotal?: number
  degradedTotal?: number
  errorRate?: number
  degradedRate?: number
  topFailedNode?: string
}

export interface ReleasePreflightResult extends Dictionary {
  ready?: boolean
  checks?: Dictionary
  recommended_actions?: string[]
  overview?: Dictionary
}

export interface GovernanceBackendProbeResult {
  ok: boolean
  mode: 'direct-api' | 'vite-proxy'
  apiBase: string
  latencyMs?: number
  checkedAt: string
  message: string
}

interface Resp<T> {
  code: number
  message: string
  data: T
}

const API_BASE = ((import.meta as any)?.env?.VITE_API_BASE as string | undefined) ?? ''

export function getGovernanceApiBase() {
  return API_BASE
}

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
    throw new Error(`治理后端不可达，请确认服务已启动并检查接口地址配置。${original}`)
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

export function fetchGovernanceDashboard(days = 7) {
  return request<GovernanceDashboard>(
    `/api/admin/governance/dashboard${toQuery({ days, releaseLimit: 8, versionLimit: 8 })}`,
  )
}

export async function probeGovernanceBackend(): Promise<GovernanceBackendProbeResult> {
  const checkedAt = new Date().toISOString()
  const mode = API_BASE ? 'direct-api' : 'vite-proxy'
  const startAt = Date.now()
  try {
    await fetchGovernanceDashboard(1)
    return {
      ok: true,
      mode,
      apiBase: API_BASE,
      latencyMs: Date.now() - startAt,
      checkedAt,
      message: '治理后端连接正常',
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '治理后端探测失败'
    return {
      ok: false,
      mode,
      apiBase: API_BASE,
      latencyMs: Date.now() - startAt,
      checkedAt,
      message,
    }
  }
}

export function fetchGovernanceMetricsDaily(days = 7) {
  return request<GovernanceMetricsDailyItem[]>(
    `/api/admin/governance/metrics/daily/list${toQuery({ page: 1, size: days })}`,
  )
}

export function fetchErrorAttributionTrend(days = 7) {
  return request<GovernanceErrorAttributionTrendItem[]>(
    `/api/admin/governance/metrics/error-attribution/trend${toQuery({ days })}`,
  )
}

export function fetchReplayList(keyword = '', limit = 20) {
  return request<ReplaySummary[]>(`/api/admin/governance/replay/list${toQuery({ keyword, limit })}`)
}

export function fetchReplayCandidates(days = 7, limit = 20, problematicOnly = true, excludeExistingQuery = true) {
  return request<ReplayCandidate[]>(
    `/api/admin/governance/replay/candidates${toQuery({ days, limit, problematicOnly, excludeExistingQuery })}`,
  )
}

export function fetchReplayByRequestId(requestId: string) {
  return request<ReplayDetail>(`/api/admin/governance/replay/request/${encodeURIComponent(requestId)}`)
}

export function fetchReplayByTraceId(traceId: string) {
  return request<ReplayDetail>(`/api/admin/governance/replay/trace/${encodeURIComponent(traceId)}`)
}

export function fetchReplayBySessionId(sessionId: string) {
  return request<ReplayDetail>(`/api/admin/governance/replay/session/${encodeURIComponent(sessionId)}`)
}

export function bootstrapEvalCasesFromReplayBatch(requestBody: Dictionary) {
  return request<Dictionary>('/api/admin/governance/eval-cases/bootstrap-batch-from-replay', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchEvalCaseStats() {
  return request<EvalCaseStats>('/api/admin/governance/eval-cases/stats')
}

export function fetchEvalCases(keyword = '', bucket = '', riskLevel = '', enabled?: number | null) {
  return request<EvalCaseItem[]>(
    `/api/admin/governance/eval-cases/list${toQuery({
      keyword,
      bucket,
      riskLevel,
      enabled: enabled ?? undefined,
      page: 1,
      size: 30,
    })}`,
  )
}

export function exportEvalCases(keyword = '', bucket = '', enabled?: number | null, limit = 200) {
  return request<EvalCaseItem[]>(
    `/api/admin/governance/eval-cases/export${toQuery({
      keyword,
      bucket,
      enabled: enabled ?? undefined,
      limit,
    })}`,
  )
}

export function createEvalCase(requestBody: Dictionary) {
  return request<EvalCaseItem>('/api/admin/governance/eval-cases/add', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function importEvalCases(requestBody: Dictionary) {
  return request<EvalCaseImportResult>('/api/admin/governance/eval-cases/import', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function updateEvalCase(requestBody: Dictionary) {
  return request<EvalCaseItem>('/api/admin/governance/eval-cases/update', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function deleteEvalCase(id: number) {
  return request<Dictionary>(`/api/admin/governance/eval-cases/delete/${id}`, {
    method: 'POST',
  })
}

export function batchToggleEvalCases(requestBody: Dictionary) {
  return request<Dictionary>('/api/admin/governance/eval-cases/batch-enable', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchEvalVersions(bucket = '', page = 1, size = 20) {
  return request<EvalCaseVersion[]>(`/api/admin/governance/eval-case-versions/list${toQuery({ bucket, page, size })}`)
}

export function createEvalCaseVersion(requestBody: Dictionary) {
  return request<EvalCaseVersion>('/api/admin/governance/eval-case-versions/create', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchEvalVersionDetail(versionId: number) {
  return request<EvalCaseVersionDetail>(`/api/admin/governance/eval-case-versions/${versionId}`)
}

export function compareEvalCaseVersions(baseVersionId: number, targetVersionId: number) {
  return request<EvalCaseVersionComparison>(
    `/api/admin/governance/eval-case-versions/compare${toQuery({ baseVersionId, targetVersionId })}`,
  )
}

export function runEvalCaseVersion(versionId: number, limit = 20) {
  return request<EvalRunResult>(
    `/api/admin/governance/eval-case-versions/${versionId}/run${toQuery({ limit })}`,
    { method: 'POST' },
  )
}

export function runEvalCaseVersionBatch(requestBody: Dictionary) {
  return request<EvalCaseVersionBatchRunResult>('/api/admin/governance/eval-case-versions/run-batch', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchEvalRuns(
  bucket = '',
  sourceType = '',
  versionId?: number | null,
  regressionSetId?: number | null,
  page = 1,
  size = 20,
) {
  return request<EvalRun[]>(
    `/api/admin/governance/eval-runs/list${toQuery({
      bucket,
      sourceType,
      versionId: versionId ?? undefined,
      regressionSetId: regressionSetId ?? undefined,
      page,
      size,
    })}`,
  )
}

export function fetchEvalRunDetail(runId: number) {
  return request<EvalRunDetail>(`/api/admin/governance/eval-runs/${runId}`)
}

export function compareEvalRuns(baseRunId: number, targetRunId: number) {
  return request<EvalRunComparison>(
    `/api/admin/governance/eval-runs/compare${toQuery({ baseRunId, targetRunId })}`,
  )
}

export function fetchRegressionSets(bucket = '', riskLevel = '', page = 1, size = 20) {
  return request<RegressionSet[]>(
    `/api/admin/governance/regression-sets/list${toQuery({ bucket, riskLevel, page, size })}`,
  )
}

export function fetchRegressionSetDetail(id: number) {
  return request<RegressionSetDetail>(`/api/admin/governance/regression-sets/${id}`)
}

export function createRegressionSet(requestBody: Dictionary) {
  return request<RegressionSet>('/api/admin/governance/regression-sets/create', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function runRegressionSet(id: number, limit?: number) {
  return request<EvalRunResult>(
    `/api/admin/governance/regression-sets/${id}/run${toQuery({ limit: limit ?? undefined })}`,
    { method: 'POST' },
  )
}

export function fetchReleaseRecords(status = '', page = 1, size = 20) {
  return request<GovernanceReleaseRecord[]>(
    `/api/admin/governance/release-records/list${toQuery({ status, page, size })}`,
  )
}

export function createReleaseRecord(requestBody: Dictionary) {
  return request<GovernanceReleaseRecord>('/api/admin/governance/release-records/add', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function updateReleaseRecord(requestBody: Dictionary) {
  return request<GovernanceReleaseRecord>('/api/admin/governance/release-records/update', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function fetchReleaseRecord(id: number) {
  return request<GovernanceReleaseRecord>(`/api/admin/governance/release-records/${id}`)
}

export function fetchReleaseVerification(id: number, minEvalPassRate = 1) {
  return request<Dictionary>(
    `/api/admin/governance/release-records/${id}/verification${toQuery({ minEvalPassRate })}`,
  )
}

export function fetchReleasePreflight(
  detailLimit = 10,
  taskLimit = 10,
  maxDegradedRate = 0.2,
  evalCaseVersionId?: number | null,
  regressionSetId?: number | null,
  minEvalPassRate = 1,
) {
  return request<ReleasePreflightResult>(
    `/api/admin/governance/release/preflight${toQuery({
      detailLimit,
      taskLimit,
      maxDegradedRate,
      evalCaseVersionId: evalCaseVersionId ?? undefined,
      regressionSetId: regressionSetId ?? undefined,
      minEvalPassRate,
    })}`,
  )
}

export function fetchReleaseEvents(id: number) {
  return request<GovernanceReleaseEvent[]>(`/api/admin/governance/release-records/${id}/events`)
}

export function runReleaseEval(id: number, limit = 20, setAsBaseline = false) {
  return request<EvalRunResult>(
    `/api/admin/governance/release-records/${id}/run-eval${toQuery({ limit, setAsBaseline })}`,
    { method: 'POST' },
  )
}

export function fetchGrayConfigs(page = 1, size = 20, enabled?: number | null) {
  return request<GovernanceGrayConfig[]>(
    `/api/admin/governance/gray-configs/list${toQuery({ page, size, enabled: enabled ?? undefined })}`,
  )
}

export function fetchGrayConfig(id: number) {
  return request<GovernanceGrayConfig>(`/api/admin/governance/gray-configs/${id}`)
}

export function createGrayConfig(requestBody: Dictionary) {
  return request<GovernanceGrayConfig>('/api/admin/governance/gray-configs/add', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function updateGrayConfig(requestBody: Dictionary) {
  return request<GovernanceGrayConfig>('/api/admin/governance/gray-configs/update', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function deleteGrayConfig(id: number) {
  return request<Dictionary>(`/api/admin/governance/gray-configs/delete/${id}`, {
    method: 'POST',
  })
}

export function applyGrayConfig(releaseId: number, configId: number) {
  return request<GovernanceReleaseRecord>(
    `/api/admin/governance/release-records/${releaseId}/apply-gray-config/${configId}`,
    { method: 'POST' },
  )
}

export function transitionRelease(
  releaseId: number,
  targetStatus: string,
  grayConfigId?: number | null,
  minEvalPassRate = 1,
) {
  return request<Dictionary>(
    `/api/admin/governance/release-records/${releaseId}/transition${toQuery({
      targetStatus,
      grayConfigId: grayConfigId ?? undefined,
      minEvalPassRate,
    })}`,
    { method: 'POST' },
  )
}
