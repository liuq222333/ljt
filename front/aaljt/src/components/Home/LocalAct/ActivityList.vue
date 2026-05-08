<template>
  <dhstyle />
  <div class="lal-page">
    <main class="lal-shell">
      <header class="page-head">
        <div class="page-head-text">
          <p class="breadcrumb">发现活动</p>
          <h1>全部活动</h1>
          <p class="subtitle">发现城市中正在发生的精彩活动，按兴趣、时间与地点筛选。</p>
        </div>
        <div class="page-head-side">
          <div class="page-head-stat">
            <strong>{{ displayTotal }}</strong>
            <span>个进行中的活动</span>
          </div>
          <button class="publish-btn" type="button" @click="goToPublish">
            <i class="fas fa-plus"></i>
            发布活动
          </button>
        </div>
      </header>

      <div class="content-grid">
        <aside class="filter-panel">
          <section v-for="group in filterGroups" :key="group.key" class="filter-group">
            <div class="filter-label">{{ group.label }}</div>
            <div class="filter-options">
              <button
                v-for="option in group.options"
                :key="option.value"
                type="button"
                :class="['filter-pill', { active: filters[group.key] === option.value }]"
                @click="setFilter(group.key, option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </section>

          <button class="reset-btn" type="button" @click="resetFilters">
            <i class="fas fa-rotate-left"></i>
            重置筛选
          </button>
        </aside>

        <section class="list-panel">
          <div class="toolbar">
            <label class="search-box">
              <i class="fas fa-magnifying-glass"></i>
              <input v-model="keyword" type="text" placeholder="搜索活动、地点或关键词" />
            </label>

            <label class="sort-select">
              <select v-model="sortMode">
                <option value="popular">最受欢迎</option>
                <option value="latest">最新发布</option>
                <option value="soon">即将开始</option>
              </select>
              <i class="fas fa-chevron-down"></i>
            </label>
          </div>

          <div v-if="loading" class="empty-state">
            <h3>正在加载活动</h3>
            <p>正在同步社区活动列表。</p>
          </div>

          <div v-else-if="sortedActivities.length" class="activity-grid">
            <article
              v-for="activity in sortedActivities"
              :key="activity.id"
              class="activity-card"
              @click="goToDetail(activity.id)"
            >
              <div class="card-cover">
                <img :src="activity.cover" :alt="activity.title" />
                <span class="card-tag" :class="activity.category">{{ activity.categoryLabel }}</span>
              </div>
              <div class="card-body">
                <h3>{{ activity.title }}</h3>
                <p class="card-desc">{{ activity.description }}</p>
                <div class="card-meta">
                  <span><i class="far fa-calendar"></i>{{ activity.date }}</span>
                  <span><i class="fas fa-location-dot"></i>{{ activity.location }}</span>
                </div>
                <div class="card-foot">
                  <span class="card-people">{{ activity.participants }} 人已报名</span>
                  <span class="card-arrow">
                    查看详情
                    <i class="fas fa-arrow-right"></i>
                  </span>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="empty-state">
            <h3>暂无匹配活动</h3>
            <p>{{ errorMsg || '可以调整分类、时间或地点筛选，再看看有没有更合适的活动。' }}</p>
            <button type="button" @click="resetFilters">重置筛选</button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchLocalActivities } from '@/api/localAct';
import { getCategoryLabel } from '@/constants/localAct';
import type { LocalActivityListItem } from '@/types/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';
import coverMarket from '../../../pictures/homePicture1.jpg';
import coverRun from '../../../pictures/homePicture2.jpg';
import coverNature from '../../../pictures/homePicture3.jpg';
import coverSalon from '../CommunityFeed/Pictures/1.png';

type FilterKey = 'category' | 'time' | 'place' | 'sort';

type Activity = {
  id: number;
  title: string;
  category: string;
  categoryLabel: string;
  organizer: string;
  relativeTime: string;
  date: string;
  location: string;
  participants: number;
  description: string;
  cover: string;
  timeType: string;
  placeType: string;
  sortType: string;
};

const router = useRouter();
const keyword = ref('');
const sortMode = ref('latest');

const filters = reactive<Record<FilterKey, string>>({
  category: 'all',
  time: 'all',
  place: 'city',
  sort: 'latest'
});

