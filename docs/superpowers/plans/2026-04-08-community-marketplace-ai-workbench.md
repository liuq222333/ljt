# Community Marketplace AI Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue` into a desktop-first AI workbench homepage with a strong first-screen AI flow and lower-priority content shelves.

**Architecture:** Keep the route surface unchanged and keep the page implemented as one Vue SFC, but replace the current chat-plus-market split layout with a single main column made of `page-header`, `ai-workbench`, `task-overview`, `featured-templates`, and `featured-market`. Reuse the existing product fetch helpers, detail navigation, and agent endpoint, while rewrapping the agent interaction as a task-launch panel instead of a chat window.

**Tech Stack:** Vue 3 Composition API, TypeScript in `<script setup>`, Vue Router 4, native `fetch`, scoped CSS, Vite build validation.

---

## File Structure

- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
  - Rewrite the template into the new single-column AI workbench structure.
  - Replace garbled copy with clear Chinese labels.
  - Keep existing API endpoints and route names intact.
- Keep as-is: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\cebianTool.vue`
  - Keep the floating shortcut rail mounted unless the refactor exposes a visual conflict during final validation.
- Reference only: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`
  - Preserve route names such as `CommunityMarketplaceFind`, `ProductDetail`, `AddProduct`, `MyProducts`, `MyOrder`, and `ShopCar`.
- Validate with: `D:\code\aaaaljt\front\aaljt\package.json`
  - Use the existing `npm run build` script for compile validation after each major step.

### Task 1: Rebuild the page view-model around the AI workbench

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\package.json`

- [ ] **Step 1: Replace the old chat-first state with typed workbench metadata**

```ts
interface TemplateCategory {
  id: string;
  label: string;
}

interface TemplateCard {
  id: string;
  categoryId: string;
  title: string;
  summary: string;
  scenario: string;
  placeholder: string;
  promptPrefix: string;
  tags: string[];
}

interface OverviewCard {
  id: string;
  label: string;
  value: string;
  helper: string;
}

const templateCategories: TemplateCategory[] = [
  { id: 'copy', label: '文案生成' },
  { id: 'campaign', label: '活动策划' },
  { id: 'visual', label: '海报配图' },
  { id: 'ops', label: '运营助手' },
  { id: 'data', label: '数据整理' }
];

const templateCards: TemplateCard[] = [
  {
    id: 'campaign-plan',
    categoryId: 'campaign',
    title: '活动策划模板',
    summary: '快速整理活动主题、亮点、执行节奏和分工。',
    scenario: '适合社区活动、节日活动、商户联动。',
    placeholder: '例如：帮我策划一个周末社区亲子市集活动，预算 5000 元。',
    promptPrefix: '请基于下面的需求输出结构化活动策划方案：',
    tags: ['活动方案', '时间排期', '执行清单']
  },
  {
    id: 'product-copy',
    categoryId: 'copy',
    title: '商品文案模板',
    summary: '生成商品卖点、标题和详情文案。',
    scenario: '适合二手商品、社区服务、活动报名文案。',
    placeholder: '例如：帮我写一段闲置婴儿推车的转让文案，突出成色和使用次数。',
    promptPrefix: '请基于下面的商品信息生成清晰可信的文案：',
    tags: ['标题优化', '卖点提炼', '详情描述']
  }
];
```

- [ ] **Step 2: Add focused workbench refs and computed state**

```ts
const selectedCategoryId = ref(templateCategories[0].id);
const selectedTemplateId = ref(templateCards[0].id);
const taskInput = ref('');
const outputTone = ref('professional');
const outputFormat = ref('plan');
const outputLength = ref('standard');

const visibleTemplates = computed(() =>
  templateCards.filter((template) => template.categoryId === selectedCategoryId.value)
);

const activeTemplate = computed(
  () => templateCards.find((template) => template.id === selectedTemplateId.value) ?? templateCards[0]
);

watch(selectedCategoryId, (categoryId) => {
  const firstTemplate = templateCards.find((template) => template.categoryId === categoryId);
  if (firstTemplate) {
    selectedTemplateId.value = firstTemplate.id;
    taskInput.value = '';
  }
}, { immediate: true });

watch(activeTemplate, (template) => {
  if (template && !taskInput.value.trim()) {
    taskInput.value = template.placeholder;
  }
}, { immediate: true });
```

- [ ] **Step 3: Convert chat session usage into a task-launch helper and overview cards**

```ts
const submitWorkbenchTask = async () => {
  const raw = taskInput.value.trim();
  if (!raw || agentLoading.value) return;

  const composedPrompt = `${activeTemplate.value.promptPrefix}\n${raw}`;
  agentMessages.value.push({
    id: `${Date.now()}-user`,
    sender: 'user',
    text: composedPrompt,
    time: formatChatTime()
  });

  agentInput.value = composedPrompt;
  await sendAgentMessage();
};

