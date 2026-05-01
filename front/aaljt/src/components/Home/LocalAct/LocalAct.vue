<template>
  <dhstyle />
  <div class="localact-page" :class="{ 'ai-open': aiWorkbenchOpen }">
    <div class="localact-shell">
      <section class="hero-banner">
        <div class="hero-media" :style="heroBackgroundStyle"></div>
        <div class="hero-shade"></div>

        <div class="hero-copy">
          <span class="hero-kicker">社区活动主场</span>
          <h1>让社区更有温度，<br />让生活更有链接</h1>
          <p class="hero-description">
            参与社区活动，分享邻里故事，连接志愿服务与互助行动，
            把身边正在发生的温暖事情集中在一个更轻松的入口里。
          </p>

          <div class="hero-actions">
            <button class="hero-btn primary" type="button" @click="scrollToSection('activity-hub')">
              探索活动
              <i class="fas fa-arrow-right"></i>
            </button>
            <button class="hero-btn secondary" type="button" @click="goToStories">
              了解更多
            </button>
            <button class="hero-btn tertiary ai-entry-btn" type="button" @click="openAiWorkbench()">
              <i class="fas fa-robot"></i>
              AI 助手
            </button>
          </div>
        </div>

        <article v-if="featuredEvent" class="hero-spotlight">
          <span class="hero-spotlight-badge">{{ resolveCategoryLabel(featuredEvent.category) }}</span>
          <h3>{{ featuredEvent.title }}</h3>
          <p>{{ formatEventDate(featuredEvent.date) }} {{ featuredEvent.timeRange }}</p>
          <p>{{ featuredEvent.location }} · {{ featuredEvent.organizer }}</p>
          <div class="hero-spotlight-stats">
            <div v-for="item in heroStats" :key="item.label" class="hero-stat-pill">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </article>
      </section>

      <section class="entry-grid">
        <button
          v-for="entry in landingEntries"
          :key="entry.id"
          type="button"
          class="entry-card"
          @click="handleTopNav(entry.id)"
        >
          <span class="entry-icon" :class="entry.tone">
            <i :class="['fas', entry.icon]"></i>
          </span>
          <div class="entry-content">
            <strong>{{ entry.title }}</strong>
            <p>{{ entry.desc }}</p>
          </div>
          <i class="fas fa-arrow-right entry-arrow"></i>
        </button>
      </section>

      <section class="page-grid">
        <div class="page-main">
          <section id="activity-hub" class="panel-card">
            <div class="section-head">
              <div>
                <h2>精选活动</h2>
                <p>保留你现有的活动数据，用更轻盈、克制的卡片方式呈现报名入口。</p>
              </div>
              <button class="section-link" type="button" @click="goToActivityList">
                查看更多
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>

            <div class="filter-bar">
              <div class="filter-group category-group">
                <span class="filter-label">活动分类</span>
                <div class="filter-chips">
                  <button
                    v-for="cat in categories"
                    :key="cat.id"
                    type="button"
                    :class="['filter-chip', { active: selectedCategory === cat.id }]"
                    @click="selectedCategory = cat.id"
                  >
                    {{ cat.label }}
                  </button>
                </div>
              </div>

              <div class="filter-group compact distance-group">
                <span class="filter-label">距离</span>
                <label class="distance-select-wrap">
                  <select
                    class="distance-select"
                    :value="selectedRadius == null ? '' : String(selectedRadius)"
                    @change="handleRadiusSelect(($event.target as HTMLSelectElement).value)"
                  >
                    <option value="">不限</option>
                    <option v-for="distance in radiusOptions" :key="distance" :value="String(distance)">
                      {{ distance }}km
                    </option>
                  </select>
                  <i class="fas fa-chevron-down"></i>
                </label>
              </div>

              <label class="hot-toggle">
                <input v-model="showOnlyHot" type="checkbox" />
                <span>只看热门</span>
              </label>
            </div>

            <div v-if="activityCards.length" class="activity-grid">
              <article
                v-for="event in activityCards"
                :key="event.id"
                class="activity-card"
                :class="{ selected: featuredEvent?.id === event.id }"
                :style="activityToneStyle(event)"
                @click="selectEvent(event)"
              >
                <div class="activity-cover" :style="{ backgroundImage: `url(${event.cover})` }">
                  <span class="activity-status">{{ statusLabel(event.status) }}</span>
                </div>
                <div class="activity-body">
                  <h3>{{ event.title }}</h3>
                  <div class="activity-meta">
                    <span><i class="far fa-calendar"></i>{{ formatEventDate(event.date) }} {{ event.timeRange }}</span>
                    <span><i class="fas fa-map-marker-alt"></i>{{ event.location }}</span>
                  </div>
                  <div class="activity-tags">
                    <span>{{ resolveCategoryLabel(event.category) }}</span>
                    <span>{{ formatDistance(event.distance) }}</span>
                    <span v-if="event.highlight">热门</span>
                  </div>
                  <div class="activity-footer">
                    <div class="activity-people">
                      <strong>{{ event.reserved }}</strong>
                      <span>人已报名</span>
                    </div>
                    <button class="activity-action" type="button" @click.stop="goToDetail(event)">
                      立即报名
                    </button>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">暂无匹配活动，请调整筛选条件后重试。</div>

            <div v-if="activityCards.length > 1" class="activity-indicators">
              <button
                v-for="event in activityCards"
                :key="event.id"
                type="button"
                :class="['indicator-dot', { active: featuredEvent?.id === event.id }]"
                @click="selectEvent(event)"
              ></button>
            </div>
          </section>

          <section id="story-hub" class="panel-card">
            <div class="section-head">
              <div>
                <h2>社区故事</h2>
                <p>把社区里的真实故事整理成更易阅读、留白更舒服的信息流。</p>
              </div>
              <button class="section-link" type="button" @click="goToStories">
                查看更多
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>

            <div class="story-grid">
              <article v-for="(story, index) in displayedStories" :key="story.id" class="story-card">
                <div class="story-cover" :style="{ backgroundImage: `url(${story.cover})` }"></div>
                <div class="story-body">
                  <h3>{{ story.title }}</h3>
                  <div class="story-meta">
                    <span>{{ story.author }}</span>
                    <span>阅读 {{ storyReads(index) }}</span>
                  </div>
                  <p>{{ story.summary }}</p>
                  <div class="story-footer">
                    <span class="story-badge">{{ storyBadge(index) }}</span>
                    <button class="story-link" type="button" @click="goToStories">阅读故事</button>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <section class="support-grid">
            <section class="panel-card compact-panel">
              <div class="section-head compact">
                <div>
                  <h2>社区公告</h2>
                  <p>把和活动有关的重要提醒收进一个更清晰的列表里。</p>
                </div>
              </div>

              <div class="notice-list">
                <button
                  v-for="notice in communityNotices"
                  :key="notice.id"
                  type="button"
                  class="notice-item"
                  @click="handleNoticeClick(notice.target)"
                >
                  <span class="notice-icon">
                    <i :class="['fas', notice.icon]"></i>
                  </span>
                  <div class="notice-content">
                    <strong>{{ notice.title }}</strong>
                    <p>{{ notice.desc }}</p>
                  </div>
                  <span class="notice-date">{{ notice.date }}</span>
                </button>
              </div>
            </section>

            <section class="panel-card compact-panel">
              <div class="section-head compact">
                <div>
                  <h2>热门话题</h2>
                  <p>让活动、公益和邻里讨论聚合在更轻松的入口中。</p>
                </div>
              </div>

              <div class="topic-cloud">
                <button
                  v-for="topic in hotTopics"
                  :key="topic.id"
                  type="button"
                  class="topic-pill"
                  @click="handleTopicClick(topic.id)"
                >
                  <span class="topic-hash"># {{ topic.label }}</span>
                  <span class="topic-count">{{ topic.count }} 讨论</span>
                </button>
              </div>
            </section>
          </section>
        </div>

        <aside class="page-side">
          <section id="signup-center" class="sidebar-card signup-card">
            <div class="sidebar-head">
              <div>
                <h3>活动报名中心</h3>
                <p>快速参与，传递温暖</p>
              </div>
              <span class="side-icon">
                <i class="far fa-calendar-check"></i>
              </span>
            </div>

            <div v-if="featuredEvent" class="signup-highlight">
              <h4>{{ featuredEvent.title }}</h4>
              <p>{{ formatEventDate(featuredEvent.date) }} {{ featuredEvent.timeRange }}</p>
              <p>{{ featuredEvent.location }}</p>
            </div>

            <div class="countdown-grid">
              <article v-for="item in countdownParts" :key="item.label">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
              </article>
            </div>

            <button class="cta-wide" type="button" @click="goToDetail(featuredEvent)">立即报名</button>
          </section>

          <section class="sidebar-card participation-card">
            <div class="sidebar-head">
              <div>
                <h3>我的参与</h3>
                <p>近期活跃情况</p>
              </div>
            </div>

            <div class="participation-top">
              <div class="score-ring" :style="participationRingStyle">
                <div class="score-ring-inner">
                  <strong>{{ participationScore }}%</strong>
                  <span>活跃度</span>
                </div>
              </div>

              <ul class="summary-list">
                <li v-for="item in participationSummary" :key="item.label">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </li>
              </ul>
            </div>

            <div class="schedule-overview">
              <article>
                <strong>{{ scheduleBuckets.upcoming }}</strong>
                <span>待开始</span>
              </article>
              <article>
                <strong>{{ scheduleBuckets.active }}</strong>
                <span>进行中</span>
              </article>
              <article>
                <strong>{{ scheduleBuckets.done }}</strong>
                <span>已完成</span>
              </article>
            </div>
          </section>

          <section class="sidebar-card ranking-card">
            <div class="sidebar-head">
              <div>
                <h3>邻里互助榜</h3>
                <p>本月热心榜单</p>
              </div>
              <button class="section-link" type="button" @click="goToNeighborSupport">
                查看更多
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>

            <div class="ranking-list">
              <article v-for="person in volunteerStars" :key="person.id" class="ranking-item">
                <span class="ranking-index">{{ person.rank }}</span>
                <div class="ranking-user">
                  <strong>{{ person.name }}</strong>
                  <span>{{ person.role }}</span>
                </div>
                <span class="ranking-value">{{ person.hours }}</span>
              </article>
            </div>
          </section>
        </aside>
      </section>

      <section id="community-connect" class="community-banner">
        <div class="community-copy">
          <span class="community-kicker">共建温暖社区</span>
          <h2>把故事、服务与互助，组织成一个更轻盈的社区入口</h2>
          <p>
            用更少的装饰、更多的留白，把活动参与、社区叙事和邻里协作放进同一套清晰的页面节奏里，
            让内容本身成为界面的主角。
          </p>

          <div class="community-actions">
            <button class="community-btn primary" type="button" @click="goToNeighborSupport">立即加入我们</button>

            <div class="member-cluster">
              <div class="member-stack">
                <span
                  v-for="member in joinMembers"
                  :key="member.id"
                  :class="['member-avatar', member.tone]"
                >
                  {{ member.name }}
                </span>
              </div>
              <span class="member-meta">已有 {{ joinMemberCount }} 位邻里加入共建</span>
            </div>
          </div>
        </div>

        <div class="community-visual">
          <div class="community-orb orb-large"></div>
          <div class="community-orb orb-small"></div>
          <div class="community-panel">
            <span>本周更新</span>
            <strong>{{ weeklyActivityCount }} 场社区活动持续开放</strong>
            <p>从报名入口到故事浏览，再到邻里互助，信息被放进了更统一也更耐看的页面层次中。</p>
          </div>
        </div>
      </section>

      <LocalActAiDrawer
        v-model="aiWorkbenchOpen"
        :messages="agentMessages"
        :input-value="agentInput"
        :loading="agentLoading"
        :error="agentError"
        @update:input-value="agentInput = $event"
        @send="submitAiMessage"
      />

      <button v-if="!aiWorkbenchOpen" class="ai-fab" type="button" @click="openAiWorkbench()">
        <i class="fas fa-robot"></i>
        <span>AI助手</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';
