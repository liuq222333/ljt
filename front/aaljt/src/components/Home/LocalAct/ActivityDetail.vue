<template>
  <dhstyle />
  <div class="lad-page">
    <section class="hero" :style="{ backgroundImage: `url(${activity.cover})` }">
      <div class="overlay"></div>
      <div class="hero-content">
        <p class="eyebrow">社区活动详情</p>
        <h1>{{ activity.title }}</h1>
        <p class="subtitle">{{ activity.subtitle }}</p>
        <div class="hero-tags">
          <span v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
        </div>
        <div class="hero-actions">
          <button class="primary">立即报名</button>
          <button class="ghost">加入提醒</button>
        </div>
      </div>
    </section>

    <section class="details">
      <div class="main">
        <article class="info-card">
          <h2>活动概览</h2>
          <div class="info-grid">
            <div>
              <h3>时间</h3>
              <p>{{ activity.date }} · {{ activity.time }}</p>
            </div>
            <div>
              <h3>地点</h3>
              <p>{{ activity.location }}</p>
            </div>
            <div>
              <h3>人数</h3>
              <p>{{ activity.reserved }}/{{ activity.capacity }} 人</p>
            </div>
            <div>
              <h3>报名方式</h3>
              <p>{{ activity.registration }}</p>
            </div>
          </div>
          <p class="desc">{{ activity.description }}</p>
        </article>

        <article class="agenda-card">
          <h2>活动流程</h2>
          <ul>
            <li v-for="step in agenda" :key="step.time">
              <div class="time">{{ step.time }}</div>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </li>
          </ul>
        </article>

        <article class="story-card">
          <h2>往期精彩</h2>
          <div class="story-grid">
            <figure v-for="story in stories" :key="story.id">
              <img :src="story.cover" :alt="story.title" />
              <figcaption>
                <strong>{{ story.title }}</strong>
                <p>{{ story.summary }}</p>
              </figcaption>
            </figure>
          </div>
        </article>
      </div>

      <aside class="sidebar">
        <div class="organizer-card">
          <h3>组织者</h3>
          <div class="organizer">
            <img :src="organizer.avatar" alt="" />
            <div>
              <strong>{{ organizer.name }}</strong>
              <p>{{ organizer.role }}</p>
            </div>
          </div>
          <button class="ghost full">联系主办方</button>
        </div>

        <div class="map-card">
          <h3>交通位置</h3>
          <div class="map-placeholder">
            <span>地图占位 · 高德地图接入</span>
          </div>
          <ul>
            <li><strong>地铁：</strong>{{ activity.transit }}</li>
            <li><strong>停车：</strong>{{ activity.parking }}</li>
          </ul>
        </div>

        <div class="checklist-card">
          <h3>温馨提示</h3>
          <ul>
            <li v-for="tip in checklist" :key="tip">{{ tip }}</li>
          </ul>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';

const activity = {
  title: '邻里共享厨房 · 秋季烘焙日',
  subtitle: '传递香气，分享手作甜点，与邻居共度下午茶时光',
  date: '10月 28日（周六）',
  time: '14:00 - 17:00',
  location: '溪语社区邻里中心 · 厨艺教室',
  capacity: 16,
  reserved: 14,
  registration: '系统报名 · 审核制',
  tags: ['亲子友好', '技能互助', '名额有限'],
  description:
    '活动将提供全套烘焙设备与基础食材，由烘焙达人现场演示，参与者可自带配料，完成属于自己的甜品作品。结束后，大家将围坐分享成果并进行邻里交流。',
  cover: 'https://images.unsplash.com/photo-1509448613959-44d527dd5d48?auto=format&fit=crop&w=1500&q=80',
  transit: '地铁 4 号线·溪语站 B 口步行 5 分钟',
  parking: '社区南门停车场（提供 2 小时免费停车）'
};

const organizer = {
  name: '社区生活委员会',
  role: '官方组织',
  avatar: 'https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=200&q=60'
};

const agenda = [
  { time: '13:30 - 14:00', title: '签到与材料发放', desc: '领取围裙、工具包，完成卫⽣与安全须知确认。' },
  { time: '14:00 - 15:00', title: '基础演示与练习', desc: '烘焙导师现场演示戚风蛋糕、秋季风味塔制作。' },
  { time: '15:00 - 16:30', title: '自由制作 & 指导', desc: '分组完成蛋糕装饰，导师巡回指导并答疑。' },
  { time: '16:30 - 17:00', title: '分享与邻里交流', desc: '展示作品，打包品尝，并完成活动合影。' }
];

const stories = [
  {
    id: 's1',
    title: '夏日冰饮工作坊',
    summary: '学员自制冰滴咖啡与果茶，72% 参与者报名下一期。',
    cover: 'https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?auto=format&fit=crop&w=600&q=60'
  },
  {
    id: 's2',
    title: '社区烘焙节',
    summary: '邻里共创 20 款创意面包，收益用于公益资金池。',
    cover: 'https://images.unsplash.com/photo-1470337458703-46ad1756a187?auto=format&fit=crop&w=600&q=60'
  }
];

const checklist = ['请提前 15 分钟到场签到', '建议自带围裙与密封餐盒', '对坚果/乳制品过敏者请提前告知', '现场提供基础清洁工具'];
</script>

<style scoped>
:global(body) {
  background: #f5f6f8;
}

.lad-page {
  padding-top: 70px;
  color: #1f2933;
}

.hero {
  position: relative;
  height: 360px;
  background-size: cover;
  background-position: center;
  border-bottom-left-radius: 32px;
  border-bottom-right-radius: 32px;
  overflow: hidden;
}

.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(7, 19, 11, 0.9), rgba(26, 160, 83, 0.55));
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 60px 48px;
  color: #fff;
  max-width: 720px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.4em;
  font-size: 13px;
  opacity: 0.85;
}

.hero-tags {
  display: flex;
  gap: 10px;
  margin: 16px 0;
  flex-wrap: wrap;
}

.hero-tags span {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary,
.ghost {
  border-radius: 999px;
  padding: 10px 24px;
  font-weight: 600;
  cursor: pointer;
  border: none;
}

.primary {
  background: linear-gradient(120deg, #1aa053, #0a6b3b);
  color: #fff;
}

.ghost {
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: transparent;
  color: #fff;
}

.details {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  margin: 32px 48px;
}

.info-card,
.agenda-card,
.story-card,
.sidebar > div {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.desc {
  color: #4a5568;
  line-height: 1.6;
}

.agenda-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.agenda-card li {
  display: flex;
  gap: 16px;
}

.time {
  min-width: 110px;
  font-weight: 600;
  color: #1aa053;
}

.story-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.story-grid figure {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #edf0f5;
}

.story-grid img {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.story-grid figcaption {
  padding: 12px;
  font-size: 14px;
  color: #475467;
}

.organizer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.organizer img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.ghost.full {
  width: 100%;
  border-color: #d5dae6;
  color: #1f2933;
}

.map-card .map-placeholder {
  background: #f3f6fb;
  border-radius: 18px;
  height: 160px;
  display: grid;
  place-items: center;
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 12px;
}

.map-card ul,
.checklist-card ul {
  padding-left: 18px;
  margin: 0;
  color: #4a5568;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .details {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-content {
    padding: 40px 20px;
  }
  .details {
    margin: 24px;
  }
}
</style>
