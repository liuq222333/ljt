<template>
  <div class="nd-page">
    <dhstyle />

    <div class="page-shell">
      <CebianTool />

      <main class="page-main">
        <section class="page-hero">
          <img class="hero-bg" :src="heroBannerImage" alt="Community marketplace hero" />
          <div class="hero-overlay"></div>

          <div class="hero-inner">
            <div class="hero-copy">
              <p class="hero-kicker">COMMUNITY MARKETPLACE · AI WORKBENCH</p>
              <h1>先选模板，再让 AI 快速处理任务</h1>
              <p class="hero-description">
                保留 Nexthome 首页的视觉语气和沉浸感，把 AI 作为第一入口，市场内容作为下层辅助区。
              </p>
              <div class="hero-meta">
                <span class="meta-pill">Nexthome 风格</span>
                <span class="meta-pill">模板启动</span>
                <span class="meta-pill">桌面优先</span>
              </div>

              <div class="hero-actions">
                <button class="primary-btn" type="button" @click="continueLatestTask">继续上次任务</button>
                <button class="ghost-btn" type="button" @click="goToMarketSearch">浏览完整市场</button>
              </div>
            </div>

            <div class="hero-side">
              <article v-for="item in heroMetrics" :key="item.id" class="hero-metric">
                <span class="metric-dot"></span>
                <div>
                  <p>{{ item.label }}</p>
                  <strong>{{ item.value }}</strong>
                  <small>{{ item.helper }}</small>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="card-panel content-section market-section market-section-priority">
          <div class="section-heading">
            <div>
              <p class="eyebrow">精选资源与服务</p>
              <h2>先看商品，再进入 AI 处理</h2>
              <p class="section-subtitle">把你最关心的商品和服务提前展示，AI 工作台放在后面随时可用。</p>
            </div>
            <button class="ghost-btn" type="button" @click="goToMarketSearch">查看更多</button>
          </div>

          <div class="market-toolbar">
            <div class="search-control">
              <input
                v-model="marketKeyword"
                type="text"
                placeholder="搜索资源、服务或商品"
                @keyup.enter="goToMarketSearch"
              />
              <button class="text-btn" type="button" @click="goToMarketSearch">搜索</button>
            </div>

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
              <button type="button" :class="['radius-btn', { active: nearbyRadius === null }]" @click="clearNearby">
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
              <button class="text-btn" type="button" @click="locateByAddress">定位</button>
            </div>
          </div>

          <div class="market-ai-cta">
            <p>浏览商品时需要生成文案或运营建议？</p>
            <button class="primary-btn" type="button" @click="openWorkbenchDrawer">打开 AI 工作台</button>
          </div>

          <div v-if="loading" class="section-state">正在加载附近资源...</div>
          <div v-else-if="errorMsg" class="section-state error-state">{{ errorMsg }}</div>
          <div v-else-if="featuredProducts.length === 0" class="section-state empty-state">
            暂时没有可展示的资源，稍后再来看看。
          </div>
          <div v-else class="market-grid">
            <article
              v-for="product in featuredProducts"
              :key="product.id"
              class="market-card"
              @click="navigateToDetail(product)"
            >
              <div class="market-image">
                <img :src="getFirstImage(product)" :alt="product.title" @error="handleImageError($event, FALLBACK_ITEM)" />
                <span v-if="isDown(product)" class="status-badge">已下架</span>
              </div>

              <div class="market-card-body">
                <div class="market-card-head">
                  <h3 :title="product.title">{{ product.title }}</h3>
                  <span class="market-price">￥{{ formatPrice(product.price) }}</span>
                </div>

                <div class="seller-row">
                  <img :src="getSellerAvatar(product)" alt="seller" @error="handleImageError($event, FALLBACK_AVATAR)" />
                  <span>{{ formatSellerId(product.seller_id ?? product.sellerId) }}</span>
                </div>

                <div class="market-meta">
                  <span>{{ formatLocation(product.location ?? product.loaction) }}</span>
                  <span v-if="product.distanceKm != null" class="dist-tag">{{ formatDistance(product.distanceKm) }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>

      </main>

      <button class="workbench-fab" type="button" @click="toggleWorkbenchDrawer">
        {{ isWorkbenchDrawerOpen ? '收起 AI 工作台' : '打开 AI 工作台' }}
      </button>

      <transition name="drawer-fade">
        <div v-if="isWorkbenchDrawerOpen" class="workbench-drawer-mask" @click="closeWorkbenchDrawer"></div>
      </transition>
      <aside :class="['workbench-drawer', { open: isWorkbenchDrawerOpen }]">
        <header class="drawer-header">
          <div>
            <p class="eyebrow">AI WORKBENCH</p>
            <h3>Chat Dialog</h3>
          </div>
          <button class="ghost-btn" type="button" @click="closeWorkbenchDrawer">Close</button>
        </header>

        <div class="drawer-body">
          <section class="card-panel drawer-chat-card">
            <header class="chat-header">
              <div class="header-left">
                <h4>AI Assistant</h4>
                <span class="status-tag">{{ agentLoading ? 'Thinking' : 'Online' }}</span>
              </div>
              <p class="chat-subtitle">Ask directly and keep chatting. Messages are sent to the backend chat API.</p>
            </header>

            <div class="chat-window" ref="chatWindowRef">
              <div v-if="!agentMessages.length" class="chat-empty">
                Hi, I am your marketplace assistant. Ask about product info, copywriting, or operations.
              </div>
              <div v-else class="chat-messages">
                <div v-for="message in agentMessages" :key="message.id" :class="['chat-row', message.sender]">
                  <div class="chat-bubble">{{ message.text }}</div>
                  <div v-if="message.sender === 'agent' && message.cards?.length" class="chat-card-list">
                    <article
                      v-for="card in message.cards"
                      :key="`${message.id}-${card.entityId || card.title}`"
                      class="chat-result-card"
                      :class="{ clickable: canOpenAgentCard(card) }"
                      @click="handleAgentCardClick(card)"
                    >
                      <div class="chat-result-card-media">
                        <img
                          :src="card.imageUrl || FALLBACK_ITEM"
                          :alt="card.title || 'result card'"
                          @error="handleImageError($event, FALLBACK_ITEM)"
                        />
                      </div>
                      <div class="chat-result-card-body">
                        <div class="chat-result-card-head">
                          <h5>{{ card.title || '未命名商品' }}</h5>
                          <span v-if="card.priceText" class="chat-result-price">{{ card.priceText }}</span>
                        </div>
                        <p v-if="card.subtitle" class="chat-result-subtitle">{{ card.subtitle }}</p>
                        <div v-if="card.locationText || card.realtimeStatusText" class="chat-result-meta">
                          <span v-if="card.locationText">{{ card.locationText }}</span>
                          <span v-if="card.realtimeStatusText">{{ card.realtimeStatusText }}</span>
                        </div>
                        <p v-if="card.recommendReason" class="chat-result-reason">{{ card.recommendReason }}</p>
                        <div v-if="card.tags?.length" class="chat-result-tags">
                          <span v-for="tag in card.tags" :key="tag" class="chat-result-tag">{{ tag }}</span>
                        </div>
                        <ul v-if="card.highlights?.length" class="chat-result-highlights">
                          <li v-for="highlight in card.highlights" :key="highlight">{{ highlight }}</li>
                        </ul>
                      </div>
                    </article>
                  </div>
                  <span class="chat-time">{{ message.time }}</span>
                </div>
                <div v-if="agentLoading" class="chat-row agent">
                  <div class="chat-bubble loading">
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                  </div>
                </div>
              </div>
            </div>

            <p v-if="agentError" class="chat-error">{{ agentError }}</p>

            <div class="chat-input-area">
              <input
                ref="chatInputRef"
                v-model="agentInput"
                type="text"
                :disabled="agentLoading"
                placeholder="Type your question, for example: help me optimize this product title"
                @keyup.enter="submitDialogMessage"
              />
              <button class="primary-btn" type="button" :disabled="agentLoading || !agentInput.trim()" @click="submitDialogMessage">
                {{ agentLoading ? 'Sending...' : 'Send' }}
              </button>
            </div>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';
import CebianTool from './cebianTool.vue';
import homePicture1 from '../../../pictures/homePicture1.jpg';

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

interface AgentEntityCard {
  entityId?: string;
  entityType?: string;
  title?: string;
  subtitle?: string;
  imageUrl?: string;
  priceText?: string;
  tags?: string[];
  highlights?: string[];
  locationText?: string;
  realtimeStatusText?: string;
  recommendReason?: string;
  sourceLabel?: string;
}

interface AgentFinalAnswer {
  answerType?: string;
  answerText?: string;
  summary?: string;
  cards?: AgentEntityCard[];
}

interface AgentMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
  preview?: string;
  cards?: AgentEntityCard[];
  answerType?: string;
}

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
interface PlaybookHighlight {
  id: string;
  label: string;
  title: string;
  summary: string;
  actionLabel: string;
  templateId: string;
}