import LocalActAiDrawer from './LocalActAiDrawer.vue';

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

type LocalStory = {
  id: string;
  title: string;
  time: string;
  summary: string;
  author: string;
  cover: string;
};

type EntryCard = {
  id: string;
  title: string;
  desc: string;
  icon: string;
  tone: string;
};

type ScheduleItem = {
  id: string;
  title: string;
  date: string;
  location: string;
  state: 'upcoming' | 'active' | 'done';
};

type NoticeItem = {
  id: string;
  title: string;
  desc: string;
  date: string;
  icon: string;
  target: 'activity' | 'story' | 'support';
};

type TopicItem = {
  id: string;
  label: string;
  count: number;
};

type JoinMember = {
  id: string;
  name: string;
  tone: string;
};

type AgentMessage = {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
};

const router = useRouter();
const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const AGENT_CHAT_API = `${API_BASE}/api/agent/chat`;
const AGENT_SESSION_STORAGE_KEY = 'localActAgentSessionId';

const events = ref<LocalEvent[]>([]);
const selectedCategory = ref('all');
const selectedRadius = ref<number | null>(null);
const showOnlyHot = ref(false);
const selectedEvent = ref<LocalEvent | null>(null);
const sortMode = ref<'latest' | 'popular' | 'distance'>('popular');
const aiWorkbenchOpen = ref(false);
const agentInput = ref('');
const agentLoading = ref(false);
const agentError = ref('');
const agentSessionId = ref(sessionStorage.getItem(AGENT_SESSION_STORAGE_KEY) || '');
const agentMessages = ref<AgentMessage[]>([
  {
    id: 'welcome',
    sender: 'agent',
    text: '你好，我是活动助手。你可以问我活动通知、首页推荐文案、报名转化建议或复盘总结。',
    time: formatChatTime()
  }
]);

