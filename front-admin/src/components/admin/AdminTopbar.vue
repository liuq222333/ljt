<template>
  <header class="admin-topbar">
    <div class="admin-topbar__meta">
      <p class="admin-topbar__section">{{ section }}</p>
      <h2 class="admin-topbar__title">{{ title }}</h2>
      <p v-if="description" class="admin-topbar__description">{{ description }}</p>
    </div>
    <div class="admin-topbar__tags">
      <AdminStatusBadge tone="info" :label="environmentLabel" />
      <AdminStatusBadge tone="neutral" :label="todayLabel" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AdminStatusBadge from './AdminStatusBadge.vue'

withDefaults(
  defineProps<{
    section?: string
    title?: string
    description?: string
  }>(),
  {
    section: '管理端',
    title: '工作台',
    description: '',
  },
)

const todayLabel = computed(() => {
  const value = new Date()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${value.getFullYear()}-${month}-${day}`
})

const environmentLabel = computed(() =>
  import.meta.env.DEV ? '开发环境' : '生产环境',
)
</script>

<style scoped>
.admin-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--admin-bg-surface);
}

.admin-topbar__meta {
  min-width: 0;
}

.admin-topbar__section {
  margin: 0;
  color: var(--admin-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.admin-topbar__title {
  margin: 4px 0 0;
  color: var(--admin-text-primary);
  font-size: 17px;
  font-weight: 700;
}

.admin-topbar__description {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.admin-topbar__tags {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
