<template>
  <div class="find-page">
    <dhstyle />
    <CebianTool />

    <div class="find-shell">
      <section class="surface-card search-hero">
        <div class="search-hero-copy">
          <p class="section-kicker">搜索结果工作台</p>
          <h1>更快筛出真正值得看的商品</h1>
          <p>
            把关键词、分类、预算、距离和快捷条件收进一个结果工作台里，浏览商品时不用再来回切页面。
          </p>
        </div>

        <div class="search-hero-stats">
          <article v-for="stat in surfaceStats" :key="stat.label" class="hero-stat-card">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
            <small>{{ stat.helper }}</small>
          </article>
        </div>
      </section>

      <section class="surface-card search-workbench">
        <div class="search-bar-row">
          <div class="search-input-wrap">
            <span class="search-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="7"></circle>
                <path d="M20 20l-3.5-3.5"></path>
              </svg>
            </span>
            <input
              v-model="keywordInput"
              type="text"
              placeholder="搜索商品、分类或你想要的好物"
              @keyup.enter="doSearch"
            />
          </div>

          <button class="primary-btn" type="button" @click="doSearch">搜索</button>
          <button class="ghost-btn" type="button" @click="openWorkbenchDrawer">打开 AI 助手</button>
          <button class="ghost-btn" type="button" @click="goToPublish">发布商品</button>
        </div>

        <div class="keyword-suggestion-row">
          <span>热门搜索</span>
          <button
            v-for="suggestion in popularKeywords"
            :key="suggestion"
            class="suggestion-btn"
            type="button"
            @click="applyKeywordSuggestion(suggestion)"
          >
            {{ suggestion }}
          </button>
        </div>
      </section>

      <div class="workspace-grid">
        <aside class="filters-column">
          <section class="surface-card filters-panel">
            <header class="panel-head">
              <div>
                <p class="section-kicker">筛选条件</p>
                <h2>按你的交易偏好收紧结果</h2>
              </div>
              <span class="panel-count">{{ activeFilterTags.length }} 项已启用</span>
            </header>

            <article class="filter-block">
              <div class="filter-block-head">
                <strong>分类</strong>
                <span>{{ selectedCategoryName || '全部分类' }}</span>
              </div>
              <div class="filter-chip-group">
                <button
                  :class="['filter-chip', { active: !selectedCategoryId }]"
                  type="button"
                  @click="selectCategory('')"
                >
                  全部
                </button>
                <button
                  v-for="category in allCategories"
                  :key="category.id"
                  :class="['filter-chip', { active: selectedCategoryId === String(category.id) }]"
                  type="button"
                  @click="selectCategory(String(category.id))"
                >
                  {{ category.name }}
                </button>
              </div>
            </article>

            <article class="filter-block">
              <div class="filter-block-head">
                <strong>价格</strong>
                <span>{{ selectedPriceRange?.label || '不限价格' }}</span>
              </div>
              <div class="filter-chip-group">
                <button
                  :class="['filter-chip', { active: !selectedPriceKey }]"
                  type="button"
                  @click="applyPriceRange('')"
                >
                  不限
                </button>
                <button
                  v-for="range in priceRanges"
                  :key="range.key"
                  :class="['filter-chip', { active: selectedPriceKey === range.key }]"
                  type="button"
                  @click="applyPriceRange(range.key)"
                >
                  {{ range.label }}
                </button>
              </div>
            </article>

            <article class="filter-block">
              <div class="filter-block-head">
                <strong>快捷筛选</strong>
                <span>提高浏览效率</span>
              </div>
              <div class="filter-chip-group">
                <button
                  v-for="option in quickFilterOptions"
                  :key="option.key"
                  :class="['filter-chip', 'ghost', { active: quick[option.key] }]"
                  type="button"
                  @click="toggleQuickFilter(option.key)"
                >
                  {{ option.label }}
                </button>
              </div>
            </article>

            <article class="filter-block">
              <div class="filter-block-head">
                <strong>同城范围</strong>
                <span>{{ nearbyRadius != null ? `${nearbyRadius}km` : '全城结果' }}</span>
              </div>
              <div class="filter-chip-group">
                <button
                  :class="['filter-chip', 'ghost', { active: nearbyRadius === null }]"
                  type="button"
                  @click="clearNearby"
                >
                  全城
                </button>
                <button
                  v-for="radius in nearbyOptions"
                  :key="radius"
                  :class="['filter-chip', 'ghost', { active: nearbyRadius === radius }]"
                  type="button"
                  @click="setNearby(radius)"
                >
                  {{ radius }}km
                </button>
              </div>

              <div class="location-input">
                <input
                  v-model="addr"
                  type="text"
                  placeholder="定位不准？输入地址重新定位"
                  @keyup.enter="locateByAddress"
                />
                <button class="ghost-btn compact" type="button" @click="locateByAddress">定位</button>
              </div>
            </article>

            <div class="filter-actions">
              <button v-if="hasActiveFilters" class="ghost-btn compact" type="button" @click="clearAllFilters">
                清空筛选
              </button>
              <button class="ghost-btn compact accent" type="button" @click="continueLatestTask">
                继续上次对话
              </button>
            </div>
          </section>

          <section class="surface-card ai-entry-card">
            <header class="panel-head compact">
              <div>
                <p class="section-kicker">AI 助手</p>
                <h2>边逛边问</h2>
              </div>
            </header>

            <p class="ai-entry-text">
              AI 保持在辅助位，帮你从当前筛选结果里判断性价比、比较商品或补充提问思路。
            </p>

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

            <div class="ai-preview-box">
              <strong>最近会话</strong>
              <p>{{ latestUserPreview || '还没有提问，可以先让 AI 根据当前列表帮你筛 3 件重点商品。' }}</p>
              <small>{{ latestAgentReplyPreview || '最近一次 AI 回复会显示在这里，方便继续追问。' }}</small>
            </div>
          </section>
        </aside>

        <main class="results-column">
          <section class="surface-card results-toolbar">
            <div class="results-summary">
              <p class="section-kicker">搜索结果</p>
              <h2>{{ resultSummary }}</h2>
              <p class="results-hint">{{ resultSummaryHint }}</p>
            </div>

            <div class="results-toolbar-actions">
              <div class="sort-group">
                <button
                  v-for="option in sortOptions"
                  :key="option.key"
                  :class="['sort-btn', { active: sortBy === option.key }]"
                  type="button"
                  @click="setSort(option.key)"
                >
                  <span>{{ option.label }}</span>
                  <span v-if="option.key === 'price' && sortBy === 'price'" class="sort-arrow">
                    {{ sortDir === 'asc' ? '↑' : '↓' }}
                  </span>
                </button>
              </div>
            </div>

            <div v-if="activeFilterTags.length" class="active-filter-bar">
              <span class="filter-label">当前筛选</span>
              <div class="active-tag-list">
                <button
                  v-for="tag in activeFilterTags"
                  :key="tag.key"
                  class="active-tag-btn"
                  type="button"
                  @click="removeFilterTag(tag.key)"
                >
                  <span>{{ tag.label }}</span>
                  <strong>×</strong>
                </button>
              </div>
            </div>
          </section>

          <section class="results-canvas">
            <div v-if="loading" class="state-box">
              <div class="spinner"></div>
              <p>正在整理符合条件的商品...</p>
            </div>

            <div v-else-if="errorMsg" class="state-box error">
              <strong>商品加载失败</strong>
              <p>{{ errorMsg }}</p>
            </div>

            <div v-else-if="visibleProducts.length === 0" class="state-box empty">
              <strong>当前条件下暂时没有找到合适商品</strong>
              <p>可以调整分类、预算或范围，也可以让 AI 根据当前需求给你新的搜索建议。</p>
              <div class="empty-actions">
                <button class="ghost-btn compact" type="button" @click="clearAllFilters">恢复默认筛选</button>
                <button class="ghost-btn compact accent" type="button" @click="startAgentPrompt(buildListAdvicePrompt())">
                  让 AI 帮我调整筛选
                </button>
              </div>
            </div>

            <div v-else class="results-grid">
              <article
                v-for="product in visibleProducts"
                :key="product.id"
                class="goods-card"
                @click="navigateToDetail(product)"
              >
                <div class="goods-media">
                  <img
                    :src="getFirstImage(product)"
                    :alt="product.title"
                    @error="handleImageError($event, FALLBACK_ITEM)"
                  />
                  <span v-if="isDown(product)" class="goods-badge muted">已下架</span>
                  <span v-else class="goods-badge">可咨询</span>
                  <span class="goods-price-chip">¥{{ formatPrice(product.price) }}</span>
                </div>

                <div class="goods-body">
                  <div class="goods-head">
                    <h3 :title="product.title">{{ product.title }}</h3>
                    <strong>¥{{ formatPrice(product.price) }}</strong>
                  </div>

                  <div class="goods-meta">
                    <span class="meta-chip">{{ formatLocation(product.location ?? product.loaction) }}</span>
                    <span v-if="product.distanceKm != null" class="meta-chip success">
                      {{ formatDistance(product.distanceKm) }}
                    </span>
                    <span v-if="getFirstImage(product) !== FALLBACK_ITEM" class="meta-chip">有实拍</span>
                  </div>

                  <div class="seller-row">
                    <div class="seller-info">
                      <img
                        :src="getSellerAvatar(product)"
                        alt="seller avatar"
                        @error="handleImageError($event, FALLBACK_AVATAR)"
                      />
                      <span>{{ formatSellerId(product.seller_id ?? product.sellerId) }}</span>
                    </div>
                    <span class="goods-status">{{ formatPublishTime(product.created_at ?? product.createdAt) }}</span>
                  </div>

                  <div class="goods-actions">
                    <button class="ghost-btn compact" type="button" @click.stop="navigateToDetail(product)">查看详情</button>
                    <button class="ghost-btn compact accent" type="button" @click.stop="askAboutProduct(product)">
                      AI 参考
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </main>
      </div>
    </div>

    <CommunityMarketplaceAiDrawer
      v-model="isWorkbenchDrawerOpen"
      kicker="AI 对话框"
      title="市场助手"
      headline="结合当前结果继续提问"
      helper-text="支持继续追问，消息会发送到后端接口，并保留当前会话上下文。"
      empty-text="你好，我是你的社区二手市场助手。你可以让我结合当前列表筛商品、比较价格、整理提问话术，或者继续深挖某一件商品。"
      placeholder="例如：帮我从当前结果里挑 3 件更值得线下看货的商品"
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
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter, type LocationQuery } from 'vue-router';
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
  free_shipping?: boolean;
  freeShipping?: boolean;
  invoice?: boolean;
  hasInvoice?: boolean;
  rating?: number | string;
  created_at?: string | number | Date;
  createdAt?: string | number | Date;
  category_id?: number | string;
  categoryId?: number | string;
  sub_category_id?: number | string;
  subCategoryId?: number | string;
  status?: string | number;
  stock_quantity?: number | string;
  stockQuantity?: number | string;
  distanceKm?: number | string | null;
}

