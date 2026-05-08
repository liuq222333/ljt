<template>
  <dhstyle />
  <div class="lad-page">
    <p v-if="loading" class="state-banner">正在加载活动详情...</p>
    <p v-else-if="errorMsg" class="state-banner error">{{ errorMsg }}</p>
    <p v-if="actionMessage" class="state-banner info">{{ actionMessage }}</p>

    <section class="hero card">
      <div class="hero-content">
        <p class="eyebrow">活动详情</p>
        <h1>{{ activity.title }}</h1>
        <p class="subtitle">{{ activity.subtitle }}</p>
        <div class="hero-tags">
          <span v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
          <span class="id-chip">ID {{ activityId }}</span>
        </div>
        <div class="hero-meta">
          <div class="hm-item">
            <i class="far fa-calendar"></i>
            <div>
              <span>时间</span>
              <strong>{{ activity.date }}</strong>
            </div>
          </div>
          <div class="hm-item">
            <i class="fas fa-location-dot"></i>
            <div>
              <span>地点</span>
              <strong>{{ activity.location }}</strong>
            </div>
          </div>
          <div class="hm-item">
            <i class="far fa-user"></i>
            <div>
              <span>报名</span>
              <strong>{{ activity.reserved }}/{{ activity.capacity }}</strong>
            </div>
          </div>
        </div>
        <div class="hero-actions">
          <button class="btn btn-primary" @click="handleEnrollClick" :disabled="enrollDisabled || enrollLoading">
            <i class="fas fa-paper-plane"></i>
            {{ enrollLoading ? '提交中...' : enrollButtonText }}
          </button>
          <button class="btn btn-light" @click="handleFavoriteClick" :disabled="favoriteLoading">
            <i :class="[activity.favorited ? 'fas' : 'far', 'fa-heart']"></i>
            {{ favoriteLoading ? '处理中...' : activity.favorited ? '已收藏' : '收藏' }}
          </button>
        </div>
      </div>
      <img class="hero-cover" :src="activity.cover" :alt="activity.title" />
    </section>

    <section class="detail-grid">
      <div class="main-col">
        <article class="panel">
          <h2>活动概览</h2>
          <div class="overview-grid">
            <div class="overview-item">
              <span>时间</span>
              <strong>{{ activity.date }} · {{ activity.time }}</strong>
            </div>
            <div class="overview-item">
              <span>地点</span>
              <strong>{{ activity.location }}</strong>
            </div>
            <div class="overview-item">
              <span>报名</span>
              <strong>{{ activity.reserved }}/{{ activity.capacity }} 人</strong>
            </div>
            <div class="overview-item">
              <span>报名方式</span>
              <strong>{{ activity.registration }}</strong>
            </div>
          </div>
          <p class="desc">{{ activity.description }}</p>
        </article>

        <article class="panel">
          <h2>活动流程</h2>
          <ul class="agenda">
            <li v-for="step in agenda" :key="step.time">
              <p class="time">{{ step.time }}</p>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </li>
          </ul>
        </article>

        <article class="panel">
          <h2>相关故事</h2>
          <div class="story-grid">
            <figure v-for="story in stories" :key="story.id" class="story-card">
              <img :src="story.cover" :alt="story.title" />
              <figcaption>
                <strong>{{ story.title }}</strong>
                <p>{{ story.summary }}</p>
              </figcaption>
            </figure>
          </div>
        </article>
      </div>

      <aside class="side-col">
        <article class="panel side-panel">
          <h3>组织者</h3>
          <div class="organizer">
            <img :src="organizer.avatar" :alt="organizer.name" />
            <div>
              <strong>{{ organizer.name }}</strong>
              <p>{{ organizer.role }}</p>
            </div>
          </div>
          <button class="btn btn-light full">联系组织者</button>
        </article>

        <article class="panel side-panel">
          <h3>到达方式</h3>
          <div class="map-placeholder">地图模块可接入高德 JS SDK</div>
          <ul class="traffic-list">
            <li><span>公交</span><strong>{{ activity.transit }}</strong></li>
            <li><span>停车</span><strong>{{ activity.parking }}</strong></li>
          </ul>
        </article>

        <article class="panel side-panel">
          <h3>参与提示</h3>
          <ul class="tips-list">
            <li v-for="tip in checklist" :key="tip">{{ tip }}</li>
          </ul>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  enrollLocalActivity,
  favoriteLocalActivity,
  fetchLocalActivityDetail,
  unfavoriteLocalActivity
} from '@/api/localAct';
import { getActivityStatusLabel, getCategoryLabel, getEnrollmentStatusLabel } from '@/constants/localAct';
import type { LocalActivityDetail } from '@/types/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';
import detailCover from '../../../pictures/homePicture2.jpg';
import storyCoverOne from '../../../pictures/homePicture1.jpg';
import storyCoverTwo from '../../../pictures/homePicture3.jpg';
import organizerAvatar from '../../User/userPictures/user1.jpg';

