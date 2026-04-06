<template>
  <component
    :is="to ? 'router-link' : 'article'"
    class="admin-metric-card admin-panel"
    :class="{ 'admin-metric-card--link': Boolean(to) }"
    v-bind="to ? { to } : {}"
  >
    <span class="admin-metric-card__label">{{ label }}</span>
    <strong class="admin-metric-card__value">{{ value }}</strong>
    <p v-if="meta" class="admin-metric-card__meta">{{ meta }}</p>
  </component>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  meta?: string
  to?: string | Record<string, unknown>
}>()
</script>

<style scoped>
.admin-metric-card {
  display: block;
  min-width: 0;
  padding: 12px;
  text-decoration: none;
}

.admin-metric-card__label {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-metric-card__value {
  display: block;
  margin-top: 8px;
  color: var(--admin-text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.admin-metric-card__meta {
  margin: 6px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.admin-metric-card--link {
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.admin-metric-card--link:hover {
  border-color: var(--admin-border-strong);
  background: var(--admin-bg-subtle);
}
</style>
