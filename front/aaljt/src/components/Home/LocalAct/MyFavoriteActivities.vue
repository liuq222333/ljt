<template>
  <dhstyle />
  <div class="fav-page">
    <section class="hero card">
      <div>
        <p class="eyebrow">我的收藏</p>
        <h1>收藏的本地活动</h1>
        <p class="subtitle">把感兴趣的活动先收起来，合适的时候再报名参加。</p>
      </div>
      <button class="btn primary" type="button" @click="goList">发现活动</button>
    </section>

    <main class="list card">
      <div class="list-head">
        <h2>收藏列表</h2>
        <button class="btn light" type="button" @click="loadFavorites">刷新</button>
      </div>

      <p v-if="loading" class="state">正在加载收藏活动...</p>
      <p v-else-if="errorMsg" class="state error">{{ errorMsg }}</p>
      <p v-else-if="!activities.length" class="state">暂无收藏活动。</p>

      <template v-else>
        <article v-for="activity in activities" :key="activity.id" class="item">
          <img :src="activity.coverUrl || fallbackCover" :alt="activity.title || '活动封面'" />
          <div class="item-main">
            <div class="meta">
              <span>{{ formatDate(activity.startAt) }}</span>
              <span>{{ activity.location || activity.locationText || activity.address || '地点待定' }}</span>
            </div>
            <h3>{{ activity.title || '未命名活动' }}</h3>
            <p>{{ activity.description || activity.subtitle || '暂无简介' }}</p>
          </div>
          <div class="actions">
            <button class="btn light" type="button" @click="goDetail(activity.id)">详情</button>
            <button class="btn danger" type="button" :disabled="removingId === activity.id" @click="removeFavorite(activity)">
              {{ removingId === activity.id ? '取消中...' : '取消收藏' }}
            </button>
          </div>
        </article>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchFavoriteLocalActivities, unfavoriteLocalActivity } from '@/api/localAct';
import type { LocalActivityListItem } from '@/types/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';
import fallbackCover from '../../../pictures/homePicture1.jpg';

const router = useRouter();
const username = ref(localStorage.getItem('username') || '');
const loading = ref(false);
const removingId = ref<number | null>(null);
const errorMsg = ref('');
const activities = ref<LocalActivityListItem[]>([]);

const normalizeDateValue = (value?: unknown) => {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value.map(Number);
    return new Date(year, month - 1, day, hour, minute);
  }
  const date = new Date(String(value).replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDate = (value?: unknown) => {
  const date = normalizeDateValue(value);
  if (!date) return '时间待定';
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
};

const normalizeActivity = (activity: LocalActivityListItem): LocalActivityListItem => ({
  ...activity,
  category: activity.category || activity.categoryCode,
  location: activity.location || activity.locationText || activity.address
});

const loadFavorites = async () => {
  if (!username.value) {
    errorMsg.value = '请先登录后查看我的收藏';
    activities.value = [];
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  try {
    const list = await fetchFavoriteLocalActivities(username.value, 1, 50);
    activities.value = list.map(normalizeActivity);
  } catch (error) {
    activities.value = [];
    errorMsg.value = error instanceof Error ? error.message : '加载收藏失败';
  } finally {
    loading.value = false;
  }
};

const removeFavorite = async (activity: LocalActivityListItem) => {
  if (!activity.id || !username.value) return;
  removingId.value = activity.id;
  errorMsg.value = '';
  try {
    await unfavoriteLocalActivity(activity.id, username.value);
    activities.value = activities.value.filter((item) => item.id !== activity.id);
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '取消收藏失败';
  } finally {
    removingId.value = null;
  }
};

const goList = () => {
  router.push('/local-act/list');
};

const goDetail = (id?: number) => {
  if (id) router.push(`/local-act/${id}`);
};

onMounted(loadFavorites);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.fav-page {
  min-height: 100vh;
  padding: 96px clamp(20px, 4vw, 56px) 72px;
  color: #0f172a;
}

.card {
  max-width: 1180px;
  margin: 0 auto 20px;
  background: #fff;
  border-radius: 18px;
}

.hero {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #ff6b2c;
  font-size: 12px;
  font-weight: 700;
}

h1,
h2,
h3,
p {
  margin: 0;
}

.subtitle {
  margin-top: 10px;
  color: #64748b;
}

.btn {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 600;
}

.btn.primary {
  background: #ff6b2c;
  color: #fff;
}

.btn.light {
  background: #f1f5f9;
  color: #475569;
}

.btn.danger {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
}

.list {
  padding: 24px;
}

.list-head,
.item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.list-head {
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.item {
  padding: 18px 0;
  border-bottom: 1px solid #f1f5f9;
}

.item img {
  width: 138px;
  height: 88px;
  object-fit: cover;
  border-radius: 12px;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.meta span {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}

.item-main p {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.state {
  padding: 48px 16px;
  text-align: center;
  color: #94a3b8;
}

.state.error {
  color: #dc2626;
}

@media (max-width: 760px) {
  .hero,
  .item {
    flex-direction: column;
  }

  .item img {
    width: 100%;
    height: 160px;
  }

  .actions {
    align-items: flex-start;
  }
}
</style>