interface Category {
  id: number | string;
  name: string;
  parentId?: number | string | null;
}

interface PriceRangeOption {
  key: string;
  label: string;
  min: number;
  max: number | null;
}

interface QuickFilterOption {
  key: QuickFilterKey;
  label: string;
}

interface ActiveFilterTag {
  key: string;
  label: string;
}

interface SurfaceStat {
  label: string;
  value: string;
  helper: string;
}

type SortKey = 'comprehensive' | 'latest' | 'price';

const API_BASE = ((import.meta as any)?.env?.VITE_API_BASE ?? (window as any)?.VITE_API_BASE ?? 'http://localhost:8080');
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '');
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';
const GEOCODER_FORWARD = 'https://restapi.amap.com/v3/geocode/geo';

const FALLBACK_ITEM =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="240"><rect width="100%" height="100%" fill="%23eef3ec"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23839280" font-size="16">暂无图片</text></svg>';
const FALLBACK_AVATAR =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect width="64" height="64" rx="18" fill="%23e9efe4"/><text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="%23667869" font-size="24">U</text></svg>';

const popularKeywords = ['通勤电脑', '自提家具', '九成新手机', '儿童书桌', '摄影设备', '宿舍电器'];
const nearbyOptions = [1, 3, 5, 10];

const priceRanges: PriceRangeOption[] = [
  { key: '0-200', label: '200 以下', min: 0, max: 200 },
  { key: '200-1000', label: '200-1000', min: 200, max: 1000 },
  { key: '1000-2700', label: '1000-2700', min: 1000, max: 2700 },
  { key: '2700-5000', label: '2700-5000', min: 2700, max: 5000 },
  { key: '5000-8300', label: '5000-8300', min: 5000, max: 8300 },
  { key: '8300+', label: '8300 以上', min: 8300, max: null }
];