interface QuickAction {
  id: string;
  label: string;
  routeName: string;
}

interface OverviewCard {
  id: string;
  label: string;
  value: string;
  helper: string;
}

interface HeroMetric {
  id: string;
  label: string;
  value: string;
  helper: string;
}

const router = useRouter();
const route = useRoute();

const API_BASE = ((import.meta as any)?.env?.VITE_API_BASE ?? (window as any)?.VITE_API_BASE ?? 'http://localhost:8080') as string;
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '') as string;
const AGENT_CHAT_API = `${API_BASE}/api/agent/chat`;
const AGENT_SESSION_STORAGE_KEY = 'communityMarketplaceAgentSessionId';
const heroBannerImage = homePicture1;

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
    scenario: '适合社区活动、节日联动和周末主题活动。',
    placeholder: '例如：帮我策划一个周末社区亲子市集活动，预算 5000 元，包含摊位、互动和宣传安排。',
    promptPrefix: '请基于下面的需求输出结构化活动策划方案，包含目标、流程、分工、物料和风险提醒。',
    tags: ['活动方案', '时间排期', '执行清单']
  },
  {
    id: 'product-copy',
    categoryId: 'copy',
    title: '商品文案模板',
    summary: '生成可信、简洁、适合社区场景的商品文案。',
    scenario: '适合闲置转让、服务介绍和报名说明。',
    placeholder: '例如：帮我写一段闲置婴儿推车的转让文案，突出成色、使用次数和自提方式。',
    promptPrefix: '请根据下面的信息生成清晰可信的商品文案，并给出标题、卖点和详情说明。',
    tags: ['标题优化', '卖点提炼', '详情描述']
  },
  {
    id: 'poster-brief',
    categoryId: 'visual',
    title: '海报配图模板',
    summary: '整理视觉需求，让海报或配图制作更明确。',
    scenario: '适合活动海报、公告图、招募图和封面图。',
    placeholder: '例如：为社区公益义卖活动整理一份海报需求，风格温暖、信息清晰，适合线上转发。',
    promptPrefix: '请把下面的需求整理成清晰的海报创作 brief，包括主题、版式、主视觉和文案层级。',
    tags: ['海报 brief', '主视觉建议', '文案层级']
  },
  {
    id: 'ops-summary',
    categoryId: 'ops',
    title: '运营总结模板',
    summary: '把零散信息整理成汇报、复盘或跟进说明。',
    scenario: '适合周报、活动复盘、项目跟进和商户沟通。',
    placeholder: '例如：根据本周活动报名、到场、反馈和问题，整理一份运营复盘，突出亮点和下周改进项。',
    promptPrefix: '请把下面的信息整理成运营总结，输出亮点、问题、原因和下一步建议。',
    tags: ['复盘总结', '重点提炼', '下一步建议']
  },
  {
    id: 'data-cleanup',
    categoryId: 'data',
    title: '数据整理模板',
    summary: '把杂乱信息整理成结构化清单或分类结果。',
    scenario: '适合报名名单、资源清单、服务汇总和事项拆分。',
    placeholder: '例如：把 30 条报名信息整理成分组名单，并标出需要电话确认的人。',
    promptPrefix: '请把下面的信息整理成结构化清单，并给出适合继续处理的分类方式。',
    tags: ['结构化清单', '分类整理', '后续跟进']
  }
];