const filterGroups: Array<{
  key: FilterKey;
  label: string;
  icon: string;
  options: Array<{ label: string; value: string }>;
}> = [
  {
    key: 'category',
    label: '活动分类',
    icon: 'fa-tag',
    options: [
      { label: '全部', value: 'all' },
      { label: '讲座', value: 'lecture' },
      { label: '运动', value: 'sport' },
      { label: '公益', value: 'welfare' },
      { label: '亲子', value: 'kids' },
      { label: '市集', value: 'market' },
      { label: '志愿者', value: 'volunteer' },
      { label: '沙龙', value: 'salon' }
    ]
  },
  {
    key: 'time',
    label: '时间筛选',
    icon: 'fa-clock',
    options: [
      { label: '全部时间', value: 'all' },
      { label: '本周末', value: 'weekend' },
      { label: '最近7天', value: 'week' },
      { label: '本月', value: 'month' }
    ]
  },
  {
    key: 'place',
    label: '地点',
    icon: 'fa-location-dot',
    options: [
      { label: '全城', value: 'city' },
      { label: '附近', value: 'nearby' },
      { label: '室内', value: 'indoor' },
      { label: '户外', value: 'outdoor' }
    ]
  },
  {
    key: 'sort',
    label: '排序方式',
    icon: 'fa-arrow-down-wide-short',
    options: [
      { label: '最新发布', value: 'latest' },
      { label: '最受欢迎', value: 'popular' },
      { label: '即将开始', value: 'soon' }
    ]
  }
];

const fallbackActivities: Activity[] = [
  {
    id: 2004,
    title: '城市夜跑计划',
    category: 'sport',
    categoryLabel: '运动',
    organizer: '活力跑团',
    relativeTime: '今天 18:30',
    date: '05月25日（周六）19:30',
    location: '滨江公园跑步广场',
    participants: 128,
    description: '一起用脚步点亮城市夜色，5公里轻松夜跑，配速友好，适合各水平跑者。',
    cover: coverRun,
    timeType: 'weekend',
    placeType: 'outdoor',
    sortType: 'popular'
  },
  {
    id: 2005,
    title: '社区公益植树日',
    category: 'welfare',
    categoryLabel: '公益',
    organizer: '绿色家园社区',
    relativeTime: '2天后',
    date: '05月26日（周日）09:00',
    location: '山林公园植树区',
    participants: 186,
    description: '为社区增添一片绿意，和邻里一起种下希望的树苗，守护我们的家园。',
    cover: coverNature,
    timeType: 'weekend',
    placeType: 'outdoor',
    sortType: 'popular'
  },
  {
    id: 2003,
    title: '亲子自然观察课',
    category: 'kids',
    categoryLabel: '亲子',
    organizer: '童心成长营',
    relativeTime: '3天后',
    date: '05月27日（周一）09:30',
    location: '湿地公园自然教育区',
    participants: 156,
    description: '走进自然，观察昆虫与植物，亲子互动学习，培养孩子的好奇心与观察力。',
    cover: coverMarket,
    timeType: 'week',
    placeType: 'outdoor',
    sortType: 'soon'
  },
  {
    id: 2006,
    title: 'AI 分享沙龙',
    category: 'salon',
    categoryLabel: '沙龙',
    organizer: '未来研究所',
    relativeTime: '4天后',
    date: '05月28日（周二）14:00',
    location: '创新空间 3F 活动厅',
    participants: 98,
    description: '探索 AI 如何改变生活与工作，行业嘉宾现场分享，交流前沿观点与应用案例。',
    cover: coverSalon,
    timeType: 'week',
    placeType: 'indoor',
    sortType: 'latest'
  },
  {
    id: 2001,
    title: '旧物再利用市集',
    category: 'market',
    categoryLabel: '市集',
    organizer: '绿色行动组',
    relativeTime: '本周末',
    date: '05月30日（周六）14:00',
    location: '社区广场 A 区',
    participants: 79,
    description: '把闲置好物重新流动起来，让社区关系在交换和分享中慢慢升温。',
    cover: coverMarket,
    timeType: 'month',
    placeType: 'nearby',
    sortType: 'latest'
  }
];

