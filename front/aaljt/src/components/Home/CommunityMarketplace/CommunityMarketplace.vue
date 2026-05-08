<template>
  <div class="market-page">
    <dhstyle />

    <div class="market-shell">
      <CebianTool />

      <main class="market-main">
        <section class="hero-banner">
          <div class="hero-copy">
            <span class="hero-chip">让资源循环 让生活更美好</span>
            <h1>让闲置流转起来</h1>

            <div class="hero-search">
              <input
                v-model="marketKeyword"
                type="text"
                placeholder="搜索你想要的宝贝"
                @keyup.enter="goToMarketSearch"
              />
              <button class="hero-search-btn" type="button" @click="goToMarketSearch">搜索好物</button>
            </div>

            <button class="primary-btn hero-btn" type="button" @click="goToPublish">立即发布</button>
          </div>
        </section>

        <section class="surface-card category-rail">
          <button
            v-for="category in marketCategories"
            :key="category.id"
            class="category-pill"
            type="button"
            @click="handleCategoryClick(category)"
          >
            <span class="category-icon" :style="{ '--category-accent': category.color }">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path v-for="path in category.iconPaths" :key="path" :d="path" />
              </svg>
            </span>
            <span class="category-text">{{ category.label }}</span>
          </button>
        </section>

        <section class="content-grid">
          <section class="surface-card goods-panel">
            <header class="section-head">
              <div>
                <p class="section-kicker">推荐好物</p>
                <h2>猜你在找这些</h2>
              </div>
              <div class="section-actions">
                <button class="ghost-btn compact accent" type="button" @click="openWorkbenchDrawer">AI 工作台</button>
                <button class="ghost-btn compact" type="button" @click="goToMarketSearch">查看更多</button>
              </div>
            </header>

            <div class="goods-overview">
              <div class="goods-result-summary">
                <strong>{{ resultSummary }}</strong>
                <span>{{ resultSummaryHint }}</span>
              </div>

              <div class="sort-group">
                <button
                  v-for="option in sortOptions"
                  :key="option.key"
                  type="button"
                  :class="['sort-btn', { active: isSortActive(option.sort, option.order) }]"
                  @click="applySort(option.sort, option.order)"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>

            <div class="goods-toolbar">
              <div class="radius-group">
                <button
                  v-for="option in nearbyOptions"
                  :key="option"
                  type="button"
                  :class="['radius-btn', { active: nearbyRadius === option }]"
                  @click="toggleNearby(option)"
                >
                  {{ option }}km
                </button>
                <button
                  type="button"
                  :class="['radius-btn', { active: nearbyRadius === null }]"
                  @click="clearNearby"
                >
                  全城
                </button>
              </div>

              <div class="location-input">
                <input
                  v-model="addr"
                  type="text"
                  placeholder="定位不准？输入地址"
                  @keyup.enter="locateByAddress"
                />
                <button class="ghost-btn compact" type="button" @click="locateByAddress">定位</button>
              </div>
            </div>

            <div class="filter-strip">
              <div class="filter-group">
                <span class="filter-label">价格区间</span>
                <button
                  v-for="option in priceRangeOptions"
                  :key="option.key"
                  type="button"
                  :class="['filter-chip', { active: isPriceRangeActive(option.min, option.max) }]"
                  @click="applyPriceRange(option.min, option.max)"
                >
                  {{ option.label }}
                </button>
              </div>

              <button v-if="hasActiveFilters" class="filter-clear-btn" type="button" @click="clearAllFilters">
                清空筛选
              </button>
            </div>

            <div v-if="activeFilterTags.length" class="active-filter-bar">
              <span class="filter-label">当前筛选</span>
              <div class="active-filter-tags">
                <span v-for="tag in activeFilterTags" :key="tag" class="active-filter-tag">{{ tag }}</span>
              </div>
            </div>

            <div v-if="loading" class="panel-state">正在加载附近好物...</div>
            <div v-else-if="errorMsg" class="panel-state error">{{ errorMsg }}</div>
            <div v-else-if="featuredProducts.length === 0" class="panel-state empty panel-state-stack">
              <strong>暂时没有匹配到合适商品</strong>
              <p>可以换个关键词，或者试试这些更常见的搜索入口。</p>
              <div class="empty-suggest-list">
                <button
                  v-for="keyword in popularKeywords"
                  :key="`empty-${keyword}`"
                  class="empty-suggest-btn"
                  type="button"
                  @click="applyKeyword(keyword)"
                >
                  {{ keyword }}
                </button>
              </div>
            </div>

            <div v-else class="goods-grid">
              <article
                v-for="(product, index) in featuredProducts"
                :key="product.id"
                class="goods-card"
                @click="navigateToDetail(product)"
              >
                <div class="goods-image-wrap">
                  <img
                    :src="getFirstImage(product)"
                    :alt="product.title"
                    @error="handleImageError($event, FALLBACK_ITEM)"
                  />
                  <span class="goods-condition">{{ getConditionBadge(product, index) }}</span>
                  <span class="goods-deliver">{{ getDeliveryBadge(product) }}</span>
                  <span class="goods-price-chip">¥{{ formatPrice(product.price) }}</span>
                </div>

                <div class="goods-body">
                  <div class="goods-head-row">
                    <h3 :title="product.title">{{ product.title }}</h3>
                    <button class="favorite-btn" type="button" @click.stop="toggleFavorite(product.id)">
                      {{ isFavorite(product.id) ? '♥' : '♡' }}
                    </button>
                  </div>

                  <div class="goods-tags-row">
                    <span class="goods-info-chip location">{{ formatLocation(product.location ?? product.loaction) }}</span>
                    <span v-if="product.distanceKm != null" class="goods-info-chip distance">{{ formatDistance(product.distanceKm) }}</span>
                  </div>

                  <div class="goods-price-row">
                    <strong>¥{{ formatPrice(product.price) }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <aside class="side-panel">
            <section class="surface-card trust-panel">
              <header class="side-head">
                <p class="section-kicker">平台保障</p>
                <h3>交易更放心</h3>
              </header>

              <div class="trust-overview">
                <div class="trust-score">
                  <strong>98%</strong>
                  <span>近 30 天交易满意度</span>
                </div>
                <div class="trust-summary">
                  <span>同城优先匹配</span>
                  <span>平台验真提醒</span>
                  <span>沟通记录留痕</span>
                </div>
              </div>

              <div class="trust-list">
                <article v-for="item in trustItems" :key="item.title" class="trust-item">
                  <span class="trust-icon" :style="{ '--trust-color': item.color }">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path v-for="path in item.iconPaths" :key="path" :d="path" />
                    </svg>
                  </span>
                  <div>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.desc }}</p>
                  </div>
                </article>
              </div>
            </section>

            <section class="surface-card ai-entry-card">
              <header class="side-head">
                <p class="section-kicker">AI 助手</p>
                <h3>边逛边问</h3>
              </header>
              <p class="ai-entry-text">AI 保持在辅助位，用来帮你筛商品、判断价格和整理转让话术。</p>

              <div class="ai-quick-list">
                <button
                  v-for="prompt in agentQuickPrompts"
                  :key="prompt"
                  class="ai-quick-btn"
                  type="button"
                  @click="startAgentPrompt(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>

              <div class="ai-entry-actions">
                <button class="primary-btn full-width" type="button" @click="openWorkbenchDrawer">打开 AI 对话框</button>
                <button class="ghost-btn full-width" type="button" @click="continueLatestTask">继续上次提问</button>
              </div>

              <div class="ai-preview-box">
                <strong>最近会话</strong>
                <p>{{ latestUserPreview || '还没有发起过询问，可以先试试“帮我找一台性价比高的二手相机”。' }}</p>
                <small>{{ latestAgentReplyPreview || 'AI 回复会显示在这里，方便你快速回到上下文。' }}</small>
              </div>
            </section>
          </aside>
        </section>

        <section class="service-strip">
          <article v-for="item in servicePromises" :key="item.title" class="service-pill">
            <span class="service-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path v-for="path in item.iconPaths" :key="path" :d="path" />
              </svg>
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </article>
        </section>

        <footer class="surface-card market-footer">
          <div class="footer-brand">
            <div>
              <p class="section-kicker">二手交易市场</p>
              <h3>让好物继续发光</h3>
            </div>
            <p>
              连接社区闲置、同城好物与可信交易，帮助每一件物品找到下一位珍惜它的人。
            </p>
          </div>

          <div class="footer-columns">
            <div v-for="group in footerGroups" :key="group.title" class="footer-column">
              <strong>{{ group.title }}</strong>
              <span v-for="item in group.items" :key="item">{{ item }}</span>
            </div>
          </div>

          <div class="footer-bottom">
            <span>© 2026 二手交易市场 · All Rights Reserved.</span>
            <div class="footer-socials">
              <span>微信</span>
              <span>微博</span>
              <span>客服</span>
            </div>
          </div>
        </footer>
      </main>

      <button class="workbench-fab" type="button" @click="toggleWorkbenchDrawer">
        {{ isWorkbenchDrawerOpen ? '收起 AI 助手' : '打开 AI 助手' }}
      </button>

      <CommunityMarketplaceAiDrawer
        v-model="isWorkbenchDrawerOpen"
        kicker="AI 对话框"
        title="市场助手"
        headline="随时提问"
        helper-text="支持继续追问，消息会发送到后端接口，并保留当前会话上下文。"
        empty-text="你好，我是你的社区二手市场助手。你可以直接问我找商品、优化商品标题、整理卖点，或者帮我判断是否值得入手。"
        placeholder="例如：帮我找一台 3000 元以内、适合同城自提的笔记本"
        :quick-prompts="agentQuickPrompts"
        :messages="agentMessages"
        :input-value="agentInput"
        :loading="agentLoading"
        :error="agentError"
        @update:input-value="agentInput = $event"
        @prompt="startAgentPrompt"
        @send="submitDialogMessage"
        @card-click="handleAgentCardClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';