const overviewCards = computed<OverviewCard[]>(() => [
  {
    id: 'recent-task',
    label: '最近任务',
    value: agentMessages.value.length > 1 ? '1 条待继续' : '暂无任务',
    helper: agentMessages.value.length > 1 ? '继续补充上次输入' : '先选择一个模板开始'
  },
  {
    id: 'processing',
    label: '处理中',
    value: agentLoading.value ? 'AI 生成中' : '空闲',
    helper: agentLoading.value ? '请稍候查看结果' : '可以立即发起新任务'
  },
  {
    id: 'featured-products',
    label: '精选资源',
    value: `${Math.min(products.value.length, 6)} 项`,
    helper: '下方货架继续浏览服务和资源'
  }
]);
```

- [ ] **Step 4: Run compile validation after the script rewrite**

Run: `npm run build`

Expected: Vite build passes without TypeScript or template compile errors from `CommunityMarketplace.vue`.

- [ ] **Step 5: Commit the state-model checkpoint**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplace.vue
git commit -m "feat: reshape community marketplace workbench state"
```

### Task 2: Replace the page template with the new top-heavy AI workbench layout

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Reference: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`

- [ ] **Step 1: Remove the old split layout and add the new header + workbench shell**

```vue
<div class="page-shell">
  <CebianTool />

  <main class="page-main">
    <section class="page-header card-panel">
      <div>
        <p class="eyebrow">AI 工作台</p>
        <h1>先选模板，再开始处理任务</h1>
        <p class="page-description">
          这里优先处理文案、活动、运营和整理类任务，市场内容放到下方作为辅助入口。
        </p>
      </div>
      <div class="header-actions">
        <button type="button" class="ghost-btn" @click="router.push({ name: 'CommunityMarketplaceFind' })">
          浏览完整市场
        </button>
        <button type="button" class="primary-btn" @click="focusWorkbenchInput">
          继续上次任务
        </button>
      </div>
    </section>

    <section class="ai-workbench card-panel">
      <div class="template-column">
        <!-- categories and template cards -->
      </div>
      <div class="composer-column">
        <!-- active template summary and input form -->
      </div>
    </section>
  </main>
</div>
```

- [ ] **Step 2: Fill the template column with category tabs and compact template cards**

```vue
<div class="template-toolbar">
  <button
    v-for="category in templateCategories"
    :key="category.id"
    type="button"
    :class="['category-chip', { active: category.id === selectedCategoryId }]"
    @click="selectedCategoryId = category.id"
  >
    {{ category.label }}
  </button>
</div>

<div class="template-list">
  <button
    v-for="template in visibleTemplates"
    :key="template.id"
    type="button"
    :class="['template-card', { active: template.id === selectedTemplateId }]"
    @click="selectedTemplateId = template.id"
  >
    <span class="template-title">{{ template.title }}</span>
    <span class="template-summary">{{ template.summary }}</span>
    <span class="template-scenario">{{ template.scenario }}</span>
  </button>
</div>
```

- [ ] **Step 3: Add the right-side task launcher, task overview row, and lower shelves**

```vue
<section class="task-overview">
  <article v-for="card in overviewCards" :key="card.id" class="overview-card card-panel">
    <span class="overview-label">{{ card.label }}</span>
    <strong class="overview-value">{{ card.value }}</strong>
    <p class="overview-helper">{{ card.helper }}</p>
  </article>
</section>

<section class="content-shelf card-panel">
  <div class="section-heading">
    <h2>精选模板</h2>
    <button type="button" class="text-btn" @click="selectedCategoryId = 'copy'">查看常用模板</button>
  </div>
  <div class="shelf-grid">
    <article v-for="template in templateCards.slice(0, 4)" :key="template.id" class="shelf-card">
      <h3>{{ template.title }}</h3>
      <p>{{ template.summary }}</p>
      <button type="button" class="text-btn" @click="selectedTemplateId = template.id">立即使用</button>
    </article>
  </div>
</section>

<section class="content-shelf card-panel">
  <div class="section-heading">
    <h2>精选资源与服务</h2>
    <button type="button" class="text-btn" @click="router.push({ name: 'CommunityMarketplaceFind' })">
      查看更多
    </button>
  </div>
  <div class="shelf-grid market-grid">
    <article
      v-for="product in products.slice(0, 6)"
      :key="product.id"
      class="market-card"
      @click="navigateToDetail(product)"
    >
      <img :src="getFirstImage(product)" :alt="product.title" />
      <div class="market-card-body">
        <h3>{{ product.title }}</h3>
        <p>{{ formatLocation(product.location ?? product.loaction) }}</p>
      </div>
    </article>
  </div>
</section>
```

- [ ] **Step 4: Run compile validation after the template rewrite**

Run: `npm run build`

Expected: The new template compiles, and route references still resolve.

- [ ] **Step 5: Commit the template checkpoint**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplace.vue
git commit -m "feat: rebuild community marketplace page layout"
```

### Task 3: Reframe the agent interaction and content shelves around the approved UX

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\package.json`

- [ ] **Step 1: Replace the old chat input area with a compact task composer**

```vue
<div class="composer-header">
  <div>
    <p class="eyebrow">当前模板</p>
    <h2>{{ activeTemplate.title }}</h2>
    <p class="composer-description">{{ activeTemplate.scenario }}</p>
  </div>
  <div class="tag-row">
    <span v-for="tag in activeTemplate.tags" :key="tag" class="info-tag">{{ tag }}</span>
  </div>