const quickFilterLabels = {
  freeShipping: '包邮',
  invoice: '有发票',
  withImage: '仅看有图',
  highRating: '高评分'
} as const;

type QuickFilterKey = keyof typeof quickFilterLabels;

const quickFilterOptions: QuickFilterOption[] = [
  { key: 'freeShipping', label: '包邮' },
  { key: 'invoice', label: '有发票' },
  { key: 'withImage', label: '仅看有图' },
  { key: 'highRating', label: '高评分' }
];

const sortOptions: Array<{ key: SortKey; label: string }> = [
  { key: 'comprehensive', label: '综合排序' },
  { key: 'latest', label: '最新发布' },
  { key: 'price', label: '价格排序' }
];

const agentQuickPrompts = [
  '帮我从当前筛选结果里挑 3 件更值得重点看的商品',
  '如果我想优先同城自提，当前筛选还应该怎么调',
  '请根据当前列表帮我整理一段问卖家的提问话术'
];

const router = useRouter();
const route = useRoute();

const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const loading = ref(false);
const errorMsg = ref('');
const categoriesLoaded = ref(false);
const hasLoadedProducts = ref(false);
const lastFetchSignature = ref('');

const keywordInput = ref('');
const keyword = ref('');
const selectedCategoryId = ref('');
const selectedPriceKey = ref('');
const sortBy = ref<SortKey>('comprehensive');
const sortDir = ref<'asc' | 'desc'>('asc');
const quick = ref<Record<QuickFilterKey, boolean>>({
  freeShipping: false,
  invoice: false,
  withImage: false,
  highRating: false
});
const userLat = ref<number | null>(null);
const userLng = ref<number | null>(null);
const nearbyRadius = ref<number | null>(null);
const addr = ref('');

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
  prefillInput: prefillAgentInput,
  submitMessage: submitDialogMessage
} = useCommunityMarketplaceAi({
  apiBase: API_BASE,
  quickPrompts: agentQuickPrompts,
  sessionStorageKey: 'communityMarketplaceFindAgentSessionId',
  initialAgentMessage: '你好，我是你的社区二手市场助手。你可以直接问我筛商品、比较价格、整理提问话术或判断是否值得入手。',
  buildUserProfile: () => ({
    keyword: keyword.value || undefined,
    categoryId: selectedCategoryId.value || undefined,
    priceRange: selectedPriceKey.value || undefined,
    latitude: userLat.value ?? undefined,
    longitude: userLng.value ?? undefined,
    nearbyRadiusKm: nearbyRadius.value ?? undefined,
    address: addr.value.trim() || undefined,
    quickFilters: quickFilterOptions.filter((item) => quick.value[item.key]).map((item) => item.label)
  })
});

const allCategories = computed(() => {
  return (Array.isArray(categories.value) ? categories.value : []).filter((item) => item?.name);
});

const selectedCategoryName = computed(() => {
  if (!selectedCategoryId.value) {
    return '';
  }

  return allCategories.value.find((item) => String(item.id) === selectedCategoryId.value)?.name || '';
});

const selectedPriceRange = computed(() => {
  return priceRanges.find((item) => item.key === selectedPriceKey.value) ?? null;
});

