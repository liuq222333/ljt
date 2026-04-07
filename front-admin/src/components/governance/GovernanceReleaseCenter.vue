<template>
  <section class="admin-page page governance-release-center">
    <header class="head">
      <div>
        <p class="kicker">治理</p>
        <h1>发布管理</h1>
        <p class="desc">围绕发布记录执行回归、预检、灰度配置和状态流转。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="ghost" @click="resetReleaseFilters">重置页面状态</button>
        <button class="primary" :disabled="loading" @click="loadData">{{ loading ? '刷新中...' : '刷新数据' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successMessage" class="success">{{ successMessage }}</div>

    <div class="toolbar admin-toolbar admin-panel">
      <label>状态过滤<select v-model="statusFilter" @change="loadData"><option value="">全部</option><option value="draft">draft</option><option value="ready">ready</option><option value="gray">gray</option><option value="released">released</option><option value="rolled_back">rolled_back</option></select></label>
      <label>最低通过率<input v-model.number="minEvalPassRate" type="number" min="0" max="1" step="0.05" /></label>
      <label>预检详情数<input v-model.number="preflightDetailLimit" type="number" min="1" max="50" /></label>
      <label>任务数<input v-model.number="preflightTaskLimit" type="number" min="1" max="50" /></label>
      <label>最大降级率<input v-model.number="maxDegradedRate" type="number" min="0" max="1" step="0.05" /></label>
      <label>Run 新鲜度(小时)<input v-model.number="maxRunAgeHours" type="number" min="1" max="720" /></label>
    </div>

    <section class="grid">
      <article class="panel admin-panel">
        <h2>{{ releaseForm.id ? '编辑 Release Record' : '新建 Release Record' }}</h2>
        <div class="row">
          <label>名称<input v-model="releaseForm.releaseName" type="text" placeholder="spring-gray-release" /></label>
          <label>目标范围<input v-model="releaseForm.targetScope" type="text" placeholder="agent / search / governance" /></label>
          <label>版本 ID<input v-model.number="releaseForm.evalCaseVersionId" type="number" min="0" /></label>
          <label>回归集 ID<input v-model.number="releaseForm.regressionSetId" type="number" min="0" /></label>
        </div>
        <label class="stack">备注<textarea v-model="releaseForm.notes" rows="3" placeholder="记录发布说明和风险点"></textarea></label>
        <div class="actions">
          <button class="secondary" :disabled="savingRelease" @click="saveReleaseRecord">{{ savingRelease ? '保存中...' : releaseForm.id ? '保存记录' : '创建记录' }}</button>
          <button class="ghost" @click="resetReleaseForm">清空</button>
        </div>

        <div class="list">
          <article v-for="item in releaseRecords" :key="item.id" class="item plain">
            <div class="item-main">
              <strong>{{ item.releaseName }}</strong>
              <p>{{ item.releaseStatus || 'draft' }} / {{ item.targetScope || '-' }}</p>
            </div>
            <div class="actions">
              <button class="ghost" @click="selectRelease(item.id)">详情</button>
              <button class="ghost" @click="fillReleaseForm(item)">编辑</button>
            </div>
          </article>
          <div v-if="releaseRecords.length === 0" class="empty">暂无发布记录</div>
        </div>
      </article>

      <article class="panel admin-panel">
        <h2>{{ grayForm.id ? '编辑 Gray Config' : '新建 Gray Config' }}</h2>
        <div class="row">
          <label>配置名称<input v-model="grayForm.configName" type="text" placeholder="gray-10-percent" /></label>
          <label>Query Bucket<input v-model="grayForm.queryBucket" type="text" placeholder="golden / realtime" /></label>
          <label>流量百分比<input v-model.number="grayForm.trafficPercent" type="number" min="0" max="100" /></label>
          <label>风险等级<select v-model="grayForm.riskLevel"><option value="">默认</option><option value="high">high</option><option value="medium">medium</option><option value="low">low</option></select></label>
          <label>启用<select v-model.number="grayForm.enabled"><option :value="1">启用</option><option :value="0">停用</option></select></label>
        </div>
        <label class="stack">目标版本 JSON<textarea v-model="grayForm.targetVersionJson" rows="3" placeholder='{"agent":"v13","router":"r3"}'></textarea></label>
        <label class="stack">备注<textarea v-model="grayForm.notes" rows="2" placeholder="说明灰度范围和观察项"></textarea></label>
        <div class="actions">
          <button class="secondary" :disabled="savingGray" @click="saveGrayConfig">{{ savingGray ? '保存中...' : grayForm.id ? '保存配置' : '创建配置' }}</button>
          <button class="ghost" @click="resetGrayForm">清空</button>
          <button class="ghost" :disabled="!selectedGrayConfigId" @click="selectedGrayConfigId = 0">清空选中</button>
          <button class="ghost danger" :disabled="!grayForm.id" @click="removeGrayConfig">删除</button>
        </div>

        <div class="list">
          <article v-for="item in grayConfigs" :key="item.id" class="item plain" :class="{ active: item.id === selectedGrayConfigId }">
            <div class="item-main">
              <strong>{{ item.configName }}</strong>
              <p>{{ item.queryBucket || 'all' }} / {{ item.trafficPercent ?? 0 }}% / {{ item.enabled ? '启用' : '停用' }}</p>
            </div>
            <div class="actions">
              <button class="ghost" @click="selectedGrayConfigId = item.id">选择</button>
              <button class="ghost" @click="fillGrayForm(item)">编辑</button>
            </div>
          </article>
          <div v-if="grayConfigs.length === 0" class="empty">暂无灰度配置</div>
        </div>
      </article>
    </section>

    <section class="panel admin-panel">
      <div class="panel-head"><h2>发布详情</h2><span>{{ activeRelease?.releaseName || '未选择' }}</span></div>
      <div v-if="activeRelease" class="detail-grid">
        <article class="detail-card admin-panel">
          <h3>记录摘要</h3>
          <div class="actions">
            <button class="ghost small" type="button" @click="exportActiveReleaseDetail">导出详情</button>
            <button class="ghost small" type="button" @click="clearActiveRelease">清空详情</button>
          </div>
          <p>状态：{{ activeRelease.releaseStatus || 'draft' }}</p>
          <p>目标范围：{{ activeRelease.targetScope || '-' }}</p>
          <p>
            版本 ID：
            <router-link
              v-if="activeRelease.evalCaseVersionId"
              class="inline-link"
              :to="{ path: '/admin/governance/eval', query: { versionId: activeRelease.evalCaseVersionId } }"
            >
              {{ activeRelease.evalCaseVersionId }}
            </router-link>
            <span v-else>-</span>
          </p>
          <p>
            回归集 ID：
            <router-link
              v-if="activeRelease.regressionSetId"
              class="inline-link"
              :to="{ path: '/admin/governance/eval', query: { regressionId: activeRelease.regressionSetId } }"
            >
              {{ activeRelease.regressionSetId }}
            </router-link>
            <span v-else>-</span>
          </p>
          <p>
            Baseline Run：
            <router-link
              v-if="activeRelease.baselineEvalRunId"
              class="inline-link"
              :to="{ path: '/admin/governance/eval', query: { runId: activeRelease.baselineEvalRunId } }"
            >
              {{ activeRelease.baselineEvalRunId }}
            </router-link>
            <span v-else>-</span>
          </p>
          <p>
            Latest Run：
            <router-link
              v-if="activeRelease.latestEvalRunId"
              class="inline-link"
              :to="{ path: '/admin/governance/eval', query: { runId: activeRelease.latestEvalRunId } }"
            >
              {{ activeRelease.latestEvalRunId }}
            </router-link>
            <span v-else>-</span>
          </p>
          <GovernanceJsonBlock title="Version Snapshot" :value="safeParse(activeRelease.versionSnapshotJson)" />
          <GovernanceJsonBlock title="Gray Strategy" :value="safeParse(activeRelease.grayStrategyJson)" />
        </article>

        <article class="detail-card admin-panel">
          <h3>校验与预检</h3>
          <div class="actions">
            <button class="ghost" @click="loadVerification">刷新 verification</button>
            <button class="ghost" @click="loadPreflight">刷新 preflight</button>
          </div>
          <GovernanceJsonBlock title="Verification" :value="verification" />
          <GovernanceJsonBlock title="Preflight" :value="preflight" />
          <div v-if="recommendedActions.length" class="tips">
            <strong>推荐动作</strong>
            <ul>
              <li v-for="item in recommendedActions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </article>

        <article class="detail-card admin-panel">
          <h3>运行与流转</h3>
          <div class="actions">
            <button class="ghost" @click="runEval(false)">执行回归</button>
            <button class="ghost" @click="runEval(true)">执行并设为 baseline</button>
            <button class="ghost" :disabled="!selectedGrayConfigId" @click="applySelectedGray">应用灰度</button>
            <button class="ghost" :disabled="!canTransitionTo('ready')" @click="transition('ready')">转 ready</button>
            <button class="ghost" :disabled="!canTransitionTo('gray')" @click="transition('gray')">转 gray</button>
            <button class="ghost" :disabled="!canTransitionTo('released')" @click="transition('released')">转 released</button>
            <button class="ghost danger" :disabled="!canTransitionTo('rolled_back')" @click="transition('rolled_back')">转 rolled_back</button>
          </div>
          <label>选择灰度配置<select v-model.number="selectedGrayConfigId"><option :value="0">不选择</option><option v-for="item in grayConfigs" :key="item.id" :value="item.id">{{ item.configName }}</option></select></label>
          <GovernanceJsonBlock
            v-if="selectedGrayConfig"
            title="Selected Gray Config"
            :value="safeParse(selectedGrayConfig.targetVersionJson)"
          />
        </article>

        <article class="detail-card admin-panel">
          <h3>事件历史</h3>
          <div class="list">
            <article v-for="event in releaseEvents" :key="event.id" class="subitem">
              <strong>{{ event.eventType }}</strong>
              <p>{{ event.fromStatus || '-' }} -> {{ event.toStatus || '-' }}</p>
              <p>{{ event.createdAt || '-' }}</p>
              <GovernanceJsonBlock title="Event Detail" :value="safeParse(event.eventDetailJson)" />
            </article>
            <div v-if="releaseEvents.length === 0" class="empty">暂无事件历史</div>
          </div>
        </article>
      </div>
      <div v-else class="empty">请选择一条发布记录查看详情</div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  applyGrayConfig,
  createGrayConfig,
  createReleaseRecord,
  deleteGrayConfig,
  fetchGrayConfig,
  fetchGrayConfigs,
  fetchReleaseEvents,
  fetchReleasePreflight,
  fetchReleaseRecord,
  fetchReleaseRecords,
  fetchReleaseVerification,
  runReleaseEval,
  transitionRelease,
  updateGrayConfig,
  updateReleaseRecord,
  type GovernanceGrayConfig,
  type GovernanceReleaseEvent,
  type GovernanceReleaseRecord,
} from '../../api/adminGovernance'
import GovernanceJsonBlock from './GovernanceJsonBlock.vue'
import { downloadJsonFile } from '../../utils/governanceFile'
import { cleanQueryRecord, readQueryNumber, readQueryString } from '../../utils/governanceRoute'
import { readGovernanceStorage, writeGovernanceStorage } from '../../utils/governanceStorage'

const route = useRoute()
const router = useRouter()

const releaseFilters = readGovernanceStorage('governance-release-filters', {
  statusFilter: '',
  minEvalPassRate: 1,
  preflightDetailLimit: 10,
  preflightTaskLimit: 10,
  maxDegradedRate: 0.2,
  maxRunAgeHours: 24,
})

const loading = ref(false)
const savingRelease = ref(false)
const savingGray = ref(false)
const error = ref('')
const successMessage = ref('')
const statusFilter = ref(readQueryString(route.query.status, releaseFilters.statusFilter))
const minEvalPassRate = ref(readQueryNumber(route.query.minEvalPassRate, releaseFilters.minEvalPassRate))
const preflightDetailLimit = ref(readQueryNumber(route.query.preflightDetailLimit, releaseFilters.preflightDetailLimit))
const preflightTaskLimit = ref(readQueryNumber(route.query.preflightTaskLimit, releaseFilters.preflightTaskLimit))
const maxDegradedRate = ref(readQueryNumber(route.query.maxDegradedRate, releaseFilters.maxDegradedRate))
const maxRunAgeHours = ref(readQueryNumber(route.query.maxRunAgeHours, releaseFilters.maxRunAgeHours))
const releaseRecords = ref<GovernanceReleaseRecord[]>([])
const grayConfigs = ref<GovernanceGrayConfig[]>([])
const activeRelease = ref<GovernanceReleaseRecord | null>(null)
const verification = ref<Record<string, unknown> | null>(null)
const preflight = ref<Record<string, unknown> | null>(null)
const releaseEvents = ref<GovernanceReleaseEvent[]>([])
const selectedGrayConfigId = ref(readQueryNumber(route.query.grayConfigId, 0))
const applyingRouteState = ref(false)
const releaseForm = reactive({
  id: 0,
  releaseName: '',
  targetScope: '',
  evalCaseVersionId: 0,
  regressionSetId: 0,
  notes: '',
})
const grayForm = reactive({
  id: 0,
  configName: '',
  queryBucket: '',
  trafficPercent: 10,
  riskLevel: '',
  enabled: 1,
  targetVersionJson: '',
  notes: '',
})

function safeParse(value?: string) {
  if (!value) {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return { raw: value }
  }
}

function resetMessages() {
  error.value = ''
  successMessage.value = ''
}

function resetReleaseForm() {
  Object.assign(releaseForm, {
    id: 0,
    releaseName: '',
    targetScope: '',
    evalCaseVersionId: 0,
    regressionSetId: 0,
    notes: '',
  })
}

function resetGrayForm() {
  Object.assign(grayForm, {
    id: 0,
    configName: '',
    queryBucket: '',
    trafficPercent: 10,
    riskLevel: '',
    enabled: 1,
    targetVersionJson: '',
    notes: '',
  })
}

function clearActiveRelease() {
  activeRelease.value = null
  verification.value = null
  preflight.value = null
  releaseEvents.value = []
}

function resetReleaseFilters() {
  statusFilter.value = ''
  minEvalPassRate.value = 1
  preflightDetailLimit.value = 10
  preflightTaskLimit.value = 10
  maxDegradedRate.value = 0.2
  maxRunAgeHours.value = 24
  selectedGrayConfigId.value = 0
  clearActiveRelease()
  writeGovernanceStorage('governance-release-filters', {
    statusFilter: '',
    minEvalPassRate: 1,
    preflightDetailLimit: 10,
    preflightTaskLimit: 10,
    maxDegradedRate: 0.2,
    maxRunAgeHours: 24,
  })
  syncRouteQuery()
}

const selectedGrayConfig = computed(
  () => grayConfigs.value.find((item) => item.id === selectedGrayConfigId.value) || null,
)

const recommendedActions = computed(() => {
  const fromVerification = Array.isArray((verification.value as any)?.recommended_actions)
    ? (verification.value as any).recommended_actions
    : []
  const fromPreflight = Array.isArray((preflight.value as any)?.recommended_actions)
    ? (preflight.value as any).recommended_actions
    : []
  return Array.from(new Set([...fromVerification, ...fromPreflight])).filter(Boolean)
})

const activeReleaseStatus = computed(() => activeRelease.value?.releaseStatus || 'draft')

function canTransitionTo(targetStatus: string) {
  const currentStatus = activeReleaseStatus.value
  if (!activeRelease.value || currentStatus === targetStatus) {
    return false
  }
  if (currentStatus === 'draft') {
    return targetStatus === 'ready'
  }
  if (currentStatus === 'ready') {
    return targetStatus === 'gray'
  }
  if (currentStatus === 'gray') {
    return targetStatus === 'released' || targetStatus === 'rolled_back'
  }
  if (currentStatus === 'released') {
    return targetStatus === 'rolled_back'
  }
  return false
}

function syncRouteQuery() {
  if (applyingRouteState.value) {
    return
  }
  const query = cleanQueryRecord({
    status: statusFilter.value || undefined,
    minEvalPassRate: minEvalPassRate.value !== 1 ? minEvalPassRate.value : undefined,
    preflightDetailLimit: preflightDetailLimit.value !== 10 ? preflightDetailLimit.value : undefined,
    preflightTaskLimit: preflightTaskLimit.value !== 10 ? preflightTaskLimit.value : undefined,
    maxDegradedRate: maxDegradedRate.value !== 0.2 ? maxDegradedRate.value : undefined,
    maxRunAgeHours: maxRunAgeHours.value !== 24 ? maxRunAgeHours.value : undefined,
    releaseId: activeRelease.value?.id,
    grayConfigId: selectedGrayConfigId.value || undefined,
  })
  router.replace({ query }).catch(() => undefined)
}

async function applyRouteStateFromQuery() {
  const nextStatusFilter = readQueryString(route.query.status, '')
  const nextMinEvalPassRate = readQueryNumber(route.query.minEvalPassRate, 1)
  const nextPreflightDetailLimit = readQueryNumber(route.query.preflightDetailLimit, 10)
  const nextPreflightTaskLimit = readQueryNumber(route.query.preflightTaskLimit, 10)
  const nextMaxDegradedRate = readQueryNumber(route.query.maxDegradedRate, 0.2)
  const nextMaxRunAgeHours = readQueryNumber(route.query.maxRunAgeHours, 24)
  const nextGrayConfigId = readQueryNumber(route.query.grayConfigId, 0)
  const releaseId = readQueryNumber(route.query.releaseId, 0)

  const filtersChanged =
    statusFilter.value !== nextStatusFilter ||
    minEvalPassRate.value !== nextMinEvalPassRate ||
    preflightDetailLimit.value !== nextPreflightDetailLimit ||
    preflightTaskLimit.value !== nextPreflightTaskLimit ||
    maxDegradedRate.value !== nextMaxDegradedRate ||
    maxRunAgeHours.value !== nextMaxRunAgeHours
  const grayConfigChanged = selectedGrayConfigId.value !== nextGrayConfigId
  const activeReleaseChanged = (activeRelease.value?.id || 0) !== releaseId

  applyingRouteState.value = true
  statusFilter.value = nextStatusFilter
  minEvalPassRate.value = nextMinEvalPassRate
  preflightDetailLimit.value = nextPreflightDetailLimit
  preflightTaskLimit.value = nextPreflightTaskLimit
  maxDegradedRate.value = nextMaxDegradedRate
  maxRunAgeHours.value = nextMaxRunAgeHours
  selectedGrayConfigId.value = nextGrayConfigId
  applyingRouteState.value = false

  if (filtersChanged || grayConfigChanged) {
    await loadData()
  }
  if (grayConfigChanged || nextGrayConfigId) {
    await ensureSelectedGrayConfigLoaded()
  }

  if (releaseId) {
    if (activeReleaseChanged) {
      await selectRelease(releaseId)
    }
  } else if (activeRelease.value) {
    clearActiveRelease()
  }
}

async function copyCurrentLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    successMessage.value = '已复制当前链接'
  } catch {
    error.value = '复制链接失败'
  }
}