</div>

<textarea
  ref="taskInputRef"
  v-model="taskInput"
  class="task-input"
  rows="8"
  :placeholder="activeTemplate.placeholder"
/>

<div class="parameter-row">
  <select v-model="outputTone" class="workbench-select">
    <option value="professional">专业稳重</option>
    <option value="friendly">亲和自然</option>
    <option value="concise">简洁直接</option>
  </select>
  <select v-model="outputFormat" class="workbench-select">
    <option value="plan">结构化方案</option>
    <option value="copy">直接文案</option>
    <option value="steps">执行步骤</option>
  </select>
  <select v-model="outputLength" class="workbench-select">
    <option value="short">简版</option>
    <option value="standard">标准</option>
    <option value="long">详细</option>
  </select>
</div>
```

- [ ] **Step 2: Add explicit button actions and fallback states for AI submissions**

```vue
<div class="composer-actions">
  <button type="button" class="ghost-btn" @click="taskInput = activeTemplate.placeholder">示例填充</button>
  <button type="button" class="ghost-btn" @click="taskInput = ''">清空输入</button>
  <button type="button" class="primary-btn" :disabled="agentLoading || !taskInput.trim()" @click="submitWorkbenchTask">
    {{ agentLoading ? '生成中...' : '立即生成' }}
  </button>
</div>

<div v-if="agentError" class="inline-state error-state">{{ agentError }}</div>
<div v-else-if="latestAgentReply" class="inline-state result-state">
  <strong>最近结果</strong>
  <p>{{ latestAgentReply }}</p>
</div>
```

- [ ] **Step 3: Tighten the lower shelves so they behave like curated content, not a feed**

```ts
const featuredProducts = computed(() => products.value.slice(0, 6));

const quickActions = [
  { id: 'publish', label: '发布资源', routeName: 'AddProduct' },
  { id: 'orders', label: '我的订单', routeName: 'MyOrder' },
  { id: 'inventory', label: '我的商品', routeName: 'MyProducts' }
];
```

```vue
<div class="quick-action-row">
  <button
    v-for="action in quickActions"
    :key="action.id"
    type="button"
    class="quick-action"
    @click="router.push({ name: action.routeName })"
  >
    {{ action.label }}
  </button>
</div>
```

- [ ] **Step 4: Run compile validation after the interaction refactor**

Run: `npm run build`

Expected: The page still builds, and the agent submission path compiles with the new workbench wrappers.

- [ ] **Step 5: Commit the interaction checkpoint**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplace.vue
git commit -m "feat: reframe marketplace agent panel as AI workbench"
```

### Task 4: Replace the visual system and finish desktop validation

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\package.json`

- [ ] **Step 1: Remove gradients, oversized radii, and floating-card styling**

```css
:root {
  --page-bg: #f3f5f7;
  --panel-bg: #ffffff;
  --panel-border: #dfe4ea;
  --text-main: #1f2933;
  --text-secondary: #667085;
  --text-muted: #98a2b3;
  --accent: #2f5d95;
  --accent-soft: #eaf1f8;
  --shadow-soft: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.card-panel {
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  background: var(--panel-bg);
  box-shadow: var(--shadow-soft);
}

.template-card,
.overview-card,
.market-card,
.shelf-card,
.quick-action,
.workbench-select,
.task-input,
.primary-btn,
.ghost-btn {
  border-radius: 8px;
}
```

- [ ] **Step 2: Add the new desktop layout grid and compact spacing**

```css
.page-main {
  width: min(1280px, calc(100vw - 120px));
  margin: 0 auto;
  padding: 88px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ai-workbench {
  display: grid;
  grid-template-columns: 0.95fr 1.35fr;
  gap: 20px;
  padding: 20px;
}

.task-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.shelf-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.market-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
```

- [ ] **Step 3: Add restrained component styling for the approved feel**

```css
.category-chip.active,
.template-card.active {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.primary-btn {
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #fff;
}

.ghost-btn,
.quick-action,
.workbench-select {
  border: 1px solid var(--panel-border);
  background: #fff;
  color: var(--text-main);
}

.task-input:focus,
.workbench-select:focus,
.category-chip:focus-visible,
.template-card:focus-visible {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(47, 93, 149, 0.12);
}
```

- [ ] **Step 4: Run final validation for desktop compile and manual smoke**

Run: `npm run build`

Expected: Build succeeds, no old hero/chat CSS remains in active use, and the page is ready for desktop smoke-checking at `1280px+`.

Manual smoke checklist:

```text
1. Open /community-marketplace on desktop width.
2. Confirm the first screen reads as AI workbench, not market homepage.
3. Select a different template and verify the placeholder updates.
4. Trigger “立即生成” and verify the loading / result strip renders.
5. Click one featured market card and verify ProductDetail still opens.
```

- [ ] **Step 5: Commit the final visual checkpoint**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplace.vue
git commit -m "feat: redesign community marketplace as AI workbench"
```