import CebianTool from './cebianTool.vue';
import CommunityMarketplaceAiDrawer from './CommunityMarketplaceAiDrawer.vue';
import { useCommunityMarketplaceAi, type MarketplaceAiCard } from './useCommunityMarketplaceAi';
interface Product {
  id: number | string;
  title: string;
  price: number | string;
  location?: string;
  loaction?: string;
  image_urls?: string | string[];
  imageUrls?: string | string[];
  seller_id?: number | string;
  sellerId?: number | string;
  seller_avatar?: string;
  sellerAvatar?: string;
  status?: string | number;
  stock_quantity?: number | string;
  stockQuantity?: number | string;
  distanceKm?: number | string | null;
}

interface MarketCategory {
  id: string;
  label: string;
  iconPaths: string[];
  color: string;
  keyword?: string;
}

interface TrustItem {
  title: string;
  desc: string;
  iconPaths: string[];
  color: string;
}

interface ServicePromise {
  title: string;
  desc: string;
  iconPaths: string[];
}

interface FooterGroup {
  title: string;
  items: string[];
}

interface SortOption {
  key: string;
  label: string;
  sort?: string;
  order?: string;
}

interface PriceRangeOption {
  key: string;
  label: string;
  min?: string;
  max?: string;
}

const router = useRouter();
const route = useRoute();

