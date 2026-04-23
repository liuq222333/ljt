<template>
  <dhstyle />
  <div class="la-page" :class="{ 'ai-open': aiWorkbenchOpen }">
    <section class="la-hero">
      <div class="hero-content">
        <p class="eyebrow">社区活动 · 邻里共建</p>
        <h1>发现身边的精彩，和邻居一起创造温暖记忆</h1>
        <p class="subtitle">
          本地活动实时更新，志愿互助、亲子游园、技能共学一站查看，快速找到本周最适合你的社区活动。
        </p>

        <div class="search-panel">
          <div class="input-wrap">
            <i class="fas fa-search"></i>
            <input v-model="searchText" type="text" placeholder="搜索活动、地点或组织者" />
          </div>
          <div class="input-wrap">
            <i class="fas fa-map-marker-alt"></i>
            <input v-model="address" type="text" placeholder="定位或输入街区" />
          </div>
          <button class="primary hero-btn" @click="pinAddress">定位附近</button>
        </div>

        <div class="hero-bottom-row">
          <div class="hero-stats">
            <div class="stat-card">
              <span class="stat-label">本周活动</span>
              <strong>{{ events.length }}</strong>
              <small>已发布</small>
            </div>
            <div class="stat-card">
              <span class="stat-label">志愿时长</span>
              <strong>{{ totalVolunteerHours }}h</strong>
              <small>累计预约</small>
            </div>
            <div class="stat-card">
              <span class="stat-label">可报名名额</span>
              <strong>{{ totalOpenSlots }}</strong>
              <small>实时更新</small>
            </div>
          </div>

          <article class="hero-heat-card">
            <div class="hero-heat-header">
              <strong>活动热力图</strong>
              <button class="ghost xs" @click="refreshHotEvents">刷新</button>
            </div>
            <div class="hero-heat-visual">
              <span class="heat-dot heat-high"></span>
              <span class="heat-dot heat-mid"></span>
              <span class="heat-dot heat-low"></span>
            </div>
            <p class="hero-heat-desc">2km 热力分布 · 更新：{{ panelRefreshedLabel }}</p>
          </article>
        </div>
      </div>

      <aside class="hero-hot-card">
        <div class="hot-card-header">
          <h3>今日焦点</h3>
          <button class="ghost xs" @click="refreshHotEvents">刷新</button>
        </div>
        <ul class="hot-list">
          <li v-for="news in focusNews" :key="news.id">
            <span class="news-tag">{{ news.tag }}</span>
            <div class="hot-info">
              <p class="hot-title">{{ news.title }}</p>
              <p class="hot-meta">{{ news.time }}</p>
            </div>
          </li>
        </ul>
        <div class="hot-foot">
          <span>更新：{{ panelRefreshedLabel }}</span>
          <button class="text-link" @click="goToNotifications">全部动态 →</button>
        </div>
      </aside>
    </section>

    <section class="la-actions">
      <div class="action-buttons">
        <button class="pill pill-primary" @click="goToPublishAct"><i class="fas fa-plus"></i> 发布活动</button>
        <button class="pill" @click="goToRecurring"><i class="fas fa-calendar-week"></i> 创建固定日程</button>
        <button class="pill" @click="goToNotifications"><i class="fas fa-bullhorn"></i> 活动通知</button>
        <button class="pill" @click="goToNeighborSupport"><i class="fas fa-hand-holding-heart"></i> 邻里互助</button>
      </div>
      <span class="highlight-tip">
        <i class="fas fa-shield-alt"></i> 已接入活动审核、人数限制与报名提醒
      </span>
    </section>

    <section class="la-layout">
      <section class="feed-panel">
        <header class="feed-header">
          <div>
            <h3>活动推荐</h3>
            <p>参考你的位置和关注方向，按列表方式快速浏览活动内容</p>
          </div>
          <div class="feed-controls">
            <button class="ai-toggle-btn" :class="{ active: aiWorkbenchOpen }" @click="toggleAiWorkbench">
              <i class="fas fa-robot"></i>
              {{ aiWorkbenchOpen ? '收起 AI 工作台' : '打开 AI 工作台' }}
            </button>
            <select v-model="sortMode" class="feed-select">
              <option value="latest">最新发布</option>
              <option value="popular">报名最多</option>
              <option value="distance">距离最近</option>
            </select>
          </div>
        </header>

        <div class="feed-filter-row">
          <div class="filter-group">
            <span class="group-label">活动类型</span>
            <div class="filter-chips">
              <button
                v-for="cat in categories"
                :key="cat.id"
                :class="['chip', selectedCategory === cat.id ? 'active' : '']"
                @click="selectedCategory = cat.id"
              >
                {{ cat.label }}
              </button>
            </div>
          </div>

          <div class="filter-group">
            <span class="group-label">距离</span>
            <div class="filter-chips">
              <button
                v-for="distance in radiusOptions"
                :key="distance"
                :class="['chip', selectedRadius === distance ? 'active' : '']"
                @click="toggleRadius(distance)"
              >
                {{ distance }}km
              </button>
              <button class="chip ghost-btn" @click="selectedRadius = null">不限</button>
            </div>
          </div>

          <label class="checkbox">
            <input type="checkbox" v-model="showOnlyHot" />
            <span>仅看热门 / 名额紧张</span>
          </label>
        </div>

        <div class="feed-list">
          <article
            v-for="(event, index) in sortedEvents"
            :key="event.id"
            class="feed-item"
            :class="{ active: selectedEvent?.id === event.id }"
            @click="selectEvent(event)"
          >
            <div class="feed-main">
              <div class="feed-head">
                <div class="feed-source">
                  <span class="source-avatar">{{ event.organizer.charAt(0) }}</span>
                  <span class="source-name">{{ event.organizer }}</span>
                  <span v-if="event.highlight" class="source-hot">热门</span>
                </div>
                <span class="source-time">{{ formatFeedTime(index) }}</span>
              </div>
              <h4 class="feed-title">{{ event.title }}</h4>
              <p class="feed-summary">
                {{ event.location }} · {{ resolveCategoryLabel(event.category) }} · 报名 {{ event.reserved }}/{{
                  event.capacity
                }}
              </p>
              <button class="feed-link" @click.stop="goToDetail(event)">查看详情 ></button>
            </div>

            <div class="feed-thumb" :style="{ backgroundImage: `url(${event.cover})` }"></div>
          </article>

          <div v-if="sortedEvents.length === 0" class="empty-state">
            暂无匹配活动，请调整筛选条件后重试。
          </div>
        </div>
      </section>

      <aside class="sidebar">
        <section class="side-card detail-card" v-if="selectedEvent">
          <p class="card-eyebrow">当前选中</p>
          <h4>{{ selectedEvent.title }}</h4>
          <p class="detail-desc">
            {{ selectedEvent.description || '本活动由社区发起，支持报名提醒、活动通知与签到管理。' }}
          </p>
          <ul class="detail-list">
            <li><i class="fas fa-clock"></i>{{ selectedEvent.date }} · {{ selectedEvent.timeRange }}</li>
            <li><i class="fas fa-map-marker-alt"></i>{{ selectedEvent.location }}</li>
            <li><i class="fas fa-users"></i>剩余 {{ selectedEvent.capacity - selectedEvent.reserved }} 个名额</li>
            <li><i class="fas fa-shield-alt"></i>{{ selectedEvent.status }}</li>
          </ul>
          <div class="detail-actions">
            <button class="primary full" @click="goToDetail()">立即报名</button>
            <button class="ghost full" @click="goToNotifications">加入提醒</button>
          </div>
        </section>

        <section class="side-card topic-card">
          <div class="side-title-row">
            <h4>热门话题</h4>
          </div>
          <ul class="topic-list">
            <li v-for="topic in hotTopics" :key="topic.id">
              <span class="topic-dot"></span>
              <div>
                <p>{{ topic.title }}</p>
                <small>{{ topic.count }} 人关注</small>
              </div>
            </li>
          </ul>
        </section>

        <section class="side-card schedule-card">
          <div class="side-title-row">
            <h4>我的日程</h4>
            <button class="text-link" @click="goToMyEnrollments">查看全部</button>
          </div>
          <ul class="schedule-list">
            <li v-for="item in schedules" :key="item.id">
              <span class="time">{{ item.date }}</span>
              <div class="info">
                <p class="title">{{ item.title }}</p>
                <small>{{ item.location }}</small>
              </div>
              <span class="status">{{ item.status }}</span>
            </li>
          </ul>
        </section>
      </aside>
    </section>

    <aside class="ai-drawer" :class="{ open: aiWorkbenchOpen }">
      <header class="ai-drawer-header">
        <div>
          <p class="ai-eyebrow">AI 工作台</p>
          <h4>活动运营助手</h4>
        </div>
        <button class="ghost xs" @click="toggleAiWorkbench">{{ aiWorkbenchOpen ? '收起' : '展开' }}</button>
      </header>

      <div class="ai-drawer-body">
        <section class="ai-card ai-chat-card">
          <div class="ai-card-title">
            <h5>AI 对话框</h5>
            <small>{{ agentLoading ? '处理中' : '在线' }}</small>
          </div>
          <p class="ai-chat-subtitle">直接输入问题，系统会调用后端接口返回内容并保留当前会话上下文。</p>

          <div class="ai-chat-window" ref="chatWindowRef">
            <div v-if="!agentMessages.length" class="ai-chat-empty">
              你好，我是活动助手。可以问我：活动通知、报名转化、内容优化、复盘建议等问题。
            </div>
            <div v-else class="ai-chat-list">
              <div v-for="message in agentMessages" :key="message.id" :class="['ai-chat-item', message.sender]">
                <div class="ai-chat-bubble">{{ message.text }}</div>
                <span class="ai-chat-time">{{ message.time }}</span>
              </div>
              <div v-if="agentLoading" class="ai-chat-item agent">
                <div class="ai-chat-bubble loading">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </div>
              </div>
            </div>
          </div>

          <p v-if="agentError" class="ai-chat-error">{{ agentError }}</p>

          <div class="ai-chat-input">
            <input
              ref="chatInputRef"
              v-model="agentInput"
              type="text"
              :disabled="agentLoading"
              placeholder="例如：帮我写一条今晚活动的群通知，语气亲切一些"
              @keyup.enter="submitAiMessage"
            />
            <button class="primary ai-send-btn" :disabled="agentLoading || !agentInput.trim()" @click="submitAiMessage">
              {{ agentLoading ? '发送中...' : '发送' }}
            </button>
          </div>
        </section>
      </div>
    </aside>

    <button v-if="!aiWorkbenchOpen" class="ai-drawer-handle" @click="toggleAiWorkbench">
      AI 工作台
    </button>

    <section class="community-highlights">
      <div class="highlights-header">
        <h3>社区故事</h3>
        <p class="desc">记录活动瞬间与邻里互助时刻，增强社区凝聚力</p>
      </div>
      <div class="story-grid">
        <article v-for="story in stories" :key="story.id" class="story-card">
          <header>
            <h4>{{ story.title }}</h4>
            <span>{{ story.time }}</span>
          </header>
          <p>{{ story.summary }}</p>
          <footer>
            <div class="author">
              <div class="avatar-placeholder">{{ story.author.charAt(0) }}</div>
              <span>{{ story.author }}</span>
            </div>
            <button class="text-link" @click="goToStories">查看详情 →</button>
          </footer>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';
