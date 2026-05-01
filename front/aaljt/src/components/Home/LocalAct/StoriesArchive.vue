<template>
  <dhstyle />
  <div class="lsa-page">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">活动故事</p>
        <h1>记录邻里之间的真实瞬间</h1>
        <p class="subtitle">居民投稿、志愿者纪实、组织者复盘……让更多好点子被看见，激发下一次参与。</p>
        <div class="hero-actions">
          <button class="primary" @click="goPublish">
            <i class="fas fa-pen-to-square"></i>
            发布故事
          </button>
        </div>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <strong>{{ stories.length || 28 }}</strong>
          <span>条已发布</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <strong>342</strong>
          <span>位作者参与</span>
        </div>
      </div>
    </section>

    <div class="filter-bar">
      <div class="quick-tags">
        <button v-for="tag in quickTags" :key="tag" class="tag-btn">{{ tag }}</button>
      </div>
      <label class="search-box">
        <i class="fas fa-magnifying-glass"></i>
        <input type="text" placeholder="搜索故事关键词" />
      </label>
    </div>

    <section class="stories">
      <article v-for="story in stories" :key="story.id" class="card" @click="openDetail(story.id)">
        <div class="cover-wrap">
          <img :src="story.coverUrl" :alt="story.title" />
          <span class="tag">{{ story.visibility || '公开' }}</span>
        </div>
        <div class="body">
          <h3>{{ story.title }}</h3>
          <p>{{ story.summary }}</p>
          <footer>
            <div class="author">
              <span class="author-avatar">{{ (story.author || '邻')[0] }}</span>
              <div>
                <strong>{{ story.author }}</strong>
                <span>{{ story.createdAt }}</span>
              </div>
            </div>
            <span class="read-link">
              阅读
              <i class="fas fa-arrow-right"></i>
            </span>
          </footer>
        </div>
      </article>
      <div v-if="!loading && stories.length === 0" class="empty">
        <i class="far fa-folder-open"></i>
        <strong>还没有故事</strong>
        <p>分享你的活动瞬间，让邻里看到更多真实的社区温度</p>
      </div>
      <p v-if="loading" class="loading">加载中...</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

type Story = {
  id: number;
  title: string;
  summary: string;
  coverUrl: string;
  author: string;
  visibility?: string;
  createdAt?: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const router = useRouter();
const stories = ref<Story[]>([]);
const loading = ref(false);
const quickTags = ['全部', '邻里互动', '环保共建', '亲子时光', '志愿服务', '社区守护'];

const fetchStories = async () => {
  loading.value = true;
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/stories`);
    const data = await resp.json();
    if (resp.ok && data?.code === 200 && Array.isArray(data.data)) {
      stories.value = data.data;
    } else {
      stories.value = [];
    }
  } catch (e) {
    stories.value = [];
  } finally {
    loading.value = false;
  }
};

const openDetail = (id: number) => {
  router.push(`/local-act/stories/${id}`);
};

const goPublish = () => {
  router.push('/local-act/stories/publish');
};

onMounted(fetchStories);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lsa-page {
  color: #0f172a;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 1fr);
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

.hero-actions .primary i {
  margin-right: 6px;
  font-size: 11px;
}

.hero-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff5ef 0%, #ffe9dc 100%);
  gap: 18px;
}

.meta-item {
  text-align: center;
  flex: 1;
}

.meta-item strong {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #ff6b2c;
  letter-spacing: -0.02em;
  line-height: 1;
}

.meta-item span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 107, 44, 0.2);
}

.filter-bar {
  max-width: 1280px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.quick-tags {
  flex: 1;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-btn {
  height: 32px;
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

.tag-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.tag-btn:first-child {
  background: #0f172a;
  color: #ffffff;
}

.search-box {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  background: #ffffff;
  color: #94a3b8;
  width: min(280px, 100%);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #0f172a;
}

.search-box input::placeholder {
  color: #94a3b8;
}

.stories {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 36px rgba(15, 23, 42, 0.08);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #f1f5f9;
  overflow: hidden;
}

.card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.card:hover img {
  transform: scale(1.04);
}

.tag {
  position: absolute;
  top: 12px;
  left: 12px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  color: #7563ff;
  font-size: 11.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
}

.body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.body h3 {
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

.body > p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.body footer {
  margin-top: 6px;
  padding-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
}

.author {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #fff1ea;
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.author strong {
  display: block;
  font-size: 12.5px;
  font-weight: 500;
  color: #0f172a;
}

.author span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.read-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 500;
  color: #ff6b2c;
  transition: gap 0.2s ease;
}

.read-link i {
  font-size: 10px;
}

.card:hover .read-link {
  gap: 10px;
}

.empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 72px 24px;
  text-align: center;
  background: #ffffff;
  border-radius: 16px;
}

.empty i {
  font-size: 36px;
  color: #cbd5e1;
}

.empty strong {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.empty p {
  margin: 0;
  max-width: 360px;
  font-size: 13px;
  color: #94a3b8;
}

.loading {
  grid-column: 1 / -1;
  text-align: center;
  color: #94a3b8;
  padding: 32px 0;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>
