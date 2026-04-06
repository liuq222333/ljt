<template>
  <section class="admin-toolbar admin-panel">
    <div class="admin-toolbar__filters">
      <slot name="filters" />
    </div>
    <div class="admin-toolbar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.admin-toolbar__filters {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.admin-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