const API_BASE = ((import.meta as any)?.env?.VITE_API_BASE ?? (window as any)?.VITE_API_BASE ?? 'http://localhost:8080') as string;
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '') as string;

const popularKeywords =['同城自提', '学生转卖', '95新数码', '低价家具', '搬家急出'];
const agentQuickPrompts = ['帮我筛 3 件适合同城自提的好物', '这个价格值不值', '帮我写一段转让文案'];
const sortOptions: SortOption[] = [
  { key: 'default', label: '推荐' },
  { key: 'latest', label: '最新发布', sort: 'createdAt', order: 'desc' },
  { key: 'price-low', label: '低价优先', sort: 'price', order: 'asc' },
  { key: 'price-high', label: '高价优先', sort: 'price', order: 'desc' }
];
const priceRangeOptions: PriceRangeOption[] = [
  { key: 'all', label: '不限' },
  { key: '0-100', label: '100 内', max: '100' },
  { key: '100-500', label: '100-500', min: '100', max: '500' },
  { key: '500-1000', label: '500-1000', min: '500', max: '1000' },
  { key: '1000+', label: '1000+', min: '1000' }
];

const marketCategories: MarketCategory[] = [
  {
    id: 'phone',
    label: '手机数码',
    iconPaths: ['M7 3.5h10A1.5 1.5 0 0 1 18.5 5v14A1.5 1.5 0 0 1 17 20.5H7A1.5 1.5 0 0 1 5.5 19V5A1.5 1.5 0 0 1 7 3.5Z', 'M11 17.5h2'],
    color: '#e5f7ea',
    keyword: '手机 数码'
  },
  {
    id: 'computer',
    label: '电脑办公',
    iconPaths: ['M4 5.5h16v10H4z', 'M8.5 18.5h7', 'M10.5 15.5v3', 'M13.5 15.5v3'],
    color: '#edf3ff',
    keyword: '电脑 办公'
  },
  {
    id: 'furniture',
    label: '家电家具',
    iconPaths: ['M5 11.5V10a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v1.5', 'M4.5 11.5h15v4h-15z', 'M7 15.5v2', 'M17 15.5v2'],
    color: '#fff3df',
    keyword: '家具 家电'
  },
  {
    id: 'books',
    label: '图书文创',
    iconPaths: ['M6 5.5h9.5a2 2 0 0 1 2 2v11H8a2 2 0 0 0-2 2z', 'M6 5.5v13', 'M8 19.5h10'],
    color: '#edf8e6',
    keyword: '图书 文创'
  },
  {
    id: 'fashion',
    label: '服饰鞋包',
    iconPaths: ['M8 9.5V8a4 4 0 0 1 8 0v1.5', 'M6.5 9.5h11a1 1 0 0 1 1 1l-.7 7a2 2 0 0 1-2 1.8H8.2a2 2 0 0 1-2-1.8l-.7-7a1 1 0 0 1 1-1Z'],
    color: '#f4ecff',
    keyword: '服饰 鞋包'
  },
  {
    id: 'sports',
    label: '运动户外',
    iconPaths: ['M4.5 18.5 12 5.5l7.5 13', 'M8.5 18.5 12 12.5l3.5 6', 'M12 5.5v13'],
    color: '#e7faf8',
    keyword: '运动 户外'
  },
  {
    id: 'more',
    label: '更多分类',
    iconPaths: ['M5.5 5.5h5v5h-5z', 'M13.5 5.5h5v5h-5z', 'M5.5 13.5h5v5h-5z', 'M13.5 13.5h5v5h-5z'],
    color: '#f5f5f5'
  }
];

const trustItems: TrustItem[] = [
  {
    title: '同城优先',
    desc: '优先展示同城好物，自提更方便。',
    iconPaths: ['M12 20s-6-4.4-6-10a6 6 0 1 1 12 0c0 5.6-6 10-6 10Z', 'M12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z'],
    color: '#e6f7eb'
  },
  {
    title: '平台验真',
    desc: '专业队列验货，保障商品信息真实。',
    iconPaths: ['M12 3.5 18.5 6v5.5c0 4.1-2.6 6.9-6.5 9-3.9-2.1-6.5-4.9-6.5-9V6L12 3.5Z', 'M9.5 12.5 11 14l4-4'],
    color: '#ebf5ff'
  },
  {
    title: '极速沟通',
    desc: '在线沟通即时回复，交易更高效。',
    iconPaths: ['M5 6.5h14v9H9l-4 3v-3H5z'],
    color: '#fff4e5'
  },
  {
    title: '低价好物',
    desc: '超值低价好物，省钱也更环保。',
    iconPaths: ['M4.5 10.5 10.5 4.5h6l3 3v6l-6 6-9-9Z', 'M14 8.5h.01'],
    color: '#f4ecff'
  }
];

