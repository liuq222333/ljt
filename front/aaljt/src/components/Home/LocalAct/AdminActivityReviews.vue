<template>
  <dhstyle />
  <div class="review-page">
    <section class="hero card">
      <div>
        <p class="eyebrow">活动审核</p>
        <h1>本地活动审核台</h1>
        <p class="subtitle">审核用户提交的本地活动，控制公开列表与报名入口。</p>
      </div>
      <button class="btn light" type="button" @click="loadReviews">刷新</button>
    </section>

    <section class="toolbar card">
      <label class="search-box">
        <span>关键词</span>
        <input v-model="keyword" type="text" placeholder="标题、地点或简介" @keyup.enter="loadReviews" />
      </label>
      <label class="status-box">
        <span>状态</span>
        <select v-model="currentStatus" @change="loadReviews">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <button class="btn primary" type="button" @click="loadReviews">查询</button>
    </section>

    <section class="records card">
      <div class="records-head">
        <h2>审核列表</h2>
        <span>{{ reviews.length }} 条</span>
      </div>

      <p v-if="loading" class="state">正在加载审核记录...</p>
      <p v-else-if="errorMsg" class="state error">{{ errorMsg }}</p>
      <p v-else-if="!reviews.length" class="state">暂无审核记录。</p>

      <template v-else>
        <article v-for="activity in reviews" :key="activity.id" class="review-item">
          <img class="cover" :src="activity.coverUrl || fallbackCover" :alt="activity.title || '活动封面'" />
          <div class="content">
            <div class="meta">
              <span>{{ getActivityStatusLabel(activity.status) }}</span>
              <span>{{ getCategoryLabel(activity.category || activity.categoryCode) }}</span>
              <span>{{ formatDate(activity.startAt) }}</span>
            </div>
            <h3>{{ activity.title || '未命名活动' }}</h3>
            <p class="location">{{ activity.location || activity.locationText || activity.address || '地点待定' }}</p>
            <p class="desc">{{ activity.description || activity.subtitle || '暂无简介' }}</p>
            <p v-if="activity.reviewNote" class="review-note">审核备注：{{ activity.reviewNote }}</p>
            <textarea
              v-model="reviewNotes[activity.id]"
              rows="2"
              placeholder="审核备注"
              :disabled="actioningId === activity.id"
            ></textarea>
          </div>
          <div class="actions">
            <button
              class="btn primary"
              type="button"
              :disabled="activity.status !== 'REVIEWING' || actioningId === activity.id"
              @click="handleApprove(activity)"
            >
              通过
            </button>
            <button
              class="btn danger"
              type="button"
              :disabled="activity.status !== 'REVIEWING' || actioningId === activity.id"
              @click="handleReject(activity)"
            >
              驳回
            </button>
            <button class="btn light" type="button" @click="goDetail(activity.id)">详情</button>
          </div>
        </article>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  approveLocalActReview,
  fetchAdminLocalActReviews,
  rejectLocalActReview
} from '@/api/localAct';
import { getActivityStatusLabel, getCategoryLabel } from '@/constants/localAct';
import type { LocalActivityListItem } from '@/types/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';
import fallbackCover from '../../../pictures/homePicture1.jpg';

const router = useRouter();
const loading = ref(false);
const errorMsg = ref('');
const keyword = ref('');
const currentStatus = ref('REVIEWING');
const reviews = ref<LocalActivityListItem[]>([]);
const reviewNotes = reactive<Record<number, string>>({});
const actioningId = ref<number | null>(null);

const statusOptions = [
  { label: '待审核', value: 'REVIEWING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '全部', value: '' }
];

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
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const normalizeActivity = (activity: LocalActivityListItem): LocalActivityListItem => ({
  ...activity,
  category: activity.category || activity.categoryCode,
  location: activity.location || activity.locationText || activity.address
});

const loadReviews = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const list = await fetchAdminLocalActReviews({
      status: currentStatus.value,
      keyword: keyword.value.trim(),
      page: 1,
      size: 50
    });
    reviews.value = list.map(normalizeActivity);
  } catch (error) {
    reviews.value = [];
    errorMsg.value = error instanceof Error ? error.message : '审核列表加载失败';
  } finally {
    loading.value = false;
  }
};

const noteOf = (id?: number) => (id ? (reviewNotes[id] || '').trim() : '');

const handleApprove = async (activity: LocalActivityListItem) => {
  if (!activity.id) return;
  actioningId.value = activity.id;
  errorMsg.value = '';
  try {
    await approveLocalActReview(activity.id, noteOf(activity.id));
    await loadReviews();
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '审核通过失败';
  } finally {
    actioningId.value = null;
  }
};

const handleReject = async (activity: LocalActivityListItem) => {
  if (!activity.id) return;
  actioningId.value = activity.id;
  errorMsg.value = '';
  try {
    await rejectLocalActReview(activity.id, noteOf(activity.id));
    await loadReviews();
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '审核驳回失败';
  } finally {
    actioningId.value = null;
  }
};

const goDetail = (id?: number) => {
  if (id) router.push(`/local-act/${id}`);
};

onMounted(loadReviews);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.review-page {
  min-height: 100vh;
  padding: 96px clamp(20px, 4vw, 56px) 72px;
  color: #0f172a;
}

.card {
  max-width: 1280px;
  margin: 0 auto 20px;
  background: #fff;
  border-radius: 18px;
}

.hero {
  padding: 28px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.eyebrow,
h1,
h2,
h3,
p {
  margin: 0;
}

.eyebrow {
  margin-bottom: 10px;
  font-size: 12px;
  color: #ff6b2c;
  font-weight: 700;
}

.subtitle {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.7;
}

.toolbar {
  padding: 18px 24px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  gap: 14px;
  align-items: end;
}

.search-box,
.status-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-box span,
.status-box span {
  font-size: 12px;
  color: #64748b;
}

.search-box input,
.status-box select,
textarea {
  width: 100%;
  border: none;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
  outline: none;
}

.search-box input,
.status-box select {
  height: 40px;
  padding: 0 12px;
}

textarea {
  margin-top: 12px;
  padding: 10px 12px;
  resize: vertical;
  font-family: inherit;
}

.btn {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 600;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  background: #fee2e2;
  color: #dc2626;
}

.records {
  padding: 24px;
}

.records-head {
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.records-head span {
  color: #94a3b8;
  font-size: 13px;
}

.review-item {
  padding: 18px 0;
  border-bottom: 1px solid #f1f5f9;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 92px;
  gap: 18px;
}

.cover {
  width: 180px;
  aspect-ratio: 16 / 10;
  border-radius: 14px;
  object-fit: cover;
  background: #f1f5f9;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.meta span {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}

.location,
.desc,
.review-note {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.6;
  font-size: 13px;
}

.review-note {
  color: #b45309;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.state {
  padding: 42px 16px;
  text-align: center;
  color: #94a3b8;
}

.state.error {
  color: #dc2626;
}

@media (max-width: 900px) {
  .hero,
  .review-item {
    display: flex;
    flex-direction: column;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .cover {
    width: 100%;
  }
}
</style>
