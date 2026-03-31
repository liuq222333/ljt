<template>
  <section class="page">
    <header class="head">
      <div>
        <p class="kicker">Replay</p>
        <h1>回放中心</h1>
        <p class="desc">查看请求回放、checkpoint、tool I/O，并把异常样本批量沉淀为评估样本。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="primary" :disabled="loading" @click="loadReplayData">{{ loading ? '刷新中...' : '刷新列表' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successMessage" class="success">{{ successMessage }}</div>

    <div class="toolbar">
      <label>查询方式<select v-model="lookupMode"><option value="request">requestId</option><option value="trace">traceId</option><option value="session">sessionId</option></select></label>
      <label class="keyword">关键字<input v-model="lookupKeyword" :placeholder="lookupPlaceholder" type="text" @keyup.enter="openByLookupKeyword" /></label>
      <label>候选天数<input v-model.number="days" type="number" min="1" max="30" /></label>
      <label>候选上限<input v-model.number="candidateLimit" type="number" min="5" max="100" /></label>
      <button class="ghost" :disabled="detailLoading || !lookupKeyword.trim()" @click="openByLookupKeyword">{{ detailLoading ? '查询中...' : '打开详情' }}</button>
      <button class="ghost" :disabled="loading" @click="loadReplayData">查询列表</button>
      <button class="ghost" @click="resetReplayFilters">重置筛选</button>
      <button class="secondary" :disabled="bootstrapping || selectedCandidateIds.length === 0" @click="bootstrapSelected">{{ bootstrapping ? '生成中...' : `批量生成样本 (${selectedCandidateIds.length})` }}</button>
    </div>

    <section class="grid">
      <article class="panel">
        <div class="panel-head"><h2>Replay 列表</h2><span>{{ replays.length }} 条</span></div>
        <div class="list">
          <button v-for="item in replays" :key="item.requestId" class="item" :class="{ active: item.requestId === activeLookupValue && activeLookupMode === 'request' }" type="button" @click="openReplay(item.requestId, 'request')">
            <div class="item-main">
              <strong>{{ item.lastUserMessage || item.requestId }}</strong>
              <p>{{ item.taskType || 'unknown' }} / {{ item.answerType || 'unknown' }}</p>
              <p>request: {{ item.requestId }}</p>
            </div>
            <div class="item-side">
              <span class="badge" :class="{ warn: item.degraded }">{{ item.degraded ? '降级' : '正常' }}</span>
              <span>{{ item.durationMs ?? 0 }} ms</span>
            </div>
          </button>
          <div v-if="replays.length === 0" class="empty">暂无 replay 数据</div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <h2>候选池</h2>
          <div class="actions">
            <span>问题样本优先</span>
            <button class="ghost small" type="button" :disabled="candidates.length === 0" @click="selectAllCandidates">全选</button>
            <button class="ghost small" type="button" :disabled="selectedCandidateIds.length === 0" @click="clearCandidateSelection">清空选择</button>
          </div>
        </div>
          <div class="list">
            <label v-for="item in candidates" :key="item.requestId" class="candidate">
              <input v-model="selectedCandidateIds" :value="item.requestId" type="checkbox" />
              <div class="item-main">
                <strong>{{ item.lastUserMessage || item.requestId }}</strong>
                <p>{{ item.reason || 'problematic replay' }}</p>
                <p>trace: {{ item.traceId || '-' }} / session: {{ item.sessionId || '-' }}</p>
              </div>
              <div class="actions candidate-actions">
                <span class="badge">{{ item.suggestedRiskLevel || 'medium' }}</span>
                <button class="ghost small" type="button" @click.stop="openReplay(item.requestId, 'request')">查看</button>
              </div>
            </label>
            <div v-if="candidates.length === 0" class="empty">暂无候选样本</div>
          </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>Replay 详情</h2>
        <div class="actions">
          <span>{{ activeLookupValue ? `${activeLookupMode}: ${activeLookupValue}` : '未选择' }}</span>
          <button class="ghost small" :disabled="!activeDetail" @click="clearActiveDetail">清空详情</button>
          <button class="ghost small" :disabled="!activeDetail" @click="exportActiveDetail">导出 JSON</button>
        </div>
      </div>
      <div v-if="detailLoading" class="empty">详情加载中...</div>
      <div v-else-if="activeDetail" class="detail-grid">
        <article class="detail-card">
          <h3>请求摘要</h3>
          <dl>
            <div>
              <dt>requestId</dt>
              <dd>
                {{ activeDetail.summary.requestId || '-' }}
                <button class="ghost tiny" type="button" @click="copyText(activeDetail.summary.requestId)">复制</button>
              </dd>
            </div>
            <div>
              <dt>traceId</dt>
              <dd>
                {{ activeDetail.summary.traceId || '-' }}
                <button class="ghost tiny" type="button" :disabled="!activeDetail.summary.traceId" @click="openReplay(activeDetail.summary.traceId!, 'trace')">按 trace 打开</button>
              </dd>
            </div>
            <div>
              <dt>sessionId</dt>
              <dd>
                {{ activeDetail.summary.sessionId || '-' }}
                <button class="ghost tiny" type="button" :disabled="!activeDetail.summary.sessionId" @click="openReplay(activeDetail.summary.sessionId!, 'session')">按 session 打开</button>
              </dd>
            </div>
            <div><dt>taskType</dt><dd>{{ activeDetail.summary.taskType || '-' }}</dd></div>
            <div><dt>planType</dt><dd>{{ activeDetail.summary.planType || '-' }}</dd></div>
            <div><dt>answerType</dt><dd>{{ activeDetail.summary.answerType || '-' }}</dd></div>
            <div><dt>failedNode</dt><dd>{{ activeDetail.summary.failedNode || '-' }}</dd></div>
            <div><dt>errorCode</dt><dd>{{ activeDetail.summary.errorCode || '-' }}</dd></div>
          </dl>
        </article>

        <article class="detail-card">
          <h3>Checkpoint</h3>
          <div class="compact">
            <article v-for="item in activeDetail.checkpoints" :key="`${item.checkpointOrder}-${item.nodeName}`" class="subitem">
              <strong>#{{ item.checkpointOrder }} / {{ item.nodeName }}</strong>
              <GovernanceJsonBlock title="State Snapshot" :value="item.stateSnapshot" />
            </article>
            <div v-if="activeDetail.checkpoints.length === 0" class="empty">暂无 checkpoint</div>
          </div>
        </article>

        <article class="detail-card">
          <h3>Tool I/O</h3>
          <div class="compact">
            <article v-for="item in activeDetail.toolIos" :key="`${item.stepOrder}-${item.stepId}`" class="subitem">
              <strong>#{{ item.stepOrder }} / {{ item.toolName }}</strong>
              <p>{{ item.purpose || '-' }} / {{ item.executionStatus || '-' }}</p>
              <GovernanceJsonBlock title="Tool Payload" :value="{ input: item.inputPayload, output: item.outputPayload }" />
            </article>
            <div v-if="activeDetail.toolIos.length === 0" class="empty">暂无 tool I/O</div>
          </div>
        </article>

        <article class="detail-card">
          <h3>请求与状态快照</h3>
          <GovernanceJsonBlock title="Request Snapshot" :value="activeDetail.requestSnapshot" />
          <GovernanceJsonBlock title="State Snapshot" :value="activeDetail.stateSnapshot" />
        </article>

        <article class="detail-card">
          <h3>Session 历史</h3>
          <div class="compact">
            <button
              v-for="item in activeDetail.sessionHistory"
              :key="item.requestId"
              class="subitem jump-item"
              type="button"
              @click="openReplay(item.requestId, 'request')"
            >
              <div class="item-main">
                <strong>{{ item.lastUserMessage || item.requestId }}</strong>
                <p>{{ item.taskType || 'unknown' }} / {{ item.answerType || 'unknown' }}</p>
                <p>{{ item.requestId }}</p>
              </div>
              <div class="item-side">
                <span class="badge" :class="{ warn: item.degraded }">{{ item.degraded ? '降级' : '正常' }}</span>
                <span>{{ item.durationMs ?? 0 }} ms</span>
              </div>
            </button>
            <div v-if="activeDetail.sessionHistory.length === 0" class="empty">暂无 session 历史</div>
          </div>
        </article>
      </div>
      <div v-else class="empty">请选择一条 replay 查看详情</div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bootstrapEvalCasesFromReplayBatch, fetchReplayByRequestId, fetchReplayBySessionId, fetchReplayByTraceId, fetchReplayCandidates, fetchReplayList, type ReplayDetail, type ReplayLookupMode, type ReplaySummary, type ReplayCandidate } from '../../api/adminGovernance'
import GovernanceJsonBlock from './GovernanceJsonBlock.vue'
import { downloadJsonFile } from '../../utils/governanceFile'
import { cleanQueryRecord, readQueryEnum, readQueryNumber, readQueryString } from '../../utils/governanceRoute'
import { readGovernanceStorage, writeGovernanceStorage } from '../../utils/governanceStorage'

const route = useRoute()
const router = useRouter()
const replayFilters = readGovernanceStorage('governance-replay-filters', {
  lookupMode: 'request' as ReplayLookupMode,
  lookupKeyword: '',
  days: 7,
  candidateLimit: 20,
})

const initialLookupMode = readQueryEnum(route.query.mode, ['request', 'trace', 'session'] as const, replayFilters.lookupMode)
const initialLookupKeyword = readQueryString(route.query.q, replayFilters.lookupKeyword)
const initialDays = readQueryNumber(route.query.days, replayFilters.days)
const initialCandidateLimit = readQueryNumber(route.query.limit, replayFilters.candidateLimit)

const loading = ref(false)
const detailLoading = ref(false)
const bootstrapping = ref(false)
const error = ref('')
const successMessage = ref('')
const lookupMode = ref<ReplayLookupMode>(initialLookupMode)
const lookupKeyword = ref(initialLookupKeyword)
const days = ref(initialDays)
const candidateLimit = ref(initialCandidateLimit)
const replays = ref<ReplaySummary[]>([])
const candidates = ref<ReplayCandidate[]>([])
const selectedCandidateIds = ref<string[]>([])
const activeDetail = ref<ReplayDetail | null>(null)
const activeLookupMode = ref<ReplayLookupMode>('request')
const activeLookupValue = ref('')

const lookupPlaceholder = computed(() => {
  if (lookupMode.value === 'trace') return '输入 traceId'
  if (lookupMode.value === 'session') return '输入 sessionId'
  return '输入 requestId'
})

async function loadReplayData() {
  loading.value = true
  error.value = ''
  successMessage.value = ''
  try {
    const [replayResult, candidateResult] = await Promise.all([
      fetchReplayList('', 20),
      fetchReplayCandidates(days.value, candidateLimit.value, true, true),
    ])
    replays.value = replayResult
    candidates.value = candidateResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回放数据加载失败'
  } finally {
    loading.value = false
  }
}

async function openReplay(value: string, mode: ReplayLookupMode) {
  detailLoading.value = true
  error.value = ''
  try {
    activeLookupMode.value = mode
    activeLookupValue.value = value
    if (mode === 'trace') activeDetail.value = await fetchReplayByTraceId(value)
    else if (mode === 'session') activeDetail.value = await fetchReplayBySessionId(value)
    else activeDetail.value = await fetchReplayByRequestId(value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回放详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

async function openByLookupKeyword() {
  const value = lookupKeyword.value.trim()
  if (!value) return
  await openReplay(value, lookupMode.value)
}

async function bootstrapSelected() {
  if (selectedCandidateIds.value.length === 0) return
  bootstrapping.value = true
  error.value = ''
  successMessage.value = ''
  try {
    const result = await bootstrapEvalCasesFromReplayBatch({
      requestIds: selectedCandidateIds.value,
      createdBy: 'front-admin',
    })
    successMessage.value = `已批量生成样本：${result.createdCount ?? selectedCandidateIds.value.length} 条`
    selectedCandidateIds.value = []
  } catch (err) {
    error.value = err instanceof Error ? err.message : '批量生成样本失败'
  } finally {
    bootstrapping.value = false
  }
}

function selectAllCandidates() {
  selectedCandidateIds.value = candidates.value.map((item) => item.requestId)
}

function clearCandidateSelection() {
  selectedCandidateIds.value = []
}

function clearActiveDetail() {
  activeDetail.value = null
  activeLookupValue.value = ''
  activeLookupMode.value = 'request'
}

function resetReplayFilters() {
  lookupMode.value = 'request'
  lookupKeyword.value = ''
  days.value = 7
  candidateLimit.value = 20
  selectedCandidateIds.value = []
  clearActiveDetail()
  writeGovernanceStorage('governance-replay-filters', {
    lookupMode: 'request',
    lookupKeyword: '',
    days: 7,
    candidateLimit: 20,
  })
  syncRouteQuery()
}

async function copyCurrentLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    successMessage.value = '已复制当前链接'
  } catch {
    error.value = '复制链接失败'
  }
}

async function copyText(value?: string) {
  if (!value) {
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    successMessage.value = '已复制'
  } catch {
    error.value = '复制失败'
  }
}

function exportActiveDetail() {
  if (!activeDetail.value) {
    return
  }
  const requestId = activeDetail.value.summary.requestId || 'replay-detail'
  downloadJsonFile(`replay-${requestId}.json`, activeDetail.value)
  successMessage.value = '已导出 replay 详情'
}

function syncRouteQuery() {
  const query = cleanQueryRecord({
    mode: lookupMode.value,
    q: lookupKeyword.value.trim() || undefined,
    days: days.value !== 7 ? days.value : undefined,
    limit: candidateLimit.value !== 20 ? candidateLimit.value : undefined,
    request: activeLookupMode.value === 'request' ? activeLookupValue.value : undefined,
    trace: activeLookupMode.value === 'trace' ? activeLookupValue.value : undefined,
    session: activeLookupMode.value === 'session' ? activeLookupValue.value : undefined,
  })
  router.replace({ query }).catch(() => undefined)
}

watch([lookupMode, lookupKeyword, days, candidateLimit], () => {
  writeGovernanceStorage('governance-replay-filters', {
    lookupMode: lookupMode.value,
    lookupKeyword: lookupKeyword.value,
    days: days.value,
    candidateLimit: candidateLimit.value,
  })
  syncRouteQuery()
})

watch([activeLookupMode, activeLookupValue], syncRouteQuery)

onMounted(async () => {
  await loadReplayData()
  const requestId = readQueryString(route.query.request)
  const traceId = readQueryString(route.query.trace)
  const sessionId = readQueryString(route.query.session)
  if (requestId) {
    await openReplay(requestId, 'request')
    return
  }
  if (traceId) {
    await openReplay(traceId, 'trace')
    return
  }
  if (sessionId) {
    await openReplay(sessionId, 'session')
  }
})
</script>

<style scoped>
.page,.list,.compact{display:flex;flex-direction:column;gap:20px}.head,.toolbar,.panel-head,.actions,.item-side{display:flex;gap:12px;flex-wrap:wrap;align-items:end}.head{justify-content:space-between;align-items:flex-start}.kicker{margin:0 0 6px;color:#0ea5e9;font-size:12px;font-weight:700;letter-spacing:.22em;text-transform:uppercase}.head h1{margin:0;font-size:30px}.desc{margin:8px 0 0;color:#52606d}.toolbar label{display:flex;flex-direction:column;gap:6px;color:#475569;font-size:13px}.toolbar .keyword{min-width:260px;flex:1}.grid,.detail-grid{display:grid;gap:16px}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.detail-grid{grid-template-columns:repeat(2,minmax(0,1fr))}input,select{border:1px solid #cbd5e1;border-radius:12px;padding:10px 12px;font:inherit}.panel,.detail-card{border-radius:20px;background:#fff;padding:18px;border:1px solid rgba(148,163,184,.12);box-shadow:0 18px 40px rgba(15,23,42,.08)}.panel-head{justify-content:space-between}.panel-head h2,.detail-card h3{margin:0}.item,.candidate,.subitem{display:flex;justify-content:space-between;gap:12px;padding:14px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0}.candidate input{width:auto;margin-top:4px}.candidate-actions{align-items:center}.item{border:0;cursor:pointer;text-align:left}.item.active{outline:2px solid #38bdf8}.jump-item{cursor:pointer;text-align:left}.jump-item:hover{border-color:#7dd3fc;background:#f0f9ff}.item-main{flex:1}.item-main strong{display:block;margin-bottom:4px}.item-main p,dt,dd{margin:0;color:#64748b;font-size:13px}dt{font-weight:700;color:#0f172a}dd{display:flex;gap:8px;flex-wrap:wrap;align-items:center}dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.badge{padding:6px 10px;border-radius:999px;background:#dbeafe;color:#1d4ed8;font-size:12px;font-weight:700}.badge.warn{background:#ffedd5;color:#9a3412}.primary,.secondary,.ghost{border:0;border-radius:14px;padding:11px 16px;cursor:pointer;font-weight:600}.ghost.small{padding:8px 12px;font-size:12px}.ghost.tiny{padding:6px 10px;font-size:11px}.primary{background:linear-gradient(135deg,#0f172a,#0f766e);color:#fff}.secondary{background:#ccfbf1;color:#0f172a}.ghost{background:#e2e8f0;color:#334155}.error,.success,.empty{padding:12px 16px;border-radius:14px}.error{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.success{background:#ecfeff;color:#155e75;border:1px solid #a5f3fc}.empty{color:#94a3b8;text-align:center;background:#f8fafc}@media (max-width:1100px){.grid,.detail-grid{grid-template-columns:1fr}.head{flex-direction:column}}
</style>