const playbookHighlights: PlaybookHighlight[] = [
  {
    id: 'highlight-campaign',
    label: '热门玩法',
    title: '活动策划先定流程再补细节',
    summary: '先把目标、节点和分工跑通，再让 AI 填充执行清单，效率更高。',
    actionLabel: '使用活动模板',
    templateId: 'campaign-plan'
  },
  {
    id: 'highlight-copy',
    label: '高频场景',
    title: '商品发布先写清成色和交付方式',
    summary: '把成色、使用次数、取货方式写明确，文案可信度会更高。',
    actionLabel: '使用商品文案',
    templateId: 'product-copy'
  },
  {
    id: 'highlight-ops',
    label: '常用建议',
    title: '复盘类任务先列事实，再补结论',
    summary: '先输入客观数据和反馈，AI 更容易输出有层次的总结。',
    actionLabel: '使用运营总结',
    templateId: 'ops-summary'
  }
];

const quickActions: QuickAction[] = [
  { id: 'publish', label: '发布资源', routeName: 'AddProduct' },
  { id: 'orders', label: '我的订单', routeName: 'MyOrder' },
  { id: 'inventory', label: '我的商品', routeName: 'MyProducts' }
];

const heroMetrics: HeroMetric[] = [
  { id: 'templates', label: '模板库', value: '5 个高频模板', helper: '覆盖文案、活动、运营和整理任务' },
  { id: 'route', label: '工作路径', value: '模板 -> 输入 -> 生成', helper: '流程对齐首页“先行动再扩展”逻辑' }
];

const toneOptions = [
  { value: 'professional', label: '专业稳重' },
  { value: 'friendly', label: '亲和自然' },
  { value: 'concise', label: '简洁直接' }
] as const;

const formatOptions = [
  { value: 'plan', label: '结构化方案' },
  { value: 'copy', label: '直接文案' },
  { value: 'steps', label: '执行步骤' }
] as const;

const lengthOptions = [
  { value: 'short', label: '简版' },
  { value: 'standard', label: '标准' },
  { value: 'long', label: '详细' }
] as const;

const nearbyOptions = [1, 3, 5] as const;
const FALLBACK_ITEM = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="220"><rect width="100%" height="100%" fill="%23f3f5f7"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%238894a0" font-size="16">暂无图片</text></svg>';
const FALLBACK_AVATAR = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36"><circle cx="18" cy="18" r="18" fill="%23e2e8f0"/><text x="18" y="23" text-anchor="middle" fill="%23576774" font-size="14" font-family="Arial">U</text></svg>';

