# Community Marketplace Multi-Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the remaining community marketplace pages into one desktop-first white-and-green marketplace system while keeping product browsing primary and AI as a reusable auxiliary drawer.

**Architecture:** Keep the existing Vue Router surface and backend APIs unchanged, but standardize the marketplace pages around one visual language and one reusable AI conversation drawer. Implement the work in thin vertical slices: shared AI support first, then buyer discovery pages, then seller management pages, then publish pages, validating with the existing Vite build after each slice.

**Tech Stack:** Vue 3 Composition API, TypeScript in `<script setup>`, Vue Router 4, native `fetch`, Element Plus messages already used in-page, scoped CSS, Vite build validation on Node 24.

---

## File Structure

- Create: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue`
  - Reusable marketplace AI drawer UI with header, quick prompts, message list, result cards, input row, loading and error states.
- Create: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts`
  - Shared Composition API state for agent messages, quick prompts, request sending, result-card parsing, preview text, scroll-to-bottom, and drawer open/close helpers.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
  - Replace the in-file drawer markup and logic with the shared drawer/composable without changing the already approved homepage layout.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceFind.vue`
  - Rebuild the search/list page into a compact result workbench that matches the homepage card system and uses the shared AI drawer.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ProductDetail.vue`
  - Rebuild the detail page into a stable purchase-decision layout and wire in the shared AI drawer for evaluation prompts.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ShopCar.vue`
  - Rebuild the cart into a list-left/summary-right layout with visible actions and homepage-aligned recommendation cards.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyProducts.vue`
  - Turn the seller inventory page into a light operations workspace with summary cards and grouped actions.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyOrder.vue`
  - Turn the order page into a status-led ledger with grouped order information and seller-side AI assistance entry.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addProduct.vue`
  - Convert the publish page into sectioned form cards while keeping category fetch, upload, locate, and submit logic intact.
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addNewProduct.vue`
  - Replace the empty shell with a real redirect / entry shell for the publish flow.
- Reference: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`
  - Keep route names and route paths unchanged.
- Validate with: `D:\code\aaaaljt\front\aaljt\package.json`
  - Use `vite build` through `npm run build`.

**Validation note:** This workspace currently uses Vite build validation instead of a dedicated UI test suite. Expected non-blocking warnings include the existing `stompjs`/`net` browser warning and the large chunk-size warning; they do not fail the task if the build exits successfully.

### Task 1: Extract a reusable marketplace AI drawer and preserve the homepage behavior

**Files:**
- Create: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts`
- Create: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue`
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\package.json`

- [ ] **Step 1: Create the shared AI composable with the existing agent endpoint contract**

```ts
// D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts
import { computed, nextTick, ref, type Ref } from 'vue';

export interface MarketplaceAiCard {
  entityId?: number | string;
  title?: string;
  priceText?: string;
  subtitle?: string;
  reason?: string;
  imageUrl?: string;
  tags?: string[];
}

export interface MarketplaceAiMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
  cards?: MarketplaceAiCard[];
}