const servicePromises: ServicePromise[] = [
  {
    title: '交易安全保障',
    desc: '平台担保交易',
    iconPaths: ['M12 3.5 18.5 6v5.5c0 4.1-2.6 6.9-6.5 9-3.9-2.1-6.5-4.9-6.5-9V6L12 3.5Z', 'M9.5 12.5 11 14l4-4']
  },
  {
    title: '7 × 24 小时客服',
    desc: '在线为你服务',
    iconPaths: ['M4.5 12a7.5 7.5 0 1 1 15 0', 'M6 13.5v3a1 1 0 0 0 1 1h1.5v-5H7a1 1 0 0 0-1 1Z', 'M18 13.5v3a1 1 0 0 1-1 1h-1.5v-5H17a1 1 0 0 1 1 1Z']
  },
  {
    title: '绿色环保理念',
    desc: '让闲置创造新价值',
    iconPaths: ['M18 5.5c-5 .2-9.7 2.3-11.5 8 3.1.4 5.9-.1 8.2-1.8 2.8-2 4.3-5.3 3.3-10.2Z', 'M8 14c2 .1 4.2-.8 6.5-2.8']
  },
  {
    title: '邻里互助交易',
    desc: '让同城流转更轻松',
    iconPaths: ['M8.5 11a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z', 'M15.5 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z', 'M4.5 18a4 4 0 0 1 8 0', 'M13 18a3 3 0 0 1 6 0']
  }
];

const footerGroups: FooterGroup[] = [
  { title: '帮助中心', items: ['新手指南', '常见问题', '联系客服'] },
  { title: '交易安全', items: ['安全提示', '举报中心', '验货说明'] },
  { title: '关于我们', items: ['平台介绍', '加入我们', '社区合作'] },
  { title: '合作伙伴', items: ['商户合作', '校园合作', '公益联动'] }
];

const nearbyOptions = [1, 3, 5] as const;
const conditionBadges = ['95新', '9成新', '品质优选', '低价转让'];
const FALLBACK_ITEM = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="220"><rect width="100%" height="100%" fill="%23eef2ec"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23839280" font-size="16">暂无图片</text></svg>';

const products = ref<Product[]>([]);
const loading = ref(false);
const errorMsg = ref('');
const marketKeyword = ref(String(route.query.keyword ?? ''));
const userLat = ref<number | null>(null);
const userLng = ref<number | null>(null);
const nearbyRadius = ref<number | null>(3);
const addr = ref('');
const favoriteIds = ref<Array<string | number>>([]);

const {
  drawerOpen: isWorkbenchDrawerOpen,
  input: agentInput,
  loading: agentLoading,
  error: agentError,
  messages: agentMessages,
  latestUserMessage,
  latestUserPreview,
  latestAgentReplyPreview,
  openDrawer: openWorkbenchDrawer,
  closeDrawer: closeWorkbenchDrawer,
  toggleDrawer: toggleWorkbenchDrawer,
  prefillInput: prefillAgentInput,
  submitMessage: submitDialogMessage
} = useCommunityMarketplaceAi({
  apiBase: API_BASE,
  quickPrompts: agentQuickPrompts,
  sessionStorageKey: 'communityMarketplaceAgentSessionId',
  initialAgentMessage: '你好，我是你的社区二手市场助手。你可以直接问我找商品、写文案、判断性价比或整理卖点。',
  buildUserProfile: () => ({
    latitude: userLat.value ?? undefined,
    longitude: userLng.value ?? undefined,
    nearbyRadiusKm: nearbyRadius.value ?? undefined,
    address: addr.value?.trim() || undefined
  })
});

const featuredProducts = computed(() => products.value.slice(0, 6));

const resultSummary = computed(() => {
  const count = featuredProducts.value.length;
  if (loading.value) {
    return '正在整理商品列表';
  }
  return nearbyRadius.value ? `已为你筛出 ${count} 件附近好物` : `当前展示 ${count} 件精选商品`;
});

const resultSummaryHint = computed(() => {
  const keyword = marketKeyword.value.trim();
  if (keyword) {
    return `当前关键词：${keyword}`;
  }
  return nearbyRadius.value ? `附近 ${nearbyRadius.value}km 内优先展示` : '可切换排序或修改关键词继续筛选';
});

const activeFilterTags = computed(() => {
  const tags: string[] = [];
  const keyword = String(route.query.keyword ?? '').trim();
  const priceMin = String(route.query.priceMin ?? '').trim();
  const priceMax = String(route.query.priceMax ?? '').trim();
  const sort = String(route.query.sort ?? '').trim();
  const order = String(route.query.order ?? '').trim();

  if (keyword) {
    tags.push(`关键词：${keyword}`);
  }

  if (priceMin || priceMax) {
    if (priceMin && priceMax) {
      tags.push(`价格：${priceMin}-${priceMax}`);
    } else if (priceMin) {
      tags.push(`价格：${priceMin}+`);
    } else if (priceMax) {
      tags.push(`价格：${priceMax} 内`);
    }
  }

  const sortLabel = sortOptions.find((item) => (item.sort ?? '') === sort && (item.order ?? '') === order)?.label;
  if (sortLabel && sortLabel !== '推荐') {
    tags.push(`排序：${sortLabel}`);
  }

  if (nearbyRadius.value != null) {
    tags.push(`范围：${nearbyRadius.value}km`);
  }

  return tags;
});

const hasActiveFilters = computed(() => {
  return activeFilterTags.value.length > 0 || !!marketKeyword.value.trim();
});

watch(
  () => route.query.keyword,
  (keyword) => {
    marketKeyword.value = String(keyword ?? '');
  }
);

watch(() => route.query, fetchProducts, { deep: true });

function toggleFavorite(productId: string | number) {
  if (favoriteIds.value.includes(productId)) {
    favoriteIds.value = favoriteIds.value.filter((id) => id !== productId);
    return;
  }
  favoriteIds.value = [...favoriteIds.value, productId];
}