const landingEntries: EntryCard[] = [
  { id: 'stories', title: '社区故事', desc: '发现身边故事', icon: 'fa-book-open', tone: 'orange' },
  { id: 'activities', title: '社区活动', desc: '参与精彩活动', icon: 'fa-calendar-days', tone: 'violet' },
  { id: 'support', title: '志愿服务', desc: '贡献你的时间', icon: 'fa-heart', tone: 'blue' },
  { id: 'volunteer', title: '邻里互动', desc: '连接邻里关系', icon: 'fa-users', tone: 'green' },
  { id: 'welfare', title: '公益捐助', desc: '爱心传递希望', icon: 'fa-hand-holding-heart', tone: 'amber' }
];

const categories = [
  { id: 'all', label: '全部活动' },
  { id: 'market', label: '市集交换' },
  { id: 'kids', label: '亲子活动' },
  { id: 'eco', label: '志愿服务' },
  { id: 'skill', label: '邻里互助' },
  { id: 'sport', label: '运动健康' }
];

const radiusOptions = [1, 2, 3, 5];

const schedules: ScheduleItem[] = [
  { id: 's1', title: '旧物再利用市集', date: '04/24', location: '社区广场 A 区', state: 'upcoming' },
  { id: 's2', title: '亲子手工体验', date: '04/25', location: '社区活动室', state: 'active' },
  { id: 's3', title: '傍晚的那盏路灯', date: '04/21', location: '邻里走廊', state: 'done' }
];

const stories: LocalStory[] = [
  {
    id: 'story-1',
    title: '共建花园计划',
    time: '04/18',
    summary: '居民自发认领花坛、补种花苗，让公共空间重新恢复四季流动的色彩。',
    author: '社区花园组',
    cover: 'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1200&q=80'
  },
  {
    id: 'story-2',
    title: '社区食堂的温暖味道',
    time: '04/16',
    summary: '邻里围坐用餐、分享故事，食堂不只是就餐点，也成了晚间最热闹的社交角。',
    author: '共餐行动队',
    cover: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80'
  },
  {
    id: 'story-3',
    title: '傍晚的那盏路灯',
    time: '04/14',
    summary: '巡查志愿者与孩子们一起守护回家路，把“安全感”变成每天都能看到的风景。',
    author: '社区守护队',
    cover: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80'
  }
];

const volunteerStars = [
  { id: 'v1', rank: '1', name: '李阿姨', role: '旧物整理与陪伴服务', hours: '28h' },
  { id: 'v2', rank: '2', name: '周明', role: '活动引导与签到协助', hours: '22h' },
  { id: 'v3', rank: '3', name: '王悦', role: '亲子课堂志愿支持', hours: '19h' }
];

const communityNotices: NoticeItem[] = [
  {
    id: 'n1',
    title: '亲子手工体验报名将于本周六截止',
    desc: '周末场次名额紧张，建议尽早完成报名与签到确认。',
    date: '04-26',
    icon: 'fa-bullhorn',
    target: 'activity'
  },
  {
    id: 'n2',
    title: '旧物再利用市集开放志愿者补位',
    desc: '现场引导、签到与秩序协助仍有少量空位可报名。',
    date: '04-24',
    icon: 'fa-handshake',
    target: 'support'
  },
  {
    id: 'n3',
    title: '社区食堂故事专题已更新本周回顾',
    desc: '新增邻里共餐照片与志愿筹备记录，欢迎继续阅读。',
    date: '04-23',
    icon: 'fa-newspaper',
    target: 'story'
  }
];

