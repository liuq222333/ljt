<template>
  <section class="page">
    <header class="head">
      <div>
        <p class="kicker">Governance</p>
        <h1>治理总览</h1>
        <p class="desc">集中查看回放、评估、发布、灰度与错误归因的当前状态。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="primary" :disabled="loading" @click="loadDashboard">{{ loading ? '刷新中...' : '刷新数据' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>

    <section class="cards">
      <component
        :is="card.to ? 'router-link' : 'article'"
        v-for="card in cards"
        :key="card.label"
        class="card"
        :class="{ linkable: Boolean(card.to) }"
        v-bind="card.to ? { to: card.to } : {}"
      >
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.meta }}</p>
      </component>
    </section>

    <section class="quick-grid">
      <router-link class="quick" to="/admin/governance/replay">
        <strong>回放中心</strong>
        <span>排查 request、checkpoint 和 tool I/O</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/eval">
        <strong>评估与回归</strong>
        <span>管理 golden cases、版本快照和回归集</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/release">
        <strong>发布与灰度</strong>
        <span>做 preflight、版本回归和灰度流转</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/diagnostics">
        <strong>联调诊断</strong>
        <span>查看连接模式、本地状态和排查建议</span>
      </router-link>
    </section>

    <section class="grid">
      <article class="panel wide">
        <h2>最近 7 天趋势</h2>
        <div class="trend-list">
          <div v-for="item in trendItems" :key="item.date" class="trend-row">
            <div class="trend-date">{{ item.date }}</div>
            <div class="trend-main">
              <div class="bar"><div class="fill replay" :style="{ width: `${item.replayWidth}%` }"></div></div>
              <div class="bar"><div class="fill degraded" :style="{ width: `${item.degradedWidth}%` }"></div></div>
            </div>
            <div class="trend-meta">回放 {{ item.replayTotal }} / 降级 {{ item.degradedTotal }}</div>
          </div>
        </div>
      </article>

      <article class="panel">
        <h2>日聚合指标</h2>
        <div class="list">
          <article v-for="item in metricsDaily" :key="item.date" class="item">
            <div class="item-main">
              <strong>{{ item.date || '-' }}</strong>
              <p>回放 {{ item.replayTotal ?? 0 }} / 降级 {{ item.degradedTotal ?? 0 }}</p>
              <p>错误 {{ item.errorTotal ?? 0 }} / 均耗时 {{ item.avgDurationMs ?? 0 }} ms</p>
            </div>
          </article>
          <div v-if="metricsDaily.length === 0" class="empty">暂无日聚合指标</div>
        </div>
      </article>

      <article class="panel">
        <h2>错误归因摘要</h2>
        <ul class="summary">
          <li><span>错误总量</span><strong>{{ dashboard.error_attribution_summary?.error_total ?? 0 }}</strong></li>
          <li><span>降级总量</span><strong>{{ dashboard.error_attribution_summary?.degraded_total ?? 0 }}</strong></li>
          <li><span>错误率</span><strong>{{ formatRate(dashboard.error_attribution_summary?.error_rate) }}</strong></li>
          <li><span>降级率</span><strong>{{ formatRate(dashboard.error_attribution_summary?.degraded_rate) }}</strong></li>
        </ul>
      </article>

      <article class="panel">
        <h2>评估池概况</h2>
        <ul class="summary">
          <li><span>总样本</span><strong>{{ dashboard.eval_case_stats?.total ?? 0 }}</strong></li>
          <li><span>启用样本</span><strong>{{ dashboard.eval_case_stats?.enabled_total ?? 0 }}</strong></li>
          <li><span>停用样本</span><strong>{{ dashboard.eval_case_stats?.disabled_total ?? 0 }}</strong></li>
          <li><span>高风险样本</span><strong>{{ riskCount('high') }}</strong></li>
        </ul>
      </article>

      <article class="panel">
        <h2>联调检查清单</h2>
        <div class="list">
          <router-link
            v-for="item in integrationChecks"
            :key="item.label"
            class="item link-item checklist-item"
            :to="item.to"
          >
            <div class="item-main">
              <strong>{{ item.label }}</strong>
              <p>{{ item.detail }}</p>
            </div>
            <span class="check-badge" :class="item.level">{{ item.status }}</span>
          </router-link>
        </div>
      </article>

      <article class="panel">
        <h2>错误趋势</h2>
        <div class="list">
          <article v-for="item in errorTrendItems" :key="item.date" class="item">
            <div class="item-main">
              <strong>{{ item.date || '-' }}</strong>
              <p>错误 {{ item.errorTotal ?? 0 }} / 降级 {{ item.degradedTotal ?? 0 }}</p>
              <p>Top 节点：{{ item.topFailedNode || '-' }}</p>
            </div>
          </article>
          <div v-if="errorTrendItems.length === 0" class="empty">暂无错误趋势</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近发布</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_releases || []"
            :key="item.id"
            class="item link-item"
            :to="{ path: '/admin/governance/release', query: { releaseId: item.id } }"
          >
            <div class="item-main">
              <strong>{{ item.releaseName }}</strong>
              <p>{{ item.releaseStatus || 'draft' }} / {{ item.targetScope || '-' }}</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_releases || []).length" class="empty">暂无发布记录</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近回放</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_replays || []"
            :key="item.requestId"
            class="item link-item"
            :to="{ path: '/admin/governance/replay', query: { request: item.requestId } }"
          >
            <div class="item-main">
              <strong>{{ item.lastUserMessage || item.requestId }}</strong>
              <p>{{ item.taskType || 'unknown' }} / {{ item.answerType || 'unknown' }}</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_replays || []).length" class="empty">暂无回放记录</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近版本快照</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_eval_versions || []"
            :key="item.id"
            class="item link-item"
            :to="{ path: '/admin/governance/eval', query: { versionId: item.id } }"
          >
            <div class="item-main">
              <strong>{{ item.versionName }}</strong>
              <p>{{ item.bucket || 'all' }} / {{ item.totalCases ?? 0 }} 条</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_eval_versions || []).length" class="empty">暂无版本快照</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近回归集</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_regression_sets || []"
            :key="item.id"
            class="item link-item"
            :to="{ path: '/admin/governance/eval', query: { regressionId: item.id } }"
          >
            <div class="item-main">
              <strong>{{ item.setName }}</strong>
              <p>{{ item.bucket || 'all' }} / {{ item.totalCases ?? 0 }} 条 / {{ item.riskLevel || 'mixed' }}</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_regression_sets || []).length" class="empty">暂无回归集</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近灰度配置</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_gray_configs || []"
            :key="item.id"
            class="item link-item"
            :to="{ path: '/admin/governance/release', query: { grayConfigId: item.id } }"
          >
            <div class="item-main">
              <strong>{{ item.configName }}</strong>
              <p>{{ item.queryBucket || 'all' }} / {{ item.trafficPercent ?? 0 }}% / {{ item.enabled ? '启用' : '停用' }}</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_gray_configs || []).length" class="empty">暂无灰度配置</div>
        </div>
      </article>

      <article class="panel">
        <h2>最近评估运行</h2>
        <div class="list">
          <router-link
            v-for="item in dashboard.recent_eval_runs || []"
            :key="item.id"
            class="item link-item"
            :to="{ path: '/admin/governance/eval', query: { runId: item.id } }"
          >
            <div class="item-main">
              <strong>#{{ item.id }} / {{ item.sourceType || 'live_cases' }}</strong>
              <p>{{ item.bucket || 'all' }} / 通过率 {{ formatRate(item.passRate) }}</p>
            </div>
          </router-link>
          <div v-if="!(dashboard.recent_eval_runs || []).length" class="empty">暂无评估运行</div>
        </div>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchErrorAttributionTrend, fetchGovernanceDashboard, fetchGovernanceMetricsDaily, getGovernanceApiBase, type GovernanceDashboard, type GovernanceErrorAttributionTrendItem, type GovernanceMetricsDailyItem } from '../../api/adminGovernance'

