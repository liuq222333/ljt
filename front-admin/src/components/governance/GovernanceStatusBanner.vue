<template>
  <div class="governance-status-banner" :class="bannerClass">
    <div class="governance-status-banner__content">
      <strong>治理联调状态</strong>
      <span>{{ description }}</span>
      <span v-if="probe.checkedAt" class="governance-status-banner__muted">最近检测：{{ checkedAtLabel }}</span>
    </div>
    <div class="governance-status-banner__actions">
      <code class="governance-status-banner__meta">{{ probe.mode }}</code>
      <code v-if="probe.latencyMs !== undefined" class="governance-status-banner__meta">{{ probe.latencyMs }} ms</code>
      <button class="governance-status-banner__button" :disabled="loading" type="button" @click="runProbe">
        {{ loading ? '检测中...' : '重新检测' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getGovernanceApiBase, probeGovernanceBackend, type GovernanceBackendProbeResult } from '../../api/adminGovernance'

const loading = ref(false)
const probe = ref<GovernanceBackendProbeResult>({
  ok: false,
  mode: getGovernanceApiBase() ? 'direct-api' : 'vite-proxy',
  apiBase: getGovernanceApiBase(),
  checkedAt: '',
  message: getGovernanceApiBase()
    ? `当前直连治理后端：${getGovernanceApiBase()}`
    : '当前使用本地 /api 代理，请先启动后端服务再开始联调。',
})

const bannerClass = computed(() => {
  if (loading.value) {
    return 'is-checking'
  }
  return probe.value.ok ? 'is-healthy' : 'is-warn'
})

const description = computed(() => {
  if (loading.value) {
    return '正在检测治理后端连接状态...'
  }
  if (probe.value.ok) {
    return probe.value.apiBase
      ? `当前直连治理后端：${probe.value.apiBase}`
      : '当前使用本地 /api 代理，治理后端连接正常。'
  }
  return probe.value.message
})

const checkedAtLabel = computed(() => {
  if (!probe.value.checkedAt) {
    return '-'
  }
  const parsed = new Date(probe.value.checkedAt)
  if (Number.isNaN(parsed.getTime())) {
    return probe.value.checkedAt
  }
  return parsed.toLocaleString()
})

async function runProbe() {
  loading.value = true
  try {
    probe.value = await probeGovernanceBackend()
  } finally {
    loading.value = false
  }
}

onMounted(runProbe)
</script>

<style scoped>
.governance-status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
}

.governance-status-banner.is-healthy {
  border-color: #b8dfcb;
  background: #ebf8f1;
}

.governance-status-banner.is-warn {
  border-color: #edd8a5;
  background: #fcf8ec;
}

.governance-status-banner.is-checking {
  border-color: #bdd2ea;
  background: #edf4fc;
}

.governance-status-banner__content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.governance-status-banner__content strong {
  color: var(--admin-text-primary);
  font-size: 12px;
}

.governance-status-banner__content span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.governance-status-banner__muted {
  color: var(--admin-text-muted);
}

.governance-status-banner__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.governance-status-banner__meta {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
  font-size: 12px;
  padding: 2px 8px;
}

.governance-status-banner__button {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 10px;
}

.governance-status-banner__button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}
</style>