const route = useRoute();
const activityId = computed(() => String(route.params.id ?? '-'));
const username = ref(localStorage.getItem('username') || '');
const loading = ref(false);
const enrollLoading = ref(false);
const favoriteLoading = ref(false);
const errorMsg = ref('');
const actionMessage = ref('');

type ActivityView = {
  title: string;
  subtitle: string;
  date: string;
  time: string;
  location: string;
  capacity: number;
  reserved: number;
  registration: string;
  tags: string[];
  description: string;
  cover: string;
  transit: string;
  parking: string;
  organizer: string;
  organizerRole: string;
  status?: string;
  enrollmentStatus?: string;
  requireCheckin?: boolean;
  favorited?: boolean;
};

const fallbackActivity: ActivityView = {
  title: '旧物再利用市集',
  subtitle: '把闲置物品重新流动起来，让交换和分享成为邻里之间的温暖连接。',
  date: '04/24 周五',
  time: '14:00 - 17:00',
  location: '社区广场 A 区',
  capacity: 16,
  reserved: 14,
  registration: '线上报名 · 自动确认',
  tags: ['市集交换', '绿色行动', '邻里共创'],
  description:
    '活动面向社区居民开放，欢迎带上闲置书籍、玩具、小家电或生活用品参与交换。现场会设置登记、估价、交换和捐赠区域，也会安排志愿者协助整理摊位。',
  cover: detailCover,
  transit: '乘坐 4 路公交至社区广场站，步行约 5 分钟',
  parking: '地下停车场开放 2 小时免费停车',
  organizer: '绿色行动组',
  organizerRole: '社区活动组织者',
  status: 'PUBLISHED',
  requireCheckin: true,
  favorited: false
};

const activity = ref<ActivityView>({ ...fallbackActivity });

const organizer = computed(() => ({
  name: activity.value.organizer || '社区组织者',
  role: activity.value.organizerRole || '社区活动组织者',
  avatar: organizerAvatar
}));

const agenda = computed(() => [
  { time: '活动前 15 分钟', title: '现场签到', desc: activity.value.requireCheckin ? '完成签到确认，领取活动说明。' : '到场后可直接参与活动。' },
  { time: activity.value.time, title: '活动进行', desc: '按组织者安排参与活动流程，注意现场秩序与安全。' },
  { time: '活动结束后', title: '反馈与故事', desc: '可在活动故事中记录收获，也可以给组织者留下反馈。' }
]);

const stories = [
  {
    id: 's1',
    title: '一张旧书桌的新旅程',
    summary: '居民把闲置书桌送给了需要学习角的邻居。',
    cover: storyCoverOne
  },
  {
    id: 's2',
    title: '交换里的邻里问候',
    summary: '20 多位居民在交换中认识了新的朋友。',
    cover: storyCoverTwo
  }
];

const checklist = computed(() => [
  activity.value.requireCheckin ? '建议提前 15 分钟到场完成签到' : '建议提前 10 分钟到场熟悉地点',
  '请留意活动开始前的站内提醒',
  '如需取消报名，请尽量提前操作，方便候补用户补位',
  '活动结束后可发布故事或反馈，帮助社区沉淀经验'
]);

const enrollButtonText = computed(() => {
  const enrollmentStatus = activity.value.enrollmentStatus;
  if (enrollmentStatus) return getEnrollmentStatusLabel(enrollmentStatus);
  if (activity.value.status && activity.value.status !== 'PUBLISHED') return getActivityStatusLabel(activity.value.status);
  if (activity.value.capacity > 0 && activity.value.reserved >= activity.value.capacity) return '加入候补';
  return '立即报名';
});

const enrollDisabled = computed(() => {
  if (activity.value.enrollmentStatus) return true;
  return Boolean(activity.value.status && activity.value.status !== 'PUBLISHED');
});