function exportActiveReleaseDetail() {
  if (!activeRelease.value) {
    return
  }
  downloadJsonFile(`release-${activeRelease.value.id}.json`, {
    release: activeRelease.value,
    verification: verification.value,
    preflight: preflight.value,
    events: releaseEvents.value,
    selectedGrayConfig: selectedGrayConfig.value,
  })
  successMessage.value = '已导出发布详情'
}

async function loadData() {
  loading.value = true
  resetMessages()
  try {
    const [releaseResult, grayResult] = await Promise.all([
      fetchReleaseRecords(statusFilter.value, 1, 20),
      fetchGrayConfigs(1, 20, null),
    ])
    releaseRecords.value = releaseResult
    grayConfigs.value = grayResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发布治理加载失败'
  } finally {
    loading.value = false
  }
}

async function ensureSelectedGrayConfigLoaded() {
  if (!selectedGrayConfigId.value) {
    return
  }
  const exists = grayConfigs.value.some((item) => item.id === selectedGrayConfigId.value)
  if (exists) {
    return
  }
  try {
    const detail = await fetchGrayConfig(selectedGrayConfigId.value)
    grayConfigs.value = [detail, ...grayConfigs.value]
  } catch (err) {
    error.value = err instanceof Error ? err.message : '灰度配置详情加载失败'
  }
}