import { useRouter } from 'vue-router';

const router = useRouter();

type LocalEvent = {
  id: number;
  title: string;
  category: string;
  date: string;
  timeRange: string;
  location: string;
  distance: number;
  tags: string[];
  capacity: number;
  reserved: number;
  organizer: string;
  status: string;
  cover: string;
  highlight?: boolean;
  description: string;
};

type AgentMessage = {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const AGENT_CHAT_API = `${API_BASE}/api/agent/chat`;
const AGENT_SESSION_STORAGE_KEY = 'localActAgentSessionId';
const events = ref<LocalEvent[]>([]);

const categories = [
  { id: 'all', label: '全部类型' },
  { id: 'skill', label: '技能分享' },
  { id: 'eco', label: '环保行动' },
  { id: 'sport', label: '运动健康' },
  { id: 'kids', label: '儿童活动' },
  { id: 'market', label: '市集/展览' }
];

const radiusOptions = [1, 2, 3, 5];

const schedules = [
  { id: 's1', title: '秋季换书角志愿', date: '10/20 15:00', location: '溪语书屋', status: '待签到' },
  { id: 's2', title: '社区义诊问诊', date: '10/22 09:00', location: '邻里服务站', status: '已确认' },
  { id: 's3', title: '邻里共餐筹备', date: '10/25 18:30', location: '共享厨房', status: '待确认' }
];

const stories = [
  {
    id: 'story-1',
    title: '共建花园计划',
    time: '10/18',
    summary: '15 位居民自发认领花坛，种下 120 株花苗，社区绿化率持续上升。',
    author: '社区花园组'
  },
  {
    id: 'story-2',
    title: '“一小时”邻里互助',
    time: '10/16',
    summary: '志愿者轮班照料独居老人，提供上门维修、陪诊服务，累计 32 小时互助记录。',
    author: '互助小组'
  },
  {
    id: 'story-3',
    title: '亲子环保课堂',
    time: '10/15',
    summary: '20 组家庭参与垃圾分类互动剧，孩子们亲手制作了回收小物件带回家。',
    author: '青藤环保社'
  }
];

const hotTopics = [
  { id: 't1', title: '周末亲子共读营', count: 128 },
  { id: 't2', title: '旧物再利用市集', count: 96 },
  { id: 't3', title: '社区慢跑打卡', count: 84 },
  { id: 't4', title: '银龄互助计划', count: 76 }
];

const focusNews = [
  { id: 'n1', tag: '今日热点', title: '周末亲子共读营报名进入最后 12 席', time: '10:30 更新' },
  { id: 'n2', tag: '活动提醒', title: '旧物再利用市集今晚 20:00 截止报名', time: '09:48 更新' },
  { id: 'n3', tag: '组织招募', title: '社区慢跑打卡开放志愿者协助岗位', time: '09:10 更新' },
  { id: 'n4', tag: '通知', title: '银龄互助计划新增 2 个上门陪伴场次', time: '08:55 更新' }
];

const fallbackEvents: LocalEvent[] = [
  {
    id: 1001,
    title: '周末亲子共读营',
    category: 'kids',
    date: '2026-04-12',
    timeRange: '10:00 - 12:00',
    location: '溪语社区图书角',
    distance: 1.1,
    tags: ['亲子', '阅读'],
    capacity: 40,
    reserved: 28,
    organizer: '阅享小组',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=600&q=60',
    highlight: true,
    description: '以绘本阅读和互动分享为主，适合 4-10 岁亲子参与。'
  },
  {
    id: 1002,
    title: '旧物再利用市集',
    category: 'market',
    date: '2026-04-13',
    timeRange: '14:00 - 18:00',
    location: '邻里广场 A 区',
    distance: 0.8,
    tags: ['环保', '市集'],
    capacity: 60,
    reserved: 49,
    organizer: '绿色行动组',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=600&q=60',
    highlight: true,
    description: '鼓励居民交换闲置物品，减少浪费并促进邻里交流。'
  },
  {
    id: 1003,
    title: '社区慢跑打卡',
    category: 'sport',
    date: '2026-04-14',
    timeRange: '19:00 - 20:30',
    location: '滨河步道',
    distance: 2.3,
    tags: ['运动', '健康'],
    capacity: 50,
    reserved: 22,
    organizer: '晨风跑团',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=600&q=60',
    description: '轻松配速慢跑活动，含热身指导与拉伸教学。'
  },
  {
    id: 1004,
    title: '社区花园补种日',
    category: 'eco',
    date: '2026-04-15',
    timeRange: '09:30 - 11:30',
    location: '中心花园',
    distance: 1.6,
    tags: ['园艺', '志愿'],
    capacity: 30,
    reserved: 18,
    organizer: '花园共建组',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=600&q=60',
    description: '一起完成花圃补种与浇灌维护，提升社区绿化。'
  }
];

const searchText = ref('');
const address = ref('');
const selectedCategory = ref('all');
const selectedRadius = ref<number | null>(null);
const showOnlyHot = ref(false);
const selectedEvent = ref<LocalEvent | null>(null);
const sortMode = ref<'latest' | 'popular' | 'distance'>('latest');
const panelUpdatedAt = ref(new Date());
const aiWorkbenchOpen = ref(false);
const chatWindowRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<HTMLInputElement | null>(null);
const agentInput = ref('');
const agentLoading = ref(false);
const agentError = ref('');
const agentSessionId = ref(sessionStorage.getItem(AGENT_SESSION_STORAGE_KEY) || '');
const agentMessages = ref<AgentMessage[]>([
  {
    id: 'welcome',
    sender: 'agent',
    text: '你好，我是活动助手。你可以直接问我活动通知、报名转化、内容优化或复盘建议。',
    time: formatChatTime()
  }
]);

const filteredEvents = computed(() =>
  events.value.filter((event) => {
    if (selectedCategory.value !== 'all' && event.category !== selectedCategory.value) return false;
    if (selectedRadius.value != null && event.distance > selectedRadius.value) return false;
    if (showOnlyHot.value && !event.highlight) return false;
    if (!searchText.value.trim()) return true;

    const text = searchText.value.trim().toLowerCase();
    return (
      event.title.toLowerCase().includes(text) ||
      event.location.toLowerCase().includes(text) ||
      event.organizer.toLowerCase().includes(text)
    );
  })
);

const sortedEvents = computed(() => {
  const list = [...filteredEvents.value];
  if (sortMode.value === 'distance') {
    list.sort((a, b) => a.distance - b.distance);
  } else if (sortMode.value === 'popular') {
    list.sort((a, b) => b.reserved - a.reserved);
  } else {
    list.sort((a, b) => b.id - a.id);
  }
  return list;
});

const formatFeedTime = (index: number) => {
  const minuteSteps = [12, 34, 58, 120, 180, 260];
  const minutes = minuteSteps[index % minuteSteps.length];
  if (minutes < 60) return `${minutes}分钟前`;
  if (minutes < 24 * 60) return `${Math.floor(minutes / 60)}小时前`;
  return `${Math.floor(minutes / (24 * 60))}天前`;
};

const panelRefreshedLabel = computed(() =>
  panelUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
);

const totalParticipants = computed(() => events.value.reduce((sum, item) => sum + item.reserved, 0));
const totalVolunteerHours = computed(() => Math.round(events.value.length * 3.5));
const totalOpenSlots = computed(() =>
  events.value.reduce((sum, item) => sum + Math.max(item.capacity - item.reserved, 0), 0)
);

const resolveCategoryLabel = (id: string) => categories.find((c) => c.id === id)?.label ?? '社区活动';

const syncSelectedEvent = () => {
  if (events.value.length === 0) {
    selectedEvent.value = null;
    return;
  }
  if (!selectedEvent.value) {
    selectedEvent.value = events.value[0];
    return;
  }
  const stillExists = events.value.some((item) => item.id === selectedEvent.value?.id);
  if (!stillExists) {
    selectedEvent.value = events.value[0];
  }
};

const normalizeEvent = (item: any, idx: number): LocalEvent => ({
  id: item.id,
  title: item.title,
  category: item.category || 'all',
  date: item.startAt ? item.startAt.split(' ')[0] : '时间待定',
  timeRange:
    item.startAt && item.endAt
      ? `${item.startAt.split(' ')[1] ?? ''} - ${item.endAt.split(' ')[1] ?? ''}`
      : '时间待定',
  location: item.location || '地点待定',
  distance: item.distanceKm ?? Math.round((Math.random() * 3 + 0.5) * 10) / 10,
  tags: ['社区活动'],
  capacity: item.capacity || 0,
  reserved: Math.min(item.capacity || 0, Math.floor((item.capacity || 0) * 0.6)),
  organizer: '社区',
  status: item.status || 'PUBLISHED',
  cover:
    item.coverUrl ||
    'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=60',
  highlight: idx < 2,
  description: ''
});

const fetchEvents = async (lat = 23.1291, lon = 113.2644) => {
  const qs = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    radiusKm: selectedRadius.value ? String(selectedRadius.value) : '5',
    size: '20'
  });
  if (selectedCategory.value && selectedCategory.value !== 'all') {
    qs.set('category', selectedCategory.value);
  }
  if (searchText.value.trim()) {
    qs.set('keyword', searchText.value.trim());
  }

  try {
    const resp = await fetch(`${API_BASE}/api/local-act/activities/nearby?${qs.toString()}`);
    const data = await resp.json().catch(() => ({}));
    if (resp.ok && data?.code === 200 && Array.isArray(data?.data) && data.data.length > 0) {
      events.value = data.data.map((item: any, idx: number) => normalizeEvent(item, idx));
    } else {
      events.value = fallbackEvents;
    }
    syncSelectedEvent();
  } catch (error) {
    if (events.value.length === 0) {
      events.value = fallbackEvents;
      syncSelectedEvent();
    }
  }
};

