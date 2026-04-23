<template>
  <dhstyle />
  <div class="la-page" :class="{ 'ai-open': aiWorkbenchOpen }">
    <div class="portal-shell">
      <section class="hero-board">
        <div id="local-home" class="hero-stage">
          <div class="hero-copy">
            <p class="hero-kicker">让社区更有温度，让邻里更有连接</p>
            <h1>把附近的活动、故事和志愿服务，集中在一个更温暖的社区入口里。</h1>
            <p class="hero-subtitle">
              邻里互动、文化建设、便民服务与志愿共建集中展示，帮助居民更快发现身边正在发生的好内容。
            </p>

            <div class="hero-focus-strip">
              <span class="hero-focus-title">社区速递</span>
              <div class="hero-focus-list">
                <article v-for="item in heroFocusNews" :key="item.id" class="hero-focus-item">
                  <small>{{ item.tag }}</small>
                  <p>{{ item.title }}</p>
                </article>
              </div>
            </div>

            <div class="hero-bullet-list">
              <article v-for="item in heroHighlights" :key="item.label" class="hero-bullet-item">
                <span class="hero-bullet-icon">
                  <i :class="['fas', item.icon]"></i>
                </span>
                <div>
                  <strong>{{ item.label }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
              </article>
            </div>

            <div class="hero-toolbar">
              <div class="hero-search-box">
                <i class="fas fa-search"></i>
                <input
                  v-model="searchText"
                  type="text"
                  placeholder="搜索活动、故事、社区内容"
                  @keyup.enter="refreshHotEvents"
                />
                <button class="hero-search-btn" type="button" @click="refreshHotEvents">搜索</button>
              </div>

              <div class="hero-toolbar-actions">
                <button class="hero-mini-btn ghost" type="button" @click="toggleAiWorkbench">
                  <i class="fas fa-robot"></i>
                  AI 助手
                </button>
                <button class="hero-mini-btn warm" type="button" @click="goToPublishAct">
                  <i class="fas fa-plus-circle"></i>
                  发布活动
                </button>
              </div>
            </div>

            <div class="hero-actions">
              <button class="cta-button primary" type="button" @click="goToDetail()">
                立即报名
                <i class="fas fa-arrow-right"></i>
              </button>
              <button class="cta-button secondary" type="button" @click="goToStories">
                查看故事
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>

            <div class="hero-location-row">
              <div class="hero-location-field">
                <i class="fas fa-map-marker-alt"></i>
                <input v-model="address" type="text" placeholder="定位或输入街区" />
              </div>
              <button class="hero-location-btn" type="button" @click="pinAddress">定位附近</button>
            </div>

            <div class="hero-stats">
              <article v-for="item in heroStats" :key="item.label" class="hero-stat-card">
                <span class="hero-stat-icon">
                  <i :class="['fas', item.icon]"></i>
                </span>
                <div>
                  <p>{{ item.label }}</p>
                  <strong>{{ item.value }}</strong>
                  <small>{{ item.note }}</small>
                </div>
              </article>
            </div>
          </div>

          <div class="hero-visual">
            <div class="hero-scene-tag">本周社区主打</div>
            <div class="hero-scene-overlay">
              <span class="scene-chip">邻里互动</span>
              <span class="scene-chip warm">志愿共建</span>
            </div>
            <div class="hero-scene-note" v-if="selectedEvent">
              <span>{{ formatEventDate(selectedEvent.date) }}</span>
              <span>{{ selectedEvent.organizer }}</span>
            </div>
            <div class="hero-floating-panel" v-if="selectedEvent">
              <strong>活动亮点</strong>
              <p>{{ selectedEvent.description || '聚焦当周主打社区活动，带动更多邻里参与。' }}</p>
              <div class="hero-floating-tags">
                <span v-for="tag in selectedEvent.tags.slice(0, 2)" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <article class="hero-feature-card" v-if="selectedEvent">
              <div class="hero-feature-thumb" :style="{ backgroundImage: `url(${selectedEvent.cover})` }"></div>
              <div class="hero-feature-info">
                <span class="feature-badge">当前推荐</span>
                <h3>{{ selectedEvent.title }}</h3>
                <p>{{ selectedEvent.location }} · {{ selectedEvent.timeRange }}</p>
                <div class="feature-progress">
                  <span>已报名 {{ selectedEvent.reserved }}/{{ selectedEvent.capacity }}</span>
                  <span>剩余 {{ remainingSlots(selectedEvent) }}</span>
                </div>
              </div>
            </article>
          </div>
        </div>

        <aside id="signup-center" class="hero-signup-card">
          <div class="signup-header">
            <div>
              <h3>活动报名中心</h3>
              <p>轻松四步，参与社区活动</p>
            </div>
            <span class="signup-star">✦</span>
          </div>

          <ol class="signup-steps">
            <li v-for="(step, index) in signupSteps" :key="step.title">
              <span class="step-index">{{ index + 1 }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </li>
          </ol>

          <div class="signup-current" v-if="selectedEvent">
            <span class="signup-current-label">当前活动</span>
            <h4>{{ selectedEvent.title }}</h4>
            <p>{{ formatEventDate(selectedEvent.date) }} · {{ selectedEvent.timeRange }}</p>
            <p>{{ selectedEvent.location }} · {{ statusLabel(selectedEvent.status) }}</p>
          </div>

          <div class="signup-mini-stats">
            <article v-for="item in signupMiniStats" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </article>
          </div>

          <div class="signup-tip-box">
            <i class="fas fa-bell"></i>
            <span>报名后可自动接收活动通知、签到提醒和变更消息。</span>
          </div>

          <button class="signup-button" type="button" @click="goToDetail()">
            <i class="fas fa-bolt"></i>
            快速报名
          </button>
        </aside>
      </section>

      <section class="entry-strip">
        <button
          v-for="entry in quickEntryCards"
          :key="entry.id"
          type="button"
          class="entry-card"
          @click="handleTopNav(entry.id)"
        >
          <span class="entry-icon" :class="entry.tone">
            <i :class="['fas', entry.icon]"></i>
          </span>
          <div class="entry-copy">
            <strong>{{ entry.title }}</strong>
            <p>{{ entry.desc }}</p>
          </div>
        </button>
      </section>

      <section class="portal-grid">
        <div class="portal-main">
          <section id="story-hub" class="content-panel story-panel">
            <div class="panel-header">
              <div>
                <span class="section-label warm">邻里记忆</span>
                <h3>社区故事</h3>
                <p>记录活动瞬间与邻里互助时刻，增强社区凝聚力</p>
              </div>
              <div class="panel-header-side">
                <span class="header-badge">{{ displayedStories.length }} 则精选</span>
                <button class="panel-link" type="button" @click="goToStories">更多故事</button>
              </div>
            </div>

            <div class="story-grid">
              <article
                v-for="(story, index) in displayedStories"
                :key="story.id"
                :class="['story-card', { featured: index === 0 }]"
              >
                <div class="story-cover" :style="{ backgroundImage: `url(${story.cover})` }">
                  <span class="story-cover-badge">{{ story.time }}</span>
                </div>
                <div class="story-body">
                  <h4>{{ story.title }}</h4>
                  <div class="story-meta-row">
                    <span>邻里纪实</span>
                    <span>{{ story.time }}</span>
                  </div>
                  <p>{{ story.summary }}</p>
                </div>
                <footer class="story-footer">
                  <div class="story-author">
                    <span class="story-avatar">{{ story.author.charAt(0) }}</span>
                    <span>{{ story.author }}</span>
                  </div>
                  <button class="story-link" type="button" @click="goToStories">阅读故事</button>
                </footer>
              </article>
            </div>
          </section>

          <section id="activity-hub" class="content-panel activity-panel">
            <div class="panel-header panel-header-split">
              <div>
                <span class="section-label cool">本周热度</span>
                <h3>热门社区活动</h3>
                <p>保留原有筛选与排序能力，用更接近参考图的门户式卡片布局呈现</p>
              </div>
              <div class="panel-controls">
                <span class="header-badge">更新于 {{ panelRefreshedLabel }}</span>
                <button
                  class="ai-toggle-btn"
                  type="button"
                  :class="{ active: aiWorkbenchOpen }"
                  @click="toggleAiWorkbench"
                >
                  <i class="fas fa-robot"></i>
                  {{ aiWorkbenchOpen ? '收起 AI 工作台' : '打开 AI 工作台' }}
                </button>
                <select v-model="sortMode" class="feed-select">
                  <option value="latest">最新发布</option>
                  <option value="popular">报名最多</option>
                  <option value="distance">距离最近</option>
                </select>
              </div>
            </div>

            <div class="filter-toolbar">
              <div class="filter-group">
                <span class="filter-label">活动类型</span>
                <div class="filter-chips">
                  <button
                    v-for="cat in categories"
                    :key="cat.id"
                    type="button"
                    :class="['chip', { active: selectedCategory === cat.id }]"
                    @click="selectedCategory = cat.id"
                  >
                    {{ cat.label }}
                  </button>
                </div>
              </div>

              <div class="filter-group">
                <span class="filter-label">距离</span>
                <div class="filter-chips">
                  <button
                    v-for="distance in radiusOptions"
                    :key="distance"
                    type="button"
                    :class="['chip', { active: selectedRadius === distance }]"
                    @click="toggleRadius(distance)"
                  >
                    {{ distance }}km
                  </button>
                  <button class="chip muted" type="button" @click="selectedRadius = null">不限</button>
                </div>
              </div>

              <label class="hot-toggle">
                <input type="checkbox" v-model="showOnlyHot" />
                <span>仅看热门 / 名额紧张</span>
              </label>
            </div>

            <div v-if="displayedEvents.length" class="activity-grid">
              <article
                v-for="(event, index) in displayedEvents"
                :key="event.id"
                class="activity-card"
                :class="{ active: selectedEvent?.id === event.id, featured: index === 0 }"
                :style="activityToneStyle(event)"
                @click="selectEvent(event)"
              >
                <div class="activity-cover" :style="{ backgroundImage: `url(${event.cover})` }">
                  <span class="activity-tag">{{ resolveCategoryLabel(event.category) }}</span>
                  <span v-if="event.highlight" class="activity-tag hot">热门</span>
                </div>
                <div class="activity-body">
                  <h4>{{ event.title }}</h4>
                  <p>{{ formatEventDate(event.date) }} · {{ event.timeRange }}</p>
                  <p>{{ event.location }} · {{ event.organizer }}</p>
                  <div class="activity-aux">
                    <span>{{ formatDistance(event.distance) }}</span>
                    <span>{{ statusLabel(event.status) }}</span>
                  </div>

                  <div class="activity-progress">
                    <div class="progress-track">
                      <span :style="{ width: `${eventFillRate(event)}%` }"></span>
                    </div>
                    <div class="progress-meta">
                      <span>{{ event.reserved }}/{{ event.capacity }} 已报名</span>
                      <span>剩余 {{ remainingSlots(event) }}</span>
                    </div>
                  </div>

                  <div class="activity-actions">
                    <button class="activity-btn secondary" type="button" @click.stop="selectEvent(event)">
                      查看详情
                    </button>
                    <button class="activity-btn primary" type="button" @click.stop="goToDetail(event)">
                      立即报名
                    </button>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">暂无匹配活动，请调整筛选条件后重试。</div>
          </section>
        </div>

        <aside class="portal-sidebar">
          <section id="notice-hub" class="sidebar-card notice-card">
            <div class="sidebar-header">
              <div>
                <span class="section-label warm">社区动态</span>
                <h4>今日公告</h4>
              </div>
              <div class="panel-header-side">
                <span class="header-badge">今日更新</span>
                <button class="panel-link" type="button" @click="goToNotifications">更多</button>
              </div>
            </div>
            <ul class="notice-list">
              <li v-for="item in sidebarNotices" :key="item.id">
                <span class="notice-dot"></span>
                <div class="notice-info">
                  <p>{{ item.title }}</p>
                  <small>{{ item.tag }}</small>
                </div>
                <time>{{ item.date }}</time>
              </li>
            </ul>
          </section>

          <section class="sidebar-card progress-card">
            <div class="sidebar-header">
              <div>
                <span class="section-label violet">数据概览</span>
                <h4>报名统计（本周）</h4>
              </div>
              <div class="panel-header-side">
                <span class="header-badge">截至 {{ panelRefreshedLabel }}</span>
                <button class="panel-link" type="button" @click="refreshHotEvents">更新</button>
              </div>
            </div>

            <div class="progress-overview">
              <div class="progress-ring" :style="progressRingStyle">
                <div class="progress-ring-inner">
                  <strong>{{ enrollmentRate }}%</strong>
                  <span>报名完成度</span>
                </div>
              </div>
              <ul class="metric-list">
                <li>
                  <span>总报名人数</span>
                  <strong>{{ totalParticipants }}</strong>
                </li>
                <li>
                  <span>已发布活动</span>
                  <strong>{{ events.length }}</strong>
                </li>
                <li>
                  <span>名额剩余</span>
                  <strong>{{ totalOpenSlots }}</strong>
                </li>
              </ul>
            </div>
            <div class="progress-mini-grid">
              <article>
                <strong>{{ hotEventCount }}</strong>
                <span>热门场次</span>
              </article>
              <article>
                <strong>{{ displayedStories.length }}</strong>
                <span>故事精选</span>
              </article>
              <article>
                <strong>{{ hotTopics.length }}</strong>
                <span>社区话题</span>
              </article>
            </div>
          </section>

          <section class="sidebar-card schedule-card">
            <div class="sidebar-header">
              <div>
                <span class="section-label green">报名管理</span>
                <h4>我的日程</h4>
              </div>
              <button class="panel-link" type="button" @click="goToMyEnrollments">查看全部</button>
            </div>
            <ul class="schedule-list">
              <li v-for="item in schedules" :key="item.id">
                <span class="schedule-time">{{ item.date }}</span>
                <div class="schedule-info">
                  <p>{{ item.title }}</p>
                  <small>{{ item.location }}</small>
                </div>
                <span class="schedule-status">{{ item.status }}</span>
              </li>
            </ul>
          </section>

          <section class="sidebar-card volunteer-card">
            <div class="sidebar-header">
              <div>
                <span class="section-label gold">志愿风采</span>
                <h4>志愿服务之星</h4>
              </div>
              <button class="panel-link" type="button" @click="goToNeighborSupport">查看全部</button>
            </div>

            <div class="volunteer-list">
              <article v-for="person in volunteerStars" :key="person.id" class="volunteer-item">
                <span class="volunteer-rank">{{ person.rank }}</span>
                <span class="volunteer-avatar">{{ person.name.charAt(0) }}</span>
                <div class="volunteer-meta">
                  <strong>{{ person.name }}</strong>
                  <p>{{ person.role }}</p>
                </div>
                <span class="volunteer-hours">{{ person.hours }}</span>
              </article>
            </div>

            <div class="topic-cloud volunteer-topic-cloud">
              <button v-for="topic in hotTopics" :key="topic.id" type="button" class="topic-pill">
                <span>{{ topic.title }}</span>
                <small>{{ topic.count }}</small>
              </button>
            </div>
          </section>
        </aside>
      </section>

      <footer class="portal-footer">
        <div class="footer-illustration">
          <div class="footer-figure-group">
            <span class="footer-figure large"></span>
            <span class="footer-figure medium"></span>
            <span class="footer-figure small"></span>
          </div>
          <div class="footer-slogan">
            <h3>共建温暖社区，共享美好生活</h3>
            <p>把活动、公告、报名、志愿服务放进同一个社区首页，让每一次参与都更轻松。</p>
            <div class="footer-socials">
              <span><i class="fab fa-weixin"></i></span>
              <span><i class="fas fa-comment-dots"></i></span>
              <span><i class="fas fa-bullhorn"></i></span>
              <span><i class="fas fa-house"></i></span>
            </div>
          </div>
        </div>

        <div class="footer-links">
          <div v-for="group in footerGroups" :key="group.title" class="footer-column">
            <h4>{{ group.title }}</h4>
            <ul>
              <li v-for="link in group.links" :key="link">{{ link }}</li>
            </ul>
          </div>
        </div>
      </footer>
    </div>

    <aside class="ai-drawer" :class="{ open: aiWorkbenchOpen }">
      <header class="ai-drawer-header">
        <div>
          <p class="ai-eyebrow">AI 工作台</p>
          <h4>活动运营助手</h4>
        </div>
        <button class="ghost xs" type="button" @click="toggleAiWorkbench">
          {{ aiWorkbenchOpen ? '收起' : '展开' }}
        </button>
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
            <button
              class="primary ai-send-btn"
              type="button"
              :disabled="agentLoading || !agentInput.trim()"
              @click="submitAiMessage"
            >
              {{ agentLoading ? '发送中...' : '发送' }}
            </button>
          </div>
        </section>
      </div>
    </aside>

    <button v-if="!aiWorkbenchOpen" class="ai-drawer-handle" type="button" @click="toggleAiWorkbench">
      AI 工作台
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
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

type LocalStory = {
  id: string;
  title: string;
  time: string;
  summary: string;
  author: string;
  cover: string;
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

const quickEntryCards = [
  { id: 'stories', title: '社区故事', desc: '发现身边的温暖故事', icon: 'fa-book-open', tone: 'peach' },
  { id: 'activities', title: '社区活动', desc: '丰富多彩的社区活动', icon: 'fa-calendar-days', tone: 'coral' },
  { id: 'enroll', title: '活动报名', desc: '快速报名参与活动', icon: 'fa-clipboard-check', tone: 'violet' },
  { id: 'volunteer', title: '志愿服务', desc: '加入志愿服务行列', icon: 'fa-hand-holding-heart', tone: 'blue' },
  { id: 'support', title: '邻里互助', desc: '互帮互助温暖邻里', icon: 'fa-heart-circle-check', tone: 'green' },
  { id: 'notice', title: '公告通知', desc: '最新公告及时获取', icon: 'fa-bell', tone: 'gold' }
];

const footerGroups = [
  { title: '帮助中心', links: ['常见问题', '使用指南', '意见反馈'] },
  { title: '平台介绍', links: ['关于我们', '平台理念', '发展历程'] },
  { title: '联系我们', links: ['400-888-1234', 'service@shequ.com', '周一至周五 9:00-18:00'] },
  { title: '合作社区', links: ['溪语社区', '幸福里社区', '和谐家园社区'] }
];

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
    author: '社区花园组',
    cover: 'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'story-2',
    title: '“一小时”邻里互助',
    time: '10/16',
    summary: '志愿者轮班照料独居老人，提供上门维修、陪诊服务，累计 32 小时互助记录。',
    author: '互助小组',
    cover: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=900&q=80'
  },
  {
    id: 'story-3',
    title: '亲子环保课堂',
    time: '10/15',
    summary: '20 组家庭参与垃圾分类互动剧，孩子们亲手制作了回收小物件带回家。',
    author: '青藤环保社',
    cover: 'https://images.unsplash.com/photo-1516627145497-ae6968895b74?auto=format&fit=crop&w=900&q=80'
  }
] satisfies LocalStory[];

const hotTopics = [
  { id: 't1', title: '周末亲子共读营', count: 128 },
  { id: 't2', title: '旧物再利用市集', count: 96 },
  { id: 't3', title: '社区慢跑打卡', count: 84 },
  { id: 't4', title: '银龄互助计划', count: 76 }
];

const volunteerStars = [
  { id: 'v1', rank: '1', name: '李明阳', role: '照护与陪诊服务', hours: '36h' },
  { id: 'v2', rank: '2', name: '王晓雨', role: '亲子课堂协助', hours: '28h' },
  { id: 'v3', rank: '3', name: '张磊', role: '社区活动引导', hours: '24h' }
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
    cover: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80',
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
    cover: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=900&q=80',
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
    cover: 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=900&q=80',
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
    cover: 'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=900&q=80',
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

const displayedEvents = computed(() => sortedEvents.value.slice(0, 4));
const displayedStories = computed(() => stories.slice(0, 3));
const panelRefreshedLabel = computed(() =>
  panelUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
);
const totalParticipants = computed(() => events.value.reduce((sum, item) => sum + item.reserved, 0));
const totalCapacity = computed(() => events.value.reduce((sum, item) => sum + item.capacity, 0));
const totalOpenSlots = computed(() =>
  events.value.reduce((sum, item) => sum + Math.max(item.capacity - item.reserved, 0), 0)
);
const hotEventCount = computed(() => events.value.filter((item) => item.highlight).length);
const enrollmentRate = computed(() =>
  totalCapacity.value ? Math.round((totalParticipants.value / totalCapacity.value) * 100) : 0
);
const progressRingStyle = computed(() => ({ '--progress': String(enrollmentRate.value) }));

const heroStats = computed(() => [
  {
    label: '本周活动',
    value: `${events.value.length}`,
    note: '已发布并持续更新',
    icon: 'fa-calendar-days'
  },
  {
    label: '已报名人数',
    value: `${totalParticipants.value}`,
    note: '累计预约参与',
    icon: 'fa-users'
  },
  {
    label: '可报名名额',
    value: `${totalOpenSlots.value}`,
    note: '支持快速报名',
    icon: 'fa-ticket-alt'
  }
]);

const heroHighlights = computed(() => [
  {
    label: '附近活动实时更新',
    desc: `当前展示 ${events.value.length} 场社区活动，支持按距离和热门度快速筛选。`,
    icon: 'fa-compass'
  },
  {
    label: '社区故事持续沉淀',
    desc: `已整理 ${displayedStories.value.length} 条邻里故事，把温暖瞬间留在社区首页。`,
    icon: 'fa-book-open'
  },
  {
    label: '报名提醒与运营协同',
    desc: `累计 ${totalParticipants.value} 人已报名，可结合 AI 助手处理通知与内容优化。`,
    icon: 'fa-bullhorn'
  }
]);

const heroFocusNews = computed(() => focusNews.slice(0, 3));

const signupSteps = computed(() => [
  {
    title: '选择活动',
    desc: selectedEvent.value ? `浏览活动列表，选择 ${selectedEvent.value.title}` : '浏览活动列表，选择感兴趣的活动'
  },
  {
    title: '填写信息',
    desc: '填写个人信息，便于活动联系和通知提醒'
  },
  {
    title: '提交报名',
    desc: '确认信息后提交报名申请，名额实时更新'
  },
  {
    title: '查看结果',
    desc: '报名成功后，可查看活动详情和后续提醒'
  }
]);

const signupMiniStats = computed(() => [
  { label: '本周活动', value: `${events.value.length}` },
  { label: '热门场次', value: `${hotEventCount.value}` },
  { label: '可报剩余', value: `${totalOpenSlots.value}` }
]);

const sidebarNotices = computed(() =>
  focusNews.map((item, index) => ({
    ...item,
    date: formatNoticeDate(index)
  }))
);

const resolveCategoryLabel = (id: string) => categories.find((c) => c.id === id)?.label ?? '社区活动';

const statusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    CLOSED: '已截止'
  };
  return statusMap[status] ?? status;
};

