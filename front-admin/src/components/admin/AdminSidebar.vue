<template>
  <aside class="admin-sidebar">
    <div class="admin-sidebar__header">
      <p class="admin-sidebar__kicker">AAALJT 管理端</p>
      <h1 class="admin-sidebar__title">后台控制台</h1>
    </div>
    <nav class="admin-sidebar__nav">
      <section v-for="group in groups" :key="group.id" class="admin-sidebar__group">
        <button class="admin-sidebar__group-toggle" type="button" @click="$emit('toggle-group', group.id)">
          <span>{{ group.title }}</span>
          <span class="admin-sidebar__chevron" :class="{ 'is-open': openGroups[group.id] }" />
        </button>
        <ul v-show="openGroups[group.id]" class="admin-sidebar__items">
          <li v-for="item in group.items" :key="item.to">
            <router-link :to="item.to" class="admin-sidebar__link">{{ item.label }}</router-link>
          </li>
        </ul>
      </section>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import type { AdminNavGroup } from '../../config/adminNavigation'

defineProps<{
  groups: AdminNavGroup[]
  openGroups: Record<string, boolean>
}>()

defineEmits<{
  (e: 'toggle-group', id: string): void
}>()
</script>

<style scoped>
.admin-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px 0;
  background: var(--admin-bg-surface);
  overflow: hidden;
}

.admin-sidebar__header {
  padding: 2px 12px 12px;
  border-bottom: 1px solid var(--admin-border);
}

.admin-sidebar__kicker {
  margin: 0;
  color: var(--admin-text-muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-sidebar__title {
  margin: 6px 0 0;
  color: var(--admin-text-primary);
  font-size: 16px;
  font-weight: 700;
}

.admin-sidebar__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-top: 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.admin-sidebar__group {
  padding: 0 8px;
}

.admin-sidebar__group + .admin-sidebar__group {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--admin-border);
}

.admin-sidebar__group-toggle {
  width: 100%;
  border: 0;
  padding: 8px 8px 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 700;
  background: transparent;
  text-align: left;
  cursor: pointer;
  letter-spacing: 0.02em;
}

.admin-sidebar__chevron::before {
  content: '>';
  display: inline-block;
  font-size: 11px;
  color: var(--admin-text-secondary);
  transform: rotate(0deg);
  transition: transform 0.15s ease;
}

.admin-sidebar__chevron.is-open::before {
  transform: rotate(90deg);
}

.admin-sidebar__items {
  list-style: none;
  margin: 0;
  padding: 0 0 4px;
}

.admin-sidebar__items li + li {
  margin-top: 2px;
}

.admin-sidebar__link {
  display: block;
  padding: 8px;
  border-radius: var(--admin-radius-control);
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.admin-sidebar__link:hover {
  background: var(--admin-accent-soft);
  color: var(--admin-text-primary);
}

.admin-sidebar__link.router-link-active {
  background: var(--admin-accent-soft);
  color: var(--admin-accent);
  font-weight: 700;
}
</style>