const toggleRadius = (distance: number) => {
  selectedRadius.value = selectedRadius.value === distance ? null : distance;
};

const selectEvent = (event: LocalEvent) => {
  selectedEvent.value = event;
};

const toggleAiWorkbench = () => {
  aiWorkbenchOpen.value = !aiWorkbenchOpen.value;
  if (aiWorkbenchOpen.value) {
    focusChatInput();
    scrollChatToBottom();
  }
};

const focusChatInput = () => {
  nextTick(() => {
    chatInputRef.value?.focus();
  });
};

const scrollChatToBottom = () => {
  nextTick(() => {
    if (!chatWindowRef.value) return;
    chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
  });
};

const submitAiMessage = async () => {
  const text = agentInput.value.trim();
  if (!text || agentLoading.value) return;

  agentError.value = '';
  const now = new Date();
  agentMessages.value.push({
    id: `${now.getTime()}-user`,
    sender: 'user',
    text,
    time: formatChatTime(now)
  });
  agentInput.value = '';
  scrollChatToBottom();

  await sendAgentMessage();
};

const sendAgentMessage = async () => {
  agentLoading.value = true;

  try {
    const token = localStorage.getItem('token') || '';
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = token;

    const requestBody = {
      messages: agentMessages.value
        .filter((message) => message.id !== 'welcome')
        .map((message) => ({
          role: message.sender === 'user' ? 'user' : 'assistant',
          content: message.text
        })),
      sessionId: agentSessionId.value || undefined
    };

    const response = await fetch(AGENT_CHAT_API, {
      method: 'POST',
      headers,
      body: JSON.stringify(requestBody)
    });

    let result: any = null;
    try {
      result = await response.json();
    } catch {
      result = null;
    }

    if (!response.ok || !result || result.code !== 200) {
      throw new Error(result?.message || 'AI 服务响应异常');
    }

    const reply = result?.data?.reply?.trim() || '已收到，请补充更多活动信息。';
    const returnedSessionId = result?.data?.sessionId?.trim?.() || '';
    if (returnedSessionId) {
      agentSessionId.value = returnedSessionId;
      sessionStorage.setItem(AGENT_SESSION_STORAGE_KEY, returnedSessionId);
    }

    agentMessages.value.push({
      id: `${Date.now()}-agent`,
      sender: 'agent',
      text: reply,
      time: formatChatTime()
    });
    scrollChatToBottom();
  } catch (error: any) {
    agentError.value = error?.message || '网络异常，请稍后重试';
    setTimeout(() => {
      agentError.value = '';
    }, 3000);
  } finally {
    agentLoading.value = false;
  }
};