const remainingSlots = (event: LocalEvent) => Math.max(event.capacity - event.reserved, 0);

const eventFillRate = (event: LocalEvent) => {
  if (!event.capacity) return 0;
  return Math.min(100, Math.round((event.reserved / event.capacity) * 100));
};

const formatDistance = (distance: number) => `${distance.toFixed(1)}km`;

const activityToneStyle = (event: LocalEvent) => {
  const toneMap: Record<string, { accent: string; soft: string; border: string }> = {
    kids: { accent: '#ff6f89', soft: 'rgba(255, 111, 137, 0.14)', border: 'rgba(255, 111, 137, 0.28)' },
    eco: { accent: '#49b57b', soft: 'rgba(73, 181, 123, 0.14)', border: 'rgba(73, 181, 123, 0.28)' },
    sport: { accent: '#4f83ff', soft: 'rgba(79, 131, 255, 0.14)', border: 'rgba(79, 131, 255, 0.28)' },
    market: { accent: '#8a68ff', soft: 'rgba(138, 104, 255, 0.14)', border: 'rgba(138, 104, 255, 0.28)' },
    skill: { accent: '#ff8b39', soft: 'rgba(255, 139, 57, 0.14)', border: 'rgba(255, 139, 57, 0.28)' },
    all: { accent: '#ff8b39', soft: 'rgba(255, 139, 57, 0.14)', border: 'rgba(255, 139, 57, 0.28)' }
  };

  const tone = toneMap[event.category] ?? toneMap.all;
  return {
    '--activity-accent': tone.accent,
    '--activity-soft': tone.soft,
    '--activity-border': tone.border
  };
};

