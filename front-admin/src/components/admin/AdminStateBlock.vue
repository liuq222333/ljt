<template>
  <div class="admin-state-block" :class="`admin-state-block--${tone}`">
    <p class="admin-state-block__message">{{ message }}</p>
    <div v-if="$slots.actions" class="admin-state-block__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    message: string
    tone?: 'neutral' | 'danger' | 'warning' | 'success'
  }>(),
  {
    tone: 'neutral',
  },
)
</script>

<style scoped>
.admin-state-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 10px 12px;
}

.admin-state-block__message {
  margin: 0;
  font-size: 12px;
  color: var(--admin-text-secondary);
}

.admin-state-block__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-state-block--danger {
  border-color: #efc3bc;
  background: #fbeeed;
}

.admin-state-block--danger .admin-state-block__message {
  color: #9f2f24;
}

.admin-state-block--warning {
  border-color: #edd8a5;
  background: #fcf8ec;
}

.admin-state-block--warning .admin-state-block__message {
  color: #8d6511;
}

.admin-state-block--success {
  border-color: #b8dfcb;
  background: #ebf8f1;
}

.admin-state-block--success .admin-state-block__message {
  color: #1f7a4d;
}
</style>