const activeFilterTags = computed<ActiveFilterTag[]>(() => {
  const tags: ActiveFilterTag[] = [];

  if (keyword.value.trim()) {
    tags.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` });
  }

  if (selectedCategoryId.value) {
    tags.push({ key: 'category', label: `分类：${selectedCategoryName.value || '已选分类'}` });
  }

  if (selectedPriceRange.value) {
    tags.push({ key: 'price', label: `价格：${selectedPriceRange.value.label}` });
  }

  quickFilterOptions.forEach((option) => {
    if (quick.value[option.key]) {
      tags.push({ key: `quick:${option.key}`, label: option.label });
    }
  });

  if (nearbyRadius.value != null) {
    const locationLabel = addr.value.trim() ? ` · ${addr.value.trim()}` : '';
    tags.push({ key: 'nearby', label: `范围：${nearbyRadius.value}km${locationLabel}` });
  }

  return tags;
});

const hasActiveFilters = computed(() => activeFilterTags.value.length > 0);

const visibleProducts = computed(() => {
  let list = Array.isArray(products.value) ? products.value.slice() : [];

  if (keyword.value.trim()) {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    list = list.filter((product) => (product.title || '').toLowerCase().includes(normalizedKeyword));
  }

  if (selectedCategoryId.value) {
    list = list.filter((product) => {
      return productCategoryIds(product).includes(selectedCategoryId.value);
    });
  }

  if (selectedPriceRange.value) {
    list = list.filter((product) => {
      const price = Number(product.price ?? 0);
      if (!Number.isFinite(price)) {
        return false;
      }

      if (selectedPriceRange.value?.max == null) {
        return price >= selectedPriceRange.value.min;
      }

      return price >= selectedPriceRange.value.min && price < selectedPriceRange.value.max;
    });
  }

  if (quick.value.withImage) {
    list = list.filter((product) => getFirstImage(product) !== FALLBACK_ITEM);
  }

  if (quick.value.freeShipping) {
    list = list.filter((product) => (product.free_shipping ?? product.freeShipping ?? false) === true);
  }

  if (quick.value.invoice) {
    list = list.filter((product) => (product.invoice ?? product.hasInvoice ?? false) === true);
  }

  if (quick.value.highRating) {
    list = list.filter((product) => Number(product.rating ?? 0) >= 4);
  }

  if (sortBy.value === 'latest') {
    list.sort((left, right) => getCreatedAtTimestamp(right) - getCreatedAtTimestamp(left));
  } else if (sortBy.value === 'price') {
    list.sort((left, right) => Number(left.price ?? 0) - Number(right.price ?? 0));
    if (sortDir.value === 'desc') {
      list.reverse();
    }
  }

  return list;
});

const nearbySummary = computed(() => {
  if (nearbyRadius.value == null) {
    return '全城';
  }
  return `${nearbyRadius.value}km`;
});

const resultSummary = computed(() => {
  if (keyword.value.trim()) {
    return `“${keyword.value.trim()}” 共匹配到 ${visibleProducts.value.length} 件商品`;
  }

  if (selectedCategoryName.value) {
    return `${selectedCategoryName.value} 共筛出 ${visibleProducts.value.length} 件商品`;
  }

  return `当前列表共展示 ${visibleProducts.value.length} 件商品`;
});

const resultSummaryHint = computed(() => {
  const hints: string[] = [];

  if (nearbyRadius.value != null) {
    hints.push(`已启用 ${nearbyRadius.value}km 范围筛选`);
  } else {
    hints.push('当前展示全城结果');
  }

  if (hasActiveFilters.value) {
    hints.push(`共 ${activeFilterTags.value.length} 个筛选条件生效`);
  } else {
    hints.push('可以继续按分类、价格或快捷条件缩小范围');
  }

  return hints.join('，');
});

const surfaceStats = computed<SurfaceStat[]>(() => [
  {
    label: '当前结果',
    value: `${visibleProducts.value.length}`,
    helper: '会随筛选条件即时变化'
  },
  {
    label: '商品池',
    value: `${products.value.length}`,
    helper: '保持现有商品获取逻辑'
  },
  {
    label: '浏览范围',
    value: nearbySummary.value,
    helper: nearbyRadius.value != null ? addr.value.trim() || '按定位结果筛选' : '默认展示全城'
  }
]);

watch(
  () => route.query,
  async (query) => {
    applyStateFromQuery(query);

    if (!categoriesLoaded.value) {
      await fetchCategories();
    }

    const nextSignature = getFetchSignature();
    if (!hasLoadedProducts.value || nextSignature !== lastFetchSignature.value) {
      lastFetchSignature.value = nextSignature;
      await fetchProductsFind();
      hasLoadedProducts.value = true;
    }
  },
  { deep: true, immediate: true }
);

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isWorkbenchDrawerOpen.value) {
    closeWorkbenchDrawer();
  }
}

function firstQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] ?? '').trim();
  }
  return String(value ?? '').trim();
}

function parseBooleanQuery(value: unknown) {
  const normalized = firstQueryValue(value).toLowerCase();
  return normalized === '1' || normalized === 'true' || normalized === 'yes';
}

function parseNumberQuery(value: unknown) {
  const normalized = firstQueryValue(value);
  if (!normalized) {
    return null;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : null;
}

function normalizeQueryObject(source: Record<string, unknown>) {
  return JSON.stringify(
    Object.keys(source)
      .sort()
      .reduce<Record<string, string>>((result, key) => {
        const value = firstQueryValue(source[key]);
        if (value) {
          result[key] = value;
        }
        return result;
      }, {})
  );
}

function getFetchSignature() {
  return JSON.stringify({
    lat: userLat.value,
    lng: userLng.value,
    radius: nearbyRadius.value
  });
}

function applyStateFromQuery(query: LocationQuery) {
  keyword.value = firstQueryValue(query.keyword);
  keywordInput.value = keyword.value;
  selectedCategoryId.value = firstQueryValue(query.categoryId || query.subCategoryId);
  selectedPriceKey.value = firstQueryValue(query.price);

  const nextSort = firstQueryValue(query.sort) as SortKey;
  sortBy.value = ['latest', 'price'].includes(nextSort) ? nextSort : 'comprehensive';

  const nextOrder = firstQueryValue(query.order).toLowerCase();
  sortDir.value = nextOrder === 'desc' ? 'desc' : 'asc';

  quick.value = {
    freeShipping: parseBooleanQuery(query.freeShipping),
    invoice: parseBooleanQuery(query.invoice),
    withImage: parseBooleanQuery(query.withImage),
    highRating: parseBooleanQuery(query.highRating)
  };

  userLat.value = parseNumberQuery(query.lat);
  userLng.value = parseNumberQuery(query.lng);
  nearbyRadius.value = parseNumberQuery(query.radius);
  addr.value = firstQueryValue(query.address);
}

function buildQueryFromState() {
  const nextQuery: Record<string, string> = {};

  if (keyword.value.trim()) {
    nextQuery.keyword = keyword.value.trim();
  }

  if (selectedCategoryId.value) {
    nextQuery.categoryId = selectedCategoryId.value;
  }

  if (selectedPriceKey.value) {
    nextQuery.price = selectedPriceKey.value;
  }

  if (sortBy.value !== 'comprehensive') {
    nextQuery.sort = sortBy.value;
  }

  if (sortBy.value === 'price' && sortDir.value === 'desc') {
    nextQuery.order = sortDir.value;
  }

  if (quick.value.freeShipping) {
    nextQuery.freeShipping = '1';
  }

  if (quick.value.invoice) {
    nextQuery.invoice = '1';
  }

  if (quick.value.withImage) {
    nextQuery.withImage = '1';
  }

  if (quick.value.highRating) {
    nextQuery.highRating = '1';
  }

  if (nearbyRadius.value != null) {
    nextQuery.radius = String(nearbyRadius.value);
    if (userLat.value != null && userLng.value != null) {
      nextQuery.lat = String(Number(userLat.value.toFixed(6)));
      nextQuery.lng = String(Number(userLng.value.toFixed(6)));
    }
    if (addr.value.trim()) {
      nextQuery.address = addr.value.trim();
    }
  }

  return nextQuery;
}

async function syncQueryWithState() {
  const nextQuery = buildQueryFromState();
  if (normalizeQueryObject(nextQuery) === normalizeQueryObject(route.query as Record<string, unknown>)) {
    return;
  }

  await router.replace({
    name: 'CommunityMarketplaceFind',
    query: nextQuery
  });
}

function doSearch() {
  keyword.value = keywordInput.value.trim();
  syncQueryWithState();
}

function applyKeywordSuggestion(value: string) {
  keywordInput.value = value;
  keyword.value = value;
  syncQueryWithState();
}

function selectCategory(categoryId: string) {
  selectedCategoryId.value = categoryId;
  syncQueryWithState();
}

function applyPriceRange(priceKey: string) {
  selectedPriceKey.value = priceKey;
  syncQueryWithState();
}

function toggleQuickFilter(key: QuickFilterKey) {
  quick.value = {
    ...quick.value,
    [key]: !quick.value[key]
  };
  syncQueryWithState();
}

function setSort(nextSort: SortKey) {
  if (nextSort === 'price') {
    if (sortBy.value === 'price') {
      sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc';
    } else {
      sortBy.value = 'price';
      sortDir.value = 'asc';
    }
  } else if (nextSort === 'latest') {
    sortBy.value = 'latest';
    sortDir.value = 'desc';
  } else {
    sortBy.value = 'comprehensive';
    sortDir.value = 'asc';
  }

  syncQueryWithState();
}

async function setNearby(radius: number) {
  errorMsg.value = '';

  if (userLat.value == null || userLng.value == null) {
    try {
      await requestLocation();
    } catch {
      errorMsg.value = '未能获取定位，暂时无法启用附近筛选';
      return;
    }
  }

  nearbyRadius.value = radius;
  syncQueryWithState();
}

function clearNearby() {
  nearbyRadius.value = null;
  userLat.value = null;
  userLng.value = null;
  addr.value = '';
  syncQueryWithState();
}

async function locateByAddress() {
  errorMsg.value = '';

  try {
    await requestLocation();
    if (nearbyRadius.value == null) {
      nearbyRadius.value = 1;
    }
    await syncQueryWithState();
    return;
  } catch {
    // Fall through to geocoder when direct location is unavailable.
  }

  const value = addr.value.trim();
  if (!value || !AMAP_KEY) {
    errorMsg.value = '定位失败，请检查权限或地址';
    return;
  }

  try {
    const response = await fetch(`${GEOCODER_FORWARD}?key=${AMAP_KEY}&address=${encodeURIComponent(value)}`);
    const result = await response.json();
    const location = result?.geocodes?.[0]?.location?.split(',');

    if (!location || location.length !== 2) {
      throw new Error('未找到定位结果');
    }

    userLng.value = Number(location[0]);
    userLat.value = Number(location[1]);
    if (nearbyRadius.value == null) {
      nearbyRadius.value = 1;
    }
    await syncQueryWithState();
  } catch {
    errorMsg.value = '定位失败，请检查权限或地址';
  }
}

function clearAllFilters() {
  keyword.value = '';
  keywordInput.value = '';
  selectedCategoryId.value = '';
  selectedPriceKey.value = '';
  sortBy.value = 'comprehensive';
  sortDir.value = 'asc';
  quick.value = {
    freeShipping: false,
    invoice: false,
    withImage: false,
    highRating: false
  };
  nearbyRadius.value = null;
  userLat.value = null;
  userLng.value = null;
  addr.value = '';
  syncQueryWithState();
}

function removeFilterTag(tagKey: string) {
  if (tagKey === 'keyword') {
    keyword.value = '';
    keywordInput.value = '';
  } else if (tagKey === 'category') {
    selectedCategoryId.value = '';
  } else if (tagKey === 'price') {
    selectedPriceKey.value = '';
  } else if (tagKey === 'nearby') {
    nearbyRadius.value = null;
    userLat.value = null;
    userLng.value = null;
    addr.value = '';
  } else if (tagKey.startsWith('quick:')) {
    const quickKey = tagKey.split(':')[1] as QuickFilterKey;
    quick.value = {
      ...quick.value,
      [quickKey]: false
    };
  }

  syncQueryWithState();
}

function buildListAdvicePrompt() {
  const parts = ['请根据我当前的二手市场搜索结果，帮我优化筛选策略。'];

  if (keyword.value.trim()) {
    parts.push(`我的关键词是“${keyword.value.trim()}”。`);
  }

  if (selectedCategoryName.value) {
    parts.push(`当前分类是 ${selectedCategoryName.value}。`);
  }

  if (selectedPriceRange.value) {
    parts.push(`预算范围是 ${selectedPriceRange.value.label}。`);
  }

  if (nearbyRadius.value != null) {
    parts.push(`我希望在 ${nearbyRadius.value}km 范围内交易。`);
  }

  parts.push(`当前列表里有 ${visibleProducts.value.length} 件商品。请告诉我下一步该怎么收紧条件。`);
  return parts.join('');
}

function continueLatestTask() {
  openWorkbenchDrawer();
  if (latestUserMessage.value?.preview) {
    prefillAgentInput(latestUserMessage.value.preview);
    return;
  }

  if (latestUserMessage.value?.text) {
    prefillAgentInput(latestUserMessage.value.text);
    return;
  }

  prefillAgentInput(buildListAdvicePrompt());
}

function startAgentPrompt(prompt: string) {
  openWorkbenchDrawer();
  prefillAgentInput(prompt);
}

function buildAgentPromptForProduct(product: Product) {
  const title = product.title || '这个商品';
  const location = formatLocation(product.location ?? product.loaction);
  const price = formatPrice(product.price);
  return `请帮我判断“${title}”是否值得继续了解，当前价格是 ${price} 元，地点在 ${location}。请从价格、同城交易便利度和和卖家沟通重点三个角度给我建议。`;
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

  openProductDetailById(card.entityId as string | number);
}

function buildProductDetailQuery() {
  const query: Record<string, string> = {};
  if (userLat.value != null && userLng.value != null) {
    query.lat = String(Number(userLat.value.toFixed(6)));
    query.lng = String(Number(userLng.value.toFixed(6)));
  }
  return query;
}

function openProductDetailById(productId: string | number) {
  const url = router.resolve({
    name: 'ProductDetail',
    params: { id: productId },
    query: buildProductDetailQuery()
  }).href;

  window.open(url, '_blank');
}

function navigateToDetail(product: Product) {
  openProductDetailById(product.id);
}

function goToPublish() {
  router.push({ name: 'AddProduct' });
}

async function fetchCategories() {
  try {
    const response = await fetch(`${API_BASE}/api/categories/getAllCategories`);
    if (!response.ok) {
      throw new Error('加载分类失败');
    }

    const result = await response.json();
    const list = Array.isArray(result) ? result : result?.items || result?.list || result?.data || [];
    categories.value = list.map((item: any) => ({
      id: item.id ?? item.categoryId ?? item.Id,
      name: item.name ?? item.categoryName ?? `分类${item.id ?? ''}`,
      parentId: item.parentId ?? null
    }));
  } catch (error) {
    console.warn('加载分类失败', error);
  } finally {
    categoriesLoaded.value = true;
  }
}

async function fetchProductsFind() {
  loading.value = true;
  errorMsg.value = '';

  try {
    let response: Response;

    if (nearbyRadius.value != null && userLat.value != null && userLng.value != null) {
      const params = new URLSearchParams();
      params.set('lat', String(userLat.value));
      params.set('lng', String(userLng.value));
      params.set('radiusKm', String(nearbyRadius.value));
      params.set('limit', '200');
      response = await fetch(`${API_BASE}/api/products/nearby?${params.toString()}`);
    } else {
      response = await fetch(`${API_BASE}/api/products/getAllProducts`);
    }

    if (!response.ok) {
      throw new Error('商品加载失败，请稍后重试');
    }

    const result = await response.json();
    products.value = Array.isArray(result) ? result : result?.data || [];
  } catch (error: any) {
    errorMsg.value = error?.message || '商品加载失败，请稍后重试';
    products.value = [];
  } finally {
    loading.value = false;
  }
}

function getCreatedAtTimestamp(value: Product['created_at']) {
  const timestamp = new Date(value ?? 0).getTime();
  return Number.isFinite(timestamp) ? timestamp : 0;
}

function formatPrice(value: Product['price']) {
  const amount = Number(value ?? 0);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function formatDistance(distance: Product['distanceKm']) {
  const numericDistance = Number(distance);
  if (!Number.isFinite(numericDistance)) {
    return '';
  }
  if (numericDistance < 1) {
    return `${Math.round(numericDistance * 1000)}m`;
  }
  return `${numericDistance.toFixed(1)}km`;
}

function formatLocation(value: Product['location']) {
  return value || '同城';
}

function formatSellerId(value: Product['seller_id']) {
  const normalized = String(value ?? '');
  if (!normalized) {
    return '卖家';
  }
  return normalized.length > 8 ? `${normalized.slice(0, 8)}...` : normalized;
}

function formatPublishTime(value: Product['created_at']) {
  const timestamp = getCreatedAtTimestamp(value);
  if (!timestamp) {
    return '刚刚更新';
  }

  const diff = Date.now() - timestamp;
  const oneHour = 60 * 60 * 1000;
  const oneDay = oneHour * 24;

  if (diff < oneHour) {
    const minutes = Math.max(1, Math.floor(diff / (60 * 1000)));
    return `${minutes} 分钟前`;
  }

  if (diff < oneDay) {
    return `${Math.max(1, Math.floor(diff / oneHour))} 小时前`;
  }

  return `${Math.max(1, Math.floor(diff / oneDay))} 天前`;
}

function handleImageError(event: Event, fallback: string) {
  const target = event.target as HTMLImageElement | null;
  if (target) {
    target.src = fallback;
  }
}

function getFirstImage(product: Product) {
  const images = product.image_urls ?? product.imageUrls;
  if (!images) {
    return FALLBACK_ITEM;
  }

  let imagePath = '';

  if (Array.isArray(images)) {
    imagePath = images[0] || '';
  } else if (typeof images === 'string') {
    const sanitized = images.trim().replace(/`/g, '');
    if (sanitized.startsWith('[')) {
      try {
        const parsed = JSON.parse(sanitized);
        if (Array.isArray(parsed) && parsed.length) {
          imagePath = parsed[0];
        }
      } catch {
        const matched = sanitized.match(/(https?:\/\/[^"'\\\]\s]+|\/[\w\-\/.]+)/);
        if (matched) {
          imagePath = matched[1];
        }
      }
    } else {
      imagePath = sanitized;
    }
  }

  if (!imagePath) {
    return FALLBACK_ITEM;
  }

  if (imagePath.startsWith('/')) {
    return `${API_BASE}${imagePath}`;
  }

  return imagePath;
}

function getSellerAvatar(product: Product) {
  const avatar = product.seller_avatar ?? product.sellerAvatar;
  if (!avatar) {
    return FALLBACK_AVATAR;
  }

  if (avatar.startsWith('/')) {
    return `${API_BASE}${avatar}`;
  }

  return avatar;
}

function productCategoryIds(product: Product) {
  return [product.category_id, product.categoryId, product.sub_category_id, product.subCategoryId]
    .filter(Boolean)
    .map((value) => String(value));
}

function isDown(product: Product) {
  const rawStatus = product.status ?? null;
  const stock = Number(product.stock_quantity ?? product.stockQuantity ?? 0);
  let downByStatus = false;

  if (rawStatus != null) {
    const normalized = String(rawStatus).trim();
    downByStatus = ['下架', '已下架', 'inactive', '2'].includes(normalized);
  }

  return downByStatus || stock <= 0;
}

function outOfChina(lng: number, lat: number) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(x: number, y: number) {
  let result =
    -100.0 +
    2.0 * x +
    3.0 * y +
    0.2 * y * y +
    0.1 * x * y +
    0.2 * Math.sqrt(Math.abs(x));
  result += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0;
  result += ((20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin((y / 3.0) * Math.PI)) * 2.0) / 3.0;
  result += ((160.0 * Math.sin((y / 12.0) * Math.PI) + 320.0 * Math.sin((y * Math.PI) / 30.0)) * 2.0) / 3.0;
  return result;
}

function transformLon(x: number, y: number) {
  let result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  result += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0;
  result += ((20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin((x / 3.0) * Math.PI)) * 2.0) / 3.0;
  result += ((150.0 * Math.sin((x / 12.0) * Math.PI) + 300.0 * Math.sin((x / 30.0) * Math.PI)) * 2.0) / 3.0;
  return result;
}

function wgs84ToGcj02(lng: number, lat: number): [number, number] {
  if (outOfChina(lng, lat)) {
    return [lng, lat];
  }

  const a = 6378245.0;
  const ee = 0.00669342162296594323;
  let deltaLat = transformLat(lng - 105.0, lat - 35.0);
  let deltaLng = transformLon(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1.0 - ee * magic * magic;
  const sqrtMagic = Math.sqrt(magic);

  deltaLat = (deltaLat * 180.0) / (((a * (1.0 - ee)) / (magic * sqrtMagic)) * Math.PI);
  deltaLng = (deltaLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI);

  return [lng + deltaLng, lat + deltaLat];
}

function requestLocation() {
  return new Promise<void>((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('no geo'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        let lat = position.coords.latitude;
        let lng = position.coords.longitude;
        let converted = false;

        if (AMAP_KEY) {
          try {
            const response = await fetch(
              `${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${lng},${lat}&coordsys=gps`
            );
            const result = await response.json();
            if (result.status === '1' && result.locations) {
              const parts = result.locations.split(',');
              if (parts.length === 2) {
                lng = Number(parts[0]);
                lat = Number(parts[1]);
                converted = true;
              }
            }
          } catch {
            converted = false;
          }
        }

        if (!converted) {
          const convertedCoords = wgs84ToGcj02(lng, lat);
          lng = convertedCoords[0];
          lat = convertedCoords[1];
        }

        userLng.value = lng;
        userLat.value = lat;
        resolve();
      },
      reject,
      {
        enableHighAccuracy: true,
        timeout: 8000
      }
    );
  });
}
</script>

<style scoped>
.find-page {
  --green: #28b75d;
  --green-deep: #1f8b48;
  --green-soft: #eef8f0;
  --bg: #f4f7f3;
  --surface: #ffffff;
  --surface-soft: #fbfcfa;
  --line: #e7eee3;
  --line-strong: #d8e4d3;
  --text-main: #1f2d21;
  --text-sub: #6a786c;
  min-height: 100vh;
  background: var(--bg);
  color: var(--text-main);
}

.find-shell {
  width: min(1440px, calc(100vw - 100px));
  margin: 0 auto;
  padding: 88px 24px 32px;
  display: grid;
  gap: 18px;
}

.surface-card {
  border-radius: 16px;
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: 0 10px 28px rgba(67, 96, 70, 0.05);
}

.search-hero,
.search-workbench,
.filters-panel,
.ai-entry-card,
.results-toolbar {
  padding: 20px;
}

.search-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 18px;
  align-items: center;
}

.section-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--green-deep);
  font-weight: 700;
}

