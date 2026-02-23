<template>
  <dhstyle />
  <div class="la-page">
    <section class="la-hero">
      <div class="hero-content">
        <p class="eyebrow">社区活动 · 邻里共建</p>
        <h1>发现身边的精彩，<br>和邻居一起创造温暖记忆</h1>
        <p class="subtitle">
          本地活动实时更新，志愿互助、亲子游园、技能共学……<br>挑一个喜欢的日程，加入你的社区时刻。
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
            <span class="stat-label">邻里报名</span>
            <strong>{{ totalParticipants }}</strong>
            <small>人次</small>
          </div>
        </div>
      </div>
      <div class="hero-map">
        <div class="map-card">
          <div class="map-header">
            <span>社区热力图</span>
            <button class="ghost xs" @click="refreshHeatmap">刷新</button>
          </div>
          <div class="map-illustration">
            <div class="pin pin-main"></div>
            <div class="pin pin-secondary"></div>
            <div class="pin pin-tertiary"></div>
            <!-- Simple decorative map grid lines -->
            <div class="map-grid"></div>
          </div>
          <p class="map-desc">
            基于高德定位，实时显示 2km 内的热门活动热度分布。<br />
            <span class="refresh-tip">上次更新：{{ heatmapRefreshedLabel }}</span>
          </p>
          <ul class="map-legend">
            <li><span class="dot dot-hot"></span>高热度</li>
            <li><span class="dot dot-mid"></span>一般</li>
            <li><span class="dot dot-low"></span>筹备中</li>
          </ul>
        </div>
      </div>
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

<div class="la-layout">
    <aside class="filters-panel">
      <h3>筛选活动</h3>
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
            {{ distance }} km
          </button>
          <button class="chip ghost" @click="selectedRadius = null">不限</button>
        </div>
      </div>

      <div class="filter-group">
        <label class="checkbox">
          <input type="checkbox" v-model="showOnlyHot" />
          <span>仅看热门 / 名额紧张</span>
        </label>
      </div>

      <div class="schedule-box">
        <div class="schedule-header">
          <div>
            <p class="group-label">我的报名</p>
            <small>签到即可累计信用分</small>
          </div>
          <button class="ghost sm" @click="goToMyEnrollments">查看全部</button>
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
      </div>
    </aside>

    <div class="layout-center">
      <section class="events-panel">
        <div class="panel-header">
          <div>
            <h3>本地活动推荐</h3>
            <p class="desc">根据你的兴趣和位置为你挑选的社区活动</p>
          </div>
          <div class="view-switch">
            <button
              :class="['ghost', 'sm', currentView === 'list' ? 'active' : '']"
              @click="setView('list')"
            >
              列表
            </button>
            <button
              :class="['ghost', 'sm', currentView === 'map' ? 'active' : '']"
              @click="setView('map')"
            >
              地图
            </button>
          </div>
        </div>

        <div class="event-cards">
          <div
            v-for="event in filteredEvents"
            :key="event.id"
            class="event-card"
            :class="{ hot: event.highlight }"
            @click="selectEvent(event)"
          >
            <div class="card-img" :style="{ backgroundImage: `url(${event.cover})` }">
              <span v-if="event.highlight" class="badge">热门</span>
            </div>
            <div class="card-body">
              <div class="card-top">
                <span class="category">{{ resolveCategoryLabel(event.category) }}</span>
                <span class="distance">{{ event.distance }}km</span>
              </div>
              <h4>{{ event.title }}</h4>
              <p class="time">{{ event.date }} · {{ event.timeRange }}</p>
              <p class="location"><i class="fas fa-map-marker-alt"></i>{{ event.location }}</p>
              <div class="tags">
                <span v-for="tag in event.tags" :key="tag">{{ tag }}</span>
              </div>
              <div class="meta">
                <span>组织者：{{ event.organizer }}</span>
                <span>{{ event.reserved }}/{{ event.capacity }} 人</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="map-panel">
        <div class="map-header">
          <div>
            <p class="eyebrow dark">附近热力</p>
            <small>基于高德地图热力图</small>
          </div>
          <button class="ghost sm" @click="refreshHeatmap">刷新热力</button>
        </div>
        <div id="heatmap-container"></div>
      </section>
    </div>

    <aside class="detail-panel" v-if="selectedEvent">
        <div class="detail-card">
          <p class="eyebrow dark">活动详情</p>
          <h3>{{ selectedEvent.title }}</h3>
          <p class="detail-desc">{{ selectedEvent.description }}</p>
          <ul class="detail-info">
            <li><i class="fas fa-clock"></i>{{ selectedEvent.date }} · {{ selectedEvent.timeRange }}</li>
            <li><i class="fas fa-map-marker-alt"></i>{{ selectedEvent.location }}</li>
            <li><i class="fas fa-users"></i>剩余 {{ selectedEvent.capacity - selectedEvent.reserved }} 个名额</li>
            <li><i class="fas fa-shield-alt"></i>{{ selectedEvent.status }}</li>
          </ul>
          <div class="detail-actions">
            <button class="primary full" @click="goToDetail()">预约报名</button>
            <button class="ghost full" @click="goToNotifications">加入提醒</button>
          </div>
          <div class="detail-tags">
            <span v-for="tag in selectedEvent.tags" :key="tag">{{ tag }}</span>
          </div>
        </div>
      </aside>
    </div>

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
import { computed, ref, onMounted } from 'vue';
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

