<template>
  <dhstyle />
  <div class="lmv-page">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">地图模式</p>
        <h1>在地图上快速浏览附近活动</h1>
        <p class="subtitle">根据定位展示 2km 内的活动热度、路线与距离，支持筛选类型和时间段。</p>
        <div class="hero-actions">
          <button class="ghost" @click="backToList">
            <i class="fas fa-arrow-left"></i>
            返回列表
          </button>
        </div>
      </div>
      <div class="hero-quick">
        <div class="quick-row" v-for="filter in filters" :key="filter.id">
          <span class="quick-icon"><i :class="['fas', filter.icon]"></i></span>
          <div class="quick-text">
            <strong>{{ filter.label }}</strong>
            <span>{{ filter.hint }}</span>
          </div>
          <span :class="['toggle-pill', filter.active ? 'on' : '']">{{ filter.active ? '已开' : '关闭' }}</span>
        </div>
      </div>
    </section>

    <section class="map-panel">
      <div class="map-placeholder">
        <i class="fas fa-map-location-dot"></i>
        <strong>地图加载中</strong>
        <span>可接入高德 JS SDK，根据定位渲染 2km 内活动</span>
        <div class="legend">
          <span><span class="dot dot-orange"></span>正在报名</span>
          <span><span class="dot dot-blue"></span>即将开始</span>
          <span><span class="dot dot-green"></span>志愿任务</span>
        </div>
      </div>
      <aside class="side-list">
        <div class="side-head">
          <h3>附近活动</h3>
          <span class="count">{{ events.length }} 个</span>
        </div>
        <article v-for="event in events" :key="event.id" class="item">
          <span class="dist-pill">{{ event.distance }}km</span>
          <div class="item-text">
            <strong>{{ event.title }}</strong>
            <span>{{ event.time }}</span>
          </div>
          <button class="primary">详情</button>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const events = [
  { id: 1, title: '邻里共享厨房', distance: 1.2, time: '10/28 14:00' },
  { id: 2, title: '公园绿色行动', distance: 0.8, time: '10/23 09:30' },
  { id: 3, title: '社区夜跑', distance: 2.0, time: '10/24 19:00' }
];

const filters = [
  { id: 'nearby', label: '2km 范围', icon: 'fa-location-crosshairs', hint: '当前定位 · 中心点', active: true },
  { id: 'today', label: '今日活动', icon: 'fa-calendar-day', hint: '仅显示当日开始', active: true },
  { id: 'volunteer', label: '志愿任务', icon: 'fa-hand-holding-heart', hint: '邻里互助场景', active: false }
];

const backToList = () => {
  router.push('/local-act');
};
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lmv-page {
  color: #0f172a;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: 32px;
  align-items: center;
}

.subtitle {
  margin: 0;
  max-width: 540px;
}

.hero-actions {
  margin-top: 22px;
}

.hero-actions .ghost i {
  margin-right: 6px;
  font-size: 11px;
}

.hero-quick {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px;
}

.quick-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #ffffff;
  color: #4e8ef7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.quick-text {
  flex: 1;
  min-width: 0;
}

.quick-text strong {
  display: block;
  font-size: 13.5px;
  font-weight: 500;
  color: #0f172a;
}

.quick-text span {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  color: #94a3b8;
}

.toggle-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: #ffffff;
  color: #94a3b8;
  font-size: 11.5px;
  font-weight: 500;
}

.toggle-pill.on {
  background: rgba(56, 185, 130, 0.1);
  color: #1aa053;
}

.map-panel {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 1fr);
  gap: 24px;
}

.map-placeholder {
  border-radius: 18px;
  background:
    radial-gradient(circle at 28% 28%, rgba(255, 107, 44, 0.1), transparent 36%),
    radial-gradient(circle at 72% 72%, rgba(78, 142, 247, 0.1), transparent 36%),
    radial-gradient(circle at 50% 50%, rgba(56, 185, 130, 0.06), transparent 50%),
    #f8fafc;
  height: 520px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.map-placeholder i {
  font-size: 36px;
  color: #ff6b2c;
}

.map-placeholder strong {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.map-placeholder > span {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

.legend {
  margin-top: 16px;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  justify-content: center;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend .dot-orange { background: #ff6b2c; }
.legend .dot-blue { background: #4e8ef7; }
.legend .dot-green { background: #38b982; }

.side-list {
  background: #ffffff;
  border-radius: 18px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-self: start;
}

.side-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.side-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.side-head .count {
  font-size: 12px;
  color: #94a3b8;
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  transition: background 0.18s ease;
}

.item:hover {
  background: #f1f5f9;
}

.dist-pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 107, 44, 0.1);
  color: #ff6b2c;
  font-size: 12px;
  font-weight: 600;
}

.item-text {
  flex: 1;
  min-width: 0;
}

.item-text strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.item-text span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 960px) {
  .hero,
  .map-panel {
    grid-template-columns: 1fr;
  }

  .map-placeholder {
    height: 380px;
  }
}
</style>
