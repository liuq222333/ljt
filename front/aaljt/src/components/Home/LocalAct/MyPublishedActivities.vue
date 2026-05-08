<template>
  <dhstyle />
  <div class="my-act-page">
    <section class="hero card">
      <div>
        <p class="eyebrow">我的发布</p>
        <h1>管理我发起的本地活动</h1>
        <p class="subtitle">查看草稿、待审核、报名中和已取消活动，快速确认活动状态。</p>
      </div>
      <button class="btn primary" type="button" @click="goPublish">发布活动</button>
    </section>

    <section class="stats">
      <article v-for="item in stats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="layout">
      <aside class="filters card">
        <h3>筛选</h3>
        <button
          v-for="item in statusOptions"
          :key="item.value"
          type="button"
          :class="{ active: filters.status === item.value }"
          @click="setStatus(item.value)"
        >
          {{ item.label }}
        </button>
      </aside>

      <main class="records card">
        <div class="records-head">
          <h2>活动记录</h2>
          <button class="btn light" type="button" @click="loadActivities">刷新</button>
        </div>

        <p v-if="loading" class="state">正在加载我发布的活动...</p>
        <p v-else-if="errorMsg" class="state error">{{ errorMsg }}</p>
        <p v-else-if="!activities.length" class="state">暂无发布记录。</p>

        <template v-else>
          <article v-for="activity in activities" :key="activity.id" class="record">
            <div class="record-main">
              <div class="record-top">
                <span class="date">{{ formatDate(activity.startAt) }}</span>
                <span class="status">{{ statusLabel(activity.status) }}</span>
              </div>
              <h3>{{ activity.title || '未命名活动' }}</h3>
              <p>{{ activity.location || activity.locationText || activity.address || '地点待定' }}</p>
              <p class="desc">{{ activity.description || activity.subtitle || '暂无简介' }}</p>
              <p v-if="activity.reviewNote" class="review-note">审核备注：{{ activity.reviewNote }}</p>
            </div>
            <div class="actions">
              <button class="btn light" type="button" @click="goDetail(activity.id)">查看详情</button>
            </div>
          </article>
        </template>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMyLocalActivities } from '@/api/localAct';
import { getActivityStatusLabel } from '@/constants/localAct';
import type { LocalActivityListItem } from '@/types/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';

const router = useRouter();
const username = ref(localStorage.getItem('username') || '');
const loading = ref(false);
const errorMsg = ref('');
const activities = ref<LocalActivityListItem[]>([]);

const filters = reactive({
  status: ''
});

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'REVIEWING' },
  { label: '报名中', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已取消', value: 'CANCELLED' }
];

const stats = computed(() => [
  { label: '全部活动', value: activities.value.length },
  { label: '待审核', value: activities.value.filter((item) => item.status === 'REVIEWING').length },
  { label: '报名中', value: activities.value.filter((item) => item.status === 'PUBLISHED').length },
  { label: '草稿', value: activities.value.filter((item) => item.status === 'DRAFT').length }
]);

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

const statusLabel = (status?: string) => getActivityStatusLabel(status);

const normalizeActivity = (activity: LocalActivityListItem): LocalActivityListItem => ({
  ...activity,
  category: activity.category || activity.categoryCode,
  location: activity.location || activity.locationText || activity.address
});

const loadActivities = async () => {
  if (!username.value) {
    errorMsg.value = '请先登录后查看我的发布';
    activities.value = [];
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  try {
    const list = await fetchMyLocalActivities({
      username: username.value,
      status: filters.status,
      page: 1,
      size: 50
    });
    activities.value = list.map(normalizeActivity);
  } catch (error) {
    activities.value = [];
    errorMsg.value = error instanceof Error ? error.message : '加载我的发布失败';
  } finally {
    loading.value = false;
  }
};

const setStatus = (status: string) => {
  filters.status = status;
  loadActivities();
};

const goPublish = () => {
  router.push('/local-act/publish');
};

const goDetail = (id?: number) => {
  if (id) router.push(`/local-act/${id}`);
};

onMounted(loadActivities);
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.my-act-page {
  min-height: 100vh;
  padding: 96px clamp(20px, 4vw, 56px) 72px;
  color: #0f172a;
}

.card,
.stats article {
  background: #fff;
  border-radius: 18px;
}

.hero {
  max-width: 1280px;
  margin: 0 auto 20px;
  padding: 28px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  color: #ff6b2c;
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
  line-height: 1.7;
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

.stats {
  max-width: 1280px;
  margin: 0 auto 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stats article {
  padding: 20px;
}

.stats span {
  color: #94a3b8;
  font-size: 13px;
}

.stats strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.layout {
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
}

.filters,
.records {
  padding: 24px;
}

.filters h3 {
  margin: 0 0 16px;
}

.filters button {
  width: 100%;
  height: 36px;
  margin-bottom: 8px;
  border: none;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  cursor: pointer;
}

.filters button.active {
  background: rgba(255, 107, 44, 0.12);
  color: #ff6b2c;
}

.records-head,
.record {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.records-head {
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.record {
  padding: 18px 0;
  border-bottom: 1px solid #f1f5f9;
}

.record-main {
  min-width: 0;
}

.record-top {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.date,
.status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
}

.record h3 {
  margin-bottom: 8px;
}

.desc {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.review-note {
  margin-top: 8px;
  color: #b45309;
  font-size: 13px;
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
  .record {
    flex-direction: column;
  }

  .stats,
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