const events = ref<LocalEvent[]>([]);
type HeatPoint = { lat: number; lon: number; weight: number };
const heatmapPoints = ref<HeatPoint[]>([]);
const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const AMAP_KEY = (import.meta as any)?.env?.VITE_AMAP_KEY ?? '';

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

const searchText = ref('');
const address = ref('');
const selectedCategory = ref('all');
const selectedRadius = ref<number | null>(null);
const showOnlyHot = ref(false);
const currentView = ref<'list' | 'map'>('list');
const heatmapUpdatedAt = ref(new Date());

const filteredEvents = computed(() => {
  return events.value.filter((event) => {
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
  });
});

const heatmapRefreshedLabel = computed(() =>
  heatmapUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
);

const selectedEvent = ref<LocalEvent | null>(events.value[0] ?? null);

const totalParticipants = computed(() => events.value.reduce((sum, item) => sum + item.reserved, 0));
const totalVolunteerHours = computed(() => Math.round(events.value.length * 3.5));

const toggleRadius = (distance: number) => {
  selectedRadius.value = selectedRadius.value === distance ? null : distance;
};

const selectEvent = (event: LocalEvent) => {
  selectedEvent.value = event;
};

const refreshHeatmap = () => {
  heatmapUpdatedAt.value = new Date();
  fetchHeatmap();
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

const setView = (view: 'list' | 'map') => {
  currentView.value = view;
  if (view === 'map') {
    router.push('/local-act/map-view');
  } else {
    router.push('/local-act');
  }
};



const resolveCategoryLabel = (id: string) => categories.find((c) => c.id === id)?.label ?? '社区活动';

const pinAddress = () => {
  if (!address.value) {
    address.value = '溪语社区 · 中央花园';
  }
};

const fetchEvents = async () => {
  // 默认经纬度：如果未能获取设备定位，使用一个社区中心点
  let lat = 23.1291;
  let lon = 113.2644;
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
    if (!resp.ok || data?.code !== 200 || !Array.isArray(data?.data)) {
      return;
    }
    events.value = data.data.map((item: any, idx: number) => {
      return {
        id: item.id,
        title: item.title,
        category: item.category || 'all',
        date: item.startAt ? item.startAt.split(' ')[0] : '时间待定',
        timeRange: item.startAt && item.endAt ? `${item.startAt.split(' ')[1] ?? ''} - ${item.endAt.split(' ')[1] ?? ''}` : '时间待定',
        location: item.location || '地点待定',
        distance: item.distanceKm ?? Math.round((Math.random() * 3 + 0.5) * 10) / 10,
        tags: ['社区活动'],
        capacity: item.capacity || 0,
        reserved: Math.min(item.capacity || 0, Math.floor((item.capacity || 0) * 0.6)),
        organizer: '社区',
        status: item.status || 'PUBLISHED',
        cover: item.coverUrl || 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=60',
        highlight: idx < 2,
        description: ''
              } as LocalEvent;
    });
    // 默认选中第一个活动
    if (events.value.length > 0 && !selectedEvent.value) {
      selectedEvent.value = events.value[0];
    }
  } catch (e) {
    // ignore
  }
};