const hotTopics: TopicItem[] = [
  { id: 'garden', label: '社区花园改造', count: 128 },
  { id: 'market', label: '旧物改造计划', count: 96 },
  { id: 'dinner', label: '邻里晚餐分享', count: 78 },
  { id: 'welfare', label: '公益助老行动', count: 65 }
];

const joinMembers: JoinMember[] = [
  { id: 'm1', name: '李', tone: 'warm' },
  { id: 'm2', name: '周', tone: 'violet' },
  { id: 'm3', name: '王', tone: 'blue' },
  { id: 'm4', name: '陈', tone: 'green' }
];

const fallbackEvents: LocalEvent[] = [
  {
    id: 2001,
    title: '旧物再利用市集',
    category: 'market',
    date: '2026-04-24',
    timeRange: '14:00 - 18:00',
    location: '社区广场 A 区',
    distance: 0.8,
    tags: ['旧物循环', '邻里交换'],
    capacity: 48,
    reserved: 32,
    organizer: '绿色行动组',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1400&q=80',
    highlight: true,
    description: '把闲置好物重新流动起来，让社区关系在交换和分享中慢慢升温。'
  },
  {
    id: 2002,
    title: '“一小时”邻里互助',
    category: 'skill',
    date: '2026-04-25',
    timeRange: '18:00 - 19:30',
    location: '社区公园步道',
    distance: 1.4,
    tags: ['陪伴散步', '邻里互助'],
    capacity: 24,
    reserved: 18,
    organizer: '互助小站',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=1400&q=80',
    highlight: true,
    description: '下班后一小时，陪伴邻里完成散步、代办、简单陪护，让互助更轻量也更可持续。'
  },
  {
    id: 2003,
    title: '亲子手工体验',
    category: 'kids',
    date: '2026-04-26',
    timeRange: '10:00 - 12:00',
    location: '社区活动室',
    distance: 1.1,
    tags: ['亲子陪伴', '手工课堂'],
    capacity: 36,
    reserved: 12,
    organizer: '青苗课堂',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1516627145497-ae6968895b74?auto=format&fit=crop&w=1400&q=80',
    highlight: false,
    description: '以拼贴和轻手工为主的周末亲子时光，帮助孩子在陪伴中建立表达和创作兴趣。'
  },
  {
    id: 2004,
    title: '社区慢跑打卡',
    category: 'sport',
    date: '2026-04-27',
    timeRange: '19:00 - 20:30',
    location: '滨河慢行道',
    distance: 2.2,
    tags: ['轻运动', '夜跑'],
    capacity: 30,
    reserved: 17,
    organizer: '晨风跑团',
    status: 'PUBLISHED',
    cover: 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1400&q=80',
    highlight: false,
    description: '适合初学者参与的社区夜跑，包含拉伸指导和热身陪跑。'
  }
];

const filteredEvents = computed(() =>
  events.value.filter((event) => {
    if (selectedCategory.value !== 'all' && event.category !== selectedCategory.value) return false;
    if (selectedRadius.value != null && event.distance > selectedRadius.value) return false;
    if (showOnlyHot.value && !event.highlight) return false;
    return true;
  })
);

const sortedEvents = computed(() => {
  const list = [...filteredEvents.value];
  if (sortMode.value === 'distance') {
    list.sort((a, b) => a.distance - b.distance);
  } else if (sortMode.value === 'latest') {
    list.sort((a, b) => b.id - a.id);
  } else {
    list.sort((a, b) => b.reserved - a.reserved);
  }
  return list;
});

const activityCards = computed(() => sortedEvents.value.slice(0, 3));
const displayedStories = computed(() => stories.slice(0, 3));
const featuredEvent = computed(() => selectedEvent.value ?? activityCards.value[0] ?? null);
const totalParticipants = computed(() => events.value.reduce((sum, item) => sum + item.reserved, 0));
const totalCapacity = computed(() => events.value.reduce((sum, item) => sum + item.capacity, 0));
const totalOpenSlots = computed(() =>
  events.value.reduce((sum, item) => sum + Math.max(item.capacity - item.reserved, 0), 0)
);
const hotEventCount = computed(() => events.value.filter((item) => item.highlight).length);
const participationScore = computed(() => Math.min(96, 35 + schedules.length * 4 + hotEventCount.value * 6 + volunteerStars.length * 2));
const participationRingStyle = computed(() => ({ '--score': String(participationScore.value) }));
const weeklyActivityCount = computed(() => events.value.length || fallbackEvents.length);
const baseParticipants = computed(
  () => totalParticipants.value || fallbackEvents.reduce((sum, item) => sum + item.reserved, 0)
);
const availableSlots = computed(
  () => totalOpenSlots.value || fallbackEvents.reduce((sum, item) => sum + Math.max(item.capacity - item.reserved, 0), 0)
);
const joinMemberCount = computed(() => Math.max(326, totalCapacity.value + volunteerStars.length * 28 + stories.length * 34));

const participationSummary = computed(() => [
  { label: '已报名活动', value: String(schedules.length + 1) },
  { label: '已完成服务', value: String(scheduleBuckets.value.done + 1) },
  { label: '帮助邻里', value: String(volunteerStars.length * 4 + 3) }
]);

const heroStats = computed(() => [
  { label: '本周活动', value: String(weeklyActivityCount.value) },
  { label: '报名人数', value: String(baseParticipants.value) },
  { label: '剩余名额', value: String(availableSlots.value) }
]);

const countdownParts = computed(() => {
  const start = featuredEvent.value ? parseEventStart(featuredEvent.value) : null;
  if (!start) {
    return [
      { label: '天后开始', value: '--' },
      { label: '小时', value: '--' },
      { label: '分钟', value: '--' }
    ];
  }

  const diff = Math.max(0, start.getTime() - Date.now());
  const totalMinutes = Math.floor(diff / 60000);
  const days = Math.floor(totalMinutes / (24 * 60));
  const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
  const minutes = totalMinutes % 60;

  return [
    { label: '天后开始', value: String(days) },
    { label: '小时', value: String(hours) },
    { label: '分钟', value: String(minutes) }
  ];
});