const formatEventDate = (dateText: string) => {
  if (!dateText || !dateText.includes('-')) return dateText;
  const date = new Date(dateText);
  if (Number.isNaN(date.getTime())) return dateText;
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
};

const formatNoticeDate = (index: number) => {
  const date = new Date(panelUpdatedAt.value);
  date.setDate(date.getDate() - Math.min(index, 2));
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

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
    'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80',
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
  } catch {
    if (events.value.length === 0) {
      events.value = fallbackEvents;
    }
  }
};

const toggleRadius = (distance: number) => {
  selectedRadius.value = selectedRadius.value === distance ? null : distance;
};

const selectEvent = (event: LocalEvent) => {
  selectedEvent.value = event;
};

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleTopNav = (id: string) => {
  switch (id) {
    case 'home':
      scrollToSection('local-home');
      break;
    case 'stories':
      scrollToSection('story-hub');
      break;
    case 'activities':
      scrollToSection('activity-hub');
      break;
    case 'enroll':
      scrollToSection('signup-center');
      break;
    case 'volunteer':
    case 'support':
      goToNeighborSupport();
      break;
    case 'notice':
      scrollToSection('notice-hub');
      break;
    default:
      break;
  }
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

watch(sortedEvents, (list) => {
  syncSelectedEvent(list);
});

watch([selectedCategory, selectedRadius], () => {
  fetchEvents();
});

watch(searchText, (value, oldValue) => {
  if (!value && oldValue) {
    fetchEvents();
  }
});

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
  background: #f7efe7;
}