const products = ref<Product[]>([]);
const loading = ref(false);
const errorMsg = ref('');
const marketKeyword = ref(String(route.query.keyword ?? ''));
const userLat = ref<number | null>(null);
const userLng = ref<number | null>(null);
const nearbyRadius = ref<number | null>(3);
const addr = ref('');

const selectedCategoryId = ref(templateCategories[0].id);
const selectedTemplateId = ref(templateCards[0].id);
const taskInput = ref(templateCards[0].placeholder);
const outputTone = ref<string>(toneOptions[0].value);
const outputFormat = ref<string>(formatOptions[0].value);
const outputLength = ref<string>(lengthOptions[0].value);
const taskInputRef = ref<HTMLTextAreaElement | null>(null);
const chatWindowRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<HTMLInputElement | null>(null);
const agentInput = ref('');
const isWorkbenchDrawerOpen = ref(false);

const agentLoading = ref(false);
const agentError = ref('');
const agentSessionId = ref(sessionStorage.getItem(AGENT_SESSION_STORAGE_KEY) || '');
const agentMessages = ref<AgentMessage[]>([
  {
    id: 'welcome',
    sender: 'agent',
    text: 'Hello, I am your community marketplace assistant. Ask me anything and I will call the backend to help.',
    time: formatChatTime()
  }
]);
const visibleTemplates = computed(() =>
  templateCards.filter((template) => template.categoryId === selectedCategoryId.value)
);

const activeTemplate = computed<TemplateCard>(() => {
  return templateCards.find((template) => template.id === selectedTemplateId.value) ?? templateCards[0];
});

const templateShowcase = computed(() => templateCards.slice(0, 4));
const featuredProducts = computed(() => products.value.slice(0, 6));

const latestUserMessage = computed(() => {
  return [...agentMessages.value].reverse().find((message) => message.sender === 'user') ?? null;
});

const latestAgentMessage = computed(() => {
  return [...agentMessages.value].reverse().find((message) => message.sender === 'agent') ?? null;
});

const latestUserPreview = computed(() => {
  return latestUserMessage.value ? summarizeText(latestUserMessage.value.preview ?? latestUserMessage.value.text, 54) : '';
});

const latestAgentReply = computed(() => latestAgentMessage.value?.text ?? '');
const latestAgentReplyPreview = computed(() => summarizeText(latestAgentReply.value, 120));
const latestAgentTime = computed(() => latestAgentMessage.value?.time ?? '刚刚');

const overviewCards = computed<OverviewCard[]>(() => [
  {
    id: 'recent-task',
    label: '最近任务',
    value: latestUserPreview.value || '暂无任务',
    helper: latestUserPreview.value ? '可以继续补充输入或直接再次生成。' : '先从上方选择模板开始。'
  },
  {
    id: 'processing',
    label: '当前状态',
    value: agentLoading.value ? 'AI 生成中' : '等待发起',
    helper: agentLoading.value ? '生成完成后会显示最近结果摘要。' : '模板和参数都可以随时切换。'
  },
  {
    id: 'recent-result',
    label: '最近结果',
    value: latestAgentReplyPreview.value || '暂无结果',
    helper: latestAgentReplyPreview.value ? '结果已保留在当前会话里，可继续追问。' : '生成完成后这里会展示摘要。'
  }
]);

watch(selectedCategoryId, (categoryId) => {
  const categoryTemplates = templateCards.filter((template) => template.categoryId === categoryId);
  if (!categoryTemplates.length) {
    return;
  }

  const stillVisible = categoryTemplates.some((template) => template.id === selectedTemplateId.value);
  if (!stillVisible) {
    selectedTemplateId.value = categoryTemplates[0].id;
    taskInput.value = categoryTemplates[0].placeholder;
  }
}, { immediate: true });

watch(
  () => route.query.keyword,
  (keyword) => {
    marketKeyword.value = String(keyword ?? '');
  }
);

watch(() => route.query, fetchProducts, { deep: true });

function scrollChatToBottom() {
  nextTick(() => {
    if (!chatWindowRef.value) {
      return;
    }
    chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
  });
}

function focusChatInput() {
  nextTick(() => {
    chatInputRef.value?.focus();
  });
}

function continueLatestTask() {
  openWorkbenchDrawer();
  if (latestUserMessage.value?.preview) {
    agentInput.value = latestUserMessage.value.preview;
  }
  focusChatInput();
}

function openWorkbenchDrawer() {
  isWorkbenchDrawerOpen.value = true;
  focusChatInput();
  scrollChatToBottom();
}

function closeWorkbenchDrawer() {
  isWorkbenchDrawerOpen.value = false;
}

function toggleWorkbenchDrawer() {
  if (isWorkbenchDrawerOpen.value) {
    closeWorkbenchDrawer();
    return;
  }

  openWorkbenchDrawer();
}

