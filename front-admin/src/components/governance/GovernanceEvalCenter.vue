<template>
  <section class="page">
    <header class="head">
      <div>
        <p class="kicker">Evaluation</p>
        <h1>评估与回归</h1>
        <p class="desc">管理样本、版本快照、回归集和评估运行，支撑发布前检查。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="ghost" @click="resetEvalFilters">重置页面状态</button>
        <button class="primary" :disabled="loading" @click="loadData">{{ loading ? '刷新中...' : '刷新数据' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successMessage" class="success">{{ successMessage }}</div>

    <section class="cards">
      <article class="card"><span>样本总量</span><strong>{{ stats.total ?? 0 }}</strong></article>
      <article class="card"><span>启用样本</span><strong>{{ stats.enabled_total ?? 0 }}</strong></article>
      <article class="card"><span>版本快照</span><strong>{{ versions.length }}</strong></article>
      <article class="card"><span>回归集</span><strong>{{ regressionSets.length }}</strong></article>
    </section>

    <section class="grid">
      <article class="panel">
        <h2>版本快照</h2>
        <div class="row">
          <label>名称<input v-model="versionForm.versionName" type="text" placeholder="refund-v1" /></label>
          <label>Bucket<input v-model="versionForm.bucket" type="text" placeholder="golden" /></label>
          <label>关键字<input v-model="versionForm.keyword" type="text" placeholder="可选" /></label>
          <label>启用状态<select v-model.number="versionForm.enabled"><option :value="-1">全部</option><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <label>上限<input v-model.number="versionForm.limit" type="number" min="1" max="1000" /></label>
        </div>
        <label class="stack">备注<textarea v-model="versionForm.notes" rows="2" placeholder="记录版本用途"></textarea></label>
        <div class="actions">
          <button class="secondary" :disabled="creatingVersion" @click="submitVersion">{{ creatingVersion ? '创建中...' : '创建版本快照' }}</button>
          <button class="ghost" @click="resetVersionForm">清空</button>
          <button class="ghost" :disabled="versions.length === 0" @click="selectAllVersions">全选当前版本</button>
          <button class="ghost" :disabled="selectedVersionIds.length === 0" @click="clearSelectedVersions">清空版本选择</button>
        </div>
        <div class="row">
          <label>Batch limit<input v-model.number="batchRunLimit" type="number" min="1" max="200" /></label>
          <button class="ghost" :disabled="selectedVersionIds.length === 0 || batchRunning" @click="runVersionBatch">{{ batchRunning ? '执行中...' : `批量运行 (${selectedVersionIds.length})` }}</button>
        </div>
        <div class="list">
          <label v-for="version in versions" :key="version.id" class="item">
            <input v-model="selectedVersionIds" :value="version.id" type="checkbox" />
            <div class="item-main">
              <strong>{{ version.versionName }}</strong>
              <p>{{ version.bucket || 'all' }} / {{ version.totalCases ?? 0 }} 条 / 启用 {{ version.enabledTotal ?? 0 }}</p>
            </div>
            <div class="actions">
              <button class="ghost" @click.prevent="inspectVersion(version.id)">详情</button>
              <button class="ghost" @click.prevent="runVersion(version.id)">运行</button>
              <button class="ghost" @click.prevent="pickCompareVersion(version.id)">对比</button>
            </div>
          </label>
        </div>
        <div class="row">
          <label>基线版本<input v-model.number="versionCompare.base" type="number" min="0" /></label>
          <label>目标版本<input v-model.number="versionCompare.target" type="number" min="0" /></label>
          <button class="ghost" @click="runVersionCompare">版本对比</button>
        </div>
        <div v-if="versionComparison" class="info">
          <span>新增 {{ versionComparison.addedCount ?? 0 }}</span>
          <span>移除 {{ versionComparison.removedCount ?? 0 }}</span>
          <span>变更 {{ versionComparison.changedCount ?? 0 }}</span>
          <span>未变 {{ versionComparison.unchangedCount ?? 0 }}</span>
        </div>
        <div v-if="versionComparison?.items?.length" class="compact">
          <article
            v-for="item in versionComparison.items.slice(0, 8)"
            :key="`${item.changeType}-${item.caseId}-${item.queryText}`"
            class="subitem"
          >
            <strong>{{ item.caseName || item.queryText || `Case ${item.caseId ?? '-'}` }}</strong>
            <p>{{ item.changeType }}</p>
          </article>
        </div>
        <div v-if="batchRunResult" class="detail">
          <h3>批量运行结果</h3>
          <div class="info">
            <span>版本 {{ batchRunResult.totalVersions ?? 0 }}</span>
            <span>成功 {{ batchRunResult.successCount ?? 0 }}</span>
            <span>失败 {{ batchRunResult.failedCount ?? 0 }}</span>
          </div>
          <div class="compact">
            <article
              v-for="item in batchRunResult.items.slice(0, 8)"
              :key="`${item.versionId}-${item.runId || 'failed'}`"
              class="subitem"
            >
              <strong>版本 {{ item.versionId }}</strong>
              <p>Run {{ item.runId || '-' }} / 通过率 {{ formatRate(item.passRate) }}</p>
              <p v-if="item.errorMessage">{{ item.errorMessage }}</p>
              <p v-else>通过 {{ item.passedTotal ?? 0 }} / 失败 {{ item.failedTotal ?? 0 }}</p>
            </article>
          </div>
        </div>
        <div v-if="activeVersionDetail" class="detail">
          <div class="detail-head">
            <h3>{{ activeVersionDetail.version.versionName }}</h3>
            <div class="actions">
              <button class="ghost small" type="button" @click="exportVersionDetail">导出详情</button>
              <button class="ghost small" type="button" @click="activeVersionDetail = null">清空</button>
            </div>
          </div>
          <p>最近运行：{{ formatRun(activeVersionDetail.latestRun) }}</p>
          <div class="compact">
            <article v-for="item in activeVersionDetail.items.slice(0, 8)" :key="`${item.caseId}-${item.queryText}`" class="subitem">
              <strong>{{ item.caseName || `Case ${item.caseId ?? '-'}` }}</strong>
              <p>{{ item.queryText || '-' }}</p>
            </article>
          </div>
        </div>
      </article>

      <article class="panel">
        <h2>运行与回归集</h2>
        <div class="row">
          <label>sourceType<input v-model="runFilter.sourceType" type="text" placeholder="version_snapshot" /></label>
          <label>versionId<input v-model.number="runFilter.versionId" type="number" min="0" /></label>
          <label>regressionSetId<input v-model.number="runFilter.regressionSetId" type="number" min="0" /></label>
          <button class="ghost" @click="loadEvalRuns">查询运行</button>
        </div>
        <div class="list">
          <article v-for="run in evalRuns" :key="run.id" class="item plain">
            <div class="item-main">
              <strong>#{{ run.id }} / {{ run.sourceType || 'live_cases' }}</strong>
              <p>{{ run.bucket || 'all' }} / {{ formatRate(run.passRate) }}</p>
            </div>
            <div class="actions">
              <button class="ghost" @click="inspectRun(run.id)">详情</button>
              <button class="ghost" @click="pickRunCompare(run.id)">对比</button>
            </div>
          </article>
        </div>
        <div class="row">
          <label>基线运行<input v-model.number="runCompare.base" type="number" min="0" /></label>
          <label>目标运行<input v-model.number="runCompare.target" type="number" min="0" /></label>
          <button class="ghost" @click="runEvalRunCompare">运行对比</button>
        </div>
        <div v-if="runComparison" class="info">
          <span>通过率变化 {{ formatDelta(runComparison.passRateDelta) }}</span>
          <span>提升 {{ runComparison.improvedCount ?? 0 }}</span>
          <span>回退 {{ runComparison.regressedCount ?? 0 }}</span>
        </div>
        <div v-if="runComparison?.items?.length" class="compact">
          <article
            v-for="item in runComparison.items.slice(0, 8)"
            :key="`${item.changeType}-${item.caseId}-${item.queryText}`"
            class="subitem"
          >
            <strong>{{ item.caseName || item.queryText || `Case ${item.caseId ?? '-'}` }}</strong>
            <p>{{ item.changeType || 'unchanged' }}</p>
            <p>{{ item.baseActualPlanType || '-' }} -> {{ item.targetActualPlanType || '-' }}</p>
          </article>
        </div>
        <div class="row">
          <label>回归集名称<input v-model="regressionForm.setName" type="text" placeholder="golden-critical" /></label>
          <label>Bucket<input v-model="regressionForm.bucket" type="text" placeholder="golden" /></label>
          <label>风险<select v-model="regressionForm.riskLevel"><option value="">全部</option><option value="high">high</option><option value="medium">medium</option><option value="low">low</option></select></label>
          <label>来源版本<input v-model.number="regressionForm.sourceVersionId" type="number" min="0" /></label>
          <label>上限<input v-model.number="regressionForm.limit" type="number" min="1" max="1000" /></label>
        </div>
        <label class="stack">备注<textarea v-model="regressionForm.notes" rows="2" placeholder="说明回归集目的"></textarea></label>
        <div class="actions">
          <button class="secondary" :disabled="creatingRegressionSet" @click="submitRegressionSet">{{ creatingRegressionSet ? '创建中...' : '创建回归集' }}</button>
        </div>
        <div class="list">
          <article v-for="set in regressionSets" :key="set.id" class="item plain">
            <div class="item-main">
              <strong>{{ set.setName }}</strong>
              <p>{{ set.bucket || 'all' }} / {{ set.riskLevel || 'mixed' }} / {{ set.totalCases ?? 0 }} 条</p>
            </div>
            <div class="actions">
              <button class="ghost" @click="inspectRegressionSet(set.id)">详情</button>
              <button class="ghost" @click="runRegression(set.id)">运行</button>
            </div>
          </article>
        </div>
        <div v-if="regressionSetDetail" class="detail">
          <div class="detail-head">
            <h3>{{ regressionSetDetail.regressionSet.setName }}</h3>
            <div class="actions">
              <button class="ghost small" type="button" @click="exportRegressionDetail">导出详情</button>
              <button class="ghost small" type="button" @click="regressionSetDetail = null">清空</button>
            </div>
          </div>
          <p>最近运行：{{ formatRun(regressionSetDetail.latestRun) }}</p>
        </div>
        <div v-if="evalRunDetail" class="detail">
          <div class="detail-head">
            <h3>运行 {{ evalRunDetail.run.id }}</h3>
            <div class="actions">
              <button class="ghost small" type="button" @click="exportRunDetail">导出详情</button>
              <button class="ghost small" type="button" @click="evalRunDetail = null">清空</button>
            </div>
          </div>
          <div class="compact">
            <article v-for="result in evalRunDetail.results.slice(0, 8)" :key="`${result.caseId}-${result.queryText}`" class="subitem">
              <strong>{{ result.caseName || result.queryText || `case-${result.caseId}` }}</strong>
              <p>{{ result.queryText || '-' }}</p>
              <p>task {{ result.expectedTaskType || '-' }} -> {{ result.actualTaskType || '-' }}</p>
              <p>plan {{ result.expectedPlanType || '-' }} -> {{ result.actualPlanType || '-' }}</p>
            </article>
          </div>
        </div>
      </article>
    </section>

    <section class="grid">
      <article class="panel">
        <h2>样本导入导出</h2>
        <div class="actions">
          <button class="ghost" :disabled="exportingCases" @click="handleExportCases">{{ exportingCases ? '导出中...' : '按当前过滤导出 JSON' }}</button>
          <button class="ghost" :disabled="exportingCases" @click="downloadExportCases">{{ exportingCases ? '导出中...' : '导出为文件' }}</button>
          <button class="ghost" @click="openImportFilePicker">选择文件</button>
          <button class="secondary" :disabled="importingCases" @click="handleImportCases">{{ importingCases ? '导入中...' : '导入 JSON' }}</button>
        </div>
        <input ref="importFileInput" class="hidden-input" type="file" accept=".json,application/json" @change="handleImportFileChange" />
        <label class="stack">样本 JSON<textarea v-model="importExportJson" rows="12" placeholder='[{"caseName":"退款规则","queryText":"活动可以提现吗？","bucket":"golden"}]'></textarea></label>
        <div v-if="importResult" class="info">
          <span>总数 {{ importResult.total }}</span>
          <span>新增 {{ importResult.inserted }}</span>
          <span>更新 {{ importResult.updated }}</span>
        </div>
      </article>

      <article class="panel">
        <h2>样本列表与编辑</h2>
        <div class="row">
          <label>关键字<input v-model="caseFilter.keyword" type="text" placeholder="caseName / queryText" /></label>
          <label>Bucket<input v-model="caseFilter.bucket" type="text" placeholder="golden" /></label>
          <label>风险<select v-model="caseFilter.riskLevel"><option value="">全部</option><option value="high">high</option><option value="medium">medium</option><option value="low">low</option></select></label>
          <label>状态<select v-model.number="caseFilter.enabled"><option :value="-1">全部</option><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <button class="ghost" @click="loadEvalCases">查询</button>
          <button class="ghost" :disabled="evalCases.length === 0" @click="selectAllCases">全选当前页</button>
          <button class="ghost" :disabled="selectedCaseIds.length === 0" @click="clearSelectedCases">清空选择</button>
          <button class="ghost" :disabled="selectedCaseIds.length === 0" @click="toggleEvalCases(1)">批量启用</button>
          <button class="ghost" :disabled="selectedCaseIds.length === 0" @click="toggleEvalCases(0)">批量停用</button>
        </div>
        <div class="list">
          <label v-for="item in evalCases" :key="item.id" class="item">
            <input v-model="selectedCaseIds" :value="item.id" type="checkbox" />
            <div class="item-main">
              <strong>{{ item.caseName || `Case ${item.id}` }}</strong>
              <p>{{ item.queryText || '-' }}</p>
              <p>{{ item.bucket || 'all' }} / {{ item.riskLevel || 'mixed' }} / {{ item.enabled ? '启用' : '停用' }}</p>
            </div>
            <div class="actions">
              <button class="ghost" @click.prevent="fillEvalCaseForm(item)">编辑</button>
            </div>
          </label>
        </div>
        <div class="row">
          <label>名称<input v-model="evalCaseForm.caseName" type="text" /></label>
          <label>Bucket<input v-model="evalCaseForm.bucket" type="text" /></label>
          <label>风险<select v-model="evalCaseForm.riskLevel"><option value="high">high</option><option value="medium">medium</option><option value="low">low</option></select></label>
          <label>启用<select v-model.number="evalCaseForm.enabled"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <label>期望 Task<input v-model="evalCaseForm.expectedTaskType" type="text" /></label>
          <label>期望 Plan<input v-model="evalCaseForm.expectedPlanType" type="text" /></label>
          <label>期望 Answer<input v-model="evalCaseForm.expectedAnswerType" type="text" /></label>
          <label>Tags JSON<input v-model="evalCaseForm.tagsJson" type="text" /></label>
        </div>
        <label class="stack">Query<textarea v-model="evalCaseForm.queryText" rows="3" placeholder="输入用于回归的用户问题"></textarea></label>
        <label class="stack">备注<textarea v-model="evalCaseForm.notes" rows="2" placeholder="记录边界和风险说明"></textarea></label>
        <div class="actions">
          <button class="secondary" :disabled="savingEvalCase" @click="saveEvalCase">{{ savingEvalCase ? '保存中...' : evalCaseForm.id ? '保存样本' : '创建样本' }}</button>
          <button class="ghost" @click="resetEvalCaseForm">清空</button>
          <button class="ghost danger" :disabled="!evalCaseForm.id || deletingEvalCase" @click="removeEvalCase">{{ deletingEvalCase ? '删除中...' : '删除样本' }}</button>
        </div>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  batchToggleEvalCases,
  compareEvalCaseVersions,
  compareEvalRuns,
  createEvalCase,
  createEvalCaseVersion,
  createRegressionSet,
  deleteEvalCase,
  exportEvalCases,
  fetchEvalCases,
  fetchEvalCaseStats,
  fetchEvalRunDetail,
  fetchEvalRuns,
  fetchEvalVersionDetail,
  fetchEvalVersions,
  fetchRegressionSetDetail,
  fetchRegressionSets,
  importEvalCases,
  runEvalCaseVersion,
  runEvalCaseVersionBatch,
  runRegressionSet,
  updateEvalCase,
  type EvalCaseImportResult,
  type EvalCaseItem,
  type EvalCaseStats,
  type EvalCaseVersion,
  type EvalCaseVersionBatchRunResult,
  type EvalCaseVersionComparison,
  type EvalCaseVersionDetail,
  type EvalRun,
  type EvalRunComparison,
  type EvalRunDetail,
  type RegressionSet,
  type RegressionSetDetail,
} from '../../api/adminGovernance'
import { downloadJsonFile } from '../../utils/governanceFile'
import { cleanQueryRecord, readQueryNumber, readQueryString } from '../../utils/governanceRoute'
import { readGovernanceStorage, writeGovernanceStorage } from '../../utils/governanceStorage'

const route = useRoute()
const router = useRouter()

const evalFilters = readGovernanceStorage('governance-eval-filters', {
  batchRunLimit: 20,
  runFilter: { sourceType: '', versionId: 0, regressionSetId: 0 },
  caseFilter: { keyword: '', bucket: '', riskLevel: '', enabled: -1 },
})

const loading = ref(false)
const error = ref('')
const successMessage = ref('')
const creatingVersion = ref(false)
const batchRunning = ref(false)
const creatingRegressionSet = ref(false)
const exportingCases = ref(false)
const importingCases = ref(false)
const savingEvalCase = ref(false)
const deletingEvalCase = ref(false)

const stats = reactive<EvalCaseStats>({
  total: 0,
  enabled_total: 0,
  disabled_total: 0,
  by_bucket: {},
  by_risk_level: {},
})

const versions = ref<EvalCaseVersion[]>([])
const regressionSets = ref<RegressionSet[]>([])
const evalRuns = ref<EvalRun[]>([])
const evalCases = ref<EvalCaseItem[]>([])
const activeVersionDetail = ref<EvalCaseVersionDetail | null>(null)
const regressionSetDetail = ref<RegressionSetDetail | null>(null)
const evalRunDetail = ref<EvalRunDetail | null>(null)
const versionComparison = ref<EvalCaseVersionComparison | null>(null)
const runComparison = ref<EvalRunComparison | null>(null)
const batchRunResult = ref<EvalCaseVersionBatchRunResult | null>(null)
const importResult = ref<EvalCaseImportResult | null>(null)
const selectedVersionIds = ref<number[]>([])
const selectedCaseIds = ref<number[]>([])
const batchRunLimit = ref(readQueryNumber(route.query.batchLimit, evalFilters.batchRunLimit))
const importExportJson = ref('')
const importFileInput = ref<HTMLInputElement | null>(null)

const versionForm = reactive({
  versionName: '',
  keyword: '',
  bucket: 'golden',
  enabled: 1,
  limit: 200,
  notes: '',
})

const versionCompare = reactive({
  base: readQueryNumber(route.query.versionCompareBase, 0),
  target: readQueryNumber(route.query.versionCompareTarget, 0),
})

const regressionForm = reactive({
  setName: '',
  bucket: 'golden',
  riskLevel: 'high',
  sourceVersionId: 0,
  limit: 100,
  notes: '',
})

const runFilter = reactive({
  sourceType: readQueryString(route.query.runSource, evalFilters.runFilter.sourceType),
  versionId: readQueryNumber(route.query.runVersionId, evalFilters.runFilter.versionId),
  regressionSetId: readQueryNumber(route.query.runRegressionId, evalFilters.runFilter.regressionSetId),
})

const runCompare = reactive({
  base: readQueryNumber(route.query.runCompareBase, 0),
  target: readQueryNumber(route.query.runCompareTarget, 0),
})

const caseFilter = reactive({
  keyword: readQueryString(route.query.caseKeyword, evalFilters.caseFilter.keyword),
  bucket: readQueryString(route.query.caseBucket, evalFilters.caseFilter.bucket),
  riskLevel: readQueryString(route.query.caseRisk, evalFilters.caseFilter.riskLevel),
  enabled: readQueryNumber(route.query.caseEnabled, evalFilters.caseFilter.enabled),
})

const evalCaseForm = reactive({
  id: 0,
  caseName: '',
  queryText: '',
  bucket: 'golden',
  riskLevel: 'high',
  enabled: 1,
  expectedTaskType: '',
  expectedPlanType: '',
  expectedAnswerType: '',
  tagsJson: '',
  notes: '',
})

const formatRate = (value?: number) =>
  typeof value === 'number' && !Number.isNaN(value) ? `${(value * 100).toFixed(1)}%` : '0%'
const formatDelta = (value?: number) =>
  typeof value === 'number' && !Number.isNaN(value) ? `${value >= 0 ? '+' : ''}${(value * 100).toFixed(1)}%` : '0%'
const formatRun = (run?: EvalRun | null) =>
  run ? `#${run.id} / ${run.sourceType || 'live_cases'} / ${formatRate(run.passRate)}` : '暂无'

function resetMessages() {
  error.value = ''
  successMessage.value = ''
}

function resetVersionForm() {
  Object.assign(versionForm, {
    versionName: '',
    keyword: '',
    bucket: 'golden',
    enabled: 1,
    limit: 200,
    notes: '',
  })
}

function resetEvalCaseForm() {
  Object.assign(evalCaseForm, {
    id: 0,
    caseName: '',
    queryText: '',
    bucket: 'golden',
    riskLevel: 'high',
    enabled: 1,
    expectedTaskType: '',
    expectedPlanType: '',
    expectedAnswerType: '',
    tagsJson: '',
    notes: '',
  })
}

function resetEvalFilters() {
  batchRunLimit.value = 20
  runFilter.sourceType = ''
  runFilter.versionId = 0
  runFilter.regressionSetId = 0
  caseFilter.keyword = ''
  caseFilter.bucket = ''
  caseFilter.riskLevel = ''
  caseFilter.enabled = -1
  versionCompare.base = 0
  versionCompare.target = 0
  runCompare.base = 0
  runCompare.target = 0
  activeVersionDetail.value = null
  regressionSetDetail.value = null
  evalRunDetail.value = null
  versionComparison.value = null
  runComparison.value = null
  batchRunResult.value = null
  selectedVersionIds.value = []
  selectedCaseIds.value = []
  writeGovernanceStorage('governance-eval-filters', {
    batchRunLimit: 20,
    runFilter: { sourceType: '', versionId: 0, regressionSetId: 0 },
    caseFilter: { keyword: '', bucket: '', riskLevel: '', enabled: -1 },
  })
  syncRouteQuery()
}

function selectAllVersions() {
  selectedVersionIds.value = versions.value.map((item) => item.id)
}

function clearSelectedVersions() {
  selectedVersionIds.value = []
}

function selectAllCases() {
  selectedCaseIds.value = evalCases.value.map((item) => item.id)
}

function clearSelectedCases() {
  selectedCaseIds.value = []
}

function syncRouteQuery() {
  const query = cleanQueryRecord({
    batchLimit: batchRunLimit.value !== 20 ? batchRunLimit.value : undefined,
    runSource: runFilter.sourceType || undefined,
    runVersionId: runFilter.versionId || undefined,
    runRegressionId: runFilter.regressionSetId || undefined,
    caseKeyword: caseFilter.keyword || undefined,
    caseBucket: caseFilter.bucket || undefined,
    caseRisk: caseFilter.riskLevel || undefined,
    caseEnabled: caseFilter.enabled >= 0 ? caseFilter.enabled : undefined,
    versionId: activeVersionDetail.value?.version.id,
    regressionId: regressionSetDetail.value?.regressionSet.id,
    runId: evalRunDetail.value?.run.id,
    versionCompareBase: versionCompare.base || undefined,
    versionCompareTarget: versionCompare.target || undefined,
    runCompareBase: runCompare.base || undefined,
    runCompareTarget: runCompare.target || undefined,
  })
  router.replace({ query }).catch(() => undefined)
}

async function copyCurrentLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    successMessage.value = '已复制当前链接'
  } catch {
    error.value = '复制链接失败'
  }
}