const scheduleBuckets = computed(() => ({
  upcoming: schedules.filter((item) => item.state === 'upcoming').length,
  active: schedules.filter((item) => item.state === 'active').length,
  done: schedules.filter((item) => item.state === 'done').length
}));

const heroBackgroundStyle = computed(() => {
  const image = featuredEvent.value?.cover || stories[1].cover;
  return { backgroundImage: `url(${image})` };
});

const resolveCategoryLabel = (id: string) => categories.find((item) => item.id === id)?.label ?? '社区活动';

const statusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    PUBLISHED: '报名中',
    DRAFT: '待发布',
    CLOSED: '已截止'
  };
  return labelMap[status] ?? status;
};

const formatDistance = (distance: number) => `${distance.toFixed(1)}km`;

const formatEventDate = (dateText: string) => {
  const date = new Date(`${dateText}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateText;
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
};

const parseEventStart = (event: LocalEvent) => {
  const date = new Date(`${event.date}T00:00:00`);
  if (Number.isNaN(date.getTime())) return null;

  const [startChunk] = event.timeRange.split('-');
  const match = startChunk?.trim().match(/(\d{1,2}):(\d{2})/);
  if (match) {
    date.setHours(Number(match[1]), Number(match[2]), 0, 0);
  }
  return date;
};

const remainingSlots = (event: LocalEvent) => Math.max(event.capacity - event.reserved, 0);

const activityToneStyle = (event: LocalEvent) => {
  const toneMap: Record<string, { accent: string; soft: string; border: string }> = {
    market: { accent: '#ff7f42', soft: 'rgba(255, 127, 66, 0.12)', border: 'rgba(255, 127, 66, 0.28)' },
    kids: { accent: '#6f6dff', soft: 'rgba(111, 109, 255, 0.12)', border: 'rgba(111, 109, 255, 0.24)' },
    eco: { accent: '#38b982', soft: 'rgba(56, 185, 130, 0.12)', border: 'rgba(56, 185, 130, 0.24)' },
    skill: { accent: '#4e8ef7', soft: 'rgba(78, 142, 247, 0.12)', border: 'rgba(78, 142, 247, 0.24)' },
    sport: { accent: '#f2a532', soft: 'rgba(242, 165, 50, 0.12)', border: 'rgba(242, 165, 50, 0.24)' }
  };

  const tone = toneMap[event.category] ?? toneMap.market;
  return {
    '--accent': tone.accent,
    '--accent-soft': tone.soft,
    '--accent-border': tone.border
  };
};

const storyReads = (index: number) => [156, 89, 64][index] ?? 48;
const storyBadge = (index: number) => ['环境共建', '邻里互动', '社区守护'][index] ?? '社区故事';

const syncSelectedEvent = (list: LocalEvent[]) => {
  if (!list.length) {
    selectedEvent.value = null;
    return;
  }
  if (!selectedEvent.value || !list.some((item) => item.id === selectedEvent.value?.id)) {
    selectedEvent.value = list[0];
  }
};

const normalizeEvent = (item: any, idx: number): LocalEvent => ({
  id: Number(item.id ?? idx + 1),
  title: item.title || `社区活动 ${idx + 1}`,
  category: item.category || 'market',
  date: item.startAt ? String(item.startAt).split(' ')[0] : fallbackEvents[idx % fallbackEvents.length].date,
  timeRange:
    item.startAt && item.endAt
      ? `${String(item.startAt).split(' ')[1] ?? ''} - ${String(item.endAt).split(' ')[1] ?? ''}`
      : fallbackEvents[idx % fallbackEvents.length].timeRange,
  location: item.location || '地点待定',
  distance: item.distanceKm ?? Math.round((Math.random() * 3 + 0.6) * 10) / 10,
  tags: Array.isArray(item.tags) && item.tags.length ? item.tags : ['社区活动'],
  capacity: Number(item.capacity || 0),
  reserved: Math.min(Number(item.capacity || 0), Math.floor(Number(item.capacity || 0) * 0.6)),
  organizer: item.organizer || '社区组织',
  status: item.status || 'PUBLISHED',
  cover: item.coverUrl || fallbackEvents[idx % fallbackEvents.length].cover,
  highlight: idx < 2,
  description: item.description || ''
});

const fetchEvents = async (lat = 23.1291, lon = 113.2644) => {
  const qs = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    radiusKm: selectedRadius.value ? String(selectedRadius.value) : '5',
    size: '20'
  });

  if (selectedCategory.value !== 'all') {
    qs.set('category', selectedCategory.value);
  }

  try {
    const resp = await fetch(`${API_BASE}/api/local-act/activities/nearby?${qs.toString()}`);
    const data = await resp.json().catch(() => ({}));
    if (resp.ok && data?.code === 200 && Array.isArray(data?.data) && data.data.length > 0) {
      events.value = data.data.map((item: any, idx: number) => normalizeEvent(item, idx));
      return;
    }
  } catch {
    // Fallback below.
  }

  events.value = fallbackEvents;
};

const handleRadiusSelect = (value: string) => {
  selectedRadius.value = value ? Number(value) : null;
};

const selectEvent = (event: LocalEvent) => {
  selectedEvent.value = event;
};

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleTopNav = (id: string) => {
  switch (id) {
    case 'stories':
      scrollToSection('story-hub');
      break;
    case 'activities':
      scrollToSection('activity-hub');
      break;
    case 'welfare':
      scrollToSection('community-connect');
      break;
    case 'support':
    case 'volunteer':
      goToNeighborSupport();
      break;
    default:
      break;
  }
};

const goToStories = () => {
  router.push('/local-act/stories');
};

const goToActivityList = () => {
  router.push('/local-act/list');
};

const goToNeighborSupport = () => {
  router.push('/local-act/neighbor-support');
};

const handleNoticeClick = (target: NoticeItem['target']) => {
  if (target === 'story') {
    goToStories();
    return;
  }

  if (target === 'support') {
    goToNeighborSupport();
    return;
  }

  scrollToSection('activity-hub');
};

const handleTopicClick = (id: string) => {
  if (id === 'welfare') {
    goToNeighborSupport();
    return;
  }

  if (id === 'market') {
    scrollToSection('activity-hub');
    return;
  }

  goToStories();
};

const openAiWorkbench = (seed = '') => {
  aiWorkbenchOpen.value = true;
  if (seed) {
    agentInput.value = seed;
  }
};

const submitAiMessage = async () => {
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
    time: formatChatTime(now)
  });
  agentInput.value = '';

  await sendAgentMessage();
};

const sendAgentMessage = async () => {
  agentLoading.value = true;

  try {
    const token = localStorage.getItem('token') || '';
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) {
      headers.Authorization = token;
    }

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
  } catch (error: any) {
    agentError.value = error?.message || '网络异常，请稍后重试';
    setTimeout(() => {
      if (agentError.value) {
        agentError.value = '';
      }
    }, 3200);
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

const goToDetail = (event?: LocalEvent | null) => {
  const target = event ?? featuredEvent.value;
  if (target) {
    router.push(`/local-act/${target.id}`);
  }
};

watch(sortedEvents, (list) => {
  syncSelectedEvent(list);
});

watch([selectedCategory, selectedRadius], () => {
  fetchEvents();
});

onMounted(() => {
  if (!navigator.geolocation) {
    fetchEvents();
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      fetchEvents(pos.coords.latitude, pos.coords.longitude);
    },
    () => fetchEvents(),
    { timeout: 3000 }
  );
});
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: #fafbfc;
}

.localact-page {
  --surface: #ffffff;
  --surface-soft: #f8fafc;
  --line: rgba(15, 23, 42, 0.05);
  --text: #0f172a;
  --text-2: #334155;
  --muted: #64748b;
  --muted-2: #94a3b8;
  --accent: #ff6b2c;
  --accent-strong: #f25a1b;
  --accent-soft: rgba(255, 107, 44, 0.08);
  --shadow-card: 0 1px 2px rgba(15, 23, 42, 0.04), 0 8px 24px rgba(15, 23, 42, 0.04);
  --shadow-hover: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 36px rgba(15, 23, 42, 0.08);
  min-height: 100vh;
  padding: 96px clamp(20px, 4vw, 56px) 80px;
  background: #fafbfc;
  color: var(--text);
  box-sizing: border-box;
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.localact-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.panel-card,
.sidebar-card {
  background: var(--surface);
  border-radius: 18px;
}

.hero-banner {
  position: relative;
  min-height: clamp(280px, 30vh, 340px);
  overflow: hidden;
  border-radius: 22px;
}

.hero-media,
.hero-shade {
  position: absolute;
  inset: 0;
}

.hero-media {
  background-position: center;
  background-size: cover;
  transform: scale(1.01);
}

.hero-shade {
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.72) 0%, rgba(15, 23, 42, 0.5) 38%, rgba(15, 23, 42, 0.18) 70%, rgba(15, 23, 42, 0.04) 100%);
}

.hero-copy,
.hero-spotlight {
  position: relative;
  z-index: 1;
}

.hero-copy {
  max-width: 560px;
  padding: 44px 48px;
  color: #ffffff;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.04em;
  backdrop-filter: blur(12px);
}

.hero-copy h1 {
  margin: 18px 0 0;
  font-size: clamp(28px, 3.2vw, 42px);
  line-height: 1.15;
  letter-spacing: -0.03em;
  font-weight: 600;
}

.hero-description {
  margin: 14px 0 0;
  max-width: 480px;
  font-size: 14.5px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.82);
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 26px;
}

.hero-btn {
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.2s ease, background 0.2s ease;
}

.hero-btn:hover {
  transform: translateY(-1px);
}

.hero-btn.primary {
  background: var(--accent);
  color: #ffffff;
}

.hero-btn.primary:hover {
  background: var(--accent-strong);
}

.hero-btn.secondary {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  backdrop-filter: blur(8px);
}

.hero-btn.secondary:hover {
  background: rgba(255, 255, 255, 0.22);
}

.hero-btn.tertiary {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  backdrop-filter: blur(8px);
}

.hero-btn.tertiary:hover {
  background: rgba(255, 255, 255, 0.22);
}

.hero-spotlight {
  position: absolute;
  right: 28px;
  bottom: 28px;
  width: min(300px, calc(100% - 56px));
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(14px);
}

.hero-spotlight-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 11.5px;
  font-weight: 500;
}

.hero-spotlight h3 {
  margin: 12px 0 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text);
}

.hero-spotlight p {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}

.hero-spotlight-stats {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-stat-pill {
  text-align: left;
}

.hero-stat-pill strong {
  display: block;
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}

.hero-stat-pill span {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--muted-2);
}

.entry-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.entry-card {
  position: relative;
  border: none;
  background: var(--surface);
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.entry-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.entry-card:hover .entry-arrow {
  opacity: 1;
  transform: translateX(2px);
}

.entry-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.entry-icon.orange {
  background: rgba(255, 107, 44, 0.1);
  color: #ff6b2c;
}

.entry-icon.violet {
  background: rgba(117, 99, 255, 0.1);
  color: #7563ff;
}

.entry-icon.blue {
  background: rgba(78, 142, 247, 0.1);
  color: #4e8ef7;
}

.entry-icon.green {
  background: rgba(56, 185, 130, 0.1);
  color: #38b982;
}

.entry-icon.amber {
  background: rgba(242, 155, 34, 0.1);
  color: #f29b22;
}

.entry-content {
  min-width: 0;
  flex: 1;
}

.entry-content strong {
  display: block;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text);
}

.entry-content p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--muted-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-arrow {
  font-size: 11px;
  color: var(--muted-2);
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-grid {
  margin-top: 32px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.page-main,
.page-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel-card,
.sidebar-card {
  border-radius: 18px;
  padding: 28px;
}

.section-head,
.sidebar-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-head h2,
.sidebar-head h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--text);
}

.section-head p,
.sidebar-head p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--muted);
}

.section-head.compact h2 {
  font-size: 18px;
}

.section-link {
  flex-shrink: 0;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: color 0.18s ease, background 0.18s ease;
}

.section-link:hover {
  color: var(--accent);
  background: var(--accent-soft);
}

.filter-bar {
  margin-top: 22px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px auto;
  gap: 20px;
  align-items: end;
  padding: 0;
  background: transparent;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.filter-label {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--muted-2);
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-group .filter-chips {
  flex-wrap: nowrap;
  overflow-x: auto;
  gap: 8px;
  padding-bottom: 2px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.category-group .filter-chips::-webkit-scrollbar {
  display: none;
}

.category-group .filter-chip {
  flex: 0 0 auto;
  white-space: nowrap;
}

.filter-chip {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: none;
  background: var(--surface-soft);
  color: var(--text-2);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.filter-chip:hover {
  background: #eef2f6;
}

.filter-chip.active {
  background: var(--text);
  color: #ffffff;
}

.distance-select-wrap {
  position: relative;
  display: block;
}

.distance-select-wrap i {
  position: absolute;
  top: 50%;
  right: 12px;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--muted-2);
  pointer-events: none;
}

.distance-select {
  width: 100%;
  height: 32px;
  padding: 0 30px 0 14px;
  border: none;
  border-radius: 999px;
  background: var(--surface-soft);
  color: var(--text-2);
  font-size: 13px;
  font-weight: 500;
  appearance: none;
  outline: none;
  cursor: pointer;
  transition: background 0.18s ease;
}

.distance-select:hover {
  background: #eef2f6;
}

.hot-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding-bottom: 2px;
  color: var(--text-2);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.hot-toggle input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent);
}

.activity-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.activity-card {
  border-radius: 14px;
  overflow: hidden;
  background: var(--surface);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.activity-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card);
}

.activity-card.selected {
  box-shadow: var(--shadow-hover);
}

.activity-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background-color: #f1f5f9;
  background-position: center;
  background-size: cover;
  overflow: hidden;
}

.activity-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(15, 23, 42, 0.18) 100%);
}

.activity-status {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--accent-strong);
  font-size: 11.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  backdrop-filter: blur(8px);
}

.activity-body {
  padding: 14px 16px 16px;
}

.activity-body h3 {
  margin: 0;
  font-size: 15.5px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-meta {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--muted-2);
}

.activity-meta i {
  width: 12px;
  text-align: center;
  font-size: 11px;
  color: #cbd5e1;
}

.activity-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.activity-tags span {
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--surface-soft);
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
}

.activity-tags span:last-child:not(:only-child) {
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.activity-footer {
  margin-top: 14px;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #f1f5f9;
}

.activity-people {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
}

.activity-people strong {
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
  color: var(--text);
}

.activity-people span {
  font-size: 11.5px;
  color: var(--muted-2);
}

.activity-action {
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  border: none;
  background: var(--accent);
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease;
}

.activity-action:hover {
  background: var(--accent-strong);
}

.activity-indicators {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  border: none;
  background: #cbd5e1;
  cursor: pointer;
  transition: width 0.2s ease, background 0.2s ease;
}

.indicator-dot.active {
  width: 18px;
  background: var(--accent);
}

.empty-state {
  margin-top: 24px;
  padding: 56px 24px;
  border-radius: 14px;
  text-align: center;
  font-size: 14px;
  color: var(--muted);
  background: var(--surface-soft);
}

.story-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.story-card {
  overflow: hidden;
  border-radius: 14px;
  background: var(--surface);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  cursor: pointer;
}

.story-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card);
}

.story-cover {
  aspect-ratio: 16 / 10;
  background-position: center;
  background-size: cover;
}

.story-body {
  padding: 16px 18px 18px;
}

.story-body h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: var(--text);
}

.story-meta {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--muted-2);
}

.story-body p {
  margin: 12px 0 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.story-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.story-badge {
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(117, 99, 255, 0.08);
  color: #7563ff;
  font-size: 11.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
}

.story-link {
  height: 28px;
  padding: 0 0;
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: gap 0.2s ease;
}

.story-link:hover {
  color: var(--accent-strong);
}

.support-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.compact-panel {
  padding: 26px 28px;
}

.notice-list {
  margin-top: 22px;
}

.notice-item {
  width: 100%;
  padding: 16px 0;
  border: none;
  border-top: 1px solid #f1f5f9;
  background: transparent;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease;
}

.notice-item:first-child {
  padding-top: 0;
  border-top: none;
}

.notice-item:hover .notice-content strong {
  color: var(--accent-strong);
}

.notice-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.notice-content strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.45;
  color: var(--text);
  transition: color 0.18s ease;
}

.notice-content p {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-date {
  font-size: 12px;
  color: var(--muted-2);
  white-space: nowrap;
  margin-top: 2px;
}

.topic-cloud {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.topic-pill {
  padding: 14px 16px;
  border-radius: 12px;
  border: none;
  background: var(--surface-soft);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease;
}

.topic-pill:hover {
  background: #eef2f6;
}

.topic-hash {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text);
}

.topic-count {
  font-size: 11.5px;
  color: var(--muted-2);
}

.side-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(78, 142, 247, 0.1);
  color: #4e8ef7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.signup-highlight {
  margin-top: 18px;
  padding: 18px;
  border-radius: 14px;
  background: var(--surface-soft);
}

.signup-highlight h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text);
}

.signup-highlight p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--muted);
}

.countdown-grid,
.schedule-overview {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.countdown-grid article,
.schedule-overview article {
  padding: 14px 8px;
  border-radius: 12px;
  border: none;
  text-align: center;
  background: var(--surface-soft);
}

.countdown-grid strong,
.schedule-overview strong {
  display: block;
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
  color: var(--text);
}

.countdown-grid span,
.schedule-overview span {
  display: block;
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--muted-2);
}

.cta-wide {
  width: 100%;
  height: 44px;
  margin-top: 18px;
  border: none;
  border-radius: 999px;
  background: var(--accent);
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.2s ease;
}

.cta-wide:hover {
  background: var(--accent-strong);
  transform: translateY(-1px);
}

.participation-top {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.score-ring {
  --score: 0;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: conic-gradient(
    var(--accent) 0deg,
    var(--accent) calc(var(--score) * 3.6deg),
    #f1f5f9 calc(var(--score) * 3.6deg),
    #f1f5f9 360deg
  );
}

.score-ring-inner {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.score-ring-inner strong {
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
  color: var(--text);
}

.score-ring-inner span {
  margin-top: 4px;
  font-size: 11px;
  color: var(--muted-2);
}

.summary-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-list li {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  font-size: 13px;
  color: var(--muted);
}

.summary-list strong {
  color: var(--text);
  font-size: 17px;
  font-weight: 600;
}

.ranking-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  background: transparent;
  border-bottom: 1px solid #f1f5f9;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-index {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--surface-soft);
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ranking-item:first-child .ranking-index {
  background: var(--accent);
  color: #ffffff;
}

.ranking-user strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.ranking-user span {
  display: block;
  margin-top: 3px;
  font-size: 11.5px;
  color: var(--muted-2);
}

.ranking-value {
  color: var(--accent);
  font-size: 13.5px;
  font-weight: 600;
}

.community-banner {
  margin-top: 32px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 0;
  overflow: hidden;
  border-radius: 22px;
  background: var(--surface);
}

.community-copy {
  padding: 40px 44px;
}

.community-kicker {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.community-copy h2 {
  margin: 16px 0 0;
  max-width: 560px;
  font-size: clamp(24px, 2.6vw, 32px);
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.025em;
  color: var(--text);
}

.community-copy p {
  margin: 14px 0 0;
  max-width: 560px;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--muted);
}

.community-actions {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.community-btn {
  height: 42px;
  padding: 0 22px;
  border: none;
  border-radius: 999px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.2s ease;
}

.community-btn.primary {
  background: var(--accent);
  color: #ffffff;
}

.community-btn.primary:hover {
  background: var(--accent-strong);
  transform: translateY(-1px);
}

.member-cluster {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.member-stack {
  display: flex;
  align-items: center;
}

.member-avatar {
  width: 32px;
  height: 32px;
  margin-left: -8px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}

.member-avatar:first-child {
  margin-left: 0;
}

.member-avatar.warm { background: #ffe4d5; }
.member-avatar.violet { background: #ece7ff; }
.member-avatar.blue { background: #e2efff; }
.member-avatar.green { background: #dcf4e9; }

.member-meta {
  font-size: 13px;
  color: var(--muted);
}

.community-visual {
  position: relative;
  min-height: 240px;
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 184, 137, 0.18), transparent 60%),
    radial-gradient(circle at 70% 70%, rgba(255, 212, 189, 0.22), transparent 60%),
    var(--surface-soft);
}

.community-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
}

.orb-large {
  right: 40px;
  top: 30px;
  width: 200px;
  height: 200px;
  background: rgba(255, 184, 137, 0.4);
}

.orb-small {
  left: 30px;
  bottom: 30px;
  width: 140px;
  height: 140px;
  background: rgba(255, 219, 198, 0.5);
}

.community-panel {
  position: relative;
  z-index: 1;
  max-width: 300px;
  padding: 22px 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(16px);
}

.community-panel span {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--accent-strong);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.community-panel strong {
  display: block;
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text);
}

.community-panel p {
  margin: 10px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
}

.ai-fab {
  position: fixed;
  right: 0;
  top: 52%;
  transform: translateY(-50%);
  width: 40px;
  min-height: 96px;
  padding: 12px 0;
  border: none;
  border-radius: 14px 0 0 14px;
  background: var(--accent);
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 24px rgba(255, 107, 44, 0.3);
  cursor: pointer;
  z-index: 1240;
  transition: background 0.18s ease, transform 0.2s ease;
}

.ai-fab span {
  writing-mode: vertical-rl;
  letter-spacing: 0;
}

.ai-fab:hover {
  background: var(--accent-strong);
  transform: translateY(calc(-50% - 1px));
}

@media (max-width: 1220px) {
  .page-grid {
    grid-template-columns: 1fr;
  }

  .entry-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .activity-grid,
  .story-grid,
  .support-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .community-banner {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .localact-page {
    padding: 88px 16px 32px;
  }

  .hero-banner {
    min-height: 360px;
  }

  .hero-copy {
    padding: 28px 22px 200px;
  }

  .hero-spotlight {
    right: 20px;
    left: 20px;
    width: auto;
    bottom: 20px;
  }

  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-grid,
  .story-grid,
  .support-grid,
  .filter-bar,
  .countdown-grid,
  .schedule-overview,
  .topic-cloud {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    gap: 16px;
  }

  .hero-actions,
  .community-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .participation-top {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .member-cluster {
    justify-content: flex-start;
  }

  .community-copy,
  .community-visual {
    padding: 28px 24px;
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    font-size: 26px;
  }

  .hero-banner,
  .panel-card,
  .sidebar-card,
  .community-banner {
    border-radius: 16px;
  }

  .entry-card {
    padding: 14px;
  }

  .section-head,
  .sidebar-head {
    flex-direction: column;
  }

  .ai-fab {
    display: none;
  }

  .notice-item {
    grid-template-columns: 32px minmax(0, 1fr);
  }

  .notice-date {
    grid-column: 2;
  }
}
</style>