function formatChatTime(date = new Date()) {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

const refreshHotEvents = async () => {
  panelUpdatedAt.value = new Date();
  await fetchEvents();
};

const pinAddress = () => {
  if (!address.value) {
    address.value = '溪语社区 · 中央花园';
  }
};

const goToPublishAct = () => {
  router.push('/local-act/publish');
};

const goToRecurring = () => {
  router.push('/local-act/recurring');
};

const goToNotifications = () => {
  router.push('/local-act/notifications');
};

const goToNeighborSupport = () => {
  router.push('/local-act/neighbor-support');
};

const goToMyEnrollments = () => {
  router.push('/local-act/my-enrollments');
};

const goToStories = () => {
  router.push('/local-act/stories');
};

const goToDetail = (event?: LocalEvent | null) => {
  const target = event ?? selectedEvent.value;
  if (target) {
    router.push(`/local-act/${target.id}`);
  }
};

onMounted(() => {
  if (!navigator.geolocation) {
    fetchEvents();
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude, longitude } = pos.coords;
      fetchEvents(latitude, longitude);
    },
    () => fetchEvents(),
    { timeout: 3000 }
  );
});
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: #f3f3f3;
}

.la-page {
  --blue-900: #0f4b8b;
  --blue-700: #1e67b7;
  --blue-500: #3b82f6;
  --blue-100: #dbeeff;
  --green-600: #16a34a;
  --green-100: #e9fff1;
  --orange-500: #fb923c;
  --text-main: #14578f;
  --text-sub: #2f7cab;
  --text-soft: #5a9ebd;
  --line: #b8e4ff;
  --card: #ffffff;

  min-height: 100vh;
  background: #f3f3f3;
  color: var(--text-main);
  padding: 76px 38px 42px;
  box-sizing: border-box;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.la-hero {
  max-width: 1540px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.95fr);
  gap: 16px;
}