function isFavorite(productId: string | number) {
  return favoriteIds.value.includes(productId);
}

function formatPrice(price: number | string) {
  const amount = Number(price);
  return Number.isFinite(amount) ? amount.toFixed(0) : '--';
}

function handleImageError(event: Event, fallback: string) {
  const target = event.target as HTMLImageElement | null;
  if (target) {
    target.src = fallback;
  }
}

function unwrapList(data: unknown): Product[] {
  if (Array.isArray(data)) {
    return data as Product[];
  }
  if (data && typeof data === 'object') {
    const record = data as Record<string, unknown>;
    for (const key of ['items', 'list', 'data', 'records', 'rows']) {
      if (Array.isArray(record[key])) {
        return record[key] as Product[];
      }
    }
  }
  return [];
}

function getFirstImage(product: Product) {
  const imgs = product.image_urls ?? product.imageUrls;
  if (!imgs) {
    return FALLBACK_ITEM;
  }

  let path = '';
  if (Array.isArray(imgs)) {
    path = imgs[0] || '';
  } else if (typeof imgs === 'string') {
    const sanitized = imgs.trim().replace(/`/g, '');
    if (sanitized.startsWith('[')) {
      try {
        const parsed = JSON.parse(sanitized);
        if (Array.isArray(parsed) && parsed.length > 0) {
          path = parsed[0];
        }
      } catch {
        const match = sanitized.match(/(https?:\/\/[^"'\]\s]+|\/[\w\-/.]+)/);
        if (match) {
          path = match[1];
        }
      }
    } else {
      path = sanitized;
    }
  }

  if (!path) {
    return FALLBACK_ITEM;
  }
  return path.startsWith('/') ? `${API_BASE}${path}` : path;
}

function formatSellerId(id?: number | string) {
  if (id == null) {
    return '社区用户';
  }
  const text = String(id);
  return text.length > 8 ? `${text.slice(0, 8)}...` : text;
}

function formatLocation(location?: string) {
  return location || '同城优先';
}

function formatDistance(distance: number | string) {
  const value = Number(distance);
  if (!Number.isFinite(value)) {
    return '';
  }
  return value < 1 ? `${Math.round(value * 1000)}m` : `${value.toFixed(1)}km`;
}

function isDown(product: Product) {
  const rawStatus = product.status;
  const stock = Number(product.stock_quantity ?? product.stockQuantity ?? 0);
  const status = rawStatus == null ? '' : String(rawStatus).trim().toLowerCase();
  const downByStatus = ['下架', '已下架', 'inactive', '2'].includes(status);
  return downByStatus || stock <= 0;
}

function getConditionBadge(product: Product, index: number) {
  if (isDown(product)) {
    return '已下架';
  }
  return conditionBadges[index % conditionBadges.length];
}

function getDeliveryBadge(product: Product) {
  const distance = Number(product.distanceKm);
  if (Number.isFinite(distance) && distance <= 3) {
    return '同城自提';
  }
  return '精选好物';
}

function goToMarketSearch() {
  router.push({
    name: 'CommunityMarketplaceFind',
    query: {
      ...route.query,
      keyword: marketKeyword.value || undefined,
      page: '1'
    }
  });
}

function handleCategoryClick(category: MarketCategory) {
  if (category.id === 'more') {
    goToMarketSearch();
    return;
  }

  router.push({
    name: 'CommunityMarketplaceFind',
    query: {
      keyword: category.keyword || category.label,
      page: '1'
    }
  });
}

function applyKeyword(keyword: string) {
  marketKeyword.value = keyword;
  goToMarketSearch();
}

function isSortActive(sort?: string, order?: string) {
  const currentSort = String(route.query.sort ?? '');
  const currentOrder = String(route.query.order ?? '');
  if (!sort && !order) {
    return !currentSort && !currentOrder;
  }
  return currentSort === String(sort ?? '') && currentOrder === String(order ?? '');
}

function updateCurrentQuery(queryPatch: Record<string, string | undefined>) {
  const nextQuery: Record<string, any> = {
    ...route.query,
    ...queryPatch,
    page: '1'
  };

  Object.keys(nextQuery).forEach((key) => {
    const value = nextQuery[key];
    if (value == null || value === '') {
      delete nextQuery[key];
    }
  });

  router.replace({
    path: route.path,
    query: nextQuery
  });
}

function applySort(sort?: string, order?: string) {
  updateCurrentQuery({
    keyword: marketKeyword.value || undefined,
    sort: sort || undefined,
    order: order || undefined
  });
}

function isPriceRangeActive(min?: string, max?: string) {
  const currentMin = String(route.query.priceMin ?? '');
  const currentMax = String(route.query.priceMax ?? '');
  return currentMin === String(min ?? '') && currentMax === String(max ?? '');
}

function applyPriceRange(min?: string, max?: string) {
  updateCurrentQuery({
    keyword: marketKeyword.value || undefined,
    priceMin: min || undefined,
    priceMax: max || undefined
  });
}

function clearAllFilters() {
  marketKeyword.value = '';
  nearbyRadius.value = null;
  updateCurrentQuery({
    keyword: undefined,
    priceMin: undefined,
    priceMax: undefined,
    sort: undefined,
    order: undefined,
    categoryId: undefined,
    subCategoryId: undefined,
    brand: undefined,
    minRating: undefined
  });
  fetchProducts();
}

function goToPublish() {
  router.push({ name: 'AddProduct' });
}

function openProductDetailById(productId: string | number | null | undefined) {
  if (productId == null || productId === '') {
    return;
  }

  const query: Record<string, string> = {};
  if (userLat.value != null && userLng.value != null) {
    query.lat = String(userLat.value);
    query.lng = String(userLng.value);
  }

  const url = router.resolve({
    name: 'ProductDetail',
    params: { id: productId },
    query
  }).href;

  window.open(url, '_blank');
}

function navigateToDetail(product: Product) {
  openProductDetailById(product.id);
}

function continueLatestTask() {
  openWorkbenchDrawer();
  if (latestUserMessage.value?.preview) {
    prefillAgentInput(latestUserMessage.value.preview);
    return;
  }
  if (latestUserMessage.value?.text) {
    prefillAgentInput(latestUserMessage.value.text);
  }
}

function startAgentPrompt(prompt: string) {
  openWorkbenchDrawer();
  prefillAgentInput(prompt);
}

function buildAgentPromptForProduct(product: Product) {
  const title = product.title || '这个商品';
  const location = formatLocation(product.location ?? product.loaction);
  const price = formatPrice(product.price);
  return `请帮我判断“${title}”是否值得买，当前价格是 ${price} 元，地点在 ${location}，请从价格、成色和同城交易风险三个角度给我建议。`;
}

function askAboutProduct(product: Product) {
  openWorkbenchDrawer();
  prefillAgentInput(buildAgentPromptForProduct(product));
}

function canOpenAgentCard(card: MarketplaceAiCard) {
  return card.entityType === 'product' && !!card.entityId;
}

function handleAgentCardClick(card: MarketplaceAiCard) {
  if (!canOpenAgentCard(card)) {
    return;
  }
  openProductDetailById(card.entityId);
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isWorkbenchDrawerOpen.value) {
    closeWorkbenchDrawer();
  }
}

async function fetchProducts() {
  loading.value = true;
  errorMsg.value = '';

  try {
    const params = new URLSearchParams();
    const keys = ['keyword', 'categoryId', 'subCategoryId', 'brand', 'priceMin', 'priceMax', 'minRating', 'sort', 'order'];
    keys.forEach((key) => {
      const value = route.query[key];
      if (value) {
        params.set(key, String(value));
      }
    });

    if (marketKeyword.value && !params.has('keyword')) {
      params.set('keyword', marketKeyword.value);
    }

    params.set('page', params.get('page') || '1');
    params.set('size', params.get('size') || '12');

    let response: Response;
    if (nearbyRadius.value && userLat.value != null && userLng.value != null) {
      params.set('lat', String(userLat.value));
      params.set('lng', String(userLng.value));
      params.set('radiusKm', String(nearbyRadius.value));
      params.set('limit', '12');
      response = await fetch(`${API_BASE}/api/products/nearby?${params.toString()}`);
    } else {
      response = await fetch(`${API_BASE}/api/products/getProducts?${params.toString()}`);
    }

    let list: Product[] = [];
    if (response.ok) {
      list = unwrapList(await response.json());
    } else {
      const fallbackResponse = await fetch(`${API_BASE}/api/products/getAllProducts`);
      if (fallbackResponse.ok) {
        list = unwrapList(await fallbackResponse.json()).slice(0, 12);
      }
    }

    products.value = list;
  } catch {
    errorMsg.value = '无法加载商品内容，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function toggleNearby(radius: number) {
  nearbyRadius.value = radius;
  if (userLat.value == null || userLng.value == null) {
    await locateByAddress();
  }
  await fetchProducts();
}

async function clearNearby() {
  nearbyRadius.value = null;
  await fetchProducts();
}

async function locateByAddress() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        userLat.value = position.coords.latitude;
        userLng.value = position.coords.longitude;
        if (!nearbyRadius.value) {
          nearbyRadius.value = 3;
        }
        fetchProducts();
      },
      async () => {
        if (addr.value && AMAP_KEY) {
          try {
            const response = await fetch(
              `https://restapi.amap.com/v3/geocode/geo?key=${AMAP_KEY}&address=${encodeURIComponent(addr.value)}`
            );
            const data = await response.json();
            const location = data.geocodes?.[0]?.location?.split(',');
            if (location) {
              userLng.value = parseFloat(location[0]);
              userLat.value = parseFloat(location[1]);
              await fetchProducts();
            }
          } catch {
            errorMsg.value = '定位失败，请检查地址后重试';
          }
        }
      }
    );
  }
}

