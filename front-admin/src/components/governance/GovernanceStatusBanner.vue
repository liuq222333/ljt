<template>
  <div class="banner" :class="bannerClass">
    <div class="content">
      <strong>治理联调状态</strong>
      <span>{{ description }}</span>
      <span v-if="probe.checkedAt" class="muted">最近检测：{{ checkedAtLabel }}</span>
    </div>
    <div class="actions">
      <code class="meta">{{ probe.mode }}</code>
      <code v-if="probe.latencyMs !== undefined" class="meta">{{ probe.latencyMs }} ms</code>
      <button class="ghost" :disabled="loading" @click="runProbe">{{ loading ? '检测中...' : '重新检测' }}</button>
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
  if (loading.value) return 'checking'
  return probe.value.ok ? 'healthy' : 'warn'
})

const description = computed(() => {
  if (loading.value) {
    return '正在探测治理后端连接状态...'
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
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  color: #0f172a;
}

.banner.healthy {
  background: linear-gradient(90deg, rgba(20, 184, 166, 0.14), rgba(45, 212, 191, 0.08));
  border-bottom-color: rgba(20, 184, 166, 0.2);
}

.banner.warn {
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.14), rgba(251, 191, 36, 0.08));
  border-bottom-color: rgba(245, 158, 11, 0.2);
}

.banner.checking {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.14), rgba(125, 211, 252, 0.08));
  border-bottom-color: rgba(59, 130, 246, 0.2);
}

.content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.content strong {
  font-size: 13px;
}

.content span {
  color: #475569;
  font-size: 13px;
}

.muted {
  color: #64748b;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #0f172a;
  font-size: 12px;
}

.ghost {
  border: 0;
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.8);
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.ghost:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

@media (max-width: 900px) {
  .banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