export function useCommunityMarketplaceAi(apiBase: string, quickPrompts: string[]) {
  const drawerOpen = ref(false);
  const loading = ref(false);
  const error = ref('');
  const input = ref('');
  const messages = ref<MarketplaceAiMessage[]>([]);
  const chatWindowRef = ref<HTMLElement | null>(null);

  const latestUserPreview = computed(() =>
    messages.value.slice().reverse().find((message) => message.sender === 'user')?.text ?? ''
  );

  const latestAgentReplyPreview = computed(() =>
    messages.value.slice().reverse().find((message) => message.sender === 'agent')?.text ?? ''
  );

  const openDrawer = () => {
    drawerOpen.value = true;
  };

  const closeDrawer = () => {
    drawerOpen.value = false;
  };

  const pushPrompt = async (prompt: string) => {
    input.value = prompt;
    await sendMessage();
  };

  const scrollToBottom = async () => {
    await nextTick();
    if (chatWindowRef.value) {
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
    }
  };

  const sendMessage = async () => {
    const question = input.value.trim();
    if (!question || loading.value) return;

    error.value = '';
    messages.value.push({
      id: `${Date.now()}-user`,
      sender: 'user',
      text: question,
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    });
    input.value = '';
    await scrollToBottom();

    loading.value = true;
    try {
      const res = await fetch(`${apiBase}/api/agent/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: question,
          history: messages.value.map((message) => ({
            role: message.sender === 'user' ? 'user' : 'assistant',
            content: message.text
          }))
        })
      });
      if (!res.ok) throw new Error('AI 助手暂时不可用，请稍后再试');
      const data = await res.json();
      messages.value.push({
        id: `${Date.now()}-agent`,
        sender: 'agent',
        text: String(data?.reply ?? data?.message ?? '已收到你的问题'),
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        cards: Array.isArray(data?.cards) ? data.cards : []
      });
    } catch (err: any) {
      error.value = err?.message || 'AI 助手暂时不可用，请稍后再试';
    } finally {
      loading.value = false;
      await scrollToBottom();
    }
  };

  return {
    drawerOpen,
    loading,
    error,
    input,
    messages,
    quickPrompts,
    chatWindowRef,
    latestUserPreview,
    latestAgentReplyPreview,
    openDrawer,
    closeDrawer,
    pushPrompt,
    sendMessage
  };
}
```

- [ ] **Step 2: Create the reusable drawer component with props for title, prompts, and card-click handling**

```vue
<!-- D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue -->
<template>
  <transition name="drawer-fade">
    <div v-if="modelValue" class="drawer-mask" @click="$emit('update:modelValue', false)"></div>
  </transition>

  <aside :class="['drawer-panel', { open: modelValue }]">
    <header class="drawer-header">
      <div>
        <p class="drawer-kicker">{{ kicker }}</p>
        <h3>{{ title }}</h3>
      </div>
      <button type="button" class="ghost-btn compact" @click="$emit('update:modelValue', false)">关闭</button>
    </header>

    <div class="drawer-body">
      <div class="quick-prompt-list">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt"
          type="button"
          class="quick-prompt-btn"
          @click="$emit('prompt', prompt)"
        >
          {{ prompt }}
        </button>
      </div>

      <div ref="chatWindowRef" class="chat-window">
        <div v-if="!messages.length" class="chat-empty">
          可以直接提问，也可以先点上面的快捷入口。
        </div>

        <div v-else class="chat-list">
          <div v-for="message in messages" :key="message.id" :class="['chat-row', message.sender]">
            <div class="chat-bubble">{{ message.text }}</div>
            <div v-if="message.sender === 'agent' && message.cards?.length" class="chat-card-list">
              <article
                v-for="card in message.cards"
                :key="`${message.id}-${card.entityId || card.title}`"
                class="chat-card"
                @click="$emit('card-click', card)"
              >
                <div class="chat-card-body">
                  <h5>{{ card.title || '推荐结果' }}</h5>
                  <p>{{ card.reason || card.subtitle || '点击查看详情' }}</p>
                </div>
                <span v-if="card.priceText" class="chat-card-price">{{ card.priceText }}</span>
              </article>
            </div>
          </div>
        </div>
      </div>

      <p v-if="error" class="chat-error">{{ error }}</p>

      <div class="chat-input-row">
        <input
          :value="inputValue"
          type="text"
          placeholder="继续输入你的问题"
          @input="$emit('update:inputValue', ($event.target as HTMLInputElement).value)"
          @keyup.enter="$emit('send')"
        />
        <button type="button" class="primary-btn" :disabled="loading || !inputValue.trim()" @click="$emit('send')">
          {{ loading ? '发送中' : '发送' }}
        </button>
      </div>
    </div>
  </aside>
</template>
```

- [ ] **Step 3: Replace the homepage’s in-file drawer usage with the shared composable and component**

```ts
// inside D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue
import CommunityMarketplaceAiDrawer from './CommunityMarketplaceAiDrawer.vue';
import { useCommunityMarketplaceAi } from './useCommunityMarketplaceAi';

const apiBase = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const marketAi = useCommunityMarketplaceAi(apiBase, agentQuickPrompts);

const openWorkbenchDrawer = () => marketAi.openDrawer();
const closeWorkbenchDrawer = () => marketAi.closeDrawer();
const toggleWorkbenchDrawer = () => {
  marketAi.drawerOpen.value ? marketAi.closeDrawer() : marketAi.openDrawer();
};

const startAgentPrompt = async (prompt: string) => {
  marketAi.openDrawer();
  await marketAi.pushPrompt(prompt);
};
```

```vue
<CommunityMarketplaceAiDrawer
  v-model="marketAi.drawerOpen"
  kicker="AI 对话框"
  title="市场助手"
  :quick-prompts="agentQuickPrompts"
  :messages="marketAi.messages"
  :input-value="marketAi.input"
  :loading="marketAi.loading"
  :error="marketAi.error"
  @update:input-value="marketAi.input = $event"
  @prompt="startAgentPrompt"
  @send="marketAi.sendMessage"
  @card-click="handleAgentCardClick"
/>
```

- [ ] **Step 4: Run the marketplace build after the extraction**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully. Existing `stompjs`/`net` and chunk-size warnings may still appear, but there are no new TypeScript or Vue SFC errors.

- [ ] **Step 5: Commit the shared-AI extraction slice**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplace.vue D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplaceAiDrawer.vue D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/useCommunityMarketplaceAi.ts
git commit -m "feat: extract shared marketplace ai drawer"
```

### Task 2: Rebuild the search/list page into a compact discovery workbench

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceFind.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts`
- Reference: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`

- [ ] **Step 1: Normalize the top-level computed state so the page exposes query-backed filters and active filter tags**

```ts
const route = useRoute();
const router = useRouter();

const selectedCategoryId = ref(String(route.query.category ?? '全部'));
const selectedPriceKey = ref(String(route.query.price ?? 'all'));
const selectedSortKey = ref(String(route.query.sort ?? 'comprehensive'));

const activeFilterTags = computed(() => {
  const tags: string[] = [];
  if (selectedCategoryId.value !== '全部') {
    const category = allCategories.value.find((item) => String(item.id) === selectedCategoryId.value);
    if (category) tags.push(`分类：${category.name}`);
  }
  if (selectedPriceKey.value !== 'all') {
    const price = priceRanges.find((item) => item.label === selectedPriceKey.value);
    if (price) tags.push(`价格：${price.label}`);
  }
  if (quick.freeShipping) tags.push('包邮');
  if (quick.invoice) tags.push('有发票');
  if (quick.withImage) tags.push('仅看有图');
  if (quick.highRating) tags.push('高评分');
  if (nearbyRadius.value != null) tags.push(`附近 ${nearbyRadius.value}km`);
  return tags;
});

watch(
  [keyword, selectedCategoryId, selectedPriceKey, selectedSortKey, nearbyRadius],
  () => {
    router.replace({
      name: 'CommunityMarketplaceFind',
      query: {
        keyword: keyword.value || undefined,
        category: selectedCategoryId.value !== '全部' ? selectedCategoryId.value : undefined,
        price: selectedPriceKey.value !== 'all' ? selectedPriceKey.value : undefined,
        sort: selectedSortKey.value !== 'comprehensive' ? selectedSortKey.value : undefined,
        radius: nearbyRadius.value != null ? String(nearbyRadius.value) : undefined
      }
    });
  },
  { deep: true }
);
```

- [ ] **Step 2: Replace the old page template with a result-summary header, grouped filters, and homepage-style product cards**

```vue
<div class="market-search-page">
  <dhstyle />
  <div class="market-search-shell">
    <CebianTool />

    <main class="market-search-main">
      <section class="surface-card result-hero">
        <div>
          <p class="section-kicker">二手市场</p>
          <h1>筛选你真正想看的商品</h1>
          <p class="section-desc">把分类、价格、范围和排序放在一个工作台里，减少来回跳转。</p>
        </div>
        <div class="hero-actions">
          <button type="button" class="ghost-btn" @click="$router.push({ name: 'CommunityMarketplace' })">返回首页</button>
          <button type="button" class="primary-btn" @click="marketAi.openDrawer()">打开 AI 助手</button>
        </div>
      </section>

      <section class="surface-card filter-panel">
        <div class="search-row">
          <input v-model="keyword" type="text" placeholder="搜索商品、分类或你想要的好物" @keyup.enter="doSearch" />
          <button type="button" class="primary-btn" @click="doSearch">搜索</button>
        </div>
        <div class="filter-row">
          <span class="filter-label">分类</span>
          <div class="chip-row">
            <button
              type="button"
              :class="['filter-chip', { active: selectedCategoryId === '全部' }]"
              @click="selectedCategoryId = '全部'"
            >
              全部
            </button>
            <button
              v-for="category in allCategories"
              :key="category.id"
              type="button"
              :class="['filter-chip', { active: selectedCategoryId === String(category.id) }]"
              @click="selectedCategoryId = String(category.id)"
            >
              {{ category.name }}
            </button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">价格</span>
          <div class="chip-row">
            <button type="button" :class="['filter-chip', { active: selectedPriceKey === 'all' }]" @click="selectedPriceKey = 'all'">不限</button>
            <button
              v-for="range in priceRanges"
              :key="range.label"
              type="button"
              :class="['filter-chip', { active: selectedPriceKey === range.label }]"
              @click="selectedPriceKey = range.label"
            >
              {{ range.label }}
            </button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">范围</span>
          <div class="chip-row">
            <button type="button" :class="['filter-chip', { active: nearbyRadius == null }]" @click="clearNearby">不限</button>
            <button type="button" :class="['filter-chip', { active: nearbyRadius === 1 }]" @click="setNearby(1)">1km</button>
            <button type="button" :class="['filter-chip', { active: nearbyRadius === 3 }]" @click="setNearby(3)">3km</button>
            <button type="button" :class="['filter-chip', { active: nearbyRadius === 5 }]" @click="setNearby(5)">5km</button>
            <button type="button" :class="['filter-chip', { active: nearbyRadius === 10 }]" @click="setNearby(10)">10km</button>
            <div class="inline-address">
              <input v-model="addr" type="text" placeholder="输入地址修正定位" @keyup.enter="locateByAddress" />
              <button type="button" class="ghost-btn compact" @click="locateByAddress">定位</button>
            </div>
          </div>
        </div>
        <div v-if="activeFilterTags.length" class="active-filter-row">
          <span v-for="tag in activeFilterTags" :key="tag" class="active-filter-tag">{{ tag }}</span>
          <button type="button" class="text-btn" @click="clearAllFilters">清空筛选</button>
        </div>
      </section>

      <section class="surface-card result-panel">
        <header class="result-panel-head">
          <div>
            <strong>{{ visibleProducts.length }} 件商品</strong>
            <span>{{ keyword ? `关键词：${keyword}` : '当前为综合浏览结果' }}</span>
          </div>
          <div class="sort-group">
            <button type="button" :class="['sort-btn', { active: selectedSortKey === 'comprehensive' }]" @click="selectedSortKey = 'comprehensive'">综合排序</button>
            <button type="button" :class="['sort-btn', { active: selectedSortKey === 'latest' }]" @click="selectedSortKey = 'latest'">最新发布</button>
            <button type="button" :class="['sort-btn', { active: selectedSortKey === 'price-asc' }]" @click="selectedSortKey = 'price-asc'">价格从低到高</button>
            <button type="button" :class="['sort-btn', { active: selectedSortKey === 'price-desc' }]" @click="selectedSortKey = 'price-desc'">价格从高到低</button>
          </div>
        </header>

        <div v-if="loading" class="panel-state">正在加载附近好物</div>
        <div v-else-if="errorMsg" class="panel-state error">{{ errorMsg }}</div>
        <div v-else-if="visibleProducts.length === 0" class="panel-state empty">换个关键词或放宽条件试试</div>

        <div v-else class="goods-grid">
          <router-link
            v-for="product in visibleProducts"
            :key="product.id"
            class="goods-card"
            :to="{ name: 'ProductDetail', params: { id: product.id } }"
          >
            <img :src="getFirstImage(product)" :alt="product.title" @error="(event) => ((event.target as HTMLImageElement).src = FALLBACK_ITEM)" />
            <div class="goods-card-body">
              <h3>{{ product.title }}</h3>
              <strong>¥{{ Number(product.price).toFixed(2) }}</strong>
              <span>{{ formatLocation(product.location ?? product.loaction) }}</span>
            </div>
          </router-link>
        </div>
      </section>
    </main>
  </div>
</div>
```

- [ ] **Step 3: Add compact white-card CSS and mount the shared AI drawer**

```ts
const marketAi = useCommunityMarketplaceAi(API_BASE, [
  '帮我筛出适合同城自提的高性价比商品',
  '这个搜索结果里哪些更值得优先联系',
  '帮我整理一个向卖家提问的清单'
]);
```

```vue
<CommunityMarketplaceAiDrawer
  v-model="marketAi.drawerOpen"
  kicker="AI 辅助筛选"
  title="搜索助手"
  :quick-prompts="marketAi.quickPrompts"
  :messages="marketAi.messages"
  :input-value="marketAi.input"
  :loading="marketAi.loading"
  :error="marketAi.error"
  @update:input-value="marketAi.input = $event"
  @prompt="marketAi.pushPrompt"
  @send="marketAi.sendMessage"
  @card-click="handleAiCardClick"
/>
```

```css
.market-search-main {
  width: min(1360px, calc(100vw - 88px));
  margin: 0 auto;
  padding: 86px 24px 40px;
  display: grid;
  gap: 18px;
}

.surface-card {
  border: 1px solid #e3eadf;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(73, 111, 76, 0.05);
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
```

- [ ] **Step 4: Run the build and smoke-check the search route**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully, and `CommunityMarketplaceFind.vue` no longer emits template or style errors.

Manual smoke:

```text
1. Open /community-marketplace-find.
2. Search with and without a keyword.
3. Toggle two filters and confirm the active tags update.
4. Refresh the page and confirm query-based filters persist.
5. Open the AI drawer and send one prompt.
```

- [ ] **Step 5: Commit the search-page slice**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/CommunityMarketplaceFind.vue
git commit -m "feat: redesign marketplace search page"
```

### Task 3: Rebuild the buyer transaction pages: product detail and cart

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ProductDetail.vue`
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ShopCar.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts`

- [ ] **Step 1: Rewrite the product detail template around media, decision info, seller trust, and AI assistance**

```vue
<div v-else-if="product" class="detail-shell">
  <section class="surface-card media-panel">
    <div class="main-image-wrap">
      <img :src="activeImage" :alt="product.title" @error="onImageError" class="main-image" />
    </div>
    <div class="thumb-row">
      <button
        v-for="(image, index) in galleryImages"
        :key="index"
        type="button"
        :class="['thumb-btn', { active: image === activeImage }]"
        @click="activeImage = image"
      >
        <img :src="image" :alt="`${product.title}-${index + 1}`" @error="onImageError" />
      </button>
    </div>
  </section>

  <section class="surface-card detail-panel">
    <p class="section-kicker">商品详情</p>
    <h1>{{ product.title }}</h1>
    <div class="price-row">
      <strong>¥{{ Number(product.price).toFixed(2) }}</strong>
      <span>{{ isDown ? '当前不可购买' : '支持加入购物车和立即购买' }}</span>
    </div>
    <div class="meta-grid">
      <article class="meta-card"><span>成色</span><strong>{{ product.condition || '待确认' }}</strong></article>
      <article class="meta-card"><span>库存</span><strong>{{ product.stockQuantity }}</strong></article>
      <article class="meta-card"><span>位置</span><strong>{{ product.location || '未知' }}</strong></article>
      <article class="meta-card"><span>发布时间</span><strong>{{ formatDate(product.createdAt) }}</strong></article>
    </div>
    <p class="detail-description">{{ product.description }}</p>
    <div class="purchase-row">
      <div class="quantity-box">
        <span>购买数量</span>
        <div class="qty-controls">
          <button type="button" class="qty-btn" @click="decreaseQty">-</button>
          <input v-model.number="quantity" type="number" min="1" :max="Number(product.stockQuantity) || 999" />
          <button type="button" class="qty-btn" @click="increaseQty">+</button>
        </div>
      </div>
      <div class="purchase-actions">
        <button type="button" class="ghost-btn" @click="addToCart" :disabled="isDown">加入购物车</button>
        <button type="button" class="primary-btn" @click="buyNow" :disabled="isDown">立即购买</button>
      </div>
    </div>
  </section>

  <aside class="side-stack">
    <section class="surface-card seller-panel">
      <div class="seller-panel-head">
        <img :src="cleanedAvatarUrl" alt="seller avatar" class="seller-avatar" @error="onAvatarError" />
        <div>
          <strong>{{ product.username || '卖家' }}</strong>
          <p>{{ product.phone || product.email || '可在下单后联系' }}</p>
        </div>
      </div>
      <button type="button" class="ghost-btn full-width">联系卖家</button>
    </section>
    <section class="surface-card trust-panel">
      <h3>交易提醒</h3>
      <ul class="trust-list">
        <li>优先确认真实成色与配件是否齐全</li>
        <li>同城交易建议当面验货</li>
        <li>高价商品建议保留沟通记录</li>
      </ul>
    </section>
    <section class="surface-card ai-panel">
      <button type="button" class="primary-btn full-width" @click="detailAi.openDrawer()">问 AI 助手</button>
    </section>
  </aside>
</div>
```

- [ ] **Step 2: Add product-detail AI prompts and keep the existing buy/add-to-cart methods**

```ts
import CommunityMarketplaceAiDrawer from './CommunityMarketplaceAiDrawer.vue';
import { useCommunityMarketplaceAi } from './useCommunityMarketplaceAi';

const detailAi = useCommunityMarketplaceAi(API_BASE, [
  '帮我判断这件商品值不值得入手',
  '帮我整理联系卖家的提问话术',
  '帮我总结这件商品需要重点确认的风险点'
]);

const askAboutCurrentProduct = async (prompt: string) => {
  if (!product.value) return;
  detailAi.openDrawer();
  await detailAi.pushPrompt(`${prompt}\n商品标题：${product.value.title}\n价格：${product.value.price}\n描述：${product.value.description}`);
};
```

- [ ] **Step 3: Rebuild the cart into a fixed summary layout with visible actions and no emoji UI**

```vue
<div v-else class="cart-layout">
  <section class="cart-list-panel">
    <header class="surface-card list-toolbar">
      <label class="toolbar-check">
        <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll($event)" />
        <span>全选 {{ items.length }} 件商品</span>
      </label>
      <button type="button" class="ghost-btn danger" :disabled="selectedCount === 0" @click="deleteSelected">删除选中</button>
    </header>

    <article v-for="(item, idx) in items" :key="idx" class="surface-card cart-row">
      <div class="cart-row-main">
        <label class="toolbar-check">
          <input type="checkbox" v-model="selectedFlags[idx]" />
          <span></span>
        </label>
        <div class="item-thumb"><img :src="getFirstImage(item.imageUrls)" @error="onImgError" /></div>
        <div class="item-info">
          <h3>{{ item.title }}</h3>
          <p>加入时间：{{ formatDate(item.createdAt) }}</p>
          <div class="item-meta">
            <strong>¥{{ formatPrice(item.price) }}</strong>
            <span>x{{ item.quantity }}</span>
          </div>
        </div>
      </div>
      <div class="cart-row-actions">
        <strong class="subtotal">¥{{ formatPrice(item.quantity * item.price) }}</strong>
        <div class="button-row">
          <button type="button" class="ghost-btn compact" @click="toggleDetails(idx)">详情</button>
          <button type="button" class="ghost-btn compact danger" @click="deleteItem(idx)">删除</button>
          <button type="button" class="primary-btn compact" v-if="!isDownItem(item)" @click="purchaseItem(idx)">立即购买</button>
        </div>
      </div>
    </article>
  </section>

  <aside class="surface-card cart-summary-panel">
    <div class="summary-row"><span>已选商品</span><strong>{{ selectedCount }} 件</strong></div>
    <div class="summary-row"><span>总计金额</span><strong class="summary-price">¥{{ formatPrice(selectedAmount) }}</strong></div>
    <div class="summary-note-list">
      <p>默认配送：线下协商 / 社区自提</p>
      <p>优惠信息：当前暂无可用优惠</p>
    </div>
    <button type="button" class="primary-btn full-width" :disabled="selectedCount === 0 || hasDownSelected" @click="purchaseSelected">
      结算 {{ selectedCount }} 件商品
    </button>
    <p v-if="hasDownSelected" class="summary-error">所选商品包含已下架商品，请先调整后再结算。</p>
  </aside>
</div>
```

- [ ] **Step 4: Run the build and smoke-check detail + cart behavior**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully, `ProductDetail.vue` and `ShopCar.vue` compile cleanly, and no new route or prop errors appear.

Manual smoke:

```text
1. Open one product detail page and switch gallery thumbnails.
2. Click “加入购物车” and confirm the action still fires.
3. Open the detail AI drawer and send one prompt.
4. Open /shop-car and confirm the summary card stays visible.
5. Confirm every cart row shows its action buttons without horizontal scrolling.
```

- [ ] **Step 5: Commit the buyer-transaction slice**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/ProductDetail.vue D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/ShopCar.vue
git commit -m "feat: redesign marketplace detail and cart pages"
```

### Task 4: Rebuild the seller management pages into a light workspace

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyProducts.vue`
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyOrder.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceAiDrawer.vue`
- Use: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\useCommunityMarketplaceAi.ts`

- [ ] **Step 1: Add seller summary cards and restructure MyProducts into state-first cards**

```ts
const inventorySummary = computed(() => {
  const total = items.value.length;
  const active = items.value.filter((item) => String(item.status ?? '').trim() !== '下架').length;
  const lowStock = items.value.filter((item) => Number(item.stockQuantity) > 0 && Number(item.stockQuantity) <= 3).length;
  return [
    { label: '商品总数', value: String(total), helper: '当前货架中的商品数量' },
    { label: '在售商品', value: String(active), helper: '仍可继续出售的商品' },
    { label: '库存紧张', value: String(lowStock), helper: '建议优先补货或下架' }
  ];
});
```

```vue
<section class="summary-grid">
  <article v-for="card in inventorySummary" :key="card.label" class="surface-card summary-card">
    <span>{{ card.label }}</span>
    <strong>{{ card.value }}</strong>
    <small>{{ card.helper }}</small>
  </article>
</section>

<section class="product-stack">
  <article v-for="(p, idx) in items" :key="p.id" class="surface-card seller-product-card">
    <div class="seller-product-main">
      <img :src="getFirstImage(p.imageUrls)" class="seller-thumb" @error="onImgError" />
      <div class="seller-copy">
        <div class="seller-copy-head">
          <h3>{{ p.title }}</h3>
          <strong>¥{{ formatPrice(p.price) }}</strong>
        </div>
        <div class="seller-meta-grid">
          <span>库存：{{ p.stockQuantity }}</span>
          <span>成色：{{ p.condition }}</span>
          <span>地址：{{ p.location }}</span>
          <span>发布时间：{{ formatDate(p.createdAt) }}</span>
        </div>
      </div>
    </div>
    <div class="seller-product-actions">
      <div class="inline-form-group">
        <input class="nd-input small" type="number" v-model.number="edit[idx].delta" min="1" placeholder="补货数量" />
        <button type="button" class="ghost-btn compact" @click="increaseStock(p.id, edit[idx].delta)">补货</button>
      </div>
      <div class="inline-form-group">
        <input class="nd-input small" type="number" step="0.01" v-model.number="edit[idx].price" placeholder="新价格" />
        <button type="button" class="ghost-btn compact" @click="updatePrice(p.id, edit[idx].price)">调价</button>
      </div>
      <div class="inline-form-group grow">
        <input class="nd-input" type="text" v-model="edit[idx].location" placeholder="新地址" />
        <button type="button" class="ghost-btn compact" @click="updateLocation(p.id, edit[idx].location)">迁址</button>
      </div>
      <button type="button" class="primary-btn compact danger" @click="takeDown(p.id)">下架</button>
    </div>
  </article>
</section>
```

- [ ] **Step 2: Add seller AI prompts to MyProducts and keep the existing action methods untouched**

```ts
const sellerAi = useCommunityMarketplaceAi(API_BASE, [
  '帮我优化当前商品的标题',
  '帮我整理一个更可信的转让描述',
  '帮我写一段回复买家议价的文案'
]);

const askSellerAiAboutProduct = async (product: ProductDTO, prompt: string) => {
  sellerAi.openDrawer();
  await sellerAi.pushPrompt(`${prompt}\n标题：${product.title}\n价格：${product.price}\n成色：${product.condition}\n地点：${product.location}`);
};
```

- [ ] **Step 3: Rebuild MyOrder into a status-led ledger with grouped information**

```ts
const activeStatus = ref<'all' | '1' | '2' | '3' | '4' | '5'>('all');

const filteredOrders = computed(() =>
  activeStatus.value === 'all'
    ? orders.value
    : orders.value.filter((order) => String(order.status) === activeStatus.value)
);

const orderStats = computed(() => [
  { key: 'all', label: '全部', value: orders.value.length },
  { key: '1', label: '已创建', value: orders.value.filter((order) => order.status === 1).length },
  { key: '2', label: '已支付', value: orders.value.filter((order) => order.status === 2).length },
  { key: '3', label: '已发货', value: orders.value.filter((order) => order.status === 3).length },
  { key: '4', label: '已完成', value: orders.value.filter((order) => order.status === 4).length },
  { key: '5', label: '已取消', value: orders.value.filter((order) => order.status === 5).length }
]);
```

```vue
<section class="status-chip-row">
  <button
    v-for="stat in orderStats"
    :key="stat.key"
    type="button"
    :class="['status-chip', { active: activeStatus === stat.key }]"
    @click="activeStatus = stat.key as any"
  >
    {{ stat.label }} {{ stat.value }}
  </button>
</section>

<section class="order-list">
  <article v-for="order in filteredOrders" :key="order.orderId" class="surface-card order-card">
    <div class="order-card-head">
      <div>
        <strong>订单号：{{ order.orderId }}</strong>
        <span>下单时间：{{ formatDate(order.createdAt) }}</span>
      </div>
      <span class="order-status-tag">{{ mapStatus(order.status) }}</span>
    </div>
    <div class="order-card-grid">
      <section class="order-block">
        <h4>订单信息</h4>
        <p>商品 ID：{{ order.productId }}</p>
        <p>订单金额：¥{{ formatPrice(order.totalAmount) }}</p>
      </section>
      <section class="order-block">
        <h4>收货信息</h4>
        <p>收件人：{{ order.receiverName }}</p>
        <p>联系电话：{{ order.receiverPhone }}</p>
      </section>
      <section class="order-block">
        <h4>履约信息</h4>
        <p>收货地址：{{ order.receiverAddress }}</p>
        <p>支付时间：{{ order.paymentTime || '未支付' }}</p>
      </section>
    </div>
  </article>
</section>
```

- [ ] **Step 4: Run the build and smoke-check MyProducts + MyOrder**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully, seller page SFCs compile, and no new runtime-only helper is missing at compile time.

Manual smoke:

```text
1. Open /my-products and confirm summary cards render above the list.
2. Use “补货 / 调价 / 迁址 / 下架” once each and confirm the original handlers still run.
3. Open the seller AI drawer on one product and send one prompt.
4. Open /my-orders and switch across all status chips.
5. Confirm one order card clearly separates order info, receiver info, and fulfillment info.
```

- [ ] **Step 5: Commit the seller-workspace slice**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/MyProducts.vue D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/MyOrder.vue
git commit -m "feat: redesign marketplace seller workspace pages"
```

### Task 5: Rebuild the publish flow pages and fix validation copy

**Files:**
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addProduct.vue`
- Modify: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addNewProduct.vue`
- Reference: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`

- [ ] **Step 1: Convert addProduct.vue into section cards while keeping the current form model and submit code**

```vue
<div class="publish-page">
  <dhstyle />
  <div class="publish-shell">
    <CebianTool />
    <main class="publish-main">
      <section class="surface-card publish-hero">
        <div>
          <p class="section-kicker">发布商品</p>
          <h1>把商品信息整理清楚，再发布出去</h1>
          <p class="section-desc">按基础信息、交易信息、位置分类和图片四个区块依次填写。</p>
        </div>
        <button type="button" class="ghost-btn" @click="router.push({ name: 'CommunityMarketplace' })">返回市场</button>
      </section>

      <form class="publish-form" @submit.prevent="onSubmit">
        <section class="surface-card form-card">
          <h2>基础信息</h2>
          <div class="field-grid">
            <label class="field-block">
              <span>商品标题</span>
              <input class="input" type="text" v-model.trim="form.title" placeholder="请输入标题" />
            </label>
            <label class="field-block full-row">
              <span>商品描述</span>
              <textarea class="textarea" v-model.trim="form.description" placeholder="请输入商品描述"></textarea>
            </label>
          </div>
        </section>
        <section class="surface-card form-card">
          <h2>交易信息</h2>
          <div class="field-grid two-column">
            <label class="field-block">
              <span>价格（¥）</span>
              <input class="input" type="number" step="0.01" v-model.number="form.price" placeholder="0.00" />
            </label>
            <label class="field-block">
              <span>库存数量</span>
              <input class="input" type="number" min="0" v-model.number="form.stockQuantity" placeholder="0" />
            </label>
            <label class="field-block">
              <span>成色</span>
              <select class="input" v-model="form.condition">
                <option value="">请选择</option>
                <option value="全新">全新</option>
                <option value="九成新">九成新</option>
                <option value="七成新">七成新</option>
                <option value="二手">二手</option>
              </select>
            </label>
            <label class="field-block">
              <span>所属分类</span>
              <select class="input" v-model="form.categoryId">
                <option value="">请选择分类</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </label>
          </div>
        </section>
        <section class="surface-card form-card">
          <h2>位置信息</h2>
          <div class="address-input-row">
            <input class="input" type="text" v-model.trim="form.location" placeholder="城市 / 区域 / 街道" />
            <button class="ghost-btn compact" type="button" @click="locateAndFillAddress" :disabled="locating">
              {{ locating ? '定位中' : '定位填充' }}
            </button>
          </div>
        </section>
        <section class="surface-card form-card">
          <h2>商品图片</h2>
          <div class="upload-list">
            <div class="upload-item" v-for="(img, idx) in uploads" :key="img.key || idx">
              <img :src="img.url" alt="预览图" @error="onUploadImgError(idx)" />
              <button type="button" class="ghost-btn compact danger" @click="removeUpload(idx)">删除</button>
            </div>
            <button type="button" class="ghost-btn" @click="triggerUpload" :disabled="uploading">
              {{ uploading ? '上传中' : '上传图片' }}
            </button>
          </div>
        </section>
        <section class="surface-card submit-card">
          <button type="submit" class="primary-btn" :disabled="submitting">{{ submitting ? '提交中' : '发布商品' }}</button>
        </section>
      </form>
    </main>
  </div>
</div>
```

- [ ] **Step 2: Fix the garbled validation text and keep every existing validation rule**

```ts
function validate() {
  if (!form.value.title) { ElMessage.error('请输入商品标题'); return false; }
  if (!form.value.description) { ElMessage.error('请输入商品描述'); return false; }
  if (!form.value.price || form.value.price <= 0) { ElMessage.error('请输入有效价格'); return false; }
  if (form.value.stockQuantity < 0) { ElMessage.error('库存数量不能为负'); return false; }
  if (!form.value.condition) { ElMessage.error('请选择成色'); return false; }
  if (!form.value.location) { ElMessage.error('请输入地点'); return false; }
  if (!form.value.categoryId) { ElMessage.error('请选择分类'); return false; }
  if (uploads.value.length === 0) { ElMessage.error('请至少上传一张商品图片'); return false; }
  return true;
}
```

- [ ] **Step 3: Turn addNewProduct.vue into a real publish entry shell instead of a blank file**

```vue
<template>
  <div class="publish-entry-page">
    <dhstyle />
    <div class="publish-entry-shell">
      <section class="surface-card entry-card">
        <p class="section-kicker">新增商品</p>
        <h1>进入发布流程</h1>
        <p>这个入口统一跳转到正式的发布商品页面，避免维护两套发布界面。</p>
        <div class="entry-actions">
          <button type="button" class="primary-btn" @click="router.replace({ name: 'AddProduct' })">去发布商品</button>
          <button type="button" class="ghost-btn" @click="router.replace({ name: 'CommunityMarketplace' })">返回市场</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';

const router = useRouter();
</script>
```

- [ ] **Step 4: Run the build and smoke-check the publish flow**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully, `addProduct.vue` and `addNewProduct.vue` compile, and the validation copy is readable Chinese.

Manual smoke:

```text
1. Open /addProduct and confirm the page is broken into clear sections.
2. Try to submit without images and confirm the message reads “请至少上传一张商品图片”.
3. Click the location-fill button and confirm the old locate handler still runs.
4. Open /add-new-product and confirm it clearly routes to /addProduct.
```

- [ ] **Step 5: Commit the publish-flow slice**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/addProduct.vue D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace/addNewProduct.vue
git commit -m "feat: redesign marketplace publish flow"
```

### Task 6: Final regression pass across the marketplace route set

**Files:**
- Validate: `D:\code\aaaaljt\front\aaljt\src\router\CommunityMarketplace\index.ts`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplace.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\CommunityMarketplaceFind.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ProductDetail.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\ShopCar.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyProducts.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\MyOrder.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addProduct.vue`
- Validate: `D:\code\aaaaljt\front\aaljt\src\components\Home\CommunityMarketplace\addNewProduct.vue`

- [ ] **Step 1: Run the final production build**

Run: `Set-Location D:\code\aaaaljt\front\aaljt; $env:PATH='C:\nvm4w\nodejs;'+$env:PATH; npm run build`

Expected: Build exits successfully with no new compile errors.

- [ ] **Step 2: Smoke-check the desktop route flow**

Run:

```text
Open these routes manually in the browser:
- /community-marketplace
- /community-marketplace-find
- /product/<existing-id>
- /shop-car
- /my-products
- /my-orders
- /addProduct
- /add-new-product
```

Expected:

```text
1. All pages share the same white-surface / green-accent / restrained-radius language.
2. The left navigation remains fixed and visually consistent.
3. AI appears as a drawer entry, not a large permanent panel.
4. No page shows emoji as UI icons.
5. No page depends on browser horizontal scrolling to expose its main actions.
```

- [ ] **Step 3: Check the preserved business paths**

Run:

```text
1. Search products from the list page.
2. Open one product detail page.
3. Add one item to cart.
4. Trigger one seller action in MyProducts.
5. Submit the add-product form with valid data.
6. Send one AI message from a buyer page and one from a seller page.
```

Expected:

```text
Each action still reaches its original fetch / submit / route logic.
```

- [ ] **Step 4: Inspect the final diff for scope control**

Run: `git -C D:\code\aaaaljt diff --stat`

Expected: The diff is limited to the planned marketplace files plus the shared AI drawer/composable.

- [ ] **Step 5: Commit the final integration checkpoint**

```bash
git add D:/code/aaaaljt/front/aaljt/src/components/Home/CommunityMarketplace
git commit -m "feat: unify community marketplace secondary pages"
```
