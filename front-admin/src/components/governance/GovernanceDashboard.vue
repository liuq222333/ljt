<template>
  <section class="admin-page governance-dashboard">
    <AdminPageHeader eyebrow="治理" title="治理总览" description="查看治理状态、最近记录与关键待办。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" @click="copyCurrentLink">
          复制链接
        </button>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="loadDashboard">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminStateBlock v-if="error" tone="danger" :message="error" />

    <section class="admin-metric-grid">
      <AdminMetricCard
        v-for="card in cards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :meta="card.meta"
        :to="card.to"
      />
    </section>

    <section class="admin-overview-grid">
      <AdminPanel class="admin-panel--wide" title="最近趋势" description="最近 7 天回放与降级走势。">
        <div class="trend-list">
          <div v-for="item in trendItems" :key="item.date" class="trend-row">
            <div class="trend-date">{{ item.date || '-' }}</div>
            <div class="trend-bars">
              <div class="trend-bar">
                <div class="trend-bar__fill trend-bar__fill--replay" :style="{ width: `${item.replayWidth}%` }" />
              </div>
              <div class="trend-bar">
                <div class="trend-bar__fill trend-bar__fill--degraded" :style="{ width: `${item.degradedWidth}%` }" />
              </div>
            </div>
            <div class="trend-meta">回放 {{ item.replayTotal }} / 降级 {{ item.degradedTotal }}</div>
          </div>
          <div v-if="trendItems.length === 0" class="admin-empty">暂无趋势数据</div>
        </div>
      </AdminPanel>

      <AdminPanel title="日聚合指标">
        <div class="admin-list">
          <article v-for="item in metricsDaily" :key="item.date" class="admin-list-item">
            <strong>{{ item.date || '-' }}</strong>
            <p>回放 {{ item.replayTotal ?? 0 }} / 降级 {{ item.degradedTotal ?? 0 }}</p>
            <p>错误 {{ item.errorTotal ?? 0 }} / 均耗时 {{ item.avgDurationMs ?? 0 }} ms</p>
          </article>
          <div v-if="metricsDaily.length === 0" class="admin-empty">暂无日聚合指标</div>
        </div>
      </AdminPanel>

      <AdminPanel title="系统状态">
        <ul class="admin-summary">
          <li><span>错误总量</span><strong>{{ dashboard.error_attribution_summary?.error_total ?? 0 }}</strong></li>
          <li><span>降级总量</span><strong>{{ dashboard.error_attribution_summary?.degraded_total ?? 0 }}</strong></li>
          <li><span>错误率</span><strong>{{ formatRate(dashboard.error_attribution_summary?.error_rate) }}</strong></li>
          <li><span>降级率</span><strong>{{ formatRate(dashboard.error_attribution_summary?.degraded_rate) }}</strong></li>
          <li><span>评估总样本</span><strong>{{ dashboard.eval_case_stats?.total ?? 0 }}</strong></li>
          <li><span>高风险样本</span><strong>{{ riskCount('high') }}</strong></li>
        </ul>
      </AdminPanel>

      <AdminPanel class="admin-panel--wide" title="最近记录">
        <div class="record-columns">
          <section class="record-group">
            <h3>最近发布</h3>
            <div class="admin-list">
              <router-link
                v-for="item in dashboard.recent_releases || []"
                :key="item.id"
                class="admin-list-item admin-list-item--link"
                :to="{ path: '/admin/governance/release', query: { releaseId: item.id } }"
              >
                <strong>{{ item.releaseName }}</strong>
                <p>{{ item.releaseStatus || 'draft' }} / {{ item.targetScope || '-' }}</p>
              </router-link>
              <div v-if="!(dashboard.recent_releases || []).length" class="admin-empty">暂无发布记录</div>
            </div>
          </section>

          <section class="record-group">
            <h3>最近回放</h3>
            <div class="admin-list">
              <router-link
                v-for="item in dashboard.recent_replays || []"
                :key="item.requestId"
                class="admin-list-item admin-list-item--link"
                :to="{ path: '/admin/governance/replay', query: { request: item.requestId } }"
              >
                <strong>{{ item.lastUserMessage || item.requestId }}</strong>
                <p>{{ item.taskType || 'unknown' }} / {{ item.answerType || 'unknown' }}</p>
              </router-link>
              <div v-if="!(dashboard.recent_replays || []).length" class="admin-empty">暂无回放记录</div>
            </div>
          </section>

          <section class="record-group">
            <h3>最近评估版本</h3>
            <div class="admin-list">
              <router-link
                v-for="item in dashboard.recent_eval_versions || []"
                :key="item.id"
                class="admin-list-item admin-list-item--link"
                :to="{ path: '/admin/governance/eval', query: { versionId: item.id } }"
              >
                <strong>{{ item.versionName }}</strong>
                <p>{{ item.bucket || 'all' }} / {{ item.totalCases ?? 0 }} 条</p>
              </router-link>
              <div v-if="!(dashboard.recent_eval_versions || []).length" class="admin-empty">暂无评估版本</div>
            </div>
          </section>
        </div>
      </AdminPanel>

      <AdminPanel title="关键待办">
        <div class="admin-list">
          <router-link
            v-for="item in integrationChecks"
            :key="item.label"
            class="admin-list-item admin-list-item--link checklist-item"
            :to="item.to"
          >
            <div class="checklist-main">
              <strong>{{ item.label }}</strong>
              <p>{{ item.detail }}</p>
            </div>
            <span class="check-badge" :class="`check-badge--${item.level}`">{{ item.status }}</span>
          </router-link>
        </div>
      </AdminPanel>

      <AdminPanel title="错误趋势">
        <div class="admin-list">
          <article v-for="item in errorTrendItems" :key="item.date" class="admin-list-item">
            <strong>{{ item.date || '-' }}</strong>
            <p>错误 {{ item.errorTotal ?? 0 }} / 降级 {{ item.degradedTotal ?? 0 }}</p>
            <p>Top 节点：{{ item.topFailedNode || '-' }}</p>
          </article>
          <div v-if="errorTrendItems.length === 0" class="admin-empty">暂无错误趋势</div>
        </div>
      </AdminPanel>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchErrorAttributionTrend,
  fetchGovernanceDashboard,
  fetchGovernanceMetricsDaily,
  getGovernanceApiBase,
  type GovernanceDashboard,
  type GovernanceErrorAttributionTrendItem,
  type GovernanceMetricsDailyItem,
} from '../../api/adminGovernance'
import AdminMetricCard from '../admin/AdminMetricCard.vue'
import AdminPageHeader from '../admin/AdminPageHeader.vue'
import AdminPanel from '../admin/AdminPanel.vue'
import AdminStateBlock from '../admin/AdminStateBlock.vue'

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
    date: item.date as string | undefined,
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
  const apiBase = getGovernanceApiBase()
  return [
    {
      label: '治理后端模式',
      status: apiBase ? '直连' : '代理',
      level: 'info',
      detail: apiBase || '使用本地 /api 代理',
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
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(window.location.href)
      return
    }
    error.value = '当前环境不支持复制链接'
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
.governance-dashboard {
  gap: 12px;
}

.admin-button {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button--secondary {
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
}

.admin-button--secondary:hover {
  border-color: var(--admin-border-strong);
  color: var(--admin-text-primary);
}

.admin-button--primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #ffffff;
}

.admin-button--primary:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.admin-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.admin-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 12px;
}