async function submitDialogMessage() {
  const text = agentInput.value.trim();
  if (!text || agentLoading.value) {
    return;
  }

  agentError.value = '';
  const now = new Date();
  agentMessages.value.push({
    id: `${now.getTime()}-user`,
    sender: 'user',
    text,
    preview: text,
    time: formatChatTime(now)
  });
  agentInput.value = '';
  scrollChatToBottom();

  await sendAgentMessage();
}

async function sendAgentMessage() {
  agentLoading.value = true;

  try {
    const token = localStorage.getItem('token') || '';
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) {
      headers.Authorization = token;
    }

    const agentRequest = {
      messages: agentMessages.value
        .filter((message) => message.id !== 'welcome')
        .map((message) => ({
          role: message.sender === 'user' ? 'user' : 'assistant',
          content: message.text
        })),
      sessionId: agentSessionId.value || undefined,
      userProfile: {
        latitude: userLat.value ?? undefined,
        longitude: userLng.value ?? undefined,
        nearbyRadiusKm: nearbyRadius.value ?? undefined,
        address: addr.value?.trim() || undefined
      }
    };

    const response = await fetch(AGENT_CHAT_API, {
      method: 'POST',
      headers,
      body: JSON.stringify(agentRequest)
    });

    let result: any = null;
    try {
      result = await response.json();
    } catch {
      result = null;
    }

    if (!response.ok || !result || result.code !== 200) {
      throw new Error(result?.message || 'Service returned an unexpected response');
    }

    const finalAnswer = (result?.data?.finalAnswer ?? null) as AgentFinalAnswer | null;
    const reply = result?.data?.reply?.trim() || finalAnswer?.answerText?.trim() || 'Received. Please provide more details if needed.';
    const returnedSessionId = result?.data?.sessionId?.trim?.() || '';
    if (returnedSessionId) {
      agentSessionId.value = returnedSessionId;
      sessionStorage.setItem(AGENT_SESSION_STORAGE_KEY, returnedSessionId);
    }

    agentMessages.value.push({
      id: `${Date.now()}-agent`,
      sender: 'agent',
      text: reply,
      preview: finalAnswer?.summary?.trim?.() || reply,
      time: formatChatTime(),
      cards: Array.isArray(finalAnswer?.cards) ? finalAnswer.cards : [],
      answerType: finalAnswer?.answerType
    });
    scrollChatToBottom();
  } catch (error: any) {
    agentError.value = error?.message || 'Network error. Please try again.';
    setTimeout(() => {
      agentError.value = '';
    }, 3000);
  } finally {
    agentLoading.value = false;
  }
}

function summarizeText(text: string, maxLength = 60) {
  const normalized = text.replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return '';
  }

  return normalized.length > maxLength ? `${normalized.slice(0, maxLength)}...` : normalized;
}

