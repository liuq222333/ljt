<template>
  <dhstyle />
  <div class="lsd-page" v-if="story">
    <button class="back-btn" @click="goBack">
      <i class="fas fa-arrow-left"></i>
      返回故事列表
    </button>
    <header class="hero">
      <p class="eyebrow">活动故事 · {{ story.visibility || '公开' }}</p>
      <h1>{{ story.title }}</h1>
      <div class="author-line">
        <span class="author-avatar">{{ (story.author || '邻')[0] }}</span>
        <div>
          <strong>{{ story.author }}</strong>
          <span>{{ story.createdAt }}</span>
        </div>
      </div>
    </header>
    <main class="content">
      <img v-if="story.coverUrl" :src="story.coverUrl" class="cover" :alt="story.title" />
      <p class="summary" v-if="story.summary">{{ story.summary }}</p>
      <div class="body" v-html="story.content"></div>
    </main>
  </div>
  <p v-else class="empty">加载中...</p>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import dhstyle from '../../dhstyle/dhstyle.vue';

type StoryDetail = {
  id: number;
  title: string;
  coverUrl?: string;
  summary?: string;
  content?: string;
  author?: string;
  visibility?: string;
  createdAt?: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const route = useRoute();
const router = useRouter();
const story = ref<StoryDetail | null>(null);

const goBack = () => {
  router.push('/local-act/stories');
};

const fetchDetail = async () => {
  const id = route.params.id;
  if (!id) return;
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/stories/${id}`);
    const data = await resp.json();
    if (resp.ok && data?.code === 200 && data.data) {
      story.value = data.data;
    }
  } catch (e) {
    story.value = null;
  }
};

onMounted(fetchDetail);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lsd-page {
  max-width: 800px;
  margin: 0 auto;
  color: #0f172a;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
  margin-bottom: 16px;
}

.back-btn i {
  font-size: 11px;
}

.back-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.hero {
  margin: 0 0 24px;
  padding: 0 8px;
}

.author-line {
  margin-top: 18px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff1ea;
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.author-line strong {
  display: block;
  font-size: 13.5px;
  font-weight: 500;
  color: #0f172a;
}

.author-line span {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  color: #94a3b8;
}

.content {
  background: #ffffff;
  border-radius: 18px;
  padding: 32px;
}

.cover {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  border-radius: 14px;
  margin-bottom: 24px;
  display: block;
}

.summary {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.65;
  color: #334155;
  margin: 0 0 18px;
  padding: 18px 22px;
  border-radius: 12px;
  background: #fff5ef;
  border-left: 3px solid #ff6b2c;
}

.body {
  color: #334155;
  font-size: 15px;
  line-height: 1.85;
}

.empty {
  padding: 80px 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

@media (max-width: 800px) {
  .content {
    padding: 22px;
  }
}
</style>
