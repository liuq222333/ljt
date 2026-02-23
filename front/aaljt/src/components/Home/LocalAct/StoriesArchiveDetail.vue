<template>
  <dhstyle />
  <div class="lsd-page" v-if="story">
    <header class="hero">
      <div>
        <p class="eyebrow">活动故事 · {{ story.visibility || '公开' }}</p>
        <h1>{{ story.title }}</h1>
        <p class="meta">{{ story.author }} · {{ story.createdAt }}</p>
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
import { useRoute } from 'vue-router';
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
const story = ref<StoryDetail | null>(null);

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
  background: #f5f6f8;
}
.lsd-page {
  padding-top: 80px;
  color: #111827;
}
.hero {
  margin: 32px 48px 0;
}
.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 13px;
  color: #7c3aed;
}
.meta {
  color: #6b7280;
  margin-top: 8px;
}
.content {
  margin: 16px 48px 60px;
  background: #fff;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}
.cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 16px;
}
.summary {
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}
.body {
  color: #4b5563;
  line-height: 1.6;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #6b7280;
}
@media (max-width: 800px) {
  .hero, .content {
    margin: 16px;
  }
}
</style>