const loading = ref(false)
const error = ref('')
const dashboard = ref<GovernanceDashboard>({})
const metricsDaily = ref<GovernanceMetricsDailyItem[]>([])
const errorTrendItems = ref<GovernanceErrorAttributionTrendItem[]>([])

const formatRate = (value?: number) =>
  typeof value === 'number' && !Number.isNaN(value) ? `${(value * 100).toFixed(1)}%` : '0%'

const cards = computed(() => [
  {
    label: '回放总量',
    value: dashboard.value.overview?.replay_total ?? 0,
    meta: `最近 ${dashboard.value.overview?.days ?? 7} 天`,
    to: {
      path: '/admin/governance/replay',
      query: { days: dashboard.value.overview?.days ?? 7 },
    },
  },
  {
    label: '降级率',
    value: formatRate(dashboard.value.metrics_summary?.degraded_rate),
    meta: `降级 ${dashboard.value.metrics_summary?.degraded_total ?? 0} 次`,
    to: '/admin/governance/replay',
  },
  {
    label: '同步基线',
    value: dashboard.value.overview?.w12_ready ? '已就绪' : '待处理',
    meta: `失败任务 ${dashboard.value.overview?.open_failure_total ?? 0}`,
    to: '/admin/governance/release',
  },
  {
    label: '治理阶段',
    value: dashboard.value.overview?.stage || 'W13',
    meta: '治理后台与管理端联调中',
    to: '/admin/governance/eval',
  },
])

