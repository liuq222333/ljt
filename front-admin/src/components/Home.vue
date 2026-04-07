<template>
  <div class="admin-shell">
    <AdminSidebar
      class="admin-shell__sidebar"
      :groups="adminNavGroups"
      :open-groups="openGroups"
      @toggle-group="toggleGroup"
    />
    <div class="admin-shell__main">
      <AdminTopbar
        class="admin-shell__topbar"
        :section="pageMeta.section"
        :title="pageMeta.title"
        :description="pageMeta.description"
      />
      <main class="admin-shell__workspace">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminSidebar from './admin/AdminSidebar.vue'
import AdminTopbar from './admin/AdminTopbar.vue'
import { adminNavGroups } from '../config/adminNavigation'
import { readGovernanceStorage, writeGovernanceStorage } from '../utils/governanceStorage'

type RouteMetaShape = {
  section?: string
  title?: string
  description?: string
}

const defaultOpenGroups = adminNavGroups.reduce<Record<string, boolean>>((accumulator, group) => {
  accumulator[group.id] = group.id === 'governance' || group.id === 'launch'
  return accumulator
}, {})

const route = useRoute()
const openGroups = reactive<Record<string, boolean>>({
  ...defaultOpenGroups,
  ...readGovernanceStorage<Record<string, boolean>>('admin-nav-groups-open', defaultOpenGroups),
})

const pageMeta = computed(() => {
  const routeMeta = (route.meta ?? {}) as RouteMetaShape
  return {
    section: routeMeta.section || '管理端',
    title: routeMeta.title || '工作台',
    description: routeMeta.description || '',
  }
})

function toggleGroup(id: string) {
  openGroups[id] = !openGroups[id]
}

function findGroupIdByPath(path: string) {
  const matchedGroup = adminNavGroups.find((group) =>
    group.items.some((item) => path === item.to || path.startsWith(`${item.to}/`)),
  )
  return matchedGroup?.id ?? null
}

watch(
  () => route.path,
  (path) => {
    const activeGroupId = findGroupIdByPath(path)
    if (activeGroupId) {
      openGroups[activeGroupId] = true
    }
  },
  { immediate: true },
)

watch(
  openGroups,
  (value) => {
    writeGovernanceStorage('admin-nav-groups-open', { ...value })
  },
  { deep: true },
)
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  height: 100vh;
  background: var(--admin-bg-canvas);
  overflow: hidden;
}

.admin-shell__sidebar {
  height: 100vh;
  border-right: 1px solid var(--admin-border);
  background: var(--admin-bg-surface);
  overflow: hidden;
}

.admin-shell__main {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-shell__topbar {
  flex: 0 0 auto;
  border-bottom: 1px solid var(--admin-border);
  background: var(--admin-bg-surface);
}

.admin-shell__workspace {
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 12px 16px 16px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
}
</style>