function formatChatTime(date = new Date()) {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatPrice(price: number | string) {
  const amount = Number(price);
  return Number.isFinite(amount) ? amount.toFixed(2) : '--';
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

function getSellerAvatar(product: Product) {
  const avatar = product.seller_avatar ?? product.sellerAvatar;
  if (!avatar) {
    return FALLBACK_AVATAR;
  }

  return avatar.startsWith('/') ? `${API_BASE}${avatar}` : avatar;
}

function formatSellerId(id?: number | string) {
  if (id == null) {
    return '社区用户';
  }

  const text = String(id);
  return text.length > 8 ? `${text.slice(0, 8)}...` : text;
}

function formatLocation(location?: string) {
  return location || '同城';
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

function navigateToDetail(product: Product) {
  openProductDetailById(product.id);
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

function canOpenAgentCard(card: AgentEntityCard) {
  return card.entityType === 'product' && !!card.entityId;
}

function handleAgentCardClick(card: AgentEntityCard) {
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
      const data = await response.json();
      list = unwrapList(data);
    } else {
      const fallbackResponse = await fetch(`${API_BASE}/api/products/getAllProducts`);
      if (fallbackResponse.ok) {
        list = unwrapList(await fallbackResponse.json()).slice(0, 12);
      }
    }

    products.value = list;
  } catch {
    errorMsg.value = '无法加载资源内容，请稍后重试';
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
.nd-page {
  --page-bg: #0a1016;
  --page-bg-soft: #121b26;
  --panel-bg: rgba(18, 27, 38, 0.9);
  --panel-bg-elevated: rgba(23, 34, 48, 0.96);
  --panel-border: rgba(255, 255, 255, 0.14);
  --panel-border-strong: rgba(255, 255, 255, 0.26);
  --text-main: #ecf2f9;
  --text-secondary: #b9c5d4;
  --text-muted: #8f9fb2;
  --brand-green: #1aa053;
  --accent: #ff7043;
  --accent-soft: rgba(255, 112, 67, 0.18);
  --accent-border: rgba(255, 112, 67, 0.55);
  --success-soft: rgba(26, 160, 83, 0.15);
  --danger-soft: rgba(255, 112, 112, 0.16);
  --surface-soft: rgba(255, 255, 255, 0.05);
  --shadow-soft: 0 16px 30px rgba(0, 0, 0, 0.34);

  min-height: 100vh;
  background: var(--page-bg);
  color: var(--text-main);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.page-shell {
  min-height: 100vh;
}

.page-main {
  width: min(1320px, calc(100vw - 110px));
  margin: 0 auto;
  padding: 88px 24px 54px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.workbench-fab {
  position: fixed;
  right: 92px;
  bottom: 28px;
  z-index: 1302;
}

.workbench-drawer-mask {
  position: fixed;
  inset: 70px 0 0;
  background: rgba(4, 7, 10, 0.54);
  z-index: 1300;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.24s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.workbench-drawer {
  position: fixed;
  top: 70px;
  right: 0;
  height: calc(100vh - 70px);
  width: min(920px, calc(100vw - 120px));
  border-left: 1px solid var(--panel-border-strong);
  background: rgba(13, 21, 30, 0.98);
  box-shadow: -14px 0 30px rgba(0, 0, 0, 0.42);
  transform: translateX(103%);
  transition: transform 0.26s ease;
  z-index: 1301;
  display: flex;
  flex-direction: column;
}

.workbench-drawer.open {
  transform: translateX(0);
}

.drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--panel-border);
  background: rgba(10, 16, 24, 0.96);
}

.drawer-header h3 {
  margin: 8px 0 0;
  font-size: 22px;
  line-height: 1.2;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-chat-card {
  height: 100%;
  min-height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--panel-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left h4 {
  margin: 0;
  font-size: 16px;
  color: var(--text-main);
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--panel-border);
  background: var(--success-soft);
  color: #8be0af;
  font-size: 12px;
}

.chat-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
}

.chat-empty {
  border: 1px dashed var(--panel-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  padding: 20px 14px;
  font-size: 14px;
  line-height: 1.7;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 86%;
}

.chat-row.user {
  margin-left: auto;
  align-items: flex-end;
}

.chat-row.agent {
  margin-right: auto;
  align-items: flex-start;
}

.chat-bubble {
  border-radius: 10px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.04);
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-row.user .chat-bubble {
  border-color: var(--accent-border);
  background: var(--accent-soft);
}

.chat-time {
  font-size: 11px;
  color: var(--text-muted);
}

.chat-card-list {
  display: grid;
  gap: 10px;
  width: 100%;
}

.chat-result-card {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
}

.chat-result-card.clickable {
  cursor: pointer;
}

.chat-result-card.clickable:hover {
  border-color: var(--accent-border);
  box-shadow: 0 0 0 1px rgba(255, 112, 67, 0.18);
}

.chat-result-card-media {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.06);
}

.chat-result-card-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.chat-result-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.chat-result-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.chat-result-card-head h5 {
  margin: 0;
  font-size: 15px;
  line-height: 1.45;
  color: var(--text-main);
}

.chat-result-price {
  flex-shrink: 0;
  color: #ffc5b1;
  font-size: 13px;
  font-weight: 700;
}

.chat-result-subtitle,
.chat-result-reason,
.chat-result-meta {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.chat-result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chat-result-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: #ffc7b4;
  font-size: 12px;
}

.chat-result-highlights {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.chat-bubble.loading {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-secondary);
  animation: typingPulse 1s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typingPulse {
  0%,
  80%,
  100% {
    transform: scale(0.72);
    opacity: 0.45;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-error {
  margin: 0 16px 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid rgba(255, 112, 112, 0.45);
  background: var(--danger-soft);
  color: #ffc9c9;
  font-size: 13px;
}

.chat-input-area {
  border-top: 1px solid var(--panel-border);
  padding: 12px 16px 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.chat-input-area input {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-main);
  padding: 0 12px;
  font-size: 14px;
}

.chat-input-area input::placeholder {
  color: var(--text-muted);
}

.page-hero {
  position: relative;
  min-height: 286px;
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
}

.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: rgba(7, 12, 18, 0.68);
}

.hero-inner {
  position: relative;
  z-index: 1;
  min-height: 286px;
  padding: 24px 28px;
  display: grid;
  grid-template-columns: 1.2fr 0.86fr;
  gap: 22px;
  align-items: end;
}

.hero-copy h1 {
  margin: 10px 0 0;
  font-size: 40px;
  line-height: 1.14;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #fff;
}

.hero-kicker {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
}

.hero-description {
  margin: 14px 0 0;
  max-width: 720px;
  font-size: 16px;
  line-height: 1.65;
  color: rgba(236, 242, 249, 0.9);
}

.hero-meta {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-side {
  display: grid;
  gap: 10px;
}

.hero-metric {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(10, 16, 22, 0.6);
}

.metric-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 7px;
  background: var(--brand-green);
  flex-shrink: 0;
}

.hero-metric p,
.hero-metric strong,
.hero-metric small {
  display: block;
}

.hero-metric p {
  margin: 0;
  font-size: 12px;
  color: rgba(236, 242, 249, 0.82);
}

.hero-metric strong {
  margin-top: 3px;
  font-size: 15px;
  line-height: 1.4;
  color: #fff;
}

.hero-metric small {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(236, 242, 249, 0.74);
}

.card-panel {
  background: var(--panel-bg-elevated);
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(6px);
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  line-height: 1;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--brand-green);
}

.section-top h2,
.composer-header h2,
.section-heading h2 {
  margin: 10px 0 0;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--text-main);
}

.section-subtitle,
.composer-description,
.note-card p,
.showcase-card p,
.quick-hint p,
.overview-helper {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.meta-pill,
.info-tag,
.radius-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.07);
  color: rgba(236, 242, 249, 0.9);
  font-size: 13px;
}

.composer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.primary-btn,
.ghost-btn,
.text-btn,
.quick-action,
.category-chip,
.template-card,
.workbench-select,
.task-input,
.search-control input,
.location-input input {
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.primary-btn,
.ghost-btn,
.text-btn,
.quick-action,
.category-chip,
.radius-btn {
  cursor: pointer;
}

.primary-btn,
.ghost-btn,
.quick-action {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

.primary-btn {
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #16120f;
}

.primary-btn:hover:not(:disabled) {
  background: #ff885f;
  border-color: #ff885f;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-btn,
.quick-action,
.category-chip,
.radius-btn,
.workbench-select,
.search-control input,
.location-input input,
.task-input,
.template-card,
.note-card,
.showcase-card,
.market-card {
  border: 1px solid var(--panel-border);
  background: var(--surface-soft);
}

.ghost-btn,
.text-btn {
  color: rgba(236, 242, 249, 0.94);
}

.ghost-btn:hover,
.quick-action:hover,
.category-chip:hover,
.radius-btn:hover,
.template-card:hover,
.text-btn:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.24);
}

.text-btn {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 14px;
  font-weight: 600;
}

.ai-workbench {
  display: grid;
  grid-template-columns: 0.96fr 1.34fr;
  gap: 20px;
  padding: 20px;
}

.template-column,
.composer-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-top,
.section-heading,
.composer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.template-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-chip {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  color: rgba(236, 242, 249, 0.86);
}

.category-chip.active,
.radius-btn.active,
.template-card.active,
.info-tag.muted {
  background: var(--accent-soft);
  border-color: var(--accent-border);
  color: #ffc2ad;
}

.template-list,
.template-side-notes,
.task-overview,
.showcase-grid,
.market-grid {
  display: grid;
  gap: 14px;
}

.template-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 16px;
  border-radius: 10px;
  text-align: left;
}

.template-head,
.showcase-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid var(--panel-border-strong);
  background: rgba(255, 255, 255, 0.08);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(236, 242, 249, 0.95);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.template-icon::before {
  content: 'AI';
}

.template-icon.tiny {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 10px;
}

.template-icon.icon-copy {
  border-color: rgba(26, 160, 83, 0.42);
  background: rgba(26, 160, 83, 0.2);
}
.template-icon.icon-copy::before {
  content: 'Aa';
}

.template-icon.icon-campaign {
  border-color: rgba(255, 112, 67, 0.48);
  background: rgba(255, 112, 67, 0.2);
}
.template-icon.icon-campaign::before {
  content: '策';
}

.template-icon.icon-visual {
  border-color: rgba(114, 184, 255, 0.48);
  background: rgba(114, 184, 255, 0.18);
}
.template-icon.icon-visual::before {
  content: '图';
}

.template-icon.icon-ops {
  border-color: rgba(255, 198, 82, 0.48);
  background: rgba(255, 198, 82, 0.2);
}
.template-icon.icon-ops::before {
  content: '运';
}

.template-icon.icon-data {
  border-color: rgba(191, 155, 255, 0.48);
  background: rgba(191, 155, 255, 0.2);
}
.template-icon.icon-data::before {
  content: '数';
}

.template-title,
.note-card h3,
.showcase-card h3,
.market-card-head h3,
.quick-panel h3 {
  font-size: 16px;
  line-height: 1.4;
  font-weight: 600;
  color: var(--text-main);
}

.template-summary,
.template-scenario,
.note-label,
.card-kicker,
.overview-label,
.field-label span,
.result-head span,
.market-meta,
.seller-row {
  font-size: 13px;
  color: rgba(236, 242, 249, 0.82);
}

.template-summary,
.template-scenario {
  line-height: 1.6;
}

.template-side-notes {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.note-card {
  padding: 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
}

.note-label,
.card-kicker {
  margin: 0 0 8px;
  color: var(--text-muted);
}

.note-card h3,
.showcase-card h3,
.quick-panel h3 {
  margin: 0;
}

.tag-row,
.showcase-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.info-tag {
  min-height: 28px;
  padding: 0 10px;
  font-size: 12px;
  color: #ffc5b1;
  background: var(--accent-soft);
  border-color: transparent;
}

.composer-column {
  padding: 4px;
}

.task-input {
  width: 100%;
  min-height: 220px;
  padding: 16px;
  border-radius: 10px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-main);
  background: rgba(8, 13, 19, 0.5);
  outline: none;
}

.task-input::placeholder,
.search-control input::placeholder,
.location-input input::placeholder {
  color: rgba(236, 242, 249, 0.45);
}

.parameter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workbench-select,
.search-control input,
.location-input input {
  min-height: 40px;
  width: 100%;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-main);
  background: rgba(8, 13, 19, 0.5);
  outline: none;
}

.inline-panel {
  min-height: 104px;
}

.inline-state {
  min-height: 104px;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.04);
}

.result-state {
  background: var(--success-soft);
  border-color: rgba(26, 160, 83, 0.36);
}
.error-state {
  background: var(--danger-soft);
  border-color: rgba(255, 112, 112, 0.42);
  color: #ffd7d7;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.task-overview {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.overview-card {
  padding: 18px;
  position: relative;
}

.overview-value {
  display: block;
  margin-top: 10px;
  font-size: 17px;
  line-height: 1.5;
  font-weight: 600;
  color: var(--text-main);
}

.overview-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 3px;
  background: var(--brand-green);
}

.content-section {
  padding: 20px;
}

.market-section-priority {
  border-color: rgba(26, 160, 83, 0.4);
}

.market-ai-cta {
  margin-top: 4px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid var(--panel-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.market-ai-cta p {
  margin: 0;
  font-size: 14px;
  color: rgba(236, 242, 249, 0.86);
}

.showcase-layout {
  display: grid;
  grid-template-columns: 1.45fr 0.75fr;
  gap: 16px;
  margin-top: 18px;
}

.showcase-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.showcase-card,
.quick-panel,
.market-card {
  padding: 16px;
  border-radius: 10px;
}

.quick-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.04);
}

.quick-action-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.quick-action {
  justify-content: flex-start;
  text-align: left;
}

.quick-hint {
  padding: 14px;
  border-radius: 8px;
  border: 1px solid var(--panel-border);
  background: rgba(8, 13, 19, 0.5);
}

.quick-hint strong {
  font-size: 14px;
  color: var(--text-main);
}

.market-toolbar {
  display: grid;
  grid-template-columns: 1.3fr auto 1fr;
  gap: 12px;
  margin-top: 18px;
  margin-bottom: 18px;
}

.search-control,
.location-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.radius-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.radius-btn {
  min-height: 40px;
  padding: 0 14px;
}

.section-state {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--panel-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
}

.market-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.market-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
}