const trendItems = computed(() => {
  const source = dashboard.value.metrics_trend || []
  const maxReplay = Math.max(1, ...source.map((item) => Number(item.replay_total || 0)))
  const maxDegraded = Math.max(1, ...source.map((item) => Number(item.degraded_total || 0)))
  return source.map((item) => ({
    date: item.date,
    replayTotal: Number(item.replay_total || 0),
    degradedTotal: Number(item.degraded_total || 0),
    replayWidth: (Number(item.replay_total || 0) / maxReplay) * 100,
    degradedWidth: (Number(item.degraded_total || 0) / maxDegraded) * 100,
  }))
})

const riskCount = (riskLevel: string) =>
  Number((dashboard.value.eval_case_stats?.by_risk_level || {})[riskLevel] || 0)

const integrationChecks = computed(() => {
  const replayTotal = Number(dashboard.value.overview?.replay_total || 0)
  const enabledCases = Number(dashboard.value.eval_case_stats?.enabled_total || 0)
  const releaseTotal = (dashboard.value.recent_releases || []).length
  const w12Ready = Boolean(dashboard.value.overview?.w12_ready)
  return [
    {
      label: '治理后端模式',
      status: getGovernanceApiBase() ? '直连' : '代理',
      level: 'info',
      detail: getGovernanceApiBase() || '使用本地 /api 代理',
      to: '/admin/governance/dashboard',
    },
    {
      label: '同步基线',
      status: w12Ready ? '就绪' : '待处理',
      level: w12Ready ? 'good' : 'warn',
      detail: `失败任务 ${dashboard.value.overview?.open_failure_total ?? 0}`,
      to: '/admin/governance/release',
    },
    {
      label: '评估样本',
      status: enabledCases > 0 ? '已就绪' : '待补充',
      level: enabledCases > 0 ? 'good' : 'warn',
      detail: `启用样本 ${enabledCases}`,
      to: '/admin/governance/eval',
    },
    {
      label: '回放数据',
      status: replayTotal > 0 ? '可排查' : '暂无数据',
      level: replayTotal > 0 ? 'good' : 'warn',
      detail: `最近 ${dashboard.value.overview?.days ?? 7} 天 ${replayTotal} 条`,
      to: '/admin/governance/replay',
    },
    {
      label: '发布治理',
      status: releaseTotal > 0 ? '有记录' : '未创建',
      level: releaseTotal > 0 ? 'info' : 'warn',
      detail: `最近发布 ${releaseTotal} 条`,
      to: '/admin/governance/release',
    },
  ]
})