onMounted(() => {
  // 先尝试获取位置，再请求
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords;
        // 更新查询参数后再请求
        const qs = new URLSearchParams({
          lat: String(latitude),
          lon: String(longitude),
          radiusKm: selectedRadius.value ? String(selectedRadius.value) : '5',
          size: '20'
        });
        if (selectedCategory.value && selectedCategory.value !== 'all') qs.set('category', selectedCategory.value);
        if (searchText.value.trim()) qs.set('keyword', searchText.value.trim());
        fetch(`${API_BASE}/api/local-act/activities/nearby?${qs.toString()}`)
          .then((resp) => resp.json().then((d) => ({ resp, d })))
          .then(({ resp, d }) => {
            if (resp.ok && d?.code === 200 && Array.isArray(d.data)) {
              events.value = d.data.map((item: any, idx: number) => ({
                id: item.id,
                title: item.title,
                category: item.category || 'all',
                date: item.startAt ? item.startAt.split(' ')[0] : '时间待定',
                timeRange: item.startAt && item.endAt ? `${item.startAt.split(' ')[1] ?? ''} - ${item.endAt.split(' ')[1] ?? ''}` : '时间待定',
                location: item.location || '地点待定',
                distance: item.distanceKm ?? Math.round((Math.random() * 3 + 0.5) * 10) / 10,
                tags: ['社区活动'],
                capacity: item.capacity || 0,
                reserved: Math.min(item.capacity || 0, Math.floor((item.capacity || 0) * 0.6)),
                organizer: '社区',
                status: item.status || 'PUBLISHED',
                cover: item.coverUrl || 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=60',
                highlight: idx < 2,
                description: ''
              } as LocalEvent));
              // 默认选中第一个活动
              if (events.value.length > 0 && !selectedEvent.value) {
                selectedEvent.value = events.value[0];
              }
            } else {
              fetchEvents();
            }
          })
          .catch(() => fetchEvents());
      },
      () => fetchEvents(),
      { timeout: 3000 }
    );
  } else {
    fetchEvents();
  }
  initHeatmap();
  fetchHeatmap();
});

// --- Heatmap (AMap) ---
const mapReady = ref(false);
let mapInstance: any = null;
let heatmapLayer: any = null;

const loadAmapScript = () =>
  new Promise<void>((resolve, reject) => {
    if ((window as any).AMap) {
      resolve();
      return;
    }
    if (!AMAP_KEY) {
      reject(new Error('缺少 VITE_AMAP_KEY'));
      return;
    }
    const script = document.createElement('script');
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.HeatMap`;
    script.onload = () => resolve();
    script.onerror = reject;
    document.head.appendChild(script);
  });

const initHeatmap = async () => {
  try {
    await loadAmapScript();
    const AMap = (window as any).AMap;
    mapInstance = new AMap.Map('heatmap-container', {
      zoom: 13,
      center: [113.2644, 23.1291],
      viewMode: '2D'
    });
    heatmapLayer = new AMap.HeatMap(mapInstance, {
      radius: 25,
      opacity: [0, 0.8]
    });
    mapReady.value = true;
    updateHeatmapLayer();
  } catch (e) {
    console.error('加载高德地图失败', e);
  }
};

const updateHeatmapLayer = () => {
  if (!mapReady.value || !heatmapLayer) return;
  const data = heatmapPoints.value.map((p) => ({ lng: p.lon, lat: p.lat, count: p.weight }));
  heatmapLayer.setDataSet({ data, max: Math.max(...(data.map((d) => d.count) || [1])) || 1 });
};

const fetchHeatmap = async () => {
  // 使用当前时间的中心点（若未获取定位则用默认中心）
  let lat = 23.1291;
  let lon = 113.2644;
  const params = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    radiusKm: selectedRadius.value ? String(selectedRadius.value) : '100000',
    size: '500'
  });
  if (selectedCategory.value && selectedCategory.value !== 'all') {
    params.set('category', selectedCategory.value);
  }
  if (searchText.value.trim()) {
    params.set('keyword', searchText.value.trim());
  }
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/heatmap?${params.toString()}`);
    const data = await resp.json().catch(() => ({}));
    if (resp.ok && data?.code === 200 && Array.isArray(data?.data)) {
      heatmapPoints.value = data.data;
      updateHeatmapLayer();
    }
  } catch (e) {
    // ignore
  }
};
</script>

