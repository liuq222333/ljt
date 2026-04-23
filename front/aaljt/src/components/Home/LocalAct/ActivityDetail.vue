<template>
  <dhstyle />
  <div class="lad-page">
    <section class="hero card">
      <div class="hero-content">
        <p class="eyebrow">????</p>
        <h1>{{ activity.title }}</h1>
        <p class="subtitle">{{ activity.subtitle }}</p>
        <div class="hero-tags">
          <span v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
          <span class="id-chip">ID {{ activityId }}</span>
        </div>
        <div class="hero-actions">
          <button class="btn btn-primary">????</button>
          <button class="btn btn-light">????</button>
        </div>
      </div>
      <img class="hero-cover" :src="activity.cover" :alt="activity.title" />
    </section>

    <section class="detail-grid">
      <div class="main-col">
        <article class="panel">
          <h2>????</h2>
          <div class="overview-grid">
            <div class="overview-item">
              <span>??</span>
              <strong>{{ activity.date }} ? {{ activity.time }}</strong>
            </div>
            <div class="overview-item">
              <span>??</span>
              <strong>{{ activity.location }}</strong>
            </div>
            <div class="overview-item">
              <span>??</span>
              <strong>{{ activity.reserved }}/{{ activity.capacity }} ?</strong>
            </div>
            <div class="overview-item">
              <span>????</span>
              <strong>{{ activity.registration }}</strong>
            </div>
          </div>
          <p class="desc">{{ activity.description }}</p>
        </article>

        <article class="panel">
          <h2>????</h2>
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
          <h2>????</h2>
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
          <h3>???</h3>
          <div class="organizer">
            <img :src="organizer.avatar" :alt="organizer.name" />
            <div>
              <strong>{{ organizer.name }}</strong>
              <p>{{ organizer.role }}</p>
            </div>
          </div>
          <button class="btn btn-light full">?????</button>
        </article>

        <article class="panel side-panel">
          <h3>????</h3>
          <div class="map-placeholder">???????????????</div>
          <ul class="traffic-list">
            <li><span>??</span><strong>{{ activity.transit }}</strong></li>
            <li><span>??</span><strong>{{ activity.parking }}</strong></li>
          </ul>
        </article>

        <article class="panel side-panel">
          <h3>????</h3>
          <ul class="tips-list">
            <li v-for="tip in checklist" :key="tip">{{ tip }}</li>
          </ul>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';

const route = useRoute();
const activityId = computed(() => String(route.params.id ?? '-'));

const activity = {
  title: '?????? ? ?????',
  subtitle: '??????????????????????',
  date: '10?28?????',
  time: '14:00 - 17:00',
  location: '???????? ? ????',
  capacity: 16,
  reserved: 14,
  registration: '???? ? ???',
  tags: ['????', '????', '????'],
  description:
    '??????????????????????????????????????????????????????????????',
  cover: 'https://images.unsplash.com/photo-1509448613959-44d527dd5d48?auto=format&fit=crop&w=1200&q=80',
  transit: '?? 4 ?? ? ??? B ??? 5 ??',
  parking: '?????????? 2 ???????'
};

const organizer = {
  name: '???????',
  role: '????',
  avatar: 'https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=200&q=60'
};

const agenda = [
  { time: '13:30 - 14:00', title: '???????', desc: '?????????????????????' },
  { time: '14:00 - 15:00', title: '???????', desc: '?????????????????' },
  { time: '15:00 - 16:30', title: '????', desc: '????????????' },
  { time: '16:30 - 17:00', title: '????', desc: '?????????????' }
];

const stories = [
  {
    id: 's1',
    title: '???????',
    summary: '????????????????????????',
    cover: 'https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?auto=format&fit=crop&w=600&q=60'
  },
  {
    id: 's2',
    title: '?????',
    summary: '???? 20 ?????????????????',
    cover: 'https://images.unsplash.com/photo-1470337458703-46ad1756a187?auto=format&fit=crop&w=600&q=60'
  }
];

