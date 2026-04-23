<template>
  <dhstyle />
  <div class="lae-page">
    <section class="lae-hero card">
      <div>
        <p class="eyebrow">????</p>
        <h1>???????</h1>
        <p class="subtitle">??????????????????????????</p>
      </div>
      <div class="hero-actions">
        <button class="btn btn-light" @click="exportEnrollments">????</button>
        <button class="btn btn-primary" @click="goToPublish">????</button>
      </div>
    </section>

    <section class="stats-row">
      <article class="stat-card" v-for="stat in statsCards" :key="stat.key">
        <p class="label">{{ stat.label }}</p>
        <strong>{{ stat.value }}</strong>
        <span>{{ stat.desc }}</span>
      </article>
    </section>

    <section class="content-layout">
      <aside class="filters card">
        <h3>????</h3>
        <label>
          ??
          <select v-model="filters.status">
            <option value="">????</option>
            <option value="confirmed">???</option>
            <option value="pending">???</option>
            <option value="waitlist">???</option>
          </select>
        </label>
        <label>
          ??
          <select v-model="filters.period">
            <option value="upcoming">????</option>
            <option value="past">????</option>
          </select>
        </label>
        <label>
          ???
          <input v-model="filters.keyword" type="text" placeholder="??? / ?? / ???" />
        </label>
        <button class="btn btn-light full" @click="refreshList">????</button>
      </aside>

      <div class="records card">
        <header class="records-header">
          <div>
            <h2>????</h2>
            <p>??? {{ filtered.length }} ?</p>
          </div>
        </header>

        <p v-if="infoMsg" class="info-msg">{{ infoMsg }}</p>

        <div class="records-list">
          <div v-if="loading" class="record empty">????????...</div>
          <div v-else-if="errorMsg" class="record empty error">{{ errorMsg }}</div>
          <div v-else-if="!filtered.length" class="record empty">???????????</div>

          <article v-else v-for="item in filtered" :key="item.id" class="record">
            <div class="record-main">
              <div class="record-head">
                <p class="date">{{ item.date }}</p>
                <span class="status" :class="item.status">{{ statusLabel(item.status) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="meta">{{ item.location }} ? {{ item.organizer }}</p>
              <div class="tags" v-if="item.tags.length">
                <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              </div>
              <p class="reminder">???{{ item.reminder }}</p>
            </div>
            <div class="record-actions">
              <button class="btn btn-light sm" @click="goToDetail(item.id)">????</button>
              <button
                v-if="item.status === 'pending'"
                class="btn btn-light sm"
                @click="remindReview(item)"
              >
                ???
              </button>
              <button v-else class="btn btn-primary sm" @click="cancelEnrollment(item)">????</button>
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
import dhstyle from '../../dhstyle/dhstyle.vue';

type Status = 'confirmed' | 'pending' | 'waitlist';

type Enrollment = {
  id: number;
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
  { key: 'upcoming', label: '????', value: 0, desc: '????' },
  { key: 'participated', label: '????', value: 0, desc: '????' },
  { key: 'hours', label: '????', value: 0, desc: '????' }
]);

const enrollments = ref<Enrollment[]>([]);
const loading = ref(false);
const errorMsg = ref('');
const infoMsg = ref('');

const filters = ref<Filters>({
  status: '',
  period: 'upcoming',
  keyword: ''
});

const formatDate = (iso?: string) => {
  if (!iso) return '????';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '????';
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}/${day}`;
};

const updateStats = (stats?: ApiStats) => {
  statsCards.value = [
    { key: 'upcoming', label: '????', value: stats?.upcomingCount ?? 0, desc: '????' },
    { key: 'participated', label: '????', value: stats?.totalParticipated ?? 0, desc: '????' },
    { key: 'hours', label: '????', value: stats?.volunteerHours ?? 0, desc: '????' }
  ];
};

const fetchEnrollments = async () => {
  if (!username.value) {
    errorMsg.value = '???????????';
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
        title: item.title,
        location: item.location,
        organizer: item.organizer,
        status: item.status,
        reminder: item.reminder,
        tags: item.tags ?? [],
        date: formatDate(item.startAt)
      })) ?? [];
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '????????';
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
  if (status === 'confirmed') return '???';
  if (status === 'pending') return '???';
  return '???';
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
  infoMsg.value = `???????????${item.title}?`;
};

const cancelEnrollment = (item: Enrollment) => {
  infoMsg.value = `????${item.title}??????????`;
};

const exportEnrollments = () => {
  if (!filtered.value.length) {
    errorMsg.value = '????????????';
    return;
  }

  const csv = [
    ['??', '??', '??', '??'].join(','),
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
  background: #f5f6f8;
}

.lae-page {
  padding: 76px 40px 36px;
  color: #1f2937;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.card {
  background: #ffffff;
  border: 1px solid #e3e9f2;
  border-radius: 12px;
}

.lae-hero {
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  color: #667085;
  letter-spacing: 0.08em;
}

.lae-hero h1 {
  margin: 8px 0 6px;
  font-size: 28px;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 8px;
}

.stats-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e3e9f2;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-card .label {
  margin: 0;
  font-size: 12px;
  color: #667085;
}

.stat-card strong {
  font-size: 28px;
  color: #2f6ea5;
  line-height: 1.2;
}

.stat-card span {
  font-size: 12px;
  color: #667085;
}

.content-layout {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 12px;
}

.filters {
  padding: 14px;
  height: fit-content;
}

.filters h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #4b5563;
}

.filters input,
.filters select {
  border: 1px solid #d9e0ea;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  color: #1f2937;
}

.records {
  padding: 14px;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.records-header h2 {
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}

.records-header p {
  margin: 2px 0 0;
  font-size: 13px;
  color: #667085;
}

.info-msg {
  margin: 0 0 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #edf5fc;
  border: 1px solid #cfe0f2;
  font-size: 12px;
  color: #2f6ea5;
}

.records-list {
  display: grid;
  gap: 8px;
}

.record {
  border: 1px solid #e5ebf3;
  border-radius: 10px;
  background: #ffffff;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.record.empty {
  justify-content: center;
  color: #667085;
  font-size: 13px;
}

.record.empty.error {
  color: #cf4f4f;
}

.record-main {
  min-width: 0;
}

.record-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date {
  margin: 0;
  font-size: 12px;
  color: #2f6ea5;
}

.record h3 {
  margin: 4px 0 2px;
  font-size: 16px;
  color: #1f2937;
}

.meta,
.reminder {
  margin: 0;
  font-size: 13px;
  color: #667085;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 7px 0;
}

.tags span {
  display: inline-flex;
  align-items: center;
  border: 1px solid #dce4ef;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
  color: #475467;
  background: #f8fafd;
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid;
}

.status.confirmed {
  color: #2f855a;
  border-color: #b9e2cc;
  background: #f2fbf6;
}

.status.pending {
  color: #c56e10;
  border-color: #f2d9b3;
  background: #fdf8ef;
}

.status.waitlist {
  color: #2f6ea5;
  border-color: #cfe0f2;
  background: #f1f7fd;
}

.btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #d2dae8;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  cursor: pointer;
}

.btn:hover {
  background: #f6f8fc;
  border-color: #c3cddd;
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

.btn.sm {
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
}

.btn.full {
  width: 100%;
}

@media (max-width: 1200px) {
  .lae-page {
    padding: 72px 20px 30px;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .filters {
    order: 2;
  }
}

@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .record {
    flex-direction: column;
  }

  .record-actions {
    justify-content: flex-start;
  }
}
</style>