.la-page {
  --bg: #f7efe7;
  --paper: #fffaf6;
  --card: #ffffff;
  --line: rgba(237, 196, 166, 0.7);
  --line-soft: rgba(242, 218, 199, 0.8);
  --text-main: #2d1f1a;
  --text-sub: #6f5b50;
  --text-soft: #9c8576;
  --orange: #ff8b39;
  --orange-deep: #ff6a37;
  --pink: #ff5b7a;
  --violet: #8a68ff;
  --blue: #4e8ef7;
  --green: #53c48b;
  --gold: #f7b63d;
  --shadow: 0 18px 38px rgba(217, 145, 91, 0.16);

  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 194, 158, 0.35), transparent 28%),
    radial-gradient(circle at bottom right, rgba(255, 126, 169, 0.18), transparent 22%),
    var(--bg);
  color: var(--text-main);
  padding: 78px 28px 40px;
  box-sizing: border-box;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.portal-shell {
  max-width: 1580px;
  margin: 0 auto;
}

.hero-location-field input,
.hero-search-box input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  color: var(--text-main);
  font-size: 14px;
}

.hero-search-box input::placeholder,
.hero-location-field input::placeholder {
  color: #b59b8a;
}

.hero-mini-btn:hover,
.entry-card:hover,
.panel-link:hover,
.story-link:hover,
.activity-btn:hover,
.signup-button:hover,
.hero-location-btn:hover,
.cta-button:hover,
.topic-pill:hover,
.ai-toggle-btn:hover,
.feed-select:hover,
.ghost:hover,
.chip:hover {
  transform: translateY(-1px);
}

.hero-board {
  margin-top: 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
}

.hero-stage {
  min-height: 520px;
  display: grid;
  grid-template-columns: minmax(320px, 1.05fr) minmax(320px, 0.95fr);
  gap: 18px;
  background:
    radial-gradient(circle at 10% 12%, rgba(255, 204, 183, 0.75), transparent 28%),
    radial-gradient(circle at 78% 20%, rgba(255, 173, 157, 0.3), transparent 26%),
    linear-gradient(135deg, #fff4ec 8%, #ffeadd 42%, #fff8f3 100%);
  border-radius: 30px;
  padding: 28px;
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow);
  border: 1px solid rgba(252, 225, 207, 0.95);
}

.hero-stage::before,
.hero-stage::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.hero-stage::before {
  width: 320px;
  height: 320px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(255, 201, 178, 0.55), transparent 70%);
}