function fillReleaseForm(item: GovernanceReleaseRecord) {
  Object.assign(releaseForm, {
    id: item.id,
    releaseName: item.releaseName || '',
    targetScope: item.targetScope || '',
    evalCaseVersionId: item.evalCaseVersionId || 0,
    regressionSetId: item.regressionSetId || 0,
    notes: item.notes || '',
  })
}

async function saveReleaseRecord() {
  if (!releaseForm.releaseName.trim()) {
    error.value = '请先填写发布名称'
    return
  }
  savingRelease.value = true
  resetMessages()
  try {
    const payload = {
      id: releaseForm.id || undefined,
      releaseName: releaseForm.releaseName.trim(),
      targetScope: releaseForm.targetScope || undefined,
      evalCaseVersionId: releaseForm.evalCaseVersionId || undefined,
      regressionSetId: releaseForm.regressionSetId || undefined,
      notes: releaseForm.notes || undefined,
    }
    if (releaseForm.id) {
      await updateReleaseRecord(payload)
    } else {
      await createReleaseRecord(payload)
    }
    successMessage.value = '发布记录已保存'
    await loadData()
    resetReleaseForm()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存发布记录失败'
  } finally {
    savingRelease.value = false
  }
}

function fillGrayForm(item: GovernanceGrayConfig) {
  Object.assign(grayForm, {
    id: item.id,
    configName: item.configName || '',
    queryBucket: item.queryBucket || '',
    trafficPercent: item.trafficPercent ?? 10,
    riskLevel: item.riskLevel || '',
    enabled: item.enabled ?? 1,
    targetVersionJson: item.targetVersionJson || '',
    notes: item.notes || '',
  })
}

