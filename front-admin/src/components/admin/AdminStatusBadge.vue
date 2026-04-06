<template>
  <span class="admin-status-badge" :class="`admin-status-badge--${tone}`">{{ label }}</span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger'
  }>(),
  {
    tone: 'neutral',
  },
)
</script>

<style scoped>
.admin-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: var(--admin-radius-control);
  border: 1px solid var(--admin-border);
  background: var(--admin-bg-subtle);
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.admin-status-badge--info {
  border-color: #bdd2ea;
  background: #edf4fc;
  color: #294f79;
}

.admin-status-badge--success {
  border-color: #b8dfcb;
  background: #ebf8f1;
  color: #1f7a4d;
}

.admin-status-badge--warning {
  border-color: #edd8a5;
  background: #fcf8ec;
  color: #8d6511;
}

.admin-status-badge--danger {
  border-color: #efc3bc;
  background: #fbeeed;
  color: #9f2f24;
}
</style>