.hero-stage::after {
  width: 240px;
  height: 240px;
  bottom: -90px;
  left: 34%;
  background: radial-gradient(circle, rgba(255, 235, 214, 0.9), transparent 70%);
}

.hero-copy,
.hero-visual,
.hero-signup-card,
.content-panel,
.sidebar-card,
.portal-footer {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-kicker {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #ff7e2f, #9b4dff);
  -webkit-background-clip: text;
  color: transparent;
}

.hero-copy h1 {
  margin: 18px 0 0;
  font-size: clamp(34px, 3.2vw, 58px);
  line-height: 1.12;
  letter-spacing: -0.03em;
  max-width: 10.5em;
}

.hero-subtitle {
  margin: 18px 0 0;
  max-width: 520px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-sub);
}

.hero-focus-strip {
  margin-top: 22px;
  padding: 16px 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.78), rgba(255, 242, 233, 0.94));
  border: 1px solid rgba(244, 220, 205, 0.96);
  box-shadow: 0 14px 28px rgba(233, 184, 144, 0.12);
}

.hero-focus-title {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 149, 85, 0.14);
  color: #d86428;
  font-size: 12px;
  font-weight: 700;
}

.hero-focus-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-focus-item {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(244, 220, 205, 0.9);
}

.hero-focus-item small,
.hero-focus-item p {
  display: block;
  margin: 0;
}

.hero-focus-item small {
  font-size: 11px;
  font-weight: 700;
  color: var(--pink);
}

.hero-focus-item p {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-sub);
}

.hero-bullet-list {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-bullet-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(244, 220, 205, 0.9);
}

.hero-bullet-icon {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 162, 93, 0.22), rgba(255, 97, 119, 0.16));
  color: var(--orange-deep);
  font-size: 15px;
}

.hero-bullet-item strong {
  display: block;
  font-size: 14px;
  line-height: 1.35;
}

.hero-bullet-item p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-soft);
}

.hero-toolbar {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.hero-search-box {
  flex: 1;
  min-width: 320px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 12px 0 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(240, 214, 196, 0.92);
  box-shadow: 0 14px 24px rgba(226, 175, 138, 0.1);
}

.hero-search-box i {
  color: var(--text-soft);
}

.hero-search-btn {
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff9c50, #ff7448);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.hero-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-mini-btn {
  height: 48px;
  padding: 0 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hero-mini-btn.ghost {
  border: 1px solid rgba(232, 210, 194, 0.95);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-main);
}

.hero-mini-btn.warm {
  border: none;
  background: linear-gradient(135deg, var(--orange), var(--pink));
  color: #ffffff;
  box-shadow: 0 16px 28px rgba(255, 110, 74, 0.2);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.cta-button {
  min-width: 154px;
  height: 50px;
  border-radius: 999px;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cta-button.primary,
.signup-button,
.primary,
.activity-btn.primary {
  background: linear-gradient(135deg, var(--orange), var(--pink));
  color: #ffffff;
  box-shadow: 0 16px 26px rgba(255, 106, 73, 0.22);
}

.cta-button.secondary {
  background: rgba(255, 255, 255, 0.86);
  color: var(--text-main);
  border: 1px solid rgba(232, 210, 194, 0.9);
}

.hero-location-row {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-location-field {
  min-width: 280px;
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(240, 214, 196, 0.92);
}

.hero-location-field i {
  color: #e2844f;
}

.hero-location-btn,
.activity-btn.secondary,
.story-link,
.panel-link,
.ghost,
.chip,
.feed-select,
.ai-toggle-btn {
  border: 1px solid rgba(232, 210, 194, 0.95);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-main);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hero-location-btn {
  height: 48px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 700;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.hero-stat-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 102px;
  padding: 14px 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(245, 223, 207, 0.96);
  box-shadow: 0 14px 28px rgba(236, 183, 146, 0.15);
}

.hero-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: linear-gradient(135deg, rgba(255, 172, 88, 0.2), rgba(255, 97, 119, 0.16));
  color: var(--orange-deep);
}

.hero-stat-card p,
.hero-stat-card small,
.signup-header p,
.story-body p,
.panel-header p,
.notice-info small,
.ai-chat-subtitle,
.metric-list span,
.schedule-info small {
  margin: 0;
}

.hero-stat-card p {
  font-size: 13px;
  color: var(--text-soft);
}

.hero-stat-card strong {
  display: block;
  margin-top: 4px;
  font-size: 26px;
  font-weight: 800;
  color: var(--text-main);
}

.hero-stat-card small {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-soft);
}

.hero-visual {
  min-height: 520px;
  border-radius: 26px;
  overflow: hidden;
  background:
    linear-gradient(rgba(255, 229, 211, 0.08), rgba(255, 239, 225, 0.18)),
    url('https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=1400&q=80') center / cover no-repeat;
  box-shadow: inset 0 0 0 1px rgba(255, 240, 228, 0.32);
  position: relative;
}

.hero-visual::before {
  content: '';
  position: absolute;
  right: -28px;
  top: 78px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.34), transparent 72%);
}

.hero-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 246, 238, 0) 0%, rgba(68, 43, 27, 0.16) 100%);
}

.hero-scene-tag,
.scene-chip,
.feature-badge,
.story-cover-badge,
.activity-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-weight: 700;
}

.hero-scene-tag {
  position: absolute;
  top: 20px;
  left: 20px;
  height: 34px;
  padding: 0 14px;
  background: rgba(255, 244, 233, 0.92);
  color: #c96b1f;
  font-size: 13px;
  z-index: 1;
}

.hero-scene-overlay {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  gap: 8px;
  z-index: 1;
}

.scene-chip {
  height: 32px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.86);
  color: #7a5443;
  font-size: 12px;
}

.scene-chip.warm {
  background: rgba(255, 140, 68, 0.9);
  color: #ffffff;
}

.hero-scene-note {
  position: absolute;
  top: 74px;
  left: 20px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  z-index: 1;
}

.hero-scene-note span,
.hero-floating-tags span {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
}

.hero-scene-note span {
  background: rgba(255, 251, 247, 0.88);
  color: #7f5a48;
}

.hero-floating-panel {
  position: absolute;
  left: 20px;
  right: 126px;
  bottom: 176px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(88, 52, 35, 0.82), rgba(132, 73, 50, 0.7));
  border: 1px solid rgba(255, 233, 220, 0.18);
  box-shadow: 0 18px 36px rgba(68, 43, 27, 0.14);
  backdrop-filter: blur(10px);
  z-index: 1;
}

.hero-floating-panel strong {
  display: block;
  font-size: 14px;
  color: #fff3ea;
}

.hero-floating-panel p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.75;
  color: rgba(255, 245, 237, 0.88);
}

.hero-floating-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-floating-tags span {
  background: rgba(255, 255, 255, 0.14);
  color: #fff8f4;
}

.hero-feature-card {
  position: absolute;
  right: 20px;
  bottom: 20px;
  left: 20px;
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 14px;
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 250, 246, 0.88);
  box-shadow: 0 18px 36px rgba(94, 58, 35, 0.14);
  backdrop-filter: blur(12px);
  z-index: 1;
}

.hero-feature-thumb {
  height: 112px;
  border-radius: 16px;
  background-size: cover;
  background-position: center;
}

.hero-feature-info h3 {
  margin: 8px 0 6px;
  font-size: 20px;
}