.admin-panel--wide {
  grid-column: 1 / -1;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trend-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 170px;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
}

.trend-date {
  color: var(--admin-text-primary);
  font-size: 12px;
  font-weight: 600;
}

.trend-bars {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.trend-bar {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: #e2e8f0;
}

.trend-bar__fill {
  height: 100%;
}

.trend-bar__fill--replay {
  background: #2f5f95;
}

.trend-bar__fill--degraded {
  background: #b5612a;
}

.trend-meta {
  text-align: right;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-list-item {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 10px;
}

.admin-list-item strong {
  display: block;
  color: var(--admin-text-primary);
  font-size: 13px;
}

.admin-list-item p {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-list-item--link {
  text-decoration: none;
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.admin-list-item--link:hover {
  border-color: var(--admin-border-strong);
  background: #f0f3f7;
}

.admin-summary {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-summary li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 9px 10px;
}

.admin-summary span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-summary strong {
  color: var(--admin-text-primary);
  font-size: 13px;
}

.record-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.record-group h3 {
  margin: 0 0 8px;
  color: var(--admin-text-primary);
  font-size: 13px;
}

.checklist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.checklist-main {
  min-width: 0;
}

.check-badge {
  border-radius: var(--admin-radius-control);
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.check-badge--good {
  background: #eaf6ef;
  color: #1f7a4d;
}

.check-badge--warn {
  background: #fcf3e5;
  color: #9b6811;
}

.check-badge--info {
  background: #eaf0f8;
  color: #2d5887;
}

.admin-empty {
  border: 1px dashed var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 10px;
  color: var(--admin-text-muted);
  font-size: 12px;
  text-align: center;
}
</style>