.search-hero-copy h1,
.panel-head h2,
.results-summary h2 {
  margin: 10px 0 0;
  font-size: 30px;
  line-height: 1.18;
  color: #172419;
}

.search-hero-copy p:last-child,
.results-hint,
.ai-entry-text,
.ai-preview-box p,
.ai-preview-box small,
.state-box p {
  margin: 10px 0 0;
  color: var(--text-sub);
  line-height: 1.7;
}

.search-hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-stat-card {
  min-height: 118px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--surface-soft);
}

.hero-stat-card span,
.hero-stat-card strong,
.hero-stat-card small {
  display: block;
}

.hero-stat-card span {
  font-size: 13px;
  color: #6c7a6e;
}

.hero-stat-card strong {
  margin-top: 10px;
  font-size: 28px;
  line-height: 1;
  color: #1b2b1d;
}

.hero-stat-card small {
  margin-top: 8px;
  font-size: 12px;
  color: #8a968b;
  line-height: 1.6;
}

.search-workbench {
  display: grid;
  gap: 14px;
}

.search-bar-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 10px;
  align-items: center;
}

.search-input-wrap,
.location-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 52px;
  padding: 0 16px;
  border-radius: 14px;
  border: 1px solid var(--line-strong);
  background: var(--surface-soft);
}

