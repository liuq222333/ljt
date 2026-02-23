<template>
  <dhstyle />
  <div class="lsa-page">
    <section class="hero">
      <div>
        <p class="eyebrow">活动故事</p>
        <h1>记录邻里之间的真实瞬间</h1>
        <p class="subtitle">居民投稿、志愿者纪实、组织者复盘……让更多好点子被看见，激发下一次参与。</p>
      </div>
      <button class="primary" @click="goPublish">发布故事</button>
    </section>

    <section class="stories">
      <article v-for="story in stories" :key="story.id" class="card" @click="openDetail(story.id)">
        <img :src="story.coverUrl" :alt="story.title" />
        <div class="body">
          <span class="tag">{{ story.visibility || '公开' }}</span>
          <h3>{{ story.title }}</h3>
          <p>{{ story.summary }}</p>
          <footer>
            <span>{{ story.author }} · {{ story.createdAt }}</span>
            <button class="ghost">查看全文</button>
          </footer>
        </div>
      </article>
      <p v-if="!loading && stories.length === 0" class="empty">暂无故事</p>
      <p v-if="loading" class="empty">加载中...</p>
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
  background: #f5f6f8;
}

.lsa-page {
  padding-top: 80px;
  color: #111827;
}

.hero {
  margin: 48px;
  padding: 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #2b0a3d, #a855f7);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.primary {
  border: none;
  border-radius: 999px;
  padding: 12px 28px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.stories {
  margin: 0 48px 60px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.card {
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag {
  background: rgba(255, 255, 255, 0.2);
  color: #7c3aed;
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(124, 58, 237, 0.2);
}

.body p {
  color: #4b5563;
}

.body footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #6b7280;
}

.ghost {
  border: 1px solid #d3d9e5;
  border-radius: 999px;
  background: transparent;
  padding: 6px 14px;
  cursor: pointer;
}

.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #6b7280;
}

@media (max-width: 800px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