async function copyCurrentLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
  } catch {
    error.value = '复制链接失败'
  }
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [dashboardResult, dailyResult, trendResult] = await Promise.all([
      fetchGovernanceDashboard(7),
      fetchGovernanceMetricsDaily(7),
      fetchErrorAttributionTrend(7),
    ])
    dashboard.value = dashboardResult
    metricsDaily.value = dailyResult
    errorTrendItems.value = trendResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '治理总览加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.page{display:flex;flex-direction:column;gap:20px}.head,.actions{display:flex;gap:16px;align-items:flex-start}.head{justify-content:space-between}.kicker{margin:0 0 6px;color:#0ea5e9;font-size:12px;font-weight:700;letter-spacing:.22em;text-transform:uppercase}.head h1{margin:0;font-size:30px}.desc{margin:8px 0 0;color:#52606d}.cards,.grid,.quick-grid{display:grid;gap:16px}.cards{grid-template-columns:repeat(4,minmax(0,1fr))}.quick-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.panel.wide{grid-column:span 2}.card,.panel,.quick{border-radius:20px;background:#fff;padding:18px;border:1px solid rgba(148,163,184,.12);box-shadow:0 18px 40px rgba(15,23,42,.08)}.card{display:block;color:#0f172a;text-decoration:none}.card.linkable{transition:.18s ease}.card.linkable:hover{border-color:#7dd3fc;background:#f0f9ff;transform:translateY(-1px)}.card span{color:#64748b;font-size:13px}.card strong{display:block;margin-top:10px;font-size:28px}.quick,.link-item{display:flex;flex-direction:column;gap:8px;text-decoration:none;color:#0f172a;background:linear-gradient(180deg,#ffffff,#f8fafc)}.quick span{color:#64748b;font-size:13px}.panel h2{margin:0 0 14px;font-size:18px}.trend-list,.list{display:flex;flex-direction:column;gap:10px}.trend-row,.item{display:flex;gap:12px;padding:12px 14px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0}.link-item{transition:.18s ease}.link-item:hover{border-color:#7dd3fc;background:#f0f9ff}.checklist-item{flex-direction:row;justify-content:space-between;align-items:center}.check-badge{padding:6px 10px;border-radius:999px;background:#dbeafe;color:#1d4ed8;font-size:12px;font-weight:700}.check-badge.good{background:#ccfbf1;color:#0f766e}.check-badge.warn{background:#ffedd5;color:#9a3412}.check-badge.info{background:#dbeafe;color:#1d4ed8}.trend-date{width:96px;font-weight:700}.trend-main{flex:1;display:flex;flex-direction:column;gap:8px}.trend-meta{width:180px;text-align:right;color:#475569;font-size:13px}.bar{height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden}.fill{height:100%;border-radius:999px}.fill.replay{background:#0284c7}.fill.degraded{background:#ea580c}.item-main strong{display:block;margin-bottom:4px}.item-main p{margin:0;color:#64748b;font-size:13px}.summary{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:12px}.summary li{display:flex;justify-content:space-between;align-items:center;padding:12px 14px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0}.summary span{color:#64748b}.primary,.ghost{border:0;border-radius:14px;padding:11px 16px;cursor:pointer;font-weight:600}.primary{background:linear-gradient(135deg,#0f172a,#0f766e);color:#fff}.ghost{background:#e2e8f0;color:#334155}.error,.empty{padding:12px 16px;border-radius:14px}.error{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.empty{color:#94a3b8;text-align:center}@media (max-width:980px){.head,.cards,.grid,.quick-grid{grid-template-columns:1fr;display:grid}.panel.wide{grid-column:auto}.trend-row,.item,.checklist-item{flex-direction:column}.trend-date,.trend-meta{width:auto;text-align:left}}
</style>
