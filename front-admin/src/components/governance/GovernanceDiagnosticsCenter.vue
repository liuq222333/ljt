<template>
  <section class="page">
    <header class="head">
      <div>
        <p class="kicker">Diagnostics</p>
        <h1>联调诊断</h1>
        <p class="desc">查看治理台前端当前的连接模式、路由参数和本地筛选状态，方便排查联调问题。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="copyCurrentLink">复制当前链接</button>
        <button class="ghost" @click="reloadSnapshots">刷新快照</button>
        <button class="primary" :disabled="probing" @click="runProbe">{{ probing ? '检测中...' : '重新检测后端' }}</button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successMessage" class="success">{{ successMessage }}</div>

    <section class="cards">
      <article class="card">
        <span>连接模式</span>
        <strong>{{ probe.mode }}</strong>
        <p>{{ probe.ok ? '治理后端已连通' : '治理后端待联通' }}</p>
      </article>
      <article class="card">
        <span>接口状态</span>
        <strong>{{ probe.ok ? '正常' : '异常' }}</strong>
        <p>{{ probe.latencyMs !== undefined ? `${probe.latencyMs} ms` : '未检测' }}</p>
      </article>
      <article class="card">
        <span>本地状态项</span>
        <strong>{{ storageEntries.length }}</strong>
        <p>治理台 localStorage 快照</p>
      </article>
      <article class="card">
        <span>当前路由</span>
        <strong>{{ route.path }}</strong>
        <p>{{ queryEntryCount }} 个 query 参数</p>
      </article>
    </section>

    <section class="quick-grid">
      <router-link class="quick" to="/admin/governance/dashboard">
        <strong>治理总览</strong>
        <span>查看整体指标和最近记录</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/replay">
        <strong>回放中心</strong>
        <span>排查 request、checkpoint 和 tool I/O</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/eval">
        <strong>评估与回归</strong>
        <span>查看版本、回归集和样本状态</span>
      </router-link>
      <router-link class="quick" to="/admin/governance/release">
        <strong>发布与灰度</strong>
        <span>查看 preflight、verification 和流转</span>
      </router-link>
    </section>

    <section class="grid">
      <article class="panel">
        <h2>连接快照</h2>
        <GovernanceJsonBlock title="Probe Result" :value="probe" />
        <GovernanceJsonBlock title="Environment" :value="environmentSnapshot" />
      </article>

      <article class="panel">
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

      <article class="panel">
        <h2>当前路由参数</h2>
        <GovernanceJsonBlock title="Route Query" :value="route.query" />
      </article>

      <article class="panel">
        <h2>联调建议</h2>
        <ul class="tips">
          <li>优先看顶部状态横幅和这里的连接快照，确认当前是直连还是本地代理。</li>
          <li>如果页面状态异常，先清空治理台状态，再回到对应页面重新操作。</li>
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
.page{display:flex;flex-direction:column;gap:20px}.head,.actions,.panel-head{display:flex;gap:12px;align-items:flex-start;flex-wrap:wrap}.head,.panel-head{justify-content:space-between}.kicker{margin:0 0 6px;color:#0ea5e9;font-size:12px;font-weight:700;letter-spacing:.22em;text-transform:uppercase}.head h1{margin:0;font-size:30px}.desc{margin:8px 0 0;color:#52606d}.cards,.grid,.quick-grid{display:grid;gap:16px}.cards{grid-template-columns:repeat(4,minmax(0,1fr))}.quick-grid{grid-template-columns:repeat(4,minmax(0,1fr))}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.card,.panel,.quick{border-radius:20px;background:#fff;padding:18px;border:1px solid rgba(148,163,184,.12);box-shadow:0 18px 40px rgba(15,23,42,.08)}.card span{color:#64748b;font-size:13px}.card strong{display:block;margin-top:10px;font-size:28px}.quick{display:flex;flex-direction:column;gap:8px;text-decoration:none;color:#0f172a;background:linear-gradient(180deg,#ffffff,#f8fafc)}.quick span{color:#64748b;font-size:13px}.panel h2{margin:0 0 14px;font-size:18px}.list{display:flex;flex-direction:column;gap:10px}.item{display:flex;flex-direction:column;gap:10px;padding:14px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0}.item-main strong{display:block;margin-bottom:4px}.item-main p{margin:0;color:#64748b;font-size:13px}.tips{margin:0;padding-left:20px;color:#475569;display:flex;flex-direction:column;gap:10px}.primary,.ghost{border:0;border-radius:14px;padding:11px 16px;cursor:pointer;font-weight:600}.ghost.small{padding:8px 12px;font-size:12px}.primary{background:linear-gradient(135deg,#0f172a,#0f766e);color:#fff}.ghost{background:#e2e8f0;color:#334155}.error,.success,.empty{padding:12px 16px;border-radius:14px}.error{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.success{background:#ecfeff;color:#155e75;border:1px solid #a5f3fc}.empty{color:#94a3b8;text-align:center}@media (max-width:1100px){.head,.cards,.grid,.quick-grid{grid-template-columns:1fr;display:grid}}
</style>