const checklist = [
  '??? 15 ???????',
  '????????????',
  '???????????????',
  '???????????'
];
</script>

<style scoped>
:global(body) {
  background: #f5f6f8;
}

.lad-page {
  padding: 76px 40px 36px;
  color: #1f2937;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.card,
.panel {
  background: #ffffff;
  border: 1px solid #e3e9f2;
  border-radius: 12px;
}

.hero {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: stretch;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #667085;
}

.hero h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #4b5563;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hero-tags span {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border: 1px solid #d7e0eb;
  border-radius: 999px;
  background: #f8fafc;
  font-size: 12px;
  color: #475467;
}

.id-chip {
  background: #edf5fc;
  border-color: #cfe0f2;
  color: #2f6ea5;
}

.hero-actions {
  display: flex;
  gap: 8px;
}

.hero-cover {
  width: 100%;
  height: 100%;
  min-height: 220px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e5ebf3;
}

.detail-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
  align-items: start;
}

.main-col,
.side-col {
  display: grid;
  gap: 10px;
}

.panel {
  padding: 14px;
}

.panel h2 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #1f2937;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.overview-item {
  border: 1px solid #e5ebf3;
  border-radius: 10px;
  background: #fafbfd;
  padding: 8px 10px;
  display: grid;
  gap: 3px;
}

.overview-item span {
  font-size: 12px;
  color: #667085;
}

.overview-item strong {
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
}

.desc {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: #4b5563;
}

.agenda {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.agenda li {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 10px;
  border: 1px solid #e5ebf3;
  border-radius: 10px;
  background: #ffffff;
  padding: 8px 10px;
}

.time {
  margin: 0;
  font-size: 12px;
  color: #2f6ea5;
  font-weight: 600;
}

.agenda strong {
  font-size: 13px;
  color: #344054;
}

.agenda p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #667085;
  line-height: 1.55;
}

.story-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.story-card {
  margin: 0;
  border: 1px solid #e5ebf3;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
}

.story-card img {
  width: 100%;
  height: 138px;
  object-fit: cover;
}

.story-card figcaption {
  padding: 8px 10px;
}

.story-card strong {
  font-size: 13px;
  color: #344054;
}

.story-card p {
  margin: 3px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: #667085;
}

.side-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.organizer {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.organizer img {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #d9e3ef;
}

.organizer strong {
  font-size: 14px;
  color: #334155;
}

.organizer p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #667085;
}

.map-placeholder {
  border: 1px dashed #c7d5e7;
  border-radius: 10px;
  background: #f8fbff;
  color: #5f7fa1;
  min-height: 110px;
  display: grid;
  place-items: center;
  font-size: 12px;
  text-align: center;
  padding: 10px;
}

.traffic-list,
.tips-list {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.traffic-list li {
  display: grid;
  gap: 2px;
}

.traffic-list span {
  font-size: 12px;
  color: #667085;
}

.traffic-list strong,
.tips-list li {
  font-size: 12px;
  line-height: 1.55;
  color: #475467;
}

.btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #d2dae8;
  background: #ffffff;
  color: #344054;
  font-size: 13px;
  cursor: pointer;
}

.btn:hover {
  background: #f6f8fc;
  border-color: #c2cede;
}

.btn-primary {
  background: #8cb4db;
  border-color: #8cb4db;
  color: #ffffff;
}

.btn-primary:hover {
  background: #7ea7cf;
  border-color: #7ea7cf;
}

.btn.full {
  width: 100%;
}

@media (max-width: 1200px) {
  .lad-page {
    padding: 72px 20px 30px;
  }

  .hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-cover {
    max-height: 280px;
  }
}

@media (max-width: 900px) {
  .overview-grid,
  .story-grid {
    grid-template-columns: 1fr;
  }

  .agenda li {
    grid-template-columns: 1fr;
  }
}
</style>