.location-input {
  grid-template-columns: minmax(0, 1fr) auto;
  margin-top: 12px;
}

.search-input-wrap input,
.location-input input {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--text-main);
  font-size: 14px;
}

.search-input-wrap input:focus,
.location-input input:focus {
  outline: none;
}

.search-input-wrap:focus-within,
.location-input:focus-within {
  border-color: rgba(40, 183, 93, 0.28);
  box-shadow: 0 0 0 3px rgba(40, 183, 93, 0.1);
}

.search-icon {
  width: 20px;
  height: 20px;
  color: #7d8a7e;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.search-icon svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.primary-btn,
.ghost-btn,
.suggestion-btn,
.filter-chip,
.sort-btn,
.active-tag-btn,
.ai-quick-btn {
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.primary-btn,
.ghost-btn {
  min-height: 46px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}

.primary-btn {
  background: var(--green);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(40, 183, 93, 0.16);
}

.primary-btn:hover {
  transform: translateY(-1px);
}

.ghost-btn {
  border-color: var(--line-strong);
  background: #ffffff;
  color: #425144;
}

.ghost-btn:hover,
.suggestion-btn:hover,
.filter-chip:hover,
.sort-btn:hover,
.active-tag-btn:hover,
.ai-quick-btn:hover {
  border-color: #bfd0bf;
  background: #f7fbf6;
}

.ghost-btn.compact {
  min-height: 38px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 600;
}

.ghost-btn.accent,
.filter-chip.active,
.sort-btn.active,
.active-tag-btn,
.meta-chip.success {
  border-color: rgba(40, 183, 93, 0.22);
  background: var(--green-soft);
  color: var(--green-deep);
}

.keyword-suggestion-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.keyword-suggestion-row span,
.filter-label,
.filter-block-head span {
  font-size: 13px;
  color: #708072;
}

.suggestion-btn {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #ffffff;
  color: #526152;
  cursor: pointer;
  font-size: 13px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.filters-column {
  display: grid;
  gap: 18px;
  position: sticky;
  top: 88px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head.compact {
  margin-bottom: 10px;
}

.panel-count {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.filter-block {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.filter-block:first-of-type {
  margin-top: 16px;
}

.filter-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.filter-block-head strong,
.ai-preview-box strong,
.state-box strong {
  font-size: 16px;
  color: #203022;
}

.filter-chip-group,
.sort-group,
.active-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip,
.sort-btn,
.active-tag-btn {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #536254;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 13px;
}

.filter-chip.ghost {
  background: var(--surface-soft);
}

.filter-actions {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-quick-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.ai-quick-btn {
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #ffffff;
  color: #475648;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
}

.ai-preview-box {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--surface-soft);
}

.ai-preview-box p,
.ai-preview-box small {
  display: block;
}

.results-column {
  display: grid;
  gap: 16px;
}

.results-toolbar {
  display: grid;
  gap: 16px;
}

.results-toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.sort-arrow {
  margin-left: 4px;
  font-size: 12px;
}

.active-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
}

.active-tag-btn {
  gap: 6px;
}

.active-tag-btn strong {
  font-size: 13px;
}

.results-canvas {
  display: grid;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.goods-card {
  border-radius: 16px;
  border: 1px solid var(--line);
  background: #ffffff;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(69, 96, 72, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-2px);
  border-color: #cfdccd;
  box-shadow: 0 18px 32px rgba(69, 96, 72, 0.1);
}

.goods-media {
  position: relative;
  aspect-ratio: 1 / 0.88;
  background: #eff4ed;
}

.goods-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.goods-badge,
.goods-price-chip,
.goods-status,
.meta-chip {
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.goods-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(34, 45, 36, 0.82);
  color: #ffffff;
}

.goods-badge.muted {
  background: rgba(72, 80, 74, 0.85);
}

.goods-price-chip {
  position: absolute;
  right: 12px;
  bottom: 12px;
  background: rgba(255, 255, 255, 0.94);
  color: #eb5721;
  font-weight: 700;
}

.goods-body {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.goods-head {
  display: grid;
  gap: 8px;
}

.goods-head h3 {
  margin: 0;
  min-height: 46px;
  font-size: 16px;
  line-height: 1.45;
  color: #203022;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-head strong {
  font-size: 28px;
  line-height: 1;
  color: #ef5622;
}

.goods-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip {
  background: var(--surface-soft);
  color: #607061;
}

.seller-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.seller-info img {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  background: #edf2ea;
}

.seller-info span,
.goods-status {
  font-size: 12px;
  color: #728073;
}

.goods-status {
  background: var(--surface-soft);
}

.goods-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.state-box {
  min-height: 360px;
  border-radius: 16px;
  border: 1px dashed var(--line-strong);
  background: #fbfcfa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
}

.state-box.error {
  background: #fff8f8;
  border-color: #f2d7d7;
}

.spinner {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 3px solid #e0e8dc;
  border-top-color: var(--green);
  animation: spin 1s linear infinite;
}

.empty-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1440px) {
  .search-hero {
    grid-template-columns: 1fr;
  }

  .search-hero-stats,
  .results-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .filters-column {
    position: static;
  }

  .search-bar-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .find-shell {
    width: calc(100vw - 24px);
    padding: 78px 12px 24px;
  }

  .search-hero-stats,
  .results-grid,
  .goods-actions {
    grid-template-columns: 1fr;
  }

  .search-bar-row {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .results-toolbar-actions,
  .active-filter-bar,
  .seller-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
