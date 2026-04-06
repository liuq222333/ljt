<template>
  <header class="admin-page-header">
    <div class="admin-page-header__meta">
      <p v-if="eyebrow" class="admin-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="admin-page-header__title">{{ title }}</h1>
      <p v-if="description" class="admin-page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="admin-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  eyebrow?: string
  description?: string
}>()
</script>

<style scoped>
.admin-page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.admin-page-header__meta {
  min-width: 0;
}

.admin-page-header__eyebrow {
  margin: 0;
  color: var(--admin-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-page-header__title {
  margin: 4px 0 0;
  color: var(--admin-text-primary);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.admin-page-header__description {
  margin: 6px 0 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.admin-page-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