const normalizeDateValue = (value?: unknown) => {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value.map(Number);
    return new Date(year, month - 1, day, hour, minute);
  }
  const date = new Date(String(value).replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDate = (value?: unknown) => {
  const date = normalizeDateValue(value);
  if (!date) return '待定日期';
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()];
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${weekday}`;
};

const formatTime = (value?: unknown) => {
  const date = normalizeDateValue(value);
  if (!date) return '';
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const formatTimeRange = (startAt?: unknown, endAt?: unknown) => {
  const start = formatTime(startAt);
  const end = formatTime(endAt);
  if (start && end) return `${start} - ${end}`;
  return start || '待定时间';
};

const mapDetailToView = (detail: LocalActivityDetail): ActivityView => {
  const categoryLabel = getCategoryLabel(detail.category);
  const statusLabel = getActivityStatusLabel(detail.status);
  const enrollmentLabel = getEnrollmentStatusLabel(detail.enrollmentStatus);
  return {
    title: detail.title || fallbackActivity.title,
    subtitle: detail.subtitle || `${categoryLabel} · ${statusLabel}`,
    date: formatDate(detail.startAt),
    time: formatTimeRange(detail.startAt, detail.endAt),
    location: detail.location || detail.address || '地点待定',
    capacity: Number(detail.capacity ?? 0),
    reserved: Number(detail.reserved ?? 0),
    registration: enrollmentLabel || `${statusLabel}${detail.allowWaitlist ? ' · 满员可候补' : ''}`,
    tags: detail.tags?.length ? detail.tags : [categoryLabel, statusLabel],
    description: detail.description || '组织者暂未填写详细介绍，请留意后续更新。',
    cover: detail.coverUrl || fallbackActivity.cover,
    transit: detail.latitude && detail.longitude ? `坐标：${detail.latitude.toFixed(5)}, ${detail.longitude.toFixed(5)}` : '组织者暂未提供具体路线',
    parking: detail.address || '请以活动地点现场安排为准',
    organizer: detail.organizer || fallbackActivity.organizer,
    organizerRole: '活动组织者',
    status: detail.status,
    enrollmentStatus: detail.enrollmentStatus,
    requireCheckin: detail.requireCheckin,
    favorited: Boolean(detail.favorited)
  };
};

const loadActivityDetail = async (clearAction = true) => {
  const id = activityId.value;
  if (!id || id === '-') return;

  loading.value = true;
  errorMsg.value = '';
  if (clearAction) {
    actionMessage.value = '';
  }
  try {
    const detail = await fetchLocalActivityDetail(id, username.value);
    activity.value = mapDetailToView(detail);
  } catch (error) {
    activity.value = { ...fallbackActivity };
    errorMsg.value = error instanceof Error ? `${error.message}，已展示演示详情。` : '活动详情加载失败，已展示演示详情。';
  } finally {
    loading.value = false;
  }
};

const handleEnrollClick = async () => {
  if (!username.value) {
    actionMessage.value = '请先登录后再报名活动。';
    return;
  }
  if (enrollDisabled.value || enrollLoading.value) {
    return;
  }
  enrollLoading.value = true;
  actionMessage.value = '';
  errorMsg.value = '';
  try {
    const result = await enrollLocalActivity(activityId.value, username.value);
    const statusLabel = getEnrollmentStatusLabel(result.status);
    actionMessage.value = result.status === 'waitlist' && result.waitlistRank
      ? `已加入候补，第 ${result.waitlistRank} 位。`
      : `报名成功，当前状态：${statusLabel || result.status}。`;
    await loadActivityDetail(false);
  } catch (error) {
    actionMessage.value = '';
    errorMsg.value = error instanceof Error ? error.message : '报名失败，请稍后重试。';
  } finally {
    enrollLoading.value = false;
  }
};

const handleFavoriteClick = async () => {
  if (!username.value) {
    actionMessage.value = '请先登录后再收藏活动。';
    return;
  }
  if (favoriteLoading.value) {
    return;
  }

  favoriteLoading.value = true;
  actionMessage.value = '';
  errorMsg.value = '';
  try {
    if (activity.value.favorited) {
      await unfavoriteLocalActivity(activityId.value, username.value);
      activity.value = { ...activity.value, favorited: false };
      actionMessage.value = '已取消收藏。';
    } else {
      await favoriteLocalActivity(activityId.value, username.value);
      activity.value = { ...activity.value, favorited: true };
      actionMessage.value = '已收藏，可在“我的收藏”中查看。';
    }
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '收藏操作失败，请稍后重试。';
  } finally {
    favoriteLoading.value = false;
  }
};

onMounted(loadActivityDetail);

watch(
  () => route.params.id,
  () => {
    loadActivityDetail();
  }
);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lad-page {
  color: #0f172a;
}

.state-banner {
  max-width: 1280px;
  margin: 0 auto 16px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(78, 142, 247, 0.08);
  color: #2563eb;
  font-size: 13px;
}

.state-banner.error {
  background: rgba(220, 38, 38, 0.06);
  color: #dc2626;
}

.state-banner.info {
  background: rgba(56, 185, 130, 0.08);
  color: #1aa053;
}

.hero {
  padding: 32px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 28px;
  align-items: stretch;
}

.hero-content {
  display: flex;
  flex-direction: column;
}

.subtitle {
  margin: 0 0 18px;
  font-size: 14.5px;
  line-height: 1.65;
  color: #64748b;
  max-width: 560px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 22px;
}

.hero-tags span {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f8fafc;
  font-size: 11.5px;
  font-weight: 500;
  color: #475569;
}

.id-chip {
  background: rgba(255, 107, 44, 0.08) !important;
  color: #f25a1b !important;
}

.hero-meta {
  margin-bottom: 22px;
  padding: 18px 20px;
  border-radius: 14px;
  background: #f8fafc;
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
}

.hm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 140px;
}

.hm-item i {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #ffffff;
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.hm-item div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.hm-item span {
  font-size: 11px;
  color: #94a3b8;
}

.hm-item strong {
  margin-top: 2px;
  font-size: 13px;
  font-weight: 500;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

.hero-cover {
  width: 100%;
  height: 100%;
  min-height: 280px;
  object-fit: cover;
  border-radius: 14px;
}

.detail-grid {
  margin-top: 24px !important;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  padding: 28px;
}

.panel h2 {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item span {
  font-size: 11.5px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.overview-item strong {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  line-height: 1.5;
}

.desc {
  margin: 18px 0 0;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
  font-size: 14px;
  line-height: 1.75;
  color: #475569;
}

.agenda {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agenda li {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.agenda li:last-child {
  border-bottom: none;
}

.time {
  margin: 0;
  font-size: 12.5px;
  color: #ff6b2c;
  font-weight: 500;
}

.agenda strong {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.agenda p {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.6;
}

.story-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.story-card {
  margin: 0;
  border-radius: 14px !important;
  overflow: hidden !important;
  background: #f8fafc !important;
  box-shadow: none !important;
  cursor: pointer;
  transition: transform 0.22s ease;
}

.story-card:hover {
  transform: translateY(-2px);
}

.story-card img {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.story-card figcaption {
  padding: 14px 16px;
}

.story-card strong {
  font-size: 13.5px;
  font-weight: 500;
  color: #0f172a;
}

.story-card p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}

.side-panel h3 {
  margin: 0 0 14px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
}

.organizer {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.organizer img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.organizer strong {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.organizer p {
  margin: 3px 0 0 !important;
  font-size: 11.5px !important;
  color: #94a3b8 !important;
}

.map-placeholder {
  border: none;
  border-radius: 12px;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 107, 44, 0.08), transparent 40%),
    #f8fafc;
  color: #94a3b8;
  min-height: 120px;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  text-align: center;
  padding: 12px;
}

.traffic-list,
.tips-list {
  margin: 14px 0 0 !important;
  padding: 0 !important;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.traffic-list li {
  display: grid;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.traffic-list li:last-child {
  border-bottom: none;
}

.traffic-list span {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.traffic-list strong {
  font-size: 13px !important;
  font-weight: 500 !important;
  color: #475569 !important;
  line-height: 1.55;
}

.tips-list li {
  position: relative;
  padding-left: 20px;
  font-size: 12.5px !important;
  line-height: 1.65 !important;
  color: #64748b !important;
}

.tips-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #ff6b2c;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px !important;
  padding: 0 20px !important;
  border-radius: 999px !important;
  border: none !important;
  font-size: 13.5px !important;
  font-weight: 500 !important;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.2s ease;
}

.btn i {
  font-size: 12px;
}

.btn-primary {
  background: #ff6b2c !important;
  color: #ffffff !important;
}

.btn-primary:hover:not(:disabled) {
  background: #f25a1b !important;
  transform: translateY(-1px);
}

.btn-light {
  background: #f8fafc !important;
  color: #475569 !important;
}

.btn-light:hover:not(:disabled) {
  background: #eef2f6 !important;
  color: #0f172a !important;
}

.btn.full {
  width: 100%;
  justify-content: center;
}

@media (max-width: 1100px) {
  .hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-cover {
    max-height: 320px;
  }
}

@media (max-width: 900px) {
  .overview-grid,
  .story-grid {
    grid-template-columns: 1fr;
  }

  .agenda li {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .hero-meta {
    gap: 16px;
  }
}
</style>