async function loadEvalRuns() {
  try {
    evalRuns.value = await fetchEvalRuns(
      '',
      runFilter.sourceType,
      runFilter.versionId || null,
      runFilter.regressionSetId || null,
      1,
      20,
    )
  } catch (err) {
    error.value = err instanceof Error ? err.message : '运行列表加载失败'
  }
}

async function loadEvalCases() {
  try {
    evalCases.value = await fetchEvalCases(
      caseFilter.keyword,
      caseFilter.bucket,
      caseFilter.riskLevel,
      caseFilter.enabled >= 0 ? caseFilter.enabled : null,
    )
    selectedCaseIds.value = []
  } catch (err) {
    error.value = err instanceof Error ? err.message : '样本列表加载失败'
  }
}

async function loadData() {
  loading.value = true
  resetMessages()
    try {
      const [statsResult, versionResult, regressionResult] = await Promise.all([
        fetchEvalCaseStats(),
        fetchEvalVersions('', 1, 20),
        fetchRegressionSets('', '', 1, 20),
      ])
    Object.assign(stats, statsResult)
    versions.value = versionResult
    regressionSets.value = regressionResult
    await Promise.all([loadEvalRuns(), loadEvalCases()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评估中心加载失败'
  } finally {
    loading.value = false
  }
}

async function submitVersion() {
  if (!versionForm.versionName.trim()) {
    error.value = '请先填写版本名称'
    return
  }
  creatingVersion.value = true
  resetMessages()
  try {
    const created = await createEvalCaseVersion({
      versionName: versionForm.versionName.trim(),
      keyword: versionForm.keyword || undefined,
      bucket: versionForm.bucket || undefined,
      enabled: versionForm.enabled >= 0 ? versionForm.enabled : undefined,
      limit: versionForm.limit,
      notes: versionForm.notes || undefined,
      createdBy: 'front-admin',
    })
    successMessage.value = `已创建版本快照：${created.versionName}`
    resetVersionForm()
    await loadData()
    await inspectVersion(created.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建版本快照失败'
  } finally {
    creatingVersion.value = false
  }
}

async function inspectVersion(versionId: number) {
  try {
    activeVersionDetail.value = await fetchEvalVersionDetail(versionId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '版本详情加载失败'
  }
}

function pickCompareVersion(versionId: number) {
  if (!versionCompare.base) versionCompare.base = versionId
  else versionCompare.target = versionId
}

async function runVersion(versionId: number) {
  resetMessages()
  try {
    await runEvalCaseVersion(versionId, 20)
    successMessage.value = `已触发版本 ${versionId} 的评估运行`
    await Promise.all([loadEvalRuns(), inspectVersion(versionId)])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '版本运行失败'
  }
}

async function runVersionCompare() {
  if (!versionCompare.base || !versionCompare.target) {
    error.value = '请先选择两个版本进行对比'
    return
  }
  resetMessages()
  try {
    versionComparison.value = await compareEvalCaseVersions(versionCompare.base, versionCompare.target)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '版本对比失败'
  }
}

async function runVersionBatch() {
  if (selectedVersionIds.value.length === 0) {
    error.value = '请先选择至少一个版本'
    return
  }
  batchRunning.value = true
  resetMessages()
  try {
    batchRunResult.value = await runEvalCaseVersionBatch({
      versionIds: selectedVersionIds.value,
      limit: batchRunLimit.value,
    })
    successMessage.value = '批量版本回归已执行'
    await loadEvalRuns()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '批量运行失败'
  } finally {
    batchRunning.value = false
  }
}

async function inspectRun(runId: number) {
  try {
    evalRunDetail.value = await fetchEvalRunDetail(runId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '运行详情加载失败'
  }
}

function pickRunCompare(runId: number) {
  if (!runCompare.base) runCompare.base = runId
  else runCompare.target = runId
}

async function runEvalRunCompare() {
  if (!runCompare.base || !runCompare.target) {
    error.value = '请先选择两次运行'
    return
  }
  resetMessages()
  try {
    runComparison.value = await compareEvalRuns(runCompare.base, runCompare.target)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '运行对比失败'
  }
}

async function submitRegressionSet() {
  if (!regressionForm.setName.trim()) {
    error.value = '请先填写回归集名称'
    return
  }
  creatingRegressionSet.value = true
  resetMessages()
  try {
    const created = await createRegressionSet({
      setName: regressionForm.setName.trim(),
      bucket: regressionForm.bucket || undefined,
      riskLevel: regressionForm.riskLevel || undefined,
      sourceVersionId: regressionForm.sourceVersionId || undefined,
      enabled: 1,
      limit: regressionForm.limit,
      notes: regressionForm.notes || undefined,
      createdBy: 'front-admin',
    })
    successMessage.value = `已创建回归集：${created.setName}`
    await loadData()
    await inspectRegressionSet(created.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建回归集失败'
  } finally {
    creatingRegressionSet.value = false
  }
}

async function inspectRegressionSet(id: number) {
  try {
    regressionSetDetail.value = await fetchRegressionSetDetail(id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回归集详情加载失败'
  }
}

async function runRegression(id: number) {
  resetMessages()
  try {
    await runRegressionSet(id, 20)
    successMessage.value = `已触发回归集 ${id} 的运行`
    await Promise.all([loadEvalRuns(), inspectRegressionSet(id)])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回归集运行失败'
  }
}

async function handleExportCases() {
  exportingCases.value = true
  resetMessages()
  try {
    const exported = await exportEvalCases(
      caseFilter.keyword,
      caseFilter.bucket,
      caseFilter.enabled >= 0 ? caseFilter.enabled : null,
      500,
    )
    importExportJson.value = JSON.stringify(exported, null, 2)
    successMessage.value = `已导出 ${exported.length} 条样本`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导出样本失败'
  } finally {
    exportingCases.value = false
  }
}

async function downloadExportCases() {
  exportingCases.value = true
  resetMessages()
  try {
    const exported = await exportEvalCases(
      caseFilter.keyword,
      caseFilter.bucket,
      caseFilter.enabled >= 0 ? caseFilter.enabled : null,
      500,
    )
    importExportJson.value = JSON.stringify(exported, null, 2)
    downloadJsonFile(`governance-eval-cases-${new Date().toISOString().slice(0, 10)}.json`, exported)
    successMessage.value = `已导出 ${exported.length} 条样本文件`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导出样本文件失败'
  } finally {
    exportingCases.value = false
  }
}

function openImportFilePicker() {
  importFileInput.value?.click()
}

async function importCasesPayload(raw: string) {
  const parsed = JSON.parse(raw || '[]')
  if (!Array.isArray(parsed)) {
    throw new Error('导入内容必须是 JSON 数组')
  }
  importResult.value = await importEvalCases({ cases: parsed })
  successMessage.value = `导入完成：新增 ${importResult.value.inserted}，更新 ${importResult.value.updated}`
  await loadData()
}

async function handleImportCases() {
  importingCases.value = true
  resetMessages()
  try {
    await importCasesPayload(importExportJson.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导入样本失败'
  } finally {
    importingCases.value = false
  }
}

async function handleImportFileChange(event: Event) {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0]
  if (!file) {
    return
  }
  importingCases.value = true
  resetMessages()
  try {
    const text = await file.text()
    importExportJson.value = text
    await importCasesPayload(text)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '导入文件失败'
  } finally {
    importingCases.value = false
    if (target) {
      target.value = ''
    }
  }
}

function fillEvalCaseForm(item: EvalCaseItem) {
  Object.assign(evalCaseForm, {
    id: item.id,
    caseName: item.caseName || '',
    queryText: item.queryText || '',
    bucket: item.bucket || 'golden',
    riskLevel: item.riskLevel || 'high',
    enabled: item.enabled ?? 1,
    expectedTaskType: item.expectedTaskType || '',
    expectedPlanType: item.expectedPlanType || '',
    expectedAnswerType: item.expectedAnswerType || '',
    tagsJson: item.tagsJson || '',
    notes: item.notes || '',
  })
}

async function saveEvalCase() {
  if (!evalCaseForm.queryText.trim()) {
    error.value = '请先填写 query'
    return
  }
  savingEvalCase.value = true
  resetMessages()
  try {
    const payload = {
      id: evalCaseForm.id || undefined,
      caseName: evalCaseForm.caseName || undefined,
      queryText: evalCaseForm.queryText.trim(),
      bucket: evalCaseForm.bucket || undefined,
      riskLevel: evalCaseForm.riskLevel || undefined,
      enabled: evalCaseForm.enabled,
      expectedTaskType: evalCaseForm.expectedTaskType || undefined,
      expectedPlanType: evalCaseForm.expectedPlanType || undefined,
      expectedAnswerType: evalCaseForm.expectedAnswerType || undefined,
      tagsJson: evalCaseForm.tagsJson || undefined,
      notes: evalCaseForm.notes || undefined,
    }
    if (evalCaseForm.id) {
      await updateEvalCase(payload)
      successMessage.value = '样本已更新'
    } else {
      await createEvalCase(payload)
      successMessage.value = '样本已创建'
    }
    await loadData()
    resetEvalCaseForm()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存样本失败'
  } finally {
    savingEvalCase.value = false
  }
}

async function removeEvalCase() {
  if (!evalCaseForm.id) {
    return
  }
  deletingEvalCase.value = true
  resetMessages()
  try {
    await deleteEvalCase(evalCaseForm.id)
    successMessage.value = '样本已删除'
    await loadData()
    resetEvalCaseForm()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除样本失败'
  } finally {
    deletingEvalCase.value = false
  }
}

async function toggleEvalCases(enabled: number) {
  if (selectedCaseIds.value.length === 0) {
    return
  }
  resetMessages()
  try {
    await batchToggleEvalCases({ ids: selectedCaseIds.value, enabled })
    successMessage.value = enabled ? '已批量启用样本' : '已批量停用样本'
    await loadData()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '批量更新样本状态失败'
  }
}

function exportVersionDetail() {
  if (!activeVersionDetail.value) {
    return
  }
  downloadJsonFile(`eval-version-${activeVersionDetail.value.version.id}.json`, activeVersionDetail.value)
  successMessage.value = '已导出版本详情'
}

function exportRegressionDetail() {
  if (!regressionSetDetail.value) {
    return
  }
  downloadJsonFile(`regression-set-${regressionSetDetail.value.regressionSet.id}.json`, regressionSetDetail.value)
  successMessage.value = '已导出回归集详情'
}

function exportRunDetail() {
  if (!evalRunDetail.value) {
    return
  }
  downloadJsonFile(`eval-run-${evalRunDetail.value.run.id}.json`, evalRunDetail.value)
  successMessage.value = '已导出运行详情'
}

watch(
  [
    batchRunLimit,
    () => runFilter.sourceType,
    () => runFilter.versionId,
    () => runFilter.regressionSetId,
    () => caseFilter.keyword,
    () => caseFilter.bucket,
    () => caseFilter.riskLevel,
    () => caseFilter.enabled,
  ],
  () => {
    writeGovernanceStorage('governance-eval-filters', {
      batchRunLimit: batchRunLimit.value,
      runFilter: { ...runFilter },
      caseFilter: { ...caseFilter },
    })
    syncRouteQuery()
  },
)

watch(
  [
    () => activeVersionDetail.value?.version.id,
    () => regressionSetDetail.value?.regressionSet.id,
    () => evalRunDetail.value?.run.id,
    () => versionCompare.base,
    () => versionCompare.target,
    () => runCompare.base,
    () => runCompare.target,
  ],
  syncRouteQuery,
)

onMounted(async () => {
  await loadData()
  const versionId = readQueryNumber(route.query.versionId, 0)
  const regressionId = readQueryNumber(route.query.regressionId, 0)
  const runId = readQueryNumber(route.query.runId, 0)
  if (versionId) {
    await inspectVersion(versionId)
  }
  if (regressionId) {
    await inspectRegressionSet(regressionId)
  }
  if (runId) {
    await inspectRun(runId)
  }
  if (versionCompare.base && versionCompare.target) {
    await runVersionCompare()
  }
  if (runCompare.base && runCompare.target) {
    await runEvalRunCompare()
  }
})
</script>

<style scoped>
.page{display:flex;flex-direction:column;gap:20px}.head,.row,.actions,.info{display:flex;gap:10px;flex-wrap:wrap;align-items:end}.head,.detail-head{justify-content:space-between;align-items:flex-start}.detail-head{display:flex;gap:12px;flex-wrap:wrap}.kicker{margin:0 0 6px;color:#0f766e;font-size:12px;font-weight:700;letter-spacing:.18em;text-transform:uppercase}.head h1{margin:0;font-size:30px}.desc{margin:8px 0 0;color:#64748b}.error,.success,.empty{padding:12px 16px;border-radius:14px}.error{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.success{background:#ecfeff;color:#155e75;border:1px solid #a5f3fc}.cards,.grid{display:grid;gap:16px}.cards{grid-template-columns:repeat(4,minmax(0,1fr))}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.card,.panel{border-radius:20px;background:#fff;padding:18px;border:1px solid rgba(148,163,184,.16);box-shadow:0 16px 40px rgba(15,23,42,.08)}.card span{color:#64748b;font-size:13px}.card strong{display:block;margin-top:10px;font-size:28px}label,.stack{display:flex;flex-direction:column;gap:6px;color:#475569;font-size:13px}.stack{margin-top:12px}.hidden-input{display:none}input,select,textarea{border:1px solid #cbd5e1;border-radius:12px;padding:10px 12px;font:inherit;width:100%;box-sizing:border-box}.list,.compact{display:flex;flex-direction:column;gap:10px;margin-top:12px}.item,.detail,.subitem{border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0}.item,.subitem{display:flex;justify-content:space-between;gap:12px;padding:14px}.item input{width:auto;margin-top:4px}.item.plain{align-items:flex-start}.item-main{flex:1}.item-main strong{display:block;margin-bottom:4px}.item-main p,.detail p,.subitem p{margin:0;color:#64748b;font-size:13px}.detail{margin-top:14px;padding:16px}.info span,.badge{padding:6px 10px;border-radius:999px;background:#dbeafe;color:#1d4ed8;font-size:12px;font-weight:700}.primary,.secondary,.ghost{border:0;border-radius:14px;padding:11px 16px;cursor:pointer;font-weight:700}.primary{background:linear-gradient(135deg,#0f172a,#0f766e);color:#fff}.secondary{background:#ccfbf1;color:#0f172a}.ghost{background:#e2e8f0;color:#334155}.ghost.danger{background:#fee2e2;color:#991b1b}.ghost.small{padding:8px 12px;font-size:12px}h2,h3{margin:0}.empty{background:#f8fafc;color:#94a3b8;text-align:center}@media (max-width:1100px){.cards,.grid{grid-template-columns:1fr}.head{flex-direction:column}}
</style>
