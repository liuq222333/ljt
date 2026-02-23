<template>
  <dhstyle />
  <div class="lmv-page">
    <section class="hero">
      <div>
        <p class="eyebrow">地图模式</p>
        <h1>在地图上快速浏览附近活动</h1>
        <p class="subtitle">根据定位展示 2km 内的活动热度、路线与距离，支持筛选类型和时间段。</p>
      </div>
      <button class="ghost" @click="backToList">返回列表</button>
    </section>

    <section class="map-panel">
      <div class="map-placeholder">
        <span>地图加载中 · 可接入高德 JS SDK</span>
      </div>
      <aside class="side-list">
        <article v-for="event in events" :key="event.id" class="item">
          <div>
            <h3>{{ event.title }}</h3>
            <p>{{ event.distance }} km · {{ event.time }}</p>
          </div>
          <button class="primary">查看详情</button>
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

const backToList = () => {
  router.push('/local-act');
};
</script>

<style scoped>
:global(body) {
  background: #f5f6f8;
}

.lmv-page {
  padding-top: 80px;
}

.hero {
  margin: 48px;
  padding: 28px 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #072f49, #0ea5e9);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ghost {
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
  padding: 10px 22px;
  background: transparent;
  color: #fff;
  cursor: pointer;
}

.map-panel {
  margin: 0 48px 60px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

.map-placeholder {
  border-radius: 28px;
  background: repeating-linear-gradient(
    -45deg,
    rgba(14, 165, 233, 0.08),
    rgba(14, 165, 233, 0.08) 20px,
    rgba(14, 165, 233, 0.2) 20px,
    rgba(14, 165, 233, 0.2) 40px
  );
  height: 480px;
  display: grid;
  place-items: center;
  color: #0f172a;
  font-weight: 600;
}

.side-list {
  background: #fff;
  border-radius: 24px;
  padding: 18px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item {
  border: 1px solid #e5e9f2;
  border-radius: 16px;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.primary {
  border: none;
  background: linear-gradient(120deg, #0ea5e9, #2563eb);
  color: #fff;
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
}

@media (max-width: 960px) {
  .map-panel {
    grid-template-columns: 1fr;
  }
}
</style>