.market-card:hover {
  border-color: var(--accent-border);
}

.market-image {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #111923;
}

.market-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  font-size: 12px;
}

.market-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.market-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.market-card-head h3 {
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.market-price {
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 700;
  color: #ffc5b1;
}

.seller-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.seller-row img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--panel-border);
}

.market-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dist-tag {
  color: #ffc8b5;
  background: var(--accent-soft);
  border-radius: 8px;
  padding: 4px 8px;
}

.task-input:focus,
.workbench-select:focus,
.search-control input:focus,
.location-input input:focus,
.chat-input-area input:focus,
.category-chip:focus-visible,
.template-card:focus-visible,
.radius-btn:focus-visible,
.primary-btn:focus-visible,
.ghost-btn:focus-visible,
.text-btn:focus-visible,
.quick-action:focus-visible {
  outline: none;
  border-color: var(--accent-border);
  box-shadow: 0 0 0 3px rgba(255, 112, 67, 0.22);
}

@media (max-width: 1480px) {
  .page-main {
    width: calc(100vw - 72px);
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .workbench-drawer {
    width: calc(100vw - 96px);
  }

  .workbench-fab {
    right: 84px;
  }
}

@media (max-width: 1320px) {
  .hero-inner {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .hero-side {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ai-workbench,
  .showcase-layout,
  .market-toolbar {
    grid-template-columns: 1fr;
  }

  .market-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-drawer {
    width: calc(100vw - 76px);
  }

  .workbench-drawer .task-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-drawer .template-side-notes {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1100px) {
  .section-top,
  .composer-header,
  .section-heading {
    flex-direction: column;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .hero-side {
    grid-template-columns: 1fr;
  }

  .template-side-notes,
  .task-overview,
  .showcase-grid,
  .parameter-grid,
  .market-grid {
    grid-template-columns: 1fr;
  }

  .workbench-drawer {
    width: 100vw;
  }

  .workbench-fab {
    right: 20px;
    bottom: 20px;
  }

  .market-ai-cta {
    flex-direction: column;
    align-items: flex-start;
  }

  .chat-result-card {
    grid-template-columns: 1fr;
  }

  .chat-result-card-media {
    width: 100%;
    height: 160px;
  }
}
</style>