.hero-feature-info p,
.feature-progress,
.signup-current p,
.notice-info p,
.metric-list li,
.schedule-status,
.topic-pill small,
.ai-chat-time {
  color: var(--text-sub);
}

.hero-feature-info p,
.signup-current p,
.notice-info p,
.schedule-info p,
.topic-pill span,
.ai-chat-bubble,
.ai-chat-empty {
  margin: 0;
}

.feature-badge,
.story-cover-badge {
  height: 28px;
  padding: 0 12px;
  background: linear-gradient(135deg, rgba(255, 152, 87, 0.16), rgba(255, 89, 121, 0.14));
  color: var(--orange-deep);
  font-size: 12px;
}

.feature-progress {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
}

.hero-signup-card,
.content-panel,
.sidebar-card,
.portal-footer {
  background: rgba(255, 250, 246, 0.92);
  border: 1px solid rgba(247, 224, 209, 0.96);
  border-radius: 28px;
  box-shadow: var(--shadow);
}

.hero-signup-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.signup-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.signup-header h3,
.panel-header h3,
.sidebar-header h4,
.footer-slogan h3 {
  margin: 0;
}

.signup-header h3 {
  font-size: 24px;
}

.signup-header p {
  margin-top: 6px;
  color: var(--text-soft);
  font-size: 14px;
}

.signup-star {
  font-size: 28px;
  color: #f7bc5e;
}

.signup-steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.signup-steps li {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.step-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 13px;
  color: #ffffff;
  background: linear-gradient(135deg, #ffbf57, #ff6f49);
}

.signup-steps strong {
  display: block;
  font-size: 15px;
}

.signup-steps p {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-soft);
}

.signup-current {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 246, 239, 0.96), rgba(255, 234, 224, 0.92));
  border: 1px solid rgba(244, 220, 205, 0.96);
}

.signup-current-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--pink);
}

.signup-current h4 {
  margin: 8px 0 8px;
  font-size: 18px;
}

.signup-current p + p {
  margin-top: 4px;
}

.signup-mini-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.signup-mini-stats article {
  padding: 12px 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(244, 220, 205, 0.96);
  text-align: center;
}

.signup-mini-stats strong {
  display: block;
  font-size: 22px;
  color: var(--text-main);
  line-height: 1;
}

.signup-mini-stats span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-soft);
}

.signup-tip-box {
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 245, 233, 0.96), rgba(255, 236, 223, 0.96));
  border: 1px solid rgba(244, 220, 205, 0.96);
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.signup-tip-box i {
  color: var(--orange-deep);
}

.signup-button {
  height: 52px;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}

.entry-strip {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.entry-card {
  border: 1px solid rgba(244, 220, 205, 0.96);
  background: rgba(255, 250, 246, 0.92);
  border-radius: 24px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 14px 24px rgba(218, 160, 117, 0.1);
}

.entry-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 23px;
  flex-shrink: 0;
}

.entry-icon.peach {
  background: linear-gradient(135deg, #ff9d5c, #ff7652);
}

.entry-icon.coral {
  background: linear-gradient(135deg, #ff8461, #ff5b6b);
}

.entry-icon.violet {
  background: linear-gradient(135deg, #9c79ff, #7c68ff);
}

.entry-icon.blue {
  background: linear-gradient(135deg, #6aa8ff, #4f7eff);
}

.entry-icon.green {
  background: linear-gradient(135deg, #66d6a1, #3fc38e);
}

.entry-icon.gold {
  background: linear-gradient(135deg, #ffc85d, #f5a93e);
}

.entry-copy strong {
  display: block;
  font-size: 16px;
}

.entry-copy p {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--text-soft);
  line-height: 1.5;
}

.portal-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.portal-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-panel {
  padding: 24px;
}

.story-panel {
  background:
    linear-gradient(180deg, rgba(255, 251, 247, 0.96), rgba(255, 246, 239, 0.92)),
    rgba(255, 250, 246, 0.92);
}

.activity-panel {
  background:
    linear-gradient(180deg, rgba(255, 250, 246, 0.96), rgba(255, 245, 239, 0.92)),
    rgba(255, 250, 246, 0.92);
}

.panel-header,
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-header h3 {
  font-size: 30px;
}

.panel-header p {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-soft);
}

.panel-header-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.section-label,
.header-badge {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.section-label {
  margin-bottom: 10px;
}

.section-label.warm {
  background: rgba(255, 149, 85, 0.14);
  color: #d86428;
}

.section-label.cool {
  background: rgba(98, 136, 255, 0.14);
  color: #4e73de;
}

.section-label.violet {
  background: rgba(138, 104, 255, 0.14);
  color: #7a5ef2;
}

.section-label.green {
  background: rgba(73, 181, 123, 0.14);
  color: #3f9b6b;
}

.section-label.gold {
  background: rgba(248, 182, 61, 0.16);
  color: #d5921c;
}

.header-badge {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(241, 222, 210, 0.96);
  color: var(--text-soft);
}

.panel-link,
.story-link {
  height: 40px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.story-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  grid-auto-flow: dense;
}

.story-card,
.activity-card {
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid rgba(241, 222, 210, 0.96);
  overflow: hidden;
  box-shadow: 0 14px 26px rgba(226, 175, 138, 0.12);
}

.story-card {
  display: flex;
  flex-direction: column;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.story-card.featured {
  display: grid;
  grid-template-columns: minmax(260px, 0.96fr) minmax(0, 1fr);
  grid-template-areas:
    'cover body'
    'cover footer';
  grid-column: span 2;
}

.story-card.featured .story-cover {
  grid-area: cover;
  height: 100%;
  min-height: 100%;
}

.story-card.featured .story-body {
  grid-area: body;
  padding: 22px 22px 12px;
}

.story-card.featured .story-body h4 {
  font-size: 28px;
}

.story-card.featured .story-body p {
  font-size: 15px;
}

.story-card.featured .story-footer {
  grid-area: footer;
  padding: 0 22px 22px;
}

.story-card:hover,
.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 36px rgba(213, 151, 113, 0.18);
}

.story-cover,
.activity-cover {
  background-size: cover;
  background-position: center;
  position: relative;
}

.story-cover {
  height: 218px;
}

.story-cover::after,
.activity-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.02) 0%, rgba(0, 0, 0, 0.2) 100%);
}

.story-cover-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 1;
}

.story-body {
  padding: 16px 16px 10px;
}

.story-body h4,
.activity-body h4,
.notice-info p,
.schedule-info p,
.topic-pill span {
  font-size: 20px;
  line-height: 1.35;
}

.story-body h4,
.activity-body h4 {
  margin: 0 0 10px;
}

.story-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.story-meta-row span {
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
  background: rgba(255, 153, 91, 0.12);
  color: #ca6d34;
}

.story-body p {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-sub);
}

.story-footer {
  margin-top: auto;
  padding: 0 16px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.story-author {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

.story-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 170, 96, 0.2), rgba(255, 113, 115, 0.15));
  color: var(--orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.panel-header-split {
  align-items: center;
}

.panel-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-toggle-btn,
.feed-select {
  height: 42px;
  padding: 0 16px;
  font-size: 13px;
}

.ai-toggle-btn.active {
  background: rgba(255, 239, 228, 0.95);
  color: var(--orange-deep);
}

.filter-toolbar {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px 18px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 246, 239, 0.96), rgba(255, 239, 233, 0.9));
  border: 1px solid rgba(244, 220, 205, 0.96);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 12px;
  color: var(--text-soft);
  font-weight: 700;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  height: 38px;
  padding: 0 14px;
  font-size: 13px;
}