async function saveGrayConfig() {
  if (!grayForm.configName.trim()) {
    error.value = '请先填写灰度配置名称'
    return
  }
  savingGray.value = true
  resetMessages()
  try {
    const payload = {
      id: grayForm.id || undefined,
      configName: grayForm.configName.trim(),
      queryBucket: grayForm.queryBucket || undefined,
      trafficPercent: grayForm.trafficPercent,
      riskLevel: grayForm.riskLevel || undefined,
      enabled: grayForm.enabled,
      targetVersionJson: grayForm.targetVersionJson || undefined,
      notes: grayForm.notes || undefined,
    }
    if (grayForm.id) {
      await updateGrayConfig(payload)
    } else {
      await createGrayConfig(payload)
    }
    successMessage.value = '灰度配置已保存'
    await loadData()
    resetGrayForm()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存灰度配置失败'
  } finally {
    savingGray.value = false
  }
}

async function removeGrayConfig() {
  if (!grayForm.id) {
    return
  }
  resetMessages()
  try {
    await deleteGrayConfig(grayForm.id)
    successMessage.value = '灰度配置已删除'
    await loadData()
    resetGrayForm()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除灰度配置失败'
  }
}

async function selectRelease(id: number) {
  resetMessages()
  try {
    activeRelease.value = await fetchReleaseRecord(id)
    await Promise.all([loadVerification(), loadPreflight(), loadReleaseEvents()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发布详情加载失败'
  }
}

async function loadVerification() {
  if (!activeRelease.value) {
    return
  }
  verification.value = await fetchReleaseVerification(activeRelease.value.id, minEvalPassRate.value, maxRunAgeHours.value)
}

async function loadPreflight() {
  if (!activeRelease.value) {
    return
  }
  preflight.value = await fetchReleasePreflight(
    preflightDetailLimit.value,
    preflightTaskLimit.value,
    maxDegradedRate.value,
    activeRelease.value.evalCaseVersionId || null,
    activeRelease.value.regressionSetId || null,
    minEvalPassRate.value,
    maxRunAgeHours.value,
  )
}

async function loadReleaseEvents() {
  if (!activeRelease.value) {
    return
  }
  releaseEvents.value = await fetchReleaseEvents(activeRelease.value.id)
}

async function runEval(setAsBaseline: boolean) {
  if (!activeRelease.value) {
    return
  }
  resetMessages()
  try {
    await runReleaseEval(activeRelease.value.id, 20, setAsBaseline)
    successMessage.value = setAsBaseline ? '已执行回归并设为 baseline' : '已执行发布回归'
    await selectRelease(activeRelease.value.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '执行发布回归失败'
  }
}

async function applySelectedGray() {
  if (!activeRelease.value || !selectedGrayConfigId.value) {
    return
  }
  resetMessages()
  try {
    activeRelease.value = await applyGrayConfig(activeRelease.value.id, selectedGrayConfigId.value)
    successMessage.value = '已应用灰度配置'
    await selectRelease(activeRelease.value.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用灰度配置失败'
  }
}

async function transition(targetStatus: string) {
  if (!activeRelease.value) {
    return
  }
  resetMessages()
  try {
    await transitionRelease(
      activeRelease.value.id,
      targetStatus,
      selectedGrayConfigId.value || null,
      minEvalPassRate.value,
      maxRunAgeHours.value,
    )
    successMessage.value = `已流转到 ${targetStatus}`
    await selectRelease(activeRelease.value.id)
    await loadData()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态流转失败'
  }
}

watch([statusFilter, minEvalPassRate, preflightDetailLimit, preflightTaskLimit, maxDegradedRate, maxRunAgeHours], () => {
  if (applyingRouteState.value) {
    return
  }
  writeGovernanceStorage('governance-release-filters', {
    statusFilter: statusFilter.value,
    minEvalPassRate: minEvalPassRate.value,
    preflightDetailLimit: preflightDetailLimit.value,
    preflightTaskLimit: preflightTaskLimit.value,
    maxDegradedRate: maxDegradedRate.value,
    maxRunAgeHours: maxRunAgeHours.value,
  })
  syncRouteQuery()
})

watch([() => activeRelease.value?.id, selectedGrayConfigId], async () => {
  if (applyingRouteState.value) {
    return
  }
  await ensureSelectedGrayConfigLoaded()
  syncRouteQuery()
})

watch(
  () => route.query,
  async () => {
    await applyRouteStateFromQuery()
  },
  { deep: true },
)

onMounted(async () => {
  await loadData()
  await applyRouteStateFromQuery()
})
</script>

<style scoped>
.page,
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.head,
.toolbar,
.row,
.actions,
.panel-head {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: flex-end;
}

.head,
.panel-head {
  justify-content: space-between;
  align-items: flex-start;
}

.kicker {
  margin: 0;
  color: var(--admin-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.head h1 {
  margin: 4px 0 0;
  font-size: 22px;
  color: var(--admin-text-primary);
}

.desc {
  margin: 6px 0 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
}

.toolbar {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-panel);
  background: var(--admin-bg-surface);
  box-shadow: var(--admin-shadow-panel);
  padding: 12px;
}

.toolbar label,
.row label,
.stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.stack {
  margin-top: 8px;
}

.grid,
.detail-grid {
  display: grid;
  gap: 12px;
}

.grid,
.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

input,
select,
textarea {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 10px;
  font: inherit;
  width: 100%;
  box-sizing: border-box;
  background: var(--admin-bg-surface);
}

.panel,
.detail-card {
  border-radius: var(--admin-radius-panel);
  background: var(--admin-bg-surface);
  padding: 12px;
  border: 1px solid var(--admin-border);
  box-shadow: var(--admin-shadow-panel);
}

.panel h2,
.detail-card h3 {
  margin: 0;
  color: var(--admin-text-primary);
  font-size: 15px;
}

.item,
.subitem {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px;
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  border: 1px solid var(--admin-border);
}

.item.plain,
.subitem {
  align-items: flex-start;
}

.item.active {
  border-color: var(--admin-border-strong);
  background: #f0f3f7;
}

.item-main {
  flex: 1;
}

.item-main strong {
  display: block;
  margin-bottom: 4px;
  color: var(--admin-text-primary);
  font-size: 13px;
}

.item-main p {
  margin: 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.inline-link {
  color: var(--admin-accent);
  font-weight: 700;
  text-decoration: none;
}

.inline-link:hover {
  text-decoration: underline;
}

.primary,
.secondary,
.ghost {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
}

.primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #ffffff;
}

.secondary {
  background: var(--admin-accent-soft);
  color: var(--admin-text-primary);
}

.ghost.danger {
  background: #fbeeed;
  color: #9f2f24;
}

.error,
.success,
.empty {
  padding: 10px 12px;
  border-radius: var(--admin-radius-control);
  font-size: 12px;
}

.error {
  background: #fbeeed;
  color: #9f2f24;
  border: 1px solid #efc3bc;
}

.success {
  background: #ebf8f1;
  color: #1f7a4d;
  border: 1px solid #b8dfcb;
}

.empty {
  color: var(--admin-text-muted);
  border: 1px dashed var(--admin-border);
  text-align: center;
}
</style>
