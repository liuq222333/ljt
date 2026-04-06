<template>
  <section class="admin-panel">
    <header v-if="title || description || $slots.actions" class="admin-panel__header">
      <div class="admin-panel__meta">
        <h2 v-if="title" class="admin-panel__title">{{ title }}</h2>
        <p v-if="description" class="admin-panel__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="admin-panel__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="admin-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.admin-panel {
  padding: 12px;
}

.admin-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.admin-panel__meta {
  min-width: 0;
}

.admin-panel__title {
  margin: 0;
  color: var(--admin-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.admin-panel__description {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-panel__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-panel__body {
  margin-top: 10px;
}
</style>