onMounted(() => {
  fetchProducts();
  locateByAddress();
  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});
</script>

<style scoped>
.market-page {
  --page-bg: #f5f7f2;
  --surface: #ffffff;
  --surface-soft: #fbfcf8;
  --line: #e6ece0;
  --line-strong: #d8e2d0;
  --text-main: #1f2f20;
  --text-sub: #617066;
  --text-soft: #89968b;
  --green: #28b75d;
  --green-deep: #149247;
  --green-soft: #eaf8ef;
  --shadow: 0 18px 42px rgba(44, 91, 48, 0.08);
  --shadow-soft: 0 10px 24px rgba(44, 91, 48, 0.06);

  min-height: 100vh;
  background: var(--page-bg);
  color: var(--text-main);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.market-shell {
  min-height: 100vh;
}

.market-main {
  width: min(1580px, calc(100vw - 96px));
  margin: 0 auto;
  padding: 86px 28px 46px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.surface-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-soft);
}

.hero-banner {
  position: relative;
  min-height: 300px;
  border-radius: 22px;
  overflow: hidden;
  background: url("../../../pictures/homePicture1.jpg") center/cover no-repeat;
  border: 1px solid #e5eee0;
  box-shadow: var(--shadow);
  display: flex;
  align-items: center;
  padding: 40px 44px;
}