<style scoped>
:global(body) {
  background: #f4f6f8;
  margin: 0;
}

.la-page {
  padding-top: 25px;
  background: linear-gradient(180deg, #eff2f5 0%, #f4f6f8 100%);
  min-height: 100vh;
  color: #1f2a37;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* --- Hero Section --- */
.la-hero {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 32px;
  padding: 80px 48px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.15em;
  font-size: 13px;
  color: #a7f3d0;
  font-weight: 700;
  margin-bottom: 16px;
  display: block;
}
.eyebrow.dark { color: #10b981; }

.hero-content {
  /* Themed Background Image */
  background: linear-gradient(135deg, rgba(6, 41, 25, 0.85) 0%, rgba(26, 160, 83, 0.5) 100%),
              url('https://images.unsplash.com/photo-1511632765486-a01980e01a18?ixlib=rb-1.2.1&auto=format&fit=crop&w=2000&q=80') center/cover no-repeat;
  color: #fff;
  padding: 48px;
  border-radius: 32px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(17, 54, 30, 0.25);
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.hero-content h1 {
  font-size: 40px;
  margin: 0 0 20px 0;
  line-height: 1.2;
  font-weight: 800;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.subtitle {
  color: rgba(255, 255, 255, 0.9);
  font-size: 17px;
  line-height: 1.6;
  margin-bottom: 32px;
  max-width: 600px;
  font-weight: 400;
}

/* Glassmorphism Inputs */
.search-panel {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 0 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
}

.input-wrap:focus-within {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
}

.input-wrap i {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
}

.input-wrap input {
  border: none;
  background: transparent;
  color: #fff;
  padding: 16px 0;
  width: 100%;
  font-size: 15px;
  outline: none;
}

.input-wrap input::placeholder {
  color: rgba(255, 255, 255, 0.75);
}

.primary.hero-btn {
  padding: 0 32px;
  font-size: 15px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.2);
}

.primary {
  border: none;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  padding: 12px 26px;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4);
  filter: brightness(1.1);
}

/* Glassmorphism Stats */
.hero-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  flex: 1;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 20px;
  padding: 18px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  text-align: center;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  background: rgba(0, 0, 0, 0.35);
}

.stat-label {
  font-size: 12px;
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.8);
  text-transform: uppercase;
}

.stat-card strong {
  font-size: 28px;
  display: block;
  margin: 8px 0;
  color: #fff;
  font-weight: 700;
}

.stat-card small {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* Map Section */
.hero-map {
  display: flex;
  align-items: stretch;
}

.map-card {
  flex: 1;
  border-radius: 32px;
  background: #fff;
  padding: 32px;
  box-shadow: 0 20px 40px rgba(148, 163, 184, 0.15);
  display: flex;
  flex-direction: column;
  border: 1px solid #f1f5f9;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  margin-bottom: 20px;
  color: #1e293b;
  font-size: 18px;
}

.map-illustration {
  flex: 1;
  background: #ecfdf5;
  border-radius: 24px;
  position: relative;
  margin-bottom: 20px;
  border: 1px dashed #6ee7b7;
  overflow: hidden;
}

/* Decorative Grid */
.map-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(#10b981 1px, transparent 1px),
    linear-gradient(90deg, #10b981 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.05;
}

.pin {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  position: absolute;
  transform: translate(-50%, -50%);
  animation: pulse 2s infinite;
  border: 3px solid #fff;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}

.pin-main {
  top: 40%; left: 55%; background: #10b981;
}
.pin-secondary {
  top: 65%; left: 30%; background: #f59e0b;
}
.pin-tertiary {
  top: 25%; left: 25%; background: #3b82f6;
}

@keyframes pulse {
  0% { transform: translate(-50%, -50%) scale(1); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  70% { transform: translate(-50%, -50%) scale(1.1); box-shadow: 0 0 0 15px rgba(16, 185, 129, 0); }
  100% { transform: translate(-50%, -50%) scale(1); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.map-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 16px;
  line-height: 1.6;
}
.refresh-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: block;
}

.map-legend {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #64748b;
  list-style: none;
  padding: 0; margin: 0;
}

.map-legend .dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 8px;
}

.dot-hot { background: #ef4444; }
.dot-mid { background: #f59e0b; }
.dot-low { background: #3b82f6; }

/* Actions */
.la-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 48px 40px;
  max-width: 1600px;
  margin: 0 auto;
}
.action-buttons { display: flex; gap: 12px; }

.pill {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  border-radius: 999px;
  padding: 10px 24px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.03);
}
.pill:hover {
  background: #f8fafc;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
  color: #10b981;
  border-color: #10b981;
}

.pill-primary {
  background: #10b981;
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}
.pill-primary:hover {
  background: #059669;
  color: #fff;
}

.highlight-tip {
  margin-left: auto;
  color: #10b981;
  font-size: 13px;
  background: #ecfdf5;
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: 500;
}

/* Layout Grid */
.la-layout {
  display: grid;
  grid-template-columns: 280px 1fr 340px;
  gap: 32px;
  padding: 0 48px 64px;
  align-items: start;
  max-width: 1600px;
  margin: 0 auto;
}

.layout-center {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.filters-panel,
.events-panel,
.detail-panel {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
  padding: 24px;
  border: 1px solid #f1f5f9;
}

/* Filters */
.filters-panel h3 { margin: 0 0 20px 0; font-size: 18px; color: #1e293b; }

.filter-group { margin-bottom: 24px; }
.group-label {
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  font-weight: 600;
  display: block;
  margin-bottom: 12px;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 14px;
  background: #fff;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.chip:hover { border-color: #cbd5e1; color: #334155; }
.chip.active {
  background: #ecfdf5;
  color: #059669;
  border-color: #10b981;
  font-weight: 600;
}
.chip.ghost { border: none; background: transparent; color: #94a3b8; padding: 8px 4px; }
.chip.ghost:hover { color: #64748b; }

.checkbox {
  display: flex; align-items: center; gap: 10px; font-size: 14px; color: #475569; cursor: pointer;
}
.checkbox input { accent-color: #10b981; width: 16px; height: 16px; }

/* Schedule */
.schedule-box { border-top: 1px solid #e2e8f0; padding-top: 24px; margin-top: 24px; }
.schedule-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.schedule-header small { color: #94a3b8; font-size: 12px; display: block; margin-top: 2px; }

.schedule-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.schedule-list li {
  display: flex; justify-content: space-between; align-items: center;
  background: #f8fafc; border-radius: 12px; padding: 12px;
  border: 1px solid #f1f5f9; transition: background 0.2s;
}
.schedule-list li:hover { background: #f1f5f9; }

.schedule-list .time { font-weight: 700; color: #10b981; font-size: 13px; }
.schedule-list .info { flex: 1; margin: 0 12px; }
.schedule-list .title { font-weight: 600; font-size: 14px; color: #334155; margin: 0 0 2px 0; }
.schedule-list small { color: #94a3b8; font-size: 12px; }
.schedule-list .status { font-size: 12px; color: #3b82f6; background: #eff6ff; padding: 2px 6px; border-radius: 4px; }

/* Events */
.panel-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.panel-header h3 { margin: 0; font-size: 20px; color: #1e293b; }
.panel-header .desc { color: #64748b; font-size: 14px; margin: 6px 0 0 0; }

.view-switch { background: #f1f5f9; padding: 4px; border-radius: 12px; display: flex; }
.ghost { border: none; background: transparent; color: #64748b; border-radius: 8px; padding: 6px 16px; cursor: pointer; transition: all 0.2s; font-size: 13px; font-weight: 600; }
.ghost.xs { padding: 4px 10px; font-size: 12px; border: 1px solid #e2e8f0; }
.ghost:hover { color: #334155; }
.ghost.active { background: #fff; color: #10b981; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }

.event-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px; }

.event-card {
  border: 1px solid #f1f5f9; border-radius: 20px; overflow: hidden; cursor: pointer;
  display: flex; flex-direction: column; transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  background: #fff;
}
.event-card:hover { transform: translateY(-6px); box-shadow: 0 20px 40px rgba(0,0,0,0.08); }
.event-card.hot { border-color: #a7f3d0; }

.card-img { height: 180px; background-size: cover; background-position: center; position: relative; }
.badge {
  position: absolute; top: 12px; left: 12px;
  background: rgba(16, 185, 129, 0.9); backdrop-filter: blur(4px);
  color: #fff; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

.card-body { padding: 20px; display: flex; flex-direction: column; flex: 1; }
.card-top { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; font-weight: 600; letter-spacing: 0.02em; text-transform: uppercase; margin-bottom: 8px; }
.category { color: #10b981; }

.card-body h4 { margin: 0 0 8px 0; font-size: 17px; color: #1e293b; line-height: 1.4; }
.time { color: #64748b; font-size: 13px; margin-bottom: 4px; }
.location { color: #64748b; font-size: 13px; margin: 0 0 12px 0; display: flex; align-items: center; gap: 6px; }

.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
.tags span { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 6px; font-size: 12px; }

.meta { margin-top: auto; display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; border-top: 1px solid #f1f5f9; padding-top: 12px; }

/* Detail Panel */
.detail-panel { position: sticky; top: 20px; }

.detail-card {
  background: linear-gradient(180deg, #ecfdf5 0%, #fff 100px);
  border-radius: 20px; padding: 24px; border: 1px solid #d1fae5;
}

.detail-card h3 { margin: 8px 0 16px; font-size: 22px; color: #064e3b; }
.detail-desc { color: #374151; font-size: 14px; line-height: 1.6; margin-bottom: 24px; }

.detail-info { list-style: none; padding: 0; margin: 0 0 24px 0; display: flex; flex-direction: column; gap: 12px; }
.detail-info li { display: flex; align-items: center; gap: 12px; color: #334155; font-size: 14px; }
.detail-info i { color: #10b981; width: 20px; text-align: center; }

.detail-actions { display: flex; flex-direction: column; gap: 12px; }
.full { width: 100%; justify-content: center; }

.detail-tags { margin-top: 20px; display: flex; flex-wrap: wrap; gap: 8px; }
.detail-tags span {
  background: #fff; border: 1px solid #a7f3d0; color: #059669;
  border-radius: 999px; padding: 4px 12px; font-size: 12px;
}

/* Highlights */
.community-highlights {
  background: #fff; margin: 0 48px 64px; padding: 40px; border-radius: 32px;
  box-shadow: 0 20px 40px rgba(148, 163, 184, 0.1); max-width: 1600px; margin: 0 auto 64px;
}

.highlights-header { margin-bottom: 32px; }
.highlights-header h3 { font-size: 24px; color: #1e293b; margin: 0 0 8px 0; }
.highlights-header .desc { color: #64748b; font-size: 15px; }

.story-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; }

.story-card {
  border: 1px solid #f1f5f9; border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px; transition: all 0.2s;
  background: #f8fafc;
}
.story-card:hover { background: #fff; box-shadow: 0 12px 24px rgba(0,0,0,0.05); transform: translateY(-4px); }

.story-card header { display: flex; justify-content: space-between; align-items: baseline; }
.story-card header h4 { margin: 0; font-size: 16px; color: #1e293b; }
.story-card header span { font-size: 12px; color: #94a3b8; }

.story-card p { margin: 0; font-size: 14px; color: #475569; line-height: 1.6; }

.story-card footer { display: flex; justify-content: space-between; align-items: center; margin-top: auto; }
.author { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #64748b; font-weight: 500; }
.avatar-placeholder {
  width: 24px; height: 24px; background: #e2e8f0; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 12px; color: #64748b;
}

.text-link { background: none; border: none; color: #10b981; font-size: 13px; font-weight: 600; cursor: pointer; padding: 0; }
.text-link:hover { text-decoration: underline; }

@media (max-width: 1200px) {
  .la-hero { grid-template-columns: 1fr; padding: 40px 24px; }
  .la-layout { grid-template-columns: 1fr; padding: 0 24px 40px; }
  .la-actions { padding: 0 24px 32px; flex-wrap: wrap; }
  .detail-panel { order: -1; } /* Detail on top in mobile */
  .community-highlights { margin: 0 24px 40px; }
}
</style>