.chip.active {
  background: linear-gradient(135deg, rgba(255, 152, 87, 0.16), rgba(255, 89, 121, 0.12));
  color: var(--orange-deep);
}

.chip.muted {
  color: var(--text-soft);
}

.hot-toggle {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-end;
  color: var(--text-sub);
  font-size: 13px;
  font-weight: 600;
}

.hot-toggle input {
  width: 16px;
  height: 16px;
  accent-color: var(--orange);
}

.activity-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  grid-auto-flow: dense;
}

.activity-card {
  cursor: pointer;
  transition: all 0.2s ease;
  border-color: var(--activity-border, rgba(241, 222, 210, 0.96));
  box-shadow: 0 14px 26px rgba(226, 175, 138, 0.12);
  grid-column: span 2;
}

.activity-card.featured {
  grid-column: span 4;
  display: grid;
  grid-template-columns: minmax(240px, 0.92fr) minmax(0, 1fr);
}

.activity-card.featured .activity-cover {
  height: 100%;
  min-height: 100%;
}

.activity-card.featured .activity-body {
  display: flex;
  flex-direction: column;
  padding: 22px;
}

.activity-card.featured .activity-body h4 {
  font-size: 26px;
}

.activity-card.featured .activity-body p {
  font-size: 14px;
}

.activity-card.featured .activity-actions {
  margin-top: auto;
}

.activity-card.active {
  border-color: var(--activity-accent, rgba(255, 136, 86, 0.8));
  box-shadow: 0 18px 32px rgba(247, 153, 98, 0.2);
}

.activity-cover {
  height: 178px;
}

.activity-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 1;
  height: 28px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--activity-accent, var(--text-main));
  font-size: 12px;
}

.activity-tag.hot {
  left: auto;
  right: 14px;
  background: linear-gradient(135deg, var(--orange), var(--pink));
  color: #ffffff;
}

.activity-body {
  padding: 16px;
}

.activity-body p {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.7;
}

.activity-body p + p {
  margin-top: 6px;
}

.activity-aux {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-aux span {
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--activity-soft, rgba(255, 139, 57, 0.14));
  color: var(--activity-accent, var(--orange-deep));
}

.activity-progress {
  margin-top: 16px;
}

.progress-track {
  width: 100%;
  height: 7px;
  border-radius: 999px;
  background: #f6e5d9;
  overflow: hidden;
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(135deg, var(--activity-accent, var(--violet)), #5f8dff);
}

.progress-meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--text-soft);
}

.activity-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.activity-btn {
  height: 42px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.activity-btn.primary {
  border: none;
  background: linear-gradient(135deg, var(--activity-accent, var(--orange)), #ff7b58);
}

.empty-state {
  margin-top: 18px;
  border: 1px dashed rgba(235, 204, 183, 0.95);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 22px;
  color: var(--text-soft);
  text-align: center;
  font-size: 14px;
  padding: 28px 18px;
}

.portal-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-card {
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.sidebar-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 18px;
  right: 18px;
  height: 4px;
  border-radius: 999px;
  background: rgba(241, 222, 210, 0.9);
}

.notice-card::before {
  background: linear-gradient(90deg, #ff9d62, #ff6e6b);
}

.progress-card::before {
  background: linear-gradient(90deg, #8a68ff, #5f8dff);
}

.schedule-card::before {
  background: linear-gradient(90deg, #55c78f, #3fb987);
}

.volunteer-card::before {
  background: linear-gradient(90deg, #ffc75a, #ff9b52);
}

.sidebar-header h4 {
  font-size: 20px;
}

.notice-list,
.schedule-list,
.metric-list,
.footer-column ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notice-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notice-list li {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.notice-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--orange);
  margin-top: 8px;
}

.notice-info p {
  font-size: 14px;
  line-height: 1.6;
}

.notice-info small,
.notice-list time {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-soft);
}

.notice-list time {
  margin-top: 2px;
}

.progress-overview {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.progress-ring {
  --progress: 0;
  width: 108px;
  height: 108px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: conic-gradient(
    var(--violet) 0deg,
    var(--pink) calc(var(--progress) * 3.6deg),
    rgba(242, 230, 219, 0.96) calc(var(--progress) * 3.6deg),
    rgba(242, 230, 219, 0.96) 360deg
  );
}

.progress-ring-inner {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  background: #fffaf6;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.progress-ring-inner strong {
  font-size: 24px;
  line-height: 1;
}

.progress-ring-inner span {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-soft);
}

.metric-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.metric-list strong {
  color: var(--text-main);
  font-size: 18px;
}

.progress-mini-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.progress-mini-grid article {
  padding: 12px 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(241, 222, 210, 0.96);
  text-align: center;
}

.progress-mini-grid strong {
  display: block;
  font-size: 20px;
  color: var(--text-main);
}

.progress-mini-grid span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-soft);
}

.schedule-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-list li {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(241, 222, 210, 0.96);
}

.schedule-time {
  font-size: 12px;
  color: var(--text-soft);
}

.schedule-info p {
  font-size: 14px;
}

.schedule-info small {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-soft);
}

.schedule-status {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 239, 228, 0.95);
}

.topic-cloud {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.volunteer-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.volunteer-item {
  display: grid;
  grid-template-columns: 28px 38px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(241, 222, 210, 0.96);
}

.volunteer-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffbb5f, #ff8750);
  color: #ffffff;
}

.volunteer-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  background: linear-gradient(135deg, rgba(255, 166, 99, 0.22), rgba(140, 105, 255, 0.16));
  color: var(--orange-deep);
}

.volunteer-meta strong {
  display: block;
  font-size: 14px;
}

.volunteer-meta p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-soft);
}

.volunteer-hours {
  font-size: 13px;
  font-weight: 700;
  color: var(--orange-deep);
}

.volunteer-topic-cloud {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed rgba(241, 222, 210, 0.96);
}

.topic-pill {
  border: 1px solid rgba(241, 222, 210, 0.96);
  background: rgba(255, 255, 255, 0.8);
  border-radius: 999px;
  padding: 10px 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.topic-pill span {
  font-size: 13px;
  color: var(--text-main);
}

.topic-pill small {
  font-size: 12px;
}

.portal-footer {
  margin-top: 18px;
  padding: 24px;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 24px;
  overflow: hidden;
}

.footer-illustration {
  min-height: 220px;
  border-radius: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 210, 179, 0.45), transparent 26%),
    linear-gradient(135deg, rgba(255, 248, 242, 0.96), rgba(255, 238, 224, 0.92));
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.footer-figure-group {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.footer-figure {
  position: relative;
  display: inline-block;
  width: 42px;
  border-radius: 999px 999px 14px 14px;
  background: linear-gradient(180deg, rgba(255, 184, 120, 0.75), rgba(255, 140, 74, 0.45));
}

.footer-figure::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -16px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 194, 138, 0.95);
  transform: translateX(-50%);
}

