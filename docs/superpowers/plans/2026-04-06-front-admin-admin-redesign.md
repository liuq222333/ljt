# Front Admin Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `D:\code\aaaaljt\front-admin` into a desktop-first admin console with a unified shell, compact neutral styling, no gradients, and shared page templates for overview, workbench, list, and form workflows.

**Architecture:** Introduce a shared admin design system first (tokens, theme CSS, shell components, reusable page primitives, and guardrail tests), then migrate the current routes in vertical slices: shell, governance overview, launch workbench, remaining governance pages, and secondary management pages. Keep the existing Vue Router map and API modules intact so the redesign stays in the frontend presentation layer.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, scoped/global CSS, Vitest, `@vue/test-utils`, jsdom

---

## File Structure

### Shared foundation

- Create: `D:\code\aaaaljt\front-admin\src\styles\admin-tokens.css`
- Create: `D:\code\aaaaljt\front-admin\src\styles\admin-theme.css`
- Create: `D:\code\aaaaljt\front-admin\src\config\adminNavigation.ts`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminSidebar.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminTopbar.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminPageHeader.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminMetricCard.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminPanel.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminToolbar.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminStatusBadge.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminStateBlock.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\style.css`
- Modify: `D:\code\aaaaljt\front-admin\src\App.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\Home.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\Router\index.ts`

### Tests and test helpers

- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-theme-guardrails.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-shell-smoke.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\governance-dashboard-layout.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\launch-workbench-layout.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\governance-workbench-source.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\secondary-admin-pages-source.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-source-audit.spec.ts`
- Modify: `D:\code\aaaaljt\front-admin\package.json`
- Modify: `D:\code\aaaaljt\front-admin\package-lock.json`
- Modify: `D:\code\aaaaljt\front-admin\vite.config.ts`

### Page migrations

- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceDashboard.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\launch\LaunchCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceReplayCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceEvalCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceReleaseCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceDiagnosticsCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceStatusBanner.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceJsonBlock.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\ActivityReview.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\NotificationPublish.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\UserManagement.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\TaskReview.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\ApiManagement.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\MarketProductsManagement.vue`

All commands below should be run from `D:\code\aaaaljt\front-admin`.

### Task 1: Add test tooling and admin visual guardrails

**Files:**
- Modify: `D:\code\aaaaljt\front-admin\package.json`
- Modify: `D:\code\aaaaljt\front-admin\package-lock.json`
- Modify: `D:\code\aaaaljt\front-admin\vite.config.ts`
- Modify: `D:\code\aaaaljt\front-admin\src\style.css`
- Create: `D:\code\aaaaljt\front-admin\src\styles\admin-tokens.css`
- Create: `D:\code\aaaaljt\front-admin\src\styles\admin-theme.css`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-theme-guardrails.spec.ts`

