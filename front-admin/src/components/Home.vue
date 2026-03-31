<template>
  <div class="settings-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <p class="sidebar-kicker">Admin Console</p>
        <h1>管理导航</h1>
      </div>
      <nav>
        <div v-for="group in navGroups" :key="group.id" class="nav-group">
          <button class="group-title toggle" @click="toggleGroup(group.id)">
            <span>{{ group.title }}</span>
            <span class="chevron" :class="{ open: open[group.id] }"></span>
          </button>
          <ul v-show="open[group.id]">
            <li v-for="item in group.items" :key="item.to">
              <router-link :to="item.to">{{ item.label }}</router-link>
            </li>
          </ul>
        </div>
      </nav>
    </aside>
    <main class="content-area">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'

import { readGovernanceStorage, writeGovernanceStorage } from '../utils/governanceStorage'

type NavItem = {
  label: string
  to: string
}

type NavGroup = {
  id: string
  title: string
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    id: 'governance',
    title: '治理台',
    items: [
      { label: '治理总览', to: '/admin/governance/dashboard' },
      { label: '回放中心', to: '/admin/governance/replay' },
      { label: '评估与回归', to: '/admin/governance/eval' },
      { label: '发布与灰度', to: '/admin/governance/release' },
      { label: '联调诊断', to: '/admin/governance/diagnostics' },
    ],
  },
  {
    id: 'local-activity',
    title: '本地活动管理',
    items: [
      { label: '活动审核', to: '/admin/local-activity/review' },
      { label: '固定日程审核', to: '/admin/local-activity/schedule-review' },
    ],
  },
  {
    id: 'neighbor',
    title: '邻里互助管理',
    items: [{ label: '互助审核', to: '/admin/neighbor-tasks/review' }],
  },
  {
    id: 'second-hand',
    title: '二手市场管理',
    items: [{ label: '二手市场管理', to: '/admin/second-hand' }],
  },
  {
    id: 'user',
    title: '用户管理',
    items: [{ label: '用户管理', to: '/admin/user-management' }],
  },
  {
    id: 'notice',
    title: '通知公告管理',
    items: [{ label: '发布通知公告', to: '/notifications/publish' }],
  },
  {
    id: 'system',
    title: '系统管理',
    items: [{ label: '接口管理', to: '/admin/api-management' }],
  },
]

const defaultOpen = navGroups.reduce<Record<string, boolean>>((acc, group) => {
  acc[group.id] = group.id === 'governance'
  return acc
}, {})

const route = useRoute()
const open = reactive<Record<string, boolean>>({
  ...defaultOpen,
  ...readGovernanceStorage<Record<string, boolean>>('admin-nav-groups-open', defaultOpen),
})

function toggleGroup(id: string) {
  open[id] = !open[id]
}

function findGroupIdByPath(path: string) {
  const matchedGroup = navGroups.find((group) =>
    group.items.some((item) => path === item.to || path.startsWith(`${item.to}/`)),
  )
  return matchedGroup?.id ?? null
}

watch(
  () => route.path,
  (path) => {
    const activeGroupId = findGroupIdByPath(path)
    if (activeGroupId) {
      open[activeGroupId] = true
    }
  },
  { immediate: true },
)

watch(
  open,
  (value) => {
    writeGovernanceStorage('admin-nav-groups-open', { ...value })
  },
  { deep: true },
)
</script>

<style scoped>
.settings-layout {
  display: flex;
  min-height: calc(100vh - 96px);
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.08), transparent 32%),
    linear-gradient(180deg, #eef4fb 0%, #f8fafc 100%);
  color: #0f172a;
}

.sidebar {
  width: 272px;
  flex-shrink: 0;
  padding: 28px 20px 40px;
  background: rgba(255, 255, 255, 0.86);
  border-right: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 12px 0 30px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
}

.sidebar-header {
  margin-bottom: 24px;
}

.sidebar-header h1 {
  margin: 4px 0 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.sidebar-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: #0f766e;
}

.nav-group {
  margin-bottom: 14px;
  padding: 12px 12px 8px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.84);
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.group-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
}

.group-title.toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  text-align: left;
}

.chevron {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.chevron::before {
  content: '▶';
  font-size: 12px;
  color: #2563eb;
}

.chevron.open {
  transform: rotate(90deg);
  background: rgba(59, 130, 246, 0.14);
}

.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar li + li {
  margin-top: 6px;
}

.sidebar li a {
  display: block;
  padding: 10px 12px;
  border-radius: 12px;
  color: #334155;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.sidebar li a:hover {
  background-color: rgba(37, 99, 235, 0.08);
  color: #0f172a;
  transform: translateX(2px);
}

.sidebar li a.router-link-active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.14), rgba(16, 185, 129, 0.1));
  color: #0f172a;
  font-weight: 700;
}

.content-area {
  flex: 1;
  min-width: 0;
  padding: 20px;
}

@media (max-width: 1080px) {
  .settings-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  }
}
</style>