.footer-figure.large {
  height: 112px;
}

.footer-figure.medium {
  height: 90px;
}

.footer-figure.small {
  height: 76px;
}

.footer-slogan h3 {
  font-size: 36px;
  line-height: 1.18;
}

.footer-slogan p {
  margin: 12px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-sub);
  max-width: 420px;
}

.footer-socials {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.footer-socials span {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(241, 222, 210, 0.96);
  color: #8b6a57;
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.footer-column h4 {
  margin: 0 0 12px;
  font-size: 16px;
}

.footer-column li {
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-sub);
}

.primary {
  border: none;
  border-radius: 999px;
  height: 42px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.ghost {
  font-size: 12px;
  padding: 0 12px;
  height: 34px;
}

.ai-drawer {
  position: fixed;
  top: 112px;
  right: 16px;
  width: 396px;
  height: calc(100vh - 132px);
  border: 1px solid rgba(241, 222, 210, 0.96);
  border-radius: 28px;
  background: rgba(255, 250, 246, 0.96);
  box-shadow: 0 24px 46px rgba(146, 93, 62, 0.18);
  z-index: 120;
  transform: translateX(calc(100% + 20px));
  transition: transform 0.24s ease;
  pointer-events: none;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(12px);
}

.ai-drawer.open {
  transform: translateX(0);
  pointer-events: auto;
}

.ai-drawer-header {
  border-bottom: 1px solid rgba(241, 222, 210, 0.96);
  padding: 18px 18px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.ai-eyebrow {
  margin: 0;
  font-size: 12px;
  color: var(--text-soft);
}

.ai-drawer-header h4,
.ai-card-title h5 {
  margin: 6px 0 0;
  color: var(--text-main);
}

.ai-drawer-header h4 {
  font-size: 18px;
}

.ai-drawer-body {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ai-card {
  border: 1px solid rgba(241, 222, 210, 0.96);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  padding: 14px;
}

.ai-chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-card-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.ai-card-title small {
  color: var(--text-soft);
  font-size: 11px;
}

.ai-chat-subtitle {
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.ai-chat-window {
  flex: 1;
  margin-top: 10px;
  border: 1px solid rgba(241, 222, 210, 0.96);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  padding: 10px;
  overflow-y: auto;
}

.ai-chat-empty {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.65;
  border: 1px dashed rgba(236, 207, 186, 0.96);
  border-radius: 14px;
  padding: 12px 10px;
  background: rgba(255, 247, 240, 0.9);
}

.ai-chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-chat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  border: 1px solid rgba(241, 222, 210, 0.96);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-main);
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 10px 12px;
}

.ai-chat-item.user .ai-chat-bubble {
  background: linear-gradient(135deg, rgba(255, 155, 87, 0.14), rgba(255, 88, 119, 0.12));
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
  background: #b98a71;
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
  margin: 10px 0 0;
  border: 1px solid rgba(255, 172, 172, 0.75);
  border-radius: 14px;
  background: rgba(255, 242, 242, 0.92);
  color: #c84c4c;
  font-size: 12px;
  padding: 8px 10px;
}

.ai-chat-input {
  margin-top: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.ai-chat-input input {
  border: 1px solid rgba(241, 222, 210, 0.96);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--text-main);
  font-size: 12px;
  padding: 0 14px;
  min-height: 42px;
  outline: none;
}

.ai-chat-input input:focus,
.hero-search-box:focus-within,
.hero-location-field:focus-within {
  border-color: rgba(255, 140, 68, 0.85);
  box-shadow: 0 0 0 4px rgba(255, 158, 98, 0.14);
}

.ai-chat-input input::placeholder {
  color: #b79c8d;
}

.ai-send-btn {
  min-width: 74px;
}

.ai-drawer-handle {
  position: fixed;
  right: 0;
  top: 46%;
  transform: translateY(-50%);
  border: none;
  background: linear-gradient(180deg, var(--orange), var(--pink));
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  padding: 12px 8px;
  border-radius: 16px 0 0 16px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.08em;
  cursor: pointer;
  z-index: 110;
  box-shadow: 0 16px 28px rgba(255, 104, 81, 0.28);
}

@media (max-width: 1480px) {
  .hero-board,
  .portal-grid,
  .portal-footer {
    grid-template-columns: 1fr;
  }

  .hero-focus-list,
  .hero-bullet-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .entry-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .story-grid,
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-card {
    grid-column: auto;
  }

  .activity-card.featured {
    grid-column: 1 / -1;
  }

  .footer-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .la-page {
    padding: 72px 18px 30px;
  }

  .hero-stage {
    grid-template-columns: 1fr;
    min-height: unset;
  }

  .hero-visual {
    min-height: 460px;
  }

  .hero-copy h1 {
    max-width: none;
  }

  .hero-focus-list,
  .hero-bullet-list,
  .hero-stats,
  .signup-mini-stats,
  .story-grid,
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .story-card.featured,
  .activity-card.featured {
    display: flex;
    flex-direction: column;
    grid-template-columns: none;
    grid-template-areas: none;
    grid-column: auto;
  }

  .story-card.featured .story-cover,
  .activity-card.featured .activity-cover {
    height: 220px;
    min-height: 220px;
  }

  .story-card.featured .story-body,
  .story-card.featured .story-footer,
  .activity-card.featured .activity-body {
    padding-left: 16px;
    padding-right: 16px;
  }

  .hero-floating-panel {
    right: 20px;
    bottom: 184px;
  }

  .entry-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .progress-overview,
  .hero-feature-card {
    grid-template-columns: 1fr;
  }

  .hero-feature-thumb {
    height: 180px;
  }

  .ai-drawer,
  .ai-drawer-handle {
    display: none;
  }
}

@media (max-width: 760px) {
  .hero-stage,
  .hero-signup-card,
  .content-panel,
  .sidebar-card,
  .portal-footer,
  .footer-illustration {
    border-radius: 22px;
  }

  .hero-toolbar,
  .hero-actions,
  .hero-location-row,
  .panel-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-toolbar-actions {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-search-box,
  .hero-mini-btn,
  .cta-button,
  .hero-location-btn,
  .signup-button {
    width: 100%;
    justify-content: center;
  }

  .hero-search-box {
    min-width: 0;
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 14px;
    border-radius: 20px;
  }

  .hero-search-box i {
    display: none;
  }

  .hero-search-btn {
    width: 100%;
    height: 44px;
  }

  .signup-mini-stats {
    grid-template-columns: 1fr;
  }

  .hero-focus-strip {
    padding: 14px;
  }

  .hero-focus-list,
  .progress-mini-grid,
  .entry-strip,
  .footer-links {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    min-height: 400px;
  }

  .hero-scene-note {
    top: 68px;
    right: 20px;
    left: 20px;
  }

  .hero-floating-panel {
    right: 20px;
    bottom: 182px;
    padding: 16px;
  }

  .filter-toolbar {
    padding: 16px;
  }

  .hot-toggle {
    margin-left: 0;
    align-self: flex-start;
  }

  .schedule-list li,
  .notice-list li,
  .progress-overview {
    grid-template-columns: 1fr;
  }

  .schedule-status {
    justify-self: flex-start;
  }

  .footer-slogan h3 {
    font-size: 28px;
  }
}
</style>