.hero-banner::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(20, 33, 22, 0.55) 0%, rgba(20, 33, 22, 0.25) 50%, rgba(20, 33, 22, 0.05) 100%);
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  max-width: 520px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #2d7a3e;
  font-size: 13px;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.hero-copy h1 {
  margin: 14px 0 0;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.hero-search {
  margin-top: 22px;
  width: min(560px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 10px;
  border-radius: 16px;
  border: 1px solid #e2eadc;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(57, 106, 61, 0.05);
}

.hero-search input,
.location-input input,
.chat-input-area input {
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  padding: 0 8px;
  font-size: 15px;
  color: var(--text-main);
}

.hero-search input::placeholder,
.location-input input::placeholder,
.chat-input-area input::placeholder {
  color: #a0aa9f;
}

.hero-search-btn,
.primary-btn,
.ghost-btn,
.radius-btn,
.category-pill,
.favorite-btn,
.workbench-fab,
.chat-result-card.clickable {
  transition: transform 0.18s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.hero-search-btn,
.primary-btn {
  border: none;
  background: var(--green);
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(35, 159, 79, 0.18);
}

.hero-search-btn:hover,
.primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(35, 159, 79, 0.22);
}

.hero-search-btn,
.hero-btn {
  min-height: 52px;
  padding: 0 22px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
}

.hero-btn {
  margin-top: 16px;
  width: fit-content;
}

.ghost-btn {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 12px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #435245;
  cursor: pointer;
}

.ghost-btn.accent {
  border-color: rgba(40, 183, 93, 0.2);
  color: var(--green-deep);
  background: #f5fbf6;
}

.ghost-btn:hover,
.radius-btn:hover,
.category-pill:hover,
.favorite-btn:hover {
  border-color: #bfd0bf;
  background: #f8fbf5;
}

.ghost-btn.compact,
.radius-btn {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 12px;
  font-size: 13px;
}

.full-width {
  width: 100%;
  justify-content: center;
}

.category-rail {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  padding: 18px;
}

.category-pill {
  min-height: 102px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
}

.category-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: var(--category-accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #304332;
  flex-shrink: 0;
}

.category-icon svg,
.trust-icon svg,
.service-icon svg {
  width: 22px;
  height: 22px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.category-text {
  font-size: 15px;
  font-weight: 600;
  color: #354336;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) 296px;
  gap: 18px;
  align-items: start;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.goods-panel,
.trust-panel,
.ai-entry-card,
.market-footer {
  padding: 20px;
}

.section-head,
.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--green-deep);
  font-weight: 700;
}

.section-head h2,
.side-head h3,
.footer-brand h3 {
  margin: 8px 0 0;
  font-size: 28px;
  line-height: 1.18;
  color: #1a281c;
}

.goods-toolbar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-strip {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-group,
.active-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  color: #6f7e70;
  font-weight: 600;
  white-space: nowrap;
}

.filter-chip,
.filter-clear-btn,
.active-filter-tag {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 13px;
}

.filter-chip,
.filter-clear-btn {
  border: 1px solid #dde7da;
  background: #ffffff;
  color: #526152;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.filter-chip:hover,
.filter-clear-btn:hover {
  border-color: #bfd0bf;
  background: #f8fbf5;
}

.filter-chip.active {
  border-color: rgba(40, 183, 93, 0.24);
  background: #eef8f0;
  color: var(--green-deep);
}

.filter-clear-btn {
  color: #687868;
}

.active-filter-bar {
  margin-top: 14px;
}

.active-filter-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.active-filter-tag {
  background: #f3f6f2;
  color: #5f6d60;
}

.goods-overview {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.goods-result-summary strong,
.goods-result-summary span {
  display: block;
}

.goods-result-summary strong {
  font-size: 16px;
  color: #203022;
}

.goods-result-summary span {
  margin-top: 6px;
  font-size: 13px;
  color: #738074;
}

.sort-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sort-btn {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #dde7da;
  background: #ffffff;
  color: #536254;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.sort-btn:hover {
  border-color: #bfd0bf;
  background: #f8fbf5;
}

.sort-btn.active {
  border-color: rgba(40, 183, 93, 0.24);
  background: #eef8f0;
  color: var(--green-deep);
}

.radius-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.radius-btn {
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #526253;
  cursor: pointer;
}

.radius-btn.active {
  border-color: rgba(40, 183, 93, 0.28);
  background: var(--green-soft);
  color: var(--green-deep);
}

.location-input {
  min-width: 320px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--surface-soft);
}

.panel-state {
  margin-top: 18px;
  min-height: 280px;
  border: 1px dashed var(--line-strong);
  border-radius: 16px;
  background: #fbfcfa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #758375;
  font-size: 15px;
}

.panel-state-stack {
  padding: 24px;
  flex-direction: column;
  gap: 10px;
  text-align: center;
}

.panel-state-stack strong {
  font-size: 18px;
  color: #223124;
}

.panel-state-stack p {
  margin: 0;
  color: #728073;
  line-height: 1.7;
}

.empty-suggest-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.empty-suggest-btn {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid #dde7da;
  background: #ffffff;
  color: #506151;
  cursor: pointer;
  font-size: 13px;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.empty-suggest-btn:hover {
  border-color: rgba(40, 183, 93, 0.24);
  background: #f4fbf5;
  color: var(--green-deep);
}

.panel-state.error {
  color: #c14d4d;
  background: #fff8f8;
}

.panel-state.empty {
  color: #7a887a;
}

.goods-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.goods-card {
  border: 1px solid #ebf0e7;
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(73, 111, 76, 0.05);
}

.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(73, 111, 76, 0.1);
}

.goods-image-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #f3f6f1;
}

.goods-image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.goods-condition,
.goods-deliver {
  position: absolute;
  top: 8px;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
}

.goods-condition {
  left: 8px;
  background: rgba(36, 178, 92, 0.92);
  color: #ffffff;
}

.goods-deliver {
  right: 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #4b5c4d;
}

.goods-price-chip {
  position: absolute;
  left: 8px;
  bottom: 8px;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(24, 33, 25, 0.82);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
}

.goods-body {
  padding: 12px;
}

.goods-head-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
}

.goods-body h3 {
  margin: 0;
  min-height: 38px;
  font-size: 14px;
  line-height: 1.4;
  color: #202f21;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-tags-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.goods-info-chip {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  white-space: nowrap;
}

.goods-info-chip.location {
  background: #f3f6f2;
  color: #5d6b5f;
}

.goods-info-chip.distance {
  background: #edf7f0;
  color: var(--green-deep);
  font-weight: 700;
}

.goods-price-row {
  margin-top: 10px;
}

.goods-price-row strong {
  font-size: 22px;
  color: #f34e22;
  line-height: 1;
}

.favorite-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #aab1aa;
  font-size: 18px;
  cursor: pointer;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 86px;
}