.hero-content {
  background:
    rgba(20, 82, 145, 0.44)
    url('https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=2000&q=80')
    center / cover
    no-repeat;
  border: 1px solid rgba(184, 228, 255, 0.6);
  border-radius: 12px;
  padding: 22px;
  color: #f7fcff;
  box-shadow: 0 12px 24px rgba(30, 103, 183, 0.2);
}

.eyebrow {
  margin: 0 0 8px;
  color: #cde9ff;
  font-size: 12px;
  letter-spacing: 0.08em;
  font-weight: 600;
}

.hero-content h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.24;
  font-weight: 800;
}

.subtitle {
  margin: 12px 0 16px;
  font-size: 14px;
  line-height: 1.5;
  color: rgba(247, 252, 255, 0.9);
}

.search-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 126px;
  gap: 10px;
  margin-bottom: 14px;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 10px;
}

.input-wrap i {
  font-size: 14px;
  color: rgba(247, 252, 255, 0.9);
}

.input-wrap input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  padding: 10px 0;
  font-size: 14px;
  color: #ffffff;
}

.input-wrap input::placeholder {
  color: rgba(247, 252, 255, 0.8);
}

.hero-bottom-row {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(220px, 0.75fr);
  gap: 10px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 10px;
  min-height: 84px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.stat-label {
  font-size: 11px;
  color: rgba(247, 252, 255, 0.88);
}

.stat-card strong {
  margin: 4px 0;
  font-size: 18px;
  color: #ffffff;
}

.stat-card small {
  font-size: 11px;
  color: rgba(247, 252, 255, 0.8);
}

.hero-heat-card {
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  min-height: 84px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-heat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-heat-header strong {
  font-size: 12px;
  color: #f8fbff;
}

.hero-heat-visual {
  position: relative;
  min-height: 64px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.16);
}

.heat-dot {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.14);
}

