<template>
  <section class="admin-page page governance-diagnostics-center">
    <header class="head">
      <div>
        <p class="kicker">治理</p>
        <h1>联调诊断</h1>
        <p class="desc">查看治理后端连接状态、当前路由参数和本地筛选缓存，快速定位联调问题。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="ghost" @click="reloadSnapshots">刷新快照</button>
        <button class="primary" :disabled="probing" @click="runProbe">{{ probing ? '检测中...' : '重新检测后端' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successMessage" class="success">{{ successMessage }}</div>

    <section class="cards admin-toolbar">
      <article class="card admin-panel">
        <span>连接模式</span>
        <strong>{{ probe.mode }}</strong>
        <p>{{ probe.ok ? '治理后端已连通' : '治理后端待联通' }}</p>
      </article>
      <article class="card admin-panel">
        <span>接口状态</span>
        <strong>{{ probe.ok ? '正常' : '异常' }}</strong>
        <p>{{ probe.latencyMs !== undefined ? `${probe.latencyMs} ms` : '未检测' }}</p>
      </article>
      <article class="card admin-panel">
        <span>本地状态项</span>
        <strong>{{ storageEntries.length }}</strong>
        <p>治理台 localStorage 快照</p>
      </article>
      <article class="card admin-panel">
        <span>当前路由</span>
        <strong>{{ route.path }}</strong>
        <p>{{ queryEntryCount }} 个 query 参数</p>
      </article>
    </section>

    <section class="quick-grid">
      <router-link class="quick admin-panel" to="/admin/governance/dashboard">
        <strong>治理总览</strong>
        <span>查看整体指标和最近记录</span>
      </router-link>
      <router-link class="quick admin-panel" to="/admin/governance/replay">
        <strong>回放中心</strong>
        <span>排查 request、checkpoint 和 tool I/O</span>
      </router-link>
      <router-link class="quick admin-panel" to="/admin/governance/eval">
        <strong>评估与回归</strong>
        <span>查看版本、回归集和样本状态</span>
      </router-link>
      <router-link class="quick admin-panel" to="/admin/governance/release">
        <strong>发布与灰度</strong>
        <span>查看 preflight、verification 和流转</span>
      </router-link>
    </section>

    <section class="grid">
      <article class="panel admin-panel">
        <h2>连接快照</h2>
        <GovernanceJsonBlock title="Probe Result" :value="probe" />
        <GovernanceJsonBlock title="Environment" :value="environmentSnapshot" />
      </article>

      <article class="panel admin-panel">
        <div class="panel-head">
          <h2>本地存储快照</h2>
          <button class="ghost small" @click="clearGovernanceStorage">清空治理台状态</button>
        </div>
        <div class="list">
          <article v-for="item in storageEntries" :key="item.key" class="item">
            <div class="item-main">
              <strong>{{ item.key }}</strong>
              <p>{{ item.summary }}</p>
            </div>
            <GovernanceJsonBlock title="Storage Value" :value="item.value" />
          </article>
          <div v-if="storageEntries.length === 0" class="empty">暂无治理台本地状态</div>
        </div>
      </article>

      <article class="panel admin-panel">
        <h2>当前路由参数</h2>
        <GovernanceJsonBlock title="Route Query" :value="route.query" />
      </article>

      <article class="panel admin-panel">
        <h2>联调建议</h2>
        <ul class="tips">
          <li>优先看顶部状态横幅和这里的连接快照，确认当前是直连还是本地代理。</li>
          <li>如果页面状态异常，先清空治理台本地状态，再回到对应页面重新操作。</li>
          <li>如果接口字段回显不一致，先从回放页或评估页导出 JSON，再对照后端 DTO 核对。</li>
        </ul>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import GovernanceJsonBlock from './GovernanceJsonBlock.vue'
import { getGovernanceApiBase, probeGovernanceBackend, type GovernanceBackendProbeResult } from '../../api/adminGovernance'
import { removeGovernanceStorage } from '../../utils/governanceStorage'

const GOVERNANCE_STORAGE_KEYS = [
  'governance-replay-filters',
  'governance-eval-filters',
  'governance-release-filters',
  'admin-nav-groups-open',
]

const route = useRoute()
const probing = ref(false)
const error = ref('')
const successMessage = ref('')
const probe = ref<GovernanceBackendProbeResult>({
  ok: false,
  mode: getGovernanceApiBase() ? 'direct-api' : 'vite-proxy',
  apiBase: getGovernanceApiBase(),
  checkedAt: '',
  message: '尚未检测',
})
const storageEntries = ref<Array<{ key: string; summary: string; value: unknown }>>([])

const environmentSnapshot = computed(() => ({
  apiBase: getGovernanceApiBase() || '/api (vite proxy)',
  currentPath: route.path,
  currentUrl: typeof window !== 'undefined' ? window.location.href : '',
  userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
}))

const queryEntryCount = computed(() => Object.keys(route.query).length)

async function runProbe() {
  probing.value = true
  error.value = ''
  try {
    probe.value = await probeGovernanceBackend()
    successMessage.value = probe.value.ok ? '治理后端连接正常' : '治理后端探测完成'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '治理后端探测失败'
  } finally {
    probing.value = false
  }
}

function rebuildStorageEntries() {
  if (typeof window === 'undefined') {
    storageEntries.value = []
    return
  }
  storageEntries.value = GOVERNANCE_STORAGE_KEYS.map((key) => {
    const raw = window.localStorage.getItem(key)
    let parsed: unknown = raw
    try {
      parsed = raw ? JSON.parse(raw) : null
    } catch {
      parsed = raw
    }
    return {
      key,
      summary: raw ? '已缓存' : '未设置',
      value: parsed,
    }
  }).filter((item) => item.value !== null)
}

function reloadSnapshots() {
  rebuildStorageEntries()
  successMessage.value = '已刷新本地诊断快照'
}

async function copyCurrentLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    successMessage.value = '已复制当前链接'
  } catch {
    error.value = '复制链接失败'
  }
}

function clearGovernanceStorage() {
  GOVERNANCE_STORAGE_KEYS.forEach((key) => removeGovernanceStorage(key))
  rebuildStorageEntries()
  successMessage.value = '已清空治理台本地状态'
}

onMounted(async () => {
  rebuildStorageEntries()
  await runProbe()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.head,
.actions,
.panel-head {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.head,
.panel-head {
  justify-content: space-between;
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
  color: var(--admin-text-primary);
  font-size: 22px;
}

.desc {
  margin: 6px 0 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
}

.cards,
.grid,
.quick-grid {
  display: grid;
  gap: 12px;
}

.cards {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.quick-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.card,
.panel,
.quick {
  border-radius: var(--admin-radius-panel);
  background: var(--admin-bg-surface);
  padding: 12px;
  border: 1px solid var(--admin-border);
  box-shadow: var(--admin-shadow-panel);
}

.card span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  color: var(--admin-text-primary);
}

.quick {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-decoration: none;
  color: var(--admin-text-primary);
}

.quick span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.panel h2 {
  margin: 0 0 10px;
  font-size: 15px;
  color: var(--admin-text-primary);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  border: 1px solid var(--admin-border);
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

.tips {
  margin: 0;
  padding-left: 18px;
  color: var(--admin-text-secondary);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.primary,
.ghost {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
}

.ghost.small {
  padding: 6px 10px;
  font-size: 12px;
}

.primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #ffffff;
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
  border: 1px dashed var(--admin-border);
  color: var(--admin-text-muted);
  text-align: center;
}
</style>
