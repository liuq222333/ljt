<template>
  <dhstyle />
  <div class="lae-page">
    <section class="lae-hero card">
      <div>
        <p class="eyebrow">我的参与</p>
        <h1>统一管理我的活动报名</h1>
        <p class="subtitle">查看即将参加、待审核和候补中的活动，也可以导出报名记录。</p>
      </div>
      <div class="hero-actions">
        <button class="btn btn-light" @click="exportEnrollments">导出记录</button>
        <button class="btn btn-primary" @click="goToPublish">发布活动</button>
      </div>
    </section>

    <section class="stats-row">
      <article class="stat-card" v-for="stat in statsCards" :key="stat.key">
        <span class="stat-icon" :class="`stat-${stat.key}`">
          <i :class="['fas', stat.icon || 'fa-calendar']"></i>
        </span>
        <div class="stat-text">
          <p class="label">{{ stat.label }}</p>
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.desc }}</span>
        </div>
      </article>
    </section>

    <section class="content-layout">
      <aside class="filters card">
        <h3>筛选</h3>
        <label>
          状态
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="confirmed">已确认</option>
            <option value="pending">待审核</option>
            <option value="waitlist">候补中</option>
          </select>
        </label>
        <label>
          时间
          <select v-model="filters.period">
            <option value="upcoming">即将开始</option>
            <option value="past">历史活动</option>
          </select>
        </label>
        <label>
          关键词
          <input v-model="filters.keyword" type="text" placeholder="活动 / 地点 / 组织者" />
        </label>
        <button class="btn btn-light full" @click="refreshList">刷新列表</button>
      </aside>

      <div class="records card">
        <header class="records-header">
          <div>
            <h2>报名记录</h2>
            <p>共 {{ filtered.length }} 条</p>
          </div>
        </header>

        <p v-if="infoMsg" class="info-msg">{{ infoMsg }}</p>

        <div class="records-list">
          <div v-if="loading" class="record empty">正在加载报名记录...</div>
          <div v-else-if="errorMsg" class="record empty error">{{ errorMsg }}</div>
          <div v-else-if="!filtered.length" class="record empty">暂无匹配的报名记录。</div>

          <article v-else v-for="item in filtered" :key="item.id" class="record">
            <div class="record-main">
              <div class="record-head">
                <p class="date">{{ item.date }}</p>
                <span class="status" :class="item.status">{{ statusLabel(item.status) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="meta">{{ item.location }} · {{ item.organizer }}</p>
              <div class="tags" v-if="item.tags.length">
                <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              </div>
              <p class="reminder">提醒：{{ item.reminder }}</p>
            </div>
            <div class="record-actions">
              <button class="btn btn-light sm" @click="goToDetail(item.activityId)">查看详情</button>
              <button
                v-if="item.status === 'pending'"
                class="btn btn-light sm"
                @click="remindReview(item)"
              >
                催审核
              </button>
              <button
                v-if="canCancel(item)"
                class="btn btn-primary sm"
                @click="cancelEnrollment(item)"
                :disabled="cancelingId === item.id"
              >
                {{ cancelingId === item.id ? '取消中...' : '取消报名' }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { cancelLocalActivityEnrollment } from '@/api/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';

type Status = 'confirmed' | 'pending' | 'waitlist' | 'cancelled' | 'checked_in' | 'completed';

type Enrollment = {
  id: number;
  activityId: number;
  title: string;
  date: string;
  location: string;
  organizer: string;
  tags: string[];
  status: Status;
  reminder: string;
};

type Filters = {
  status: '' | Status;
  period: '' | 'upcoming' | 'past';
  keyword: string;
};

type ApiEnrollmentItem = {
  id: number;
  activityId: number;
  title: string;
  location: string;
  organizer: string;
  status: Status;
  reminder: string;
  tags?: string[];
  startAt?: string;
};

type ApiStats = {
  upcomingCount?: number;
  totalParticipated?: number;
  volunteerHours?: number;
};

type ApiResponse = {
  items?: ApiEnrollmentItem[];
  stats?: ApiStats;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const router = useRouter();
const username = ref(localStorage.getItem('username') || '');

const statsCards = ref([
  { key: 'upcoming', label: '即将开始', value: 0, desc: '已报名活动', icon: 'fa-calendar-day' },
  { key: 'participated', label: '累计参与', value: 0, desc: '历史参与记录', icon: 'fa-clock-rotate-left' },
  { key: 'hours', label: '志愿时长', value: 0, desc: '累计服务小时', icon: 'fa-hand-holding-heart' }
]);

const enrollments = ref<Enrollment[]>([]);
const loading = ref(false);
const cancelingId = ref<number | null>(null);
const errorMsg = ref('');
const infoMsg = ref('');

const filters = ref<Filters>({
  status: '',
  period: 'upcoming',
  keyword: ''
});

const formatDate = (iso?: string) => {
  if (!iso) return '待定';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '待定';
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}/${day}`;
};

const updateStats = (stats?: ApiStats) => {
  statsCards.value = [
    { key: 'upcoming', label: '即将开始', value: stats?.upcomingCount ?? 0, desc: '已报名活动', icon: 'fa-calendar-day' },
    { key: 'participated', label: '累计参与', value: stats?.totalParticipated ?? 0, desc: '历史参与记录', icon: 'fa-clock-rotate-left' },
    { key: 'hours', label: '志愿时长', value: stats?.volunteerHours ?? 0, desc: '累计服务小时', icon: 'fa-hand-holding-heart' }
  ];
};

const fetchEnrollments = async () => {
  if (!username.value) {
    errorMsg.value = '请先登录后查看报名记录';
    enrollments.value = [];
    updateStats();
    return;
  }

  loading.value = true;
  errorMsg.value = '';
  try {
    const params = new URLSearchParams({ username: username.value });
    if (filters.value.status) params.append('status', filters.value.status);
    if (filters.value.period) params.append('period', filters.value.period);
    if (filters.value.keyword.trim()) params.append('keyword', filters.value.keyword.trim());

    const resp = await fetch(`${API_BASE}/api/local-act/enrollments?${params.toString()}`);
    if (!resp.ok) {
      throw new Error(await resp.text());
    }

    const data = (await resp.json()) as ApiResponse;
    updateStats(data.stats);
    enrollments.value =
      data.items?.map((item) => ({
        id: item.id,
        activityId: item.activityId,
        title: item.title,
        location: item.location,
        organizer: item.organizer,
        status: item.status,
        reminder: item.reminder,
        tags: item.tags ?? [],
        date: formatDate(item.startAt)
      })) ?? [];
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载报名记录失败';
  } finally {
    loading.value = false;
  }
};

onMounted(fetchEnrollments);

watch(
  () => ({ ...filters.value }),
  () => {
    fetchEnrollments();
  },
  { deep: true }
);

const filtered = computed(() => enrollments.value);

const statusLabel = (status: Status) => {
  if (status === 'confirmed') return '已确认';
  if (status === 'pending') return '待审核';
  if (status === 'waitlist') return '候补中';
  if (status === 'checked_in') return '已签到';
  if (status === 'completed') return '已完成';
  if (status === 'cancelled') return '已取消';
  return status;
};

const goToPublish = () => {
  router.push('/local-act/publish');
};

const goToDetail = (id: number) => {
  router.push(`/local-act/${id}`);
};

const refreshList = () => {
  infoMsg.value = '';
  fetchEnrollments();
};

const remindReview = (item: Enrollment) => {
  infoMsg.value = `已提醒组织者尽快审核「${item.title}」。`;
};

const canCancel = (item: Enrollment) =>
  ['confirmed', 'pending', 'waitlist'].includes(item.status);

const cancelEnrollment = async (item: Enrollment) => {
  if (!username.value) {
    errorMsg.value = '请先登录后再取消报名';
    return;
  }
  cancelingId.value = item.id;
  errorMsg.value = '';
  infoMsg.value = '';
  try {
    await cancelLocalActivityEnrollment(item.activityId, username.value, '用户主动取消');
    infoMsg.value = `已取消「${item.title}」的报名。`;
    await fetchEnrollments();
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '取消报名失败';
  } finally {
    cancelingId.value = null;
  }
};

const exportEnrollments = () => {
  if (!filtered.value.length) {
    errorMsg.value = '当前没有可导出的报名记录';
    return;
  }

  const csv = [
    ['活动', '日期', '地点', '状态'].join(','),
    ...filtered.value.map((item) => [item.title, item.date, item.location, statusLabel(item.status)].join(','))
  ].join('\\n');

  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'local-act-enrollments.csv';
  link.click();
  URL.revokeObjectURL(link.href);
};
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lae-page {
  color: #0f172a;
}

.lae-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.subtitle {
  margin: 14px 0 0;
  max-width: 540px;
  font-size: 14px;
  line-height: 1.65;
  color: #64748b;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.stats-row {
  max-width: 1280px;
  margin: 0 auto 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  background: #ffffff;
  border-radius: 16px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.stat-icon.stat-upcoming {
  background: rgba(255, 107, 44, 0.1);
  color: #ff6b2c;
}

.stat-icon.stat-participated {
  background: rgba(78, 142, 247, 0.1);
  color: #4e8ef7;
}

.stat-icon.stat-hours {
  background: rgba(56, 185, 130, 0.1);
  color: #38b982;
}

.stat-text {
  display: flex;
  flex-direction: column;
}

.stat-card .label {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
}

.stat-card strong {
  font-size: 26px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-card span:last-child {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.content-layout {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.filters {
  position: sticky;
  top: 96px;
  padding: 24px;
  background: #ffffff;
  border-radius: 18px;
}

.filters h3 {
  margin: 0 0 18px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 12.5px;
  font-weight: 500;
  color: #475569;
}

.filters input,
.filters select {
  border: none;
  height: 38px;
  border-radius: 10px;
  padding: 0 12px;
  background: #f8fafc;
  font-size: 13px;
  color: #0f172a;
  outline: none;
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.filters input:focus,
.filters select:focus {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
}

.records {
  padding: 28px;
  background: #ffffff;
  border-radius: 18px;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f1f5f9;
}

.records-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.records-header p {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}

.info-msg {
  margin: 0 0 16px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(78, 142, 247, 0.08);
  font-size: 13px;
  color: #2563eb;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record {
  padding: 18px 0;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.record:last-child {
  border-bottom: none;
}

.record.empty {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  color: #94a3b8;
  font-size: 13.5px;
  border-bottom: none;
  background: #f8fafc;
  border-radius: 14px;
}

.record.empty.error {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.04);
}

.record-main {
  min-width: 0;
  flex: 1;
}

.record-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.date {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 107, 44, 0.08);
}

.record h3 {
  margin: 4px 0 6px;
  font-size: 15.5px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.005em;
}

.meta,
.reminder {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
}

.reminder {
  margin-top: 8px;
  color: #94a3b8;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 0;
}

.tags span {
  display: inline-flex;
  align-items: center;
  border: none;
  border-radius: 999px;
  padding: 0 10px;
  height: 22px;
  font-size: 11.5px;
  font-weight: 500;
  color: #475569;
  background: #f1f5f9;
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
  border: none;
}

.status.confirmed {
  color: #1aa053;
  background: rgba(56, 185, 130, 0.1);
}

.status.pending {
  color: #d97706;
  background: rgba(245, 158, 11, 0.1);
}

.status.waitlist {
  color: #4e8ef7;
  background: rgba(78, 142, 247, 0.1);
}

.btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: none;
  background: #f8fafc;
  color: #475569;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.btn:hover {
  background: #eef2f6;
  color: #0f172a;
}

.btn-primary {
  background: #ff6b2c;
  color: #ffffff;
}

.btn-primary:hover {
  background: #f25a1b;
  color: #ffffff;
}

.btn.sm {
  height: 30px;
  padding: 0 14px;
  font-size: 12px;
}

.btn.full {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 1024px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .filters {
    position: static;
    order: 2;
  }
}

@media (max-width: 900px) {
  .lae-hero {
    flex-direction: column;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .record {
    flex-direction: column;
  }

  .record-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