const activities = ref<Activity[]>([]);
const loading = ref(false);
const errorMsg = ref('');

const normalizeDateValue = (value?: unknown) => {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value.map(Number);
    return new Date(year, month - 1, day, hour, minute);
  }
  const date = new Date(String(value).replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatActivityDate = (startAt?: unknown) => {
  const date = normalizeDateValue(startAt);
  if (!date) return '时间待定';
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()];
  return `${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日（${weekday}）${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const resolveTimeType = (startAt?: unknown) => {
  const date = normalizeDateValue(startAt);
  if (!date) return 'month';
  const diffDays = Math.ceil((date.getTime() - Date.now()) / 86400000);
  if ([0, 6].includes(date.getDay())) return 'weekend';
  if (diffDays >= 0 && diffDays <= 7) return 'week';
  return 'month';
};

const mapApiActivity = (item: LocalActivityListItem, index: number): Activity => {
  const category = item.category || item.categoryCode || 'market';
  const fallback = fallbackActivities[index % fallbackActivities.length];
  return {
    id: Number(item.id),
    title: item.title || fallback.title,
    category,
    categoryLabel: getCategoryLabel(category),
    organizer: item.organizer || '社区组织',
    relativeTime: '',
    date: formatActivityDate(item.startAt),
    location: item.location || item.locationText || item.address || '地点待定',
    participants: Number(item.reserved ?? 0),
    description: item.description || item.subtitle || '组织者暂未填写活动简介。',
    cover: item.coverUrl || fallback.cover,
    timeType: resolveTimeType(item.startAt),
    placeType: item.distanceKm != null && item.distanceKm <= 3 ? 'nearby' : 'outdoor',
    sortType: 'latest'
  };
};

const loadActivities = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const list = await fetchLocalActivities({ status: 'PUBLISHED', size: 50 });
    activities.value = list.length ? list.map(mapApiActivity) : fallbackActivities;
  } catch (error) {
    activities.value = fallbackActivities;
    errorMsg.value = error instanceof Error ? `${error.message}，已展示演示活动。` : '活动列表加载失败，已展示演示活动。';
  } finally {
    loading.value = false;
  }
};

const filteredActivities = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  return activities.value.filter((activity) => {
    if (filters.category !== 'all' && activity.category !== filters.category) return false;
    if (filters.time !== 'all' && activity.timeType !== filters.time && !(filters.time === 'month' && ['week', 'weekend', 'month'].includes(activity.timeType))) return false;
    if (filters.place !== 'city' && activity.placeType !== filters.place) return false;
    if (!text) return true;
    return [activity.title, activity.organizer, activity.location, activity.description].some((value) =>
      value.toLowerCase().includes(text)
    );
  });
});

const sortedActivities = computed(() => {
  const list = [...filteredActivities.value];
  const mode = sortMode.value || filters.sort;
  if (mode === 'popular') {
    return list.sort((a, b) => b.participants - a.participants);
  }
  if (mode === 'soon') {
    return list.sort((a, b) => a.id - b.id);
  }
  return list;
});

const displayTotal = computed(() => {
  const hasActiveFilter = filters.category !== 'all' || filters.time !== 'all' || filters.place !== 'city' || keyword.value.trim();
  return hasActiveFilter ? filteredActivities.value.length : activities.value.length;
});

const resetFilters = () => {
  filters.category = 'all';
  filters.time = 'all';
  filters.place = 'city';
  filters.sort = 'latest';
  sortMode.value = 'latest';
  keyword.value = '';
};

const setFilter = (key: FilterKey, value: string) => {
  filters[key] = value;
  if (key === 'sort') {
    sortMode.value = value;
  }
};

const goToDetail = (id: number) => {
  router.push(`/local-act/${id}`);
};

const goToPublish = () => {
  router.push('/local-act/publish');
};

onMounted(loadActivities);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lal-page {
  min-height: 100vh;
  padding: 96px clamp(20px, 4vw, 56px) 72px;
  background: #fafbfc;
  color: #0f172a;
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.lal-shell {
  max-width: 1280px;
  margin: 0 auto;
}

/* page head */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 8px 4px 32px;
}

.breadcrumb {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.page-head h1 {
  margin: 14px 0 0;
  font-size: 36px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  font-weight: 600;
  color: #0f172a;
}

.subtitle {
  margin: 12px 0 0;
  max-width: 620px;
  color: #64748b;
  font-size: 14.5px;
  line-height: 1.65;
}

.page-head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
  padding-bottom: 4px;
}

.page-head-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  white-space: nowrap;
}

.page-head-stat strong {
  font-size: 28px;
  font-weight: 600;
  color: #ff6b2c;
  letter-spacing: -0.02em;
}

.page-head-stat span {
  font-size: 13px;
  color: #94a3b8;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: 999px;
  background: #ff6b2c;
  color: #ffffff;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.2s ease;
}

.publish-btn i {
  font-size: 11px;
}

.publish-btn:hover {
  background: #f25a1b;
  transform: translateY(-1px);
}

/* layout */
.content-grid {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 40px;
  align-items: start;
}

/* filter panel */
.filter-panel {
  position: sticky;
  top: 96px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 4px 0;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-pill {
  min-height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.filter-pill:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.filter-pill.active {
  background: #0f172a;
  color: #ffffff;
}

.reset-btn {
  margin-top: 4px;
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.18s ease;
}

.reset-btn:hover {
  color: #ff6b2c;
}

/* list panel */
.list-panel {
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.search-box,
.sort-select {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  border-radius: 12px;
  background: #ffffff;
  color: #94a3b8;
  transition: box-shadow 0.18s ease;
}

.search-box {
  flex: 1;
  padding: 0 16px;
}

.search-box:focus-within,
.sort-select:focus-within {
  box-shadow: 0 0 0 2px rgba(255, 107, 44, 0.12);
}

.search-box input,
.sort-select select {
  border: none;
  outline: none;
  background: transparent;
  color: #0f172a;
  font: inherit;
  width: 100%;
}

.search-box input::placeholder {
  color: #94a3b8;
}

.sort-select {
  position: relative;
  padding: 0 36px 0 14px;
}

.sort-select select {
  appearance: none;
  min-width: 132px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
}

.sort-select i {
  position: absolute;
  right: 14px;
  font-size: 11px;
  color: #94a3b8;
  pointer-events: none;
}

/* card grid */
.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.activity-card {
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.activity-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
}

.card-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #f1f5f9;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.activity-card:hover .card-cover img {
  transform: scale(1.04);
}

.card-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  color: #239b61;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.card-tag.kids { color: #ff6b2c; }
.card-tag.salon { color: #7563ff; }
.card-tag.market { color: #d98200; }
.card-tag.welfare { color: #239b61; }
.card-tag.sport { color: #4e8ef7; }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 18px 20px;
}

.card-body h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: #0f172a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  font-size: 12.5px;
  color: #94a3b8;
}

.card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.card-meta i {
  width: 12px;
  text-align: center;
  font-size: 11px;
  color: #cbd5e1;
}

.card-foot {
  margin-top: 6px;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
}

.card-people {
  font-size: 12.5px;
  color: #64748b;
}

.card-arrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 500;
  color: #ff6b2c;
  transition: gap 0.2s ease;
}

.activity-card:hover .card-arrow {
  gap: 10px;
}

.card-arrow i {
  font-size: 11px;
}

/* empty state */
.empty-state {
  padding: 72px 24px;
  text-align: center;
  border-radius: 16px;
  background: #ffffff;
}

.empty-state h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.empty-state p {
  margin: 10px 0 22px;
  color: #94a3b8;
  font-size: 13.5px;
}

.empty-state button {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 999px;
  background: #0f172a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 200px minmax(0, 1fr);
    gap: 28px;
  }
}

@media (max-width: 860px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .filter-panel {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 18px 24px;
  }

  .filter-group {
    flex: 1 1 220px;
  }
}

@media (max-width: 600px) {
  .lal-page {
    padding: 88px 16px 40px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
    padding-bottom: 24px;
  }

  .page-head-side {
    align-items: flex-start;
    flex-direction: row;
    width: 100%;
    justify-content: space-between;
  }

  .page-head h1 {
    font-size: 28px;
  }

  .activity-grid {
    grid-template-columns: 1fr;
  }
}
</style>