- [ ] **Step 1: Write the failing guardrail test**

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('admin visual guardrails', () => {
  it('defines shared admin tokens and bans gradients/glass effects', () => {
    const tokens = readSource('styles/admin-tokens.css')
    const theme = readSource('styles/admin-theme.css')

    expect(tokens).toContain('--admin-bg-canvas')
    expect(tokens).toContain('--admin-radius-panel: 8px')
    expect(theme).toContain('.admin-page')
    expect(`${tokens}\n${theme}`).not.toMatch(/gradient/i)
    expect(`${tokens}\n${theme}`).not.toMatch(/backdrop-filter/i)
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test:unit -- src/__tests__/admin-theme-guardrails.spec.ts`
Expected: FAIL with either `Missing script: "test:unit"` or file-not-found errors for the new CSS files.

- [ ] **Step 3: Add the test runner and the initial admin theme files**

```bash
npm install -D vitest @vue/test-utils jsdom
```

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "test:unit": "vitest run"
  },
  "devDependencies": {
    "@types/node": "^24.10.1",
    "@vitejs/plugin-vue": "^6.0.1",
    "@vue/test-utils": "^2.4.6",
    "@vue/tsconfig": "^0.8.1",
    "jsdom": "^26.1.0",
    "typescript": "~5.9.3",
    "vite": "^7.2.4",
    "vitest": "^3.2.4",
    "vue-tsc": "^3.1.4"
  }
}
```

```ts
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    test: {
      environment: 'jsdom',
      css: true,
      include: ['src/__tests__/**/*.spec.ts'],
    },
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
```

```css
:root {
  --admin-bg-canvas: #f3f4f6;
  --admin-bg-surface: #ffffff;
  --admin-bg-subtle: #f7f8fa;
  --admin-border: #d7dbe2;
  --admin-border-strong: #c4cbd4;
  --admin-text-primary: #1f2937;
  --admin-text-secondary: #5b6472;
  --admin-text-muted: #6b7280;
  --admin-accent: #253244;
  --admin-accent-soft: #e9eef5;
  --admin-success: #1f7a4d;
  --admin-warning: #a16207;
  --admin-danger: #b42318;
  --admin-radius-panel: 8px;
  --admin-radius-control: 6px;
  --admin-shadow-panel: 0 1px 2px rgba(15, 23, 42, 0.06);
}
```

```css
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.admin-panel {
  background: var(--admin-bg-surface);
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-panel);
  box-shadow: var(--admin-shadow-panel);
}
```

```css
@import './styles/admin-tokens.css';
@import './styles/admin-theme.css';

:root {
  font-family: "Microsoft YaHei UI", "PingFang SC", "Segoe UI", sans-serif;
  line-height: 1.5;
  font-weight: 400;
  color: var(--admin-text-primary);
  background: var(--admin-bg-canvas);
  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: var(--admin-bg-canvas);
}

a {
  color: inherit;
  text-decoration: none;
}
```

- [ ] **Step 4: Run the test again and verify it passes**

Run: `npm run test:unit -- src/__tests__/admin-theme-guardrails.spec.ts`
Expected: PASS with 1 test passing and no gradient/glass-effect matches.

- [ ] **Step 5: Commit the foundation change**

```bash
git add package.json package-lock.json vite.config.ts src/style.css src/styles/admin-tokens.css src/styles/admin-theme.css src/__tests__/admin-theme-guardrails.spec.ts
git commit -m "test: add admin theme guardrails"
```

### Task 2: Replace the root shell with a real admin frame

**Files:**
- Modify: `D:\code\aaaaljt\front-admin\src\App.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\Home.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\Router\index.ts`
- Create: `D:\code\aaaaljt\front-admin\src\config\adminNavigation.ts`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminSidebar.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminTopbar.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminStatusBadge.vue`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-shell-smoke.spec.ts`

- [ ] **Step 1: Write the failing shell smoke test**

```ts
import { createMemoryHistory, createRouter } from 'vue-router'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Home from '../components/Home.vue'

const DummyScreen = { template: '<div class="dummy-screen">screen</div>' }

describe('admin shell', () => {
  it('renders sidebar, topbar, and workspace around child routes', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: Home,
          children: [
            {
              path: '',
              component: DummyScreen,
              meta: {
                section: '治理',
                title: '治理总览',
                description: '查看治理状态、最近记录与关键待办。',
              },
            },
          ],
        },
      ],
    })

    router.push('/')
    await router.isReady()

    const wrapper = mount(Home, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: true,
        },
      },
    })

    expect(wrapper.find('.admin-shell').exists()).toBe(true)
    expect(wrapper.find('.admin-shell__sidebar').exists()).toBe(true)
    expect(wrapper.find('.admin-shell__topbar').exists()).toBe(true)
    expect(wrapper.text()).toContain('治理总览')
    expect(wrapper.find('.dummy-screen').exists()).toBe(true)
  })
})
```

- [ ] **Step 2: Run the shell test to verify it fails**

Run: `npm run test:unit -- src/__tests__/admin-shell-smoke.spec.ts`
Expected: FAIL because the current shell still uses legacy `.layout`, `.topbar`, and `.settings-layout` markup and does not expose the new admin-frame classes.

- [ ] **Step 3: Implement the shell, central navigation, and route meta**

```ts
export type AdminNavItem = {
  label: string
  to: string
}

export type AdminNavGroup = {
  id: string
  title: string
  items: AdminNavItem[]
}

export const adminNavGroups: AdminNavGroup[] = [
  {
    id: 'governance',
    title: '治理',
    items: [
      { label: '治理总览', to: '/admin/governance/dashboard' },
      { label: '回放中心', to: '/admin/governance/replay' },
      { label: '评估中心', to: '/admin/governance/eval' },
      { label: '发布管理', to: '/admin/governance/release' },
      { label: '诊断中心', to: '/admin/governance/diagnostics' },
    ],
  },
  {
    id: 'launch',
    title: '上线准备',
    items: [{ label: 'Launch Center', to: '/admin/launch/center' }],
  },
  {
    id: 'ops',
    title: '运营管理',
    items: [
      { label: '活动审核', to: '/admin/local-activity/review' },
      { label: '邻里任务审核', to: '/admin/neighbor-tasks/review' },
      { label: '通知发布', to: '/notifications/publish' },
      { label: '用户管理', to: '/admin/user-management' },
      { label: '接口管理', to: '/admin/api-management' },
      { label: '二手市场管理', to: '/admin/second-hand' },
    ],
  },
]
```

```ts
{
  path: 'admin/governance/dashboard',
  name: 'GovernanceDashboard',
  component: () => import('../components/governance/GovernanceDashboard.vue'),
  meta: {
    section: '治理',
    title: '治理总览',
    description: '查看治理状态、最近记录与关键待办。',
  },
}
```

```vue
<template>
  <router-view />
</template>
```

```vue
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
```

```css
.admin-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--admin-bg-canvas);
}

.admin-shell__sidebar {
  border-right: 1px solid var(--admin-border);
  background: var(--admin-bg-surface);
}

.admin-shell__main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
}

.admin-shell__topbar {
  border-bottom: 1px solid var(--admin-border);
  background: var(--admin-bg-surface);
}

.admin-shell__workspace {
  padding: 16px 20px 20px;
  min-width: 0;
}
```

- [ ] **Step 4: Run the shell test again and verify it passes**

Run: `npm run test:unit -- src/__tests__/admin-shell-smoke.spec.ts`
Expected: PASS with the new shell classes and route-driven title content present.

- [ ] **Step 5: Commit the shell refactor**

```bash
git add src/App.vue src/components/Home.vue src/Router/index.ts src/config/adminNavigation.ts src/components/admin/AdminSidebar.vue src/components/admin/AdminTopbar.vue src/components/admin/AdminStatusBadge.vue src/__tests__/admin-shell-smoke.spec.ts src/styles/admin-theme.css
git commit -m "refactor: build admin shell frame"
```

### Task 3: Create overview primitives and migrate Governance Dashboard

**Files:**
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminPageHeader.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminMetricCard.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminPanel.vue`
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminStateBlock.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceDashboard.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceStatusBanner.vue`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\governance-dashboard-layout.spec.ts`

- [ ] **Step 1: Write the failing overview-layout test**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import GovernanceDashboard from '../components/governance/GovernanceDashboard.vue'

vi.mock('../api/adminGovernance', () => ({
  fetchGovernanceDashboard: vi.fn().mockResolvedValue({
    overview: { replay_total: 18, days: 7, w12_ready: true, open_failure_total: 2, stage: 'W13' },
    metrics_summary: { degraded_rate: 0.12, degraded_total: 2 },
    recent_releases: [],
    recent_replays: [],
    recent_eval_versions: [],
    recent_regression_sets: [],
    recent_gray_configs: [],
    recent_eval_runs: [],
    metrics_trend: [],
    eval_case_stats: { total: 5, enabled_total: 4, disabled_total: 1, by_risk_level: { high: 1 } },
    error_attribution_summary: { error_total: 1, degraded_total: 2, error_rate: 0.05, degraded_rate: 0.12 },
  }),
  fetchGovernanceMetricsDaily: vi.fn().mockResolvedValue([]),
  fetchErrorAttributionTrend: vi.fn().mockResolvedValue([]),
  getGovernanceApiBase: vi.fn().mockReturnValue(''),
}))

describe('GovernanceDashboard layout', () => {
  it('uses the overview template and shared admin panels', async () => {
    const wrapper = mount(GovernanceDashboard, {
      global: {
        stubs: { RouterLink: true },
      },
    })

    await flushPromises()

    expect(wrapper.find('.admin-page').exists()).toBe(true)
    expect(wrapper.find('.admin-page-header').exists()).toBe(true)
    expect(wrapper.findAll('.admin-metric-card')).toHaveLength(4)
    expect(wrapper.find('.admin-overview-grid').exists()).toBe(true)
    expect(wrapper.findAll('.admin-panel').length).toBeGreaterThanOrEqual(4)
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test:unit -- src/__tests__/governance-dashboard-layout.spec.ts`
Expected: FAIL because the current page still uses legacy `.card`, `.quick-grid`, and ad-hoc panel structure.

- [ ] **Step 3: Build shared overview primitives and rewrite the dashboard composition**

```vue
<template>
  <header class="admin-page-header">
    <div>
      <p v-if="eyebrow" class="admin-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="admin-page-header__title">{{ title }}</h1>
      <p v-if="description" class="admin-page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="admin-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
```

```vue
<template>
  <article class="admin-metric-card admin-panel">
    <span class="admin-metric-card__label">{{ label }}</span>
    <strong class="admin-metric-card__value">{{ value }}</strong>
    <p v-if="meta" class="admin-metric-card__meta">{{ meta }}</p>
  </article>
</template>
```

```vue
<template>
  <section class="admin-page governance-dashboard">
    <AdminPageHeader eyebrow="治理" title="治理总览" description="查看治理状态、最近记录与关键待办。">
      <template #actions>
        <button class="admin-button admin-button--secondary" @click="copyCurrentLink">复制链接</button>
        <button class="admin-button admin-button--primary" :disabled="loading" @click="loadDashboard">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminStateBlock v-if="error" tone="danger" :message="error" />

    <section class="admin-metric-grid">
      <AdminMetricCard v-for="card in cards" :key="card.label" :label="card.label" :value="card.value" :meta="card.meta" />
    </section>

    <section class="admin-overview-grid">
      <AdminPanel title="最近趋势">
        <!-- trend list -->
      </AdminPanel>
      <AdminPanel title="系统状态">
        <!-- readiness + attribution summary -->
      </AdminPanel>
      <AdminPanel title="最近记录" class="admin-panel--wide">
        <!-- releases / replays / eval versions -->
      </AdminPanel>
      <AdminPanel title="关键待办">
        <!-- integration checks -->
      </AdminPanel>
    </section>
  </section>
</template>
```

```css
.admin-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.admin-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 12px;
}

.admin-panel--wide {
  grid-column: 1 / -1;
}
```

- [ ] **Step 4: Run the overview test and verify it passes**

Run: `npm run test:unit -- src/__tests__/governance-dashboard-layout.spec.ts`
Expected: PASS with 4 metric cards and the new overview grid present.

- [ ] **Step 5: Commit the dashboard migration**

```bash
git add src/components/admin/AdminPageHeader.vue src/components/admin/AdminMetricCard.vue src/components/admin/AdminPanel.vue src/components/admin/AdminStateBlock.vue src/components/governance/GovernanceDashboard.vue src/components/governance/GovernanceStatusBanner.vue src/__tests__/governance-dashboard-layout.spec.ts src/styles/admin-theme.css
git commit -m "refactor: migrate governance overview"
```

### Task 4: Create workbench primitives and migrate Launch Center

**Files:**
- Create: `D:\code\aaaaljt\front-admin\src\components\admin\AdminToolbar.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\launch\LaunchCenter.vue`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\launch-workbench-layout.spec.ts`

- [ ] **Step 1: Write the failing workbench-layout test**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import LaunchCenter from '../components/launch/LaunchCenter.vue'

vi.mock('../api/adminLaunch', () => ({
  fetchLaunchReadiness: vi.fn().mockResolvedValue({ overallReady: true, gates: { checklist: true } }),
  fetchLaunchChecklist: vi.fn().mockResolvedValue([]),
  fetchLaunchFinalSummary: vi.fn().mockResolvedValue({ finalReady: true, gates: { readinessGate: true } }),
  fetchLaunchHandoffSummary: vi.fn().mockResolvedValue({ finalReady: true, ownerActions: [] }),
  fetchLaunchRunbookBundle: vi.fn().mockResolvedValue({ documents: {} }),
  fetchLaunchTimeline: vi.fn().mockResolvedValue([]),
  fetchLaunchPackage: vi.fn().mockResolvedValue({}),
  listLaunchLoadTests: vi.fn().mockResolvedValue([]),
  listLaunchDrills: vi.fn().mockResolvedValue([]),
  listLaunchWindows: vi.fn().mockResolvedValue([]),
  runLaunchSmoke: vi.fn(),
  forceLaunchRealtimeFallback: vi.fn(),
  recoverLaunchRealtimeFallback: vi.fn(),
  recordLaunchLoadTest: vi.fn(),
  recordLaunchDrill: vi.fn(),
  recordLaunchSignoff: vi.fn(),
  recordLaunchDependencyCheck: vi.fn(),
  createLaunchWindow: vi.fn(),
  closeLaunchWindow: vi.fn(),
}))

vi.mock('../utils/governanceFile', () => ({
  downloadJsonFile: vi.fn(),
}))

describe('LaunchCenter layout', () => {
  it('renders the workbench template with toolbar, content column, and side column', async () => {
    const wrapper = mount(LaunchCenter)
    await flushPromises()

    expect(wrapper.find('.admin-page').exists()).toBe(true)
    expect(wrapper.find('.admin-toolbar').exists()).toBe(true)
    expect(wrapper.find('.admin-workbench').exists()).toBe(true)
    expect(wrapper.findAll('.admin-panel').length).toBeGreaterThanOrEqual(4)
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test:unit -- src/__tests__/launch-workbench-layout.spec.ts`
Expected: FAIL because the page still uses the old hero/cards/grid composition rather than the new workbench classes.

- [ ] **Step 3: Implement the shared toolbar and restructure Launch Center**

```vue
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
```

```vue
<template>
  <section class="admin-page launch-center">
    <AdminPageHeader eyebrow="上线准备" title="Launch Center" description="处理 readiness、drill、smoke、signoff 与交付记录。">
      <template #actions>
        <button class="admin-button admin-button--secondary" @click="copyCurrentLink">复制链接</button>
        <button class="admin-button admin-button--secondary" @click="exportHandoffSummary">导出 Handoff</button>
        <button class="admin-button admin-button--primary" :disabled="loading" @click="refreshAll">{{ loading ? '刷新中...' : '刷新全部' }}</button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <div class="admin-toolbar__summary">
          <AdminStatusBadge :tone="readiness.overallReady ? 'success' : 'warning'" :label="readiness.overallReady ? 'Ready' : 'Pending'" />
          <AdminStatusBadge :tone="finalSummary.finalReady ? 'success' : 'warning'" :label="finalSummary.finalReady ? 'Final Ready' : 'Need Action'" />
        </div>
      </template>
      <template #actions>
        <button class="admin-button admin-button--secondary" @click="loadTimeline">刷新时间线</button>
      </template>
    </AdminToolbar>

    <section class="admin-workbench">
      <div class="admin-workbench__main">
        <!-- checklist, smoke, drill, records, timeline -->
      </div>
      <aside class="admin-workbench__side">
        <!-- final summary, handoff summary, runbooks -->
      </aside>
    </section>
  </section>
</template>
```

```css
.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.admin-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
  gap: 12px;
}

.admin-workbench__main,
.admin-workbench__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
```

- [ ] **Step 4: Run the workbench test and verify it passes**

Run: `npm run test:unit -- src/__tests__/launch-workbench-layout.spec.ts`
Expected: PASS with `.admin-toolbar`, `.admin-workbench`, and multiple `.admin-panel` sections rendered.

- [ ] **Step 5: Commit the Launch Center migration**

```bash
git add src/components/admin/AdminToolbar.vue src/components/launch/LaunchCenter.vue src/__tests__/launch-workbench-layout.spec.ts src/styles/admin-theme.css
git commit -m "refactor: migrate launch center workbench"
```

### Task 5: Migrate the remaining governance pages to the shared workbench pattern

**Files:**
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceReplayCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceEvalCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceReleaseCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceDiagnosticsCenter.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceJsonBlock.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceStatusBanner.vue`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\governance-workbench-source.spec.ts`

- [ ] **Step 1: Write the failing source-audit test for governance pages**

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const files = [
  'components/governance/GovernanceReplayCenter.vue',
  'components/governance/GovernanceEvalCenter.vue',
  'components/governance/GovernanceReleaseCenter.vue',
  'components/governance/GovernanceDiagnosticsCenter.vue',
]

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('governance workbench pages', () => {
  it('use shared admin workbench classes and remove legacy visual effects', () => {
    for (const file of files) {
      const source = readSource(file)
      expect(source).toContain('class="admin-page')
      expect(source).toMatch(/admin-toolbar|AdminToolbar/)
      expect(source).toMatch(/admin-panel|AdminPanel/)
      expect(source).not.toMatch(/linear-gradient|radial-gradient|backdrop-filter|translateY|translateX/)
    }
  })
})
```

- [ ] **Step 2: Run the governance audit test to verify it fails**

Run: `npm run test:unit -- src/__tests__/governance-workbench-source.spec.ts`
Expected: FAIL because the current files still contain legacy gradients, hover transforms, or missing shared workbench classes.

- [ ] **Step 3: Rewrite the governance pages around the shared workbench structure**

```vue
<template>
  <section class="admin-page governance-replay-center">
    <AdminPageHeader eyebrow="治理" title="回放中心" description="筛选请求、查看会话、定位失败节点与工具调用。" />

    <AdminToolbar>
      <template #filters>
        <!-- keyword / lookup mode / day range -->
      </template>
      <template #actions>
        <button class="admin-button admin-button--secondary">重置筛选</button>
        <button class="admin-button admin-button--primary">查询</button>
      </template>
    </AdminToolbar>

    <section class="admin-workbench">
      <div class="admin-workbench__main">
        <AdminPanel title="回放列表"><!-- results table/list --></AdminPanel>
        <AdminPanel title="详细信息"><!-- checkpoints/tool I/O --></AdminPanel>
      </div>
      <aside class="admin-workbench__side">
        <AdminPanel title="筛选摘要" />
        <AdminPanel title="排查建议" />
      </aside>
    </section>
  </section>
</template>
```

```vue
<template>
  <pre class="admin-json-block"><slot /></pre>
</template>
```

```css
.admin-json-block {
  margin: 0;
  padding: 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: #f7f8fa;
  color: var(--admin-text-primary);
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
}
```

- [ ] **Step 4: Run the governance audit test again and verify it passes**

Run: `npm run test:unit -- src/__tests__/governance-workbench-source.spec.ts`
Expected: PASS with all governance pages containing shared admin classes and no banned legacy effects.

- [ ] **Step 5: Commit the governance workbench migration**

```bash
git add src/components/governance/GovernanceReplayCenter.vue src/components/governance/GovernanceEvalCenter.vue src/components/governance/GovernanceReleaseCenter.vue src/components/governance/GovernanceDiagnosticsCenter.vue src/components/governance/GovernanceJsonBlock.vue src/components/governance/GovernanceStatusBanner.vue src/__tests__/governance-workbench-source.spec.ts src/styles/admin-theme.css
git commit -m "refactor: unify governance workbench pages"
```

### Task 6: Migrate the secondary admin pages, normalize copy, and run the final audit

**Files:**
- Modify: `D:\code\aaaaljt\front-admin\src\components\ActivityReview.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\NotificationPublish.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\UserManagement.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\TaskReview.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\ApiManagement.vue`
- Modify: `D:\code\aaaaljt\front-admin\src\components\MarketProductsManagement.vue`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\secondary-admin-pages-source.spec.ts`
- Create: `D:\code\aaaaljt\front-admin\src\__tests__\admin-source-audit.spec.ts`

- [ ] **Step 1: Write the failing source-audit tests for the remaining pages and the final no-gradient sweep**

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const pageFiles = [
  'components/ActivityReview.vue',
  'components/NotificationPublish.vue',
  'components/UserManagement.vue',
  'components/TaskReview.vue',
  'components/ApiManagement.vue',
  'components/MarketProductsManagement.vue',
]

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('secondary admin pages', () => {
  it('use shared admin page structure and clean copy', () => {
    for (const file of pageFiles) {
      const source = readSource(file)
      expect(source).toContain('class="admin-page')
      expect(source).toMatch(/AdminPageHeader|admin-page-header/)
      expect(source).toMatch(/AdminPanel|admin-panel/)
      expect(source).not.toMatch(/linear-gradient|radial-gradient|backdrop-filter/)
    }
  })
})
```

```ts
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const collectVueFiles = (root: string): string[] => {
  const entries = readdirSync(root)
  return entries.flatMap((entry) => {
    const fullPath = join(root, entry)
    if (statSync(fullPath).isDirectory()) {
      return collectVueFiles(fullPath)
    }
    return fullPath.endsWith('.vue') ? [fullPath] : []
  })
}

describe('admin source audit', () => {
  it('removes gradients and glass effects from admin components', () => {
    const files = collectVueFiles(join(process.cwd(), 'src', 'components'))
    const source = files.map((file) => readFileSync(file, 'utf8')).join('\n')

    expect(source).not.toMatch(/linear-gradient|radial-gradient|backdrop-filter/)
  })
})
```

- [ ] **Step 2: Run the remaining-page audit tests to verify they fail**

Run: `npm run test:unit -- src/__tests__/secondary-admin-pages-source.spec.ts src/__tests__/admin-source-audit.spec.ts`
Expected: FAIL because the secondary pages still use old gradients, oversized radii, or inconsistent shell markup.

- [ ] **Step 3: Rewrite the secondary pages onto the shared admin templates and normalize the admin wording**

```vue
<template>
  <section class="admin-page activity-review-page">
    <AdminPageHeader eyebrow="运营管理" title="活动审核" description="筛选待审核活动并执行通过/拒绝操作。" />

    <AdminToolbar>
      <template #filters>
        <label class="admin-field">
          <span>状态</span>
          <select v-model="status"><option value="">全部</option></select>
        </label>
        <label class="admin-field admin-field--wide">
          <span>关键字</span>
          <input v-model="keyword" type="text" placeholder="标题 / 地点 / 描述" />
        </label>
      </template>
      <template #actions>
        <button class="admin-button admin-button--primary" :disabled="loading" @click="fetchList">查询</button>
      </template>
    </AdminToolbar>

    <section class="admin-workbench">
      <div class="admin-workbench__main">
        <AdminPanel title="审核列表"><!-- table --></AdminPanel>
      </div>
      <aside class="admin-workbench__side">
        <AdminPanel title="分页信息" />
        <AdminPanel title="审核说明" />
      </aside>
    </section>
  </section>
</template>
```

```vue
<template>
  <section class="admin-page api-management-page">
    <AdminPageHeader eyebrow="系统管理" title="接口管理" description="维护接口定义、状态和基础说明。" />
    <AdminToolbar>
      <template #filters>
        <label class="admin-field admin-field--wide">
          <span>关键字</span>
          <input v-model="keyword" type="text" placeholder="Resource / Path / Description" />
        </label>
      </template>
      <template #actions>
        <button class="admin-button admin-button--primary" @click="openModal()">新增接口</button>
      </template>
    </AdminToolbar>
    <!-- table panel + side panel + modal using admin panel styling -->
  </section>
</template>
```

```css
.admin-table {
  width: 100%;
  border-collapse: collapse;
}

.admin-table th,
.admin-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eceff3;
  text-align: left;
}

.admin-button {
  border: 1px solid transparent;
  border-radius: var(--admin-radius-control);
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button--primary {
  background: var(--admin-accent);
  color: #fff;
}

.admin-button--secondary {
  background: var(--admin-bg-surface);
  border-color: var(--admin-border);
  color: var(--admin-text-primary);
}
```

- [ ] **Step 4: Run the full test suite and build to verify the redesign holds together**

Run: `npm run test:unit && npm run build`
Expected: PASS with all spec files passing, `vue-tsc` clean, and Vite build output generated successfully.

- [ ] **Step 5: Commit the remaining page migration and final audit**

```bash
git add src/components/ActivityReview.vue src/components/NotificationPublish.vue src/components/UserManagement.vue src/components/TaskReview.vue src/components/ApiManagement.vue src/components/MarketProductsManagement.vue src/__tests__/secondary-admin-pages-source.spec.ts src/__tests__/admin-source-audit.spec.ts src/styles/admin-theme.css
git commit -m "refactor: unify secondary admin pages"
```

## Self-Review

- **Spec coverage:** The plan covers shell, tokens, shared components, overview template, launch workbench, governance workbench pages, secondary admin pages, copy cleanup, and validation.
- **Placeholder scan:** No task uses TBD/TODO placeholders; each task names exact files, commands, test files, and representative code.
- **Type consistency:** Shared class names and component names stay consistent across tasks: `AdminPageHeader`, `AdminPanel`, `AdminToolbar`, `.admin-page`, `.admin-workbench`, `.admin-panel`.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-06-front-admin-admin-redesign.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