.heat-high {
  top: 24%;
  left: 64%;
  background: #fb923c;
}

.heat-mid {
  top: 56%;
  left: 35%;
  background: #22c55e;
}

.heat-low {
  top: 38%;
  left: 20%;
  background: #3b82f6;
}

.hero-heat-desc {
  margin: 0;
  font-size: 11px;
  line-height: 1.4;
  color: rgba(247, 252, 255, 0.9);
}

.hero-hot-card {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 12px 24px rgba(30, 103, 183, 0.12);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hot-card-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--blue-900);
}

.hot-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-list li {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 8px;
  border-radius: 10px;
  border: 1px solid #e1f1ff;
}

.hot-list li:hover {
  border-color: #9ad2ff;
  background: #f4fbff;
}

.news-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  font-size: 11px;
  color: #ffffff;
  background: #1e67b7;
  border-radius: 7px;
  margin-top: 2px;
}

.hot-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--blue-900);
}

.hot-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-soft);
  line-height: 1.4;
}

.hot-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-soft);
}

.la-actions {
  max-width: 1540px;
  margin: 12px auto 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.highlight-tip {
  margin-left: auto;
  background: #ffffff;
  border: 1px solid #dcdcdc;
  color: #666666;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 12px;
}

.la-layout {
  max-width: 1540px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 332px;
  gap: 16px;
  align-items: start;
}

.feed-panel,
.side-card,
.community-highlights {
  background: var(--card);
  border: 1px solid #ececec;
  border-radius: 12px;
  box-shadow: none;
}

.feed-panel {
  padding: 16px;
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.feed-header h3 {
  margin: 0;
  color: #222222;
  font-size: 18px;
}

.feed-header p {
  margin: 4px 0 0;
  color: #666666;
  font-size: 13px;
}

.feed-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feed-select {
  height: 34px;
  border: 1px solid #dfdfdf;
  border-radius: 8px;
  padding: 0 10px;
  color: #333333;
  background: #ffffff;
  outline: none;
}

.feed-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  align-items: flex-end;
  border-top: 1px solid #ececec;
  border-bottom: 1px solid #ececec;
  padding: 12px 0;
  margin-bottom: 10px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-label {
  font-size: 11px;
  color: var(--text-soft);
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid #dfdfdf;
  background: #ffffff;
  color: #444444;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.chip.active {
  border-color: #cfcfcf;
  background: #ffffff;
  color: #222222;
  font-weight: 600;
}

.chip.ghost-btn {
  background: #ffffff;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #555555;
  font-size: 12px;
  margin-left: auto;
  padding-bottom: 6px;
}

.checkbox input {
  width: 14px;
  height: 14px;
  accent-color: var(--blue-500);
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feed-item {
  border: 1px solid #ececec;
  border-radius: 12px;
  background: #ffffff;
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 226px;
  gap: 14px;
  align-items: center;
  cursor: pointer;
}

.feed-item:hover,
.feed-item.active {
  border-color: #d6d6d6;
  background: #ffffff;
}

.feed-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feed-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.feed-source {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.source-avatar {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #777777;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.source-name {
  font-size: 13px;
  color: #4b6ea1;
}

.source-hot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: #fb923c;
  color: #ffffff;
  font-size: 11px;
}

.source-time {
  flex-shrink: 0;
  color: #a0a0a0;
  font-size: 12px;
}

.feed-title {
  margin: 0;
  font-size: 18px;
  color: #111111;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.feed-summary {
  margin: 0;
  font-size: 12px;
  color: #666666;
  line-height: 1.45;
}

.feed-link {
  border: none;
  background: none;
  color: #9c9c9c;
  font-size: 12px;
  padding: 0;
  text-align: left;
  width: fit-content;
  cursor: pointer;
}

.feed-link:hover {
  color: #666666;
}

.feed-thumb {
  width: 226px;
  height: 112px;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  border: 1px solid #efefef;
}

.empty-state {
  border: 1px dashed #d8d8d8;
  border-radius: 10px;
  background: #ffffff;
  color: #666666;
  font-size: 13px;
  text-align: center;
  padding: 18px 10px;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card {
  padding: 14px;
}

.card-eyebrow {
  margin: 0;
  font-size: 11px;
  color: #9a9a9a;
}

.detail-card h4 {
  margin: 6px 0 8px;
  font-size: 16px;
  color: #222222;
}

.detail-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #666666;
  line-height: 1.5;
}

.detail-list {
  list-style: none;
  margin: 0 0 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-list li {
  font-size: 13px;
  color: #666666;
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-list i {
  color: #9c9c9c;
  width: 14px;
}

.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.side-title-row h4 {
  margin: 0;
  color: #222222;
  font-size: 15px;
}

.topic-list,
.schedule-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.topic-list li {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.topic-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b5b5b5;
  margin-top: 6px;
}

.topic-list p {
  margin: 0;
  color: #333333;
  font-size: 13px;
  line-height: 1.4;
}

.topic-list small {
  color: #9a9a9a;
  font-size: 12px;
}

.schedule-list li {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #ffffff;
  padding: 8px;
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) 50px;
  gap: 8px;
  align-items: center;
}

.schedule-list .time {
  font-size: 12px;
  color: #666666;
}

.schedule-list .title {
  margin: 0;
  font-size: 12px;
  color: #222222;
}

.schedule-list small {
  color: #9a9a9a;
  font-size: 11px;
}

.schedule-list .status {
  justify-self: end;
  font-size: 11px;
  color: #7f7f7f;
}

.community-highlights {
  max-width: 1540px;
  margin: 16px auto 0;
  padding: 16px;
}

.highlights-header {
  margin-bottom: 12px;
}

.highlights-header h3 {
  margin: 0;
  color: #222222;
  font-size: 17px;
}

.highlights-header .desc {
  margin: 4px 0 0;
  color: #666666;
  font-size: 13px;
}

.story-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.story-card {
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #ffffff;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.story-card header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.story-card h4 {
  margin: 0;
  color: #222222;
  font-size: 14px;
}

.story-card header span {
  font-size: 11px;
  color: #9a9a9a;
}

.story-card p {
  margin: 0;
  color: #666666;
  line-height: 1.5;
  font-size: 13px;
}

.story-card footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666666;
  font-size: 12px;
}

.avatar-placeholder {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #7a7a7a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.primary {
  border: none;
  background: #7faed9;
  color: #ffffff;
  border-radius: 10px;
  padding: 0 14px;
  height: 34px;
  font-size: 13px;
  cursor: pointer;
}

.primary:hover {
  background: #6fa1cf;
}

.hero-btn {
  height: 38px;
}

.pill {
  height: 34px;
  border: 1px solid #dcdcdc;
  border-radius: 10px;
  background: #ffffff;
  color: #444444;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
}

.pill:hover {
  border-color: #cccccc;
  color: #222222;
  background: #f7f7f7;
}

.pill-primary {
  background: #f4b889;
  border-color: #f4b889;
  color: #ffffff;
}

.pill-primary:hover {
  background: #eeab76;
  border-color: #eeab76;
  color: #ffffff;
}

.ghost {
  border: 1px solid #dcdcdc;
  background: #ffffff;
  color: #555555;
  border-radius: 8px;
  cursor: pointer;
}

.ghost:hover {
  border-color: #c8c8c8;
  color: #333333;
  background: #f8f8f8;
}

.ghost.xs {
  font-size: 11px;
  height: 26px;
  padding: 0 8px;
}

.full {
  width: 100%;
  justify-content: center;
}

.text-link {
  border: none;
  background: none;
  color: #666666;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.text-link:hover {
  color: #333333;
  text-decoration: underline;
}

.ai-toggle-btn {
  height: 34px;
  border: 1px solid #d7d7d7;
  background: #ffffff;
  color: #444444;
  border-radius: 8px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  cursor: pointer;
}

.ai-toggle-btn:hover {
  border-color: #c9c9c9;
  background: #f7f7f7;
}

.ai-toggle-btn.active {
  color: #222222;
  background: #f5f5f5;
  border-color: #cfcfcf;
}

.ai-drawer {
  position: fixed;
  top: 86px;
  right: 14px;
  width: 396px;
  height: calc(100vh - 104px);
  border: 1px solid #dcdcdc;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.12);
  z-index: 120;
  transform: translateX(calc(100% + 18px));
  transition: transform 0.22s ease;
  pointer-events: none;
  display: flex;
  flex-direction: column;
}

.ai-drawer.open {
  transform: translateX(0);
  pointer-events: auto;
}

.ai-drawer-header {
  border-bottom: 1px solid #e8e8e8;
  padding: 14px 14px 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.ai-eyebrow {
  margin: 0;
  font-size: 11px;
  color: #9a9a9a;
}

.ai-drawer-header h4 {
  margin: 4px 0 0;
  color: #222222;
  font-size: 16px;
}

.ai-drawer-body {
  flex: 1;
  padding: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-card {
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #ffffff;
  padding: 10px;
}

.ai-chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.ai-card-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.ai-card-title h5 {
  margin: 0;
  color: #222222;
  font-size: 13px;
}

.ai-card-title small {
  color: #9a9a9a;
  font-size: 11px;
}

.ai-chat-subtitle {
  margin: 0 0 8px;
  color: #7a7a7a;
  font-size: 12px;
  line-height: 1.45;
}

.ai-chat-window {
  flex: 1;
  border: 1px solid #ececec;
  border-radius: 8px;
  background: #fbfbfb;
  padding: 8px;
  overflow-y: auto;
}

.ai-chat-empty {
  color: #777777;
  font-size: 12px;
  line-height: 1.55;
  border: 1px dashed #e1e1e1;
  border-radius: 8px;
  padding: 12px 10px;
  background: #ffffff;
}

.ai-chat-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-chat-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-width: 88%;
}

.ai-chat-item.user {
  margin-left: auto;
  align-items: flex-end;
}

.ai-chat-item.agent {
  margin-right: auto;
  align-items: flex-start;
}

.ai-chat-bubble {
  border: 1px solid #dfdfdf;
  border-radius: 8px;
  background: #ffffff;
  color: #333333;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px 10px;
}

.ai-chat-item.user .ai-chat-bubble {
  border-color: #bfd4ea;
  background: #edf4fb;
}

.ai-chat-time {
  font-size: 11px;
  color: #a0a0a0;
}

.ai-chat-bubble.loading {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #97a4b2;
  animation: typing-pulse 1s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-pulse {
  0%,
  80%,
  100% {
    opacity: 0.4;
    transform: scale(0.72);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.ai-chat-error {
  margin: 8px 0 0;
  border: 1px solid #f1c7c7;
  border-radius: 8px;
  background: #fff4f4;
  color: #c54848;
  font-size: 12px;
  padding: 7px 9px;
}

.ai-chat-input {
  margin-top: 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.ai-chat-input input {
  border: 1px solid #d7d7d7;
  border-radius: 8px;
  background: #ffffff;
  color: #444444;
  font-size: 12px;
  padding: 0 10px;
  min-height: 34px;
  outline: none;
}

.ai-chat-input input:focus {
  border-color: #b6cce2;
  box-shadow: 0 0 0 2px rgba(140, 180, 219, 0.18);
}

.ai-chat-input input::placeholder {
  color: #a0a0a0;
}

.ai-send-btn {
  min-width: 72px;
}

.ai-action-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.ai-action-btn {
  border: 1px solid #dcdcdc;
  background: #ffffff;
  color: #444444;
  border-radius: 8px;
  min-height: 34px;
  padding: 0 8px;
  font-size: 12px;
  cursor: pointer;
}

.ai-action-btn:hover {
  border-color: #c7c7c7;
  background: #f8f8f8;
}

.ai-recommend-list,
.ai-insight-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-recommend-list li {
  border: 1px solid #ececec;
  border-radius: 8px;
  padding: 8px;
  background: #ffffff;
  cursor: pointer;
}

.ai-recommend-list li:hover {
  border-color: #d5d5d5;
  background: #fafafa;
}

.ai-recommend-list p {
  margin: 0;
  color: #222222;
  font-size: 12px;
  line-height: 1.4;
}

.ai-recommend-list small {
  color: #999999;
  font-size: 11px;
}

.ai-insight-list li {
  color: #666666;
  font-size: 12px;
  line-height: 1.45;
  padding-left: 10px;
  position: relative;
}

.ai-insight-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #b1b1b1;
}

.ai-prompt {
  width: 100%;
  min-height: 68px;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  padding: 8px;
  resize: vertical;
  box-sizing: border-box;
  font-size: 12px;
  color: #555555;
  background: #ffffff;
  outline: none;
}

.ai-generate-btn {
  width: 100%;
  margin-top: 8px;
}

.ai-result {
  margin-top: 8px;
  border: 1px solid #e4e4e4;
  border-radius: 8px;
  background: #ffffff;
  padding: 8px;
  color: #666666;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-line;
}

.ai-drawer-handle {
  position: fixed;
  right: 0;
  top: 45%;
  transform: translateY(-50%);
  border: none;
  background: #8d98a6;
  color: #ffffff;
  font-size: 12px;
  padding: 10px 8px;
  border-radius: 8px 0 0 8px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.08em;
  cursor: pointer;
  z-index: 110;
}

.ai-drawer-handle:hover {
  background: #7b8898;
}

@media (max-width: 1280px) {
  .la-page {
    padding: 70px 20px 32px;
  }

  .la-hero {
    grid-template-columns: 1fr;
  }

  .la-layout {
    grid-template-columns: 1fr;
  }

  .story-grid {
    grid-template-columns: 1fr;
  }

  .feed-header {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .feed-controls {
    width: 100%;
    justify-content: flex-start;
  }

  .feed-item {
    grid-template-columns: 1fr;
  }

  .feed-thumb {
    width: 100%;
    height: 140px;
  }

  .ai-drawer,
  .ai-drawer-handle {
    display: none;
  }
}
</style>