.trust-overview {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #ebf1e6;
  background: #fbfdf9;
}

.trust-score {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.trust-score strong {
  font-size: 32px;
  line-height: 1;
  color: #1e8a4a;
}

.trust-score span {
  font-size: 13px;
  color: #6f7d70;
}

.trust-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trust-summary span {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f7f2;
  color: #536354;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
}

.trust-list {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.trust-item {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #edf2ea;
  background: #fbfdf9;
}

.trust-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: var(--trust-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #345138;
}

.trust-item strong {
  font-size: 16px;
  color: #223123;
}

.trust-item p,
.ai-entry-text,
.ai-preview-box p,
.ai-preview-box small,
.footer-brand p,
.footer-column span,
.service-pill p,
.chat-header p,
.chat-result-subtitle,
.chat-result-reason,
.chat-result-meta {
  margin: 6px 0 0;
  color: var(--text-sub);
  line-height: 1.65;
}

.ai-entry-actions {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.ai-quick-list {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.ai-quick-btn {
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid #e7eee3;
  background: #ffffff;
  color: #465547;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.ai-quick-btn:hover {
  border-color: rgba(40, 183, 93, 0.24);
  background: #f6fbf6;
  color: var(--green-deep);
}

.ai-preview-box {
  margin-top: 18px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fbf6;
  border: 1px solid #e9efe4;
}

.ai-preview-box strong {
  font-size: 15px;
  color: #223123;
}

.service-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.service-pill {
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: #ffffff;
  display: flex;
  align-items: center;
  gap: 14px;
}

.service-icon {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.service-pill strong {
  display: block;
  font-size: 16px;
  color: #223123;
}

.market-footer {
  display: grid;
  gap: 24px;
}

.footer-brand {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.footer-brand p {
  max-width: 520px;
}

.footer-columns {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.footer-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-column strong {
  font-size: 16px;
  color: #223123;
}

.footer-column span {
  margin: 0;
  font-size: 14px;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-top: 4px;
  color: #95a094;
  font-size: 13px;
}

.footer-socials {
  display: flex;
  gap: 10px;
}

.footer-socials span {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #f9fbf7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.workbench-fab {
  position: fixed;
  right: 92px;
  bottom: 28px;
  z-index: 1302;
  min-height: 48px;
  padding: 0 20px;
  border-radius: 18px;
  border: none;
  background: var(--green);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 16px 32px rgba(35, 159, 79, 0.22);
}

.workbench-fab:hover {
  transform: translateY(-2px);
}


.hero-search input:focus,
.location-input input:focus {
  box-shadow: 0 0 0 3px rgba(40, 183, 93, 0.12);
}

@media (max-width: 1560px) {
  .market-main {
    width: calc(100vw - 78px);
  }

  .category-rail {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1360px) {
  .hero-banner {
    grid-template-columns: 1fr;
  }

  .hero-mini-grid,
  .hero-secondary-list,
  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .service-strip,
  .footer-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .market-main {
    padding-inline: 20px;
  }

  .side-panel {
    position: static;
  }

  .category-rail,
  .service-strip,
  .footer-columns {
    grid-template-columns: 1fr;
  }

  .goods-toolbar,
  .filter-strip,
  .active-filter-bar,
  .goods-overview,
  .footer-brand,
  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
  }

  .location-input {
    width: 100%;
    min-width: 0;
  }

}

@media (max-width: 780px) {
  .market-main {
    width: calc(100vw - 24px);
    padding: 78px 12px 30px;
  }

  .hero-banner,
  .goods-panel,
  .trust-panel,
  .ai-entry-card,
  .market-footer {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .goods-grid {
    grid-template-columns: 1fr;
  }

  .section-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-search,
  .location-input {
    grid-template-columns: 1fr;
  }

  .workbench-fab {
    right: 16px;
    bottom: 18px;
  }
}
</style>
