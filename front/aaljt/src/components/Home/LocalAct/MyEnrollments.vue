<template>
  <dhstyle />
  <div class="lae-page">
    <section class="lae-hero">
      <div>
        <p class="eyebrow">��������</p>
        <h1>��ע��������г̣�ǩ�����򲹡�����һվ����</h1>
      </div>
      <div class="hero-actions">
        <button class="ghost" @click="exportEnrollments">����������¼</button>
        <button class="primary" @click="goToPublish">�����</button>
      </div>
    </section>

    <section class="stats">
      <div class="stat-card" v-for="stat in statsCards" :key="stat.key">
        <p class="label">{{ stat.label }}</p>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.desc }}</small>
      </div>
    </section>

    <div class="layout">
      <aside class="filters">
        <h3>ɸѡ</h3>
        <label>
          ״̬
          <select v-model="filters.status">
            <option value="">ȫ��</option>
            <option value="confirmed">��ȷ��</option>
            <option value="pending">�����</option>
            <option value="waitlist">��</option>
          </select>
        </label>
        <label>
          ʱ��
          <select v-model="filters.period">
            <option value="upcoming">������ʼ</option>
            <option value="past">��ʷ�</option>
          </select>
        </label>
        <label>
          �ؼ���
          <input v-model="filters.keyword" type="text" placeholder="�������֯��" />
        </label>
      </aside>

      <section class="list">
        <header class="list-header">
          <div>
            <h2>�ҵı���</h2>
            <p>��ǰ��ʾ {{ filtered.length }} ����¼</p>
          </div>
          <button class="ghost sm">��������</button>
        </header>

        <div class="cards">
          <div v-if="loading" class="card info-card">���ڼ��ر�������...</div>
          <div v-else-if="errorMsg" class="card info-card error-card">{{ errorMsg }}</div>
          <div v-else-if="!filtered.length" class="card info-card">���ޱ�����¼</div>
          <article v-else v-for="item in filtered" :key="item.id" class="card">
            <div class="card-left">
              <p class="date">{{ item.date }}</p>
              <h3>{{ item.title }}</h3>
              <p class="meta">{{ item.location }} �� {{ item.organizer }}</p>
              <div class="tags">
                <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <div class="card-right">
              <span class="status" :class="item.status">{{ statusLabel(item.status) }}</span>
              <p class="reminder">{{ item.reminder }}</p>
              <div class="actions">
                <button class="ghost sm">�鿴����</button>
                <button v-if="item.status === 'pending'" class="primary sm">��������</button>
                <button v-else class="ghost sm">ȡ������</button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
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
  { key: 'upcoming', label: '������ʼ', value: 0, desc: '��Ҫǩ��' },
  { key: 'participated', label: '�ۼƲ���', value: 0, desc: '�����' },
  { key: 'hours', label: '־Ըʱ��', value: 0, desc: '�Ѽ�¼' }
]);

const enrollments = ref<Enrollment[]>([]);
const loading = ref(false);
const errorMsg = ref('');

const filters = ref<Filters>({
  status: '',
  period: 'upcoming',
  keyword: ''
});

const formatDate = (iso?: string) => {
  if (!iso) return 'ʱ�����';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return 'ʱ�����';
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}/${day}`;
};

const updateStats = (stats?: ApiStats) => {
  statsCards.value = [
    { key: 'upcoming', label: '������ʼ', value: stats?.upcomingCount ?? 0, desc: '��Ҫǩ��' },
    { key: 'participated', label: '�ۼƲ���', value: stats?.totalParticipated ?? 0, desc: '�����' },
    { key: 'hours', label: '־Ըʱ��', value: stats?.volunteerHours ?? 0, desc: '�Ѽ�¼' }
  ];
};

const fetchEnrollments = async () => {
  if (!username.value) {
    errorMsg.value = '���ȵ�¼��鿴������Ϣ';
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
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '���ر�������ʧ��';
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
  if (status === 'confirmed') return '��ȷ��';
  if (status === 'pending') return '�����';
  return '����';
};

const goToPublish = () => {
  router.push('/local-act/publish');
};

const exportEnrollments = () => {
  if (!filtered.value.length) {
    errorMsg.value = '���޿ɵ����ı�����¼';
    return;
  }
  const csv = [
    ['�', 'ʱ��', '�ص�', '״̬'].join(','),
    ...filtered.value.map((item) => [item.title, item.date, item.location, statusLabel(item.status)].join(','))
  ].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'local-act-enrollments.csv';
  link.click();
  URL.revokeObjectURL(link.href);
};
</script>

<style scoped>
:global(body) {
  background: #f4f6f8;
}

.lae-page {
  padding-top: 80px;
  color: #111827;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.lae-hero {
  margin: 48px;
  padding: 28px 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #122a19, #1aa053);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin: 0 48px 24px;
}

.stat-card {
  background: #fff;
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.stat-card .label {
  font-size: 13px;
  color: #6b7280;
}

.stat-card strong {
  font-size: 32px;
}

.layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  margin: 0 48px 56px;
  align-items: start;
}

.filters,
.list {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  padding: 24px;
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 14px;
}

.filters input,
.filters select {
  border: 1px solid #e0e5f0;
  border-radius: 12px;
  padding: 10px 12px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card {
  border: 1px solid #edf0f5;
  border-radius: 18px;
  padding: 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.card-left .date {
  color: #1aa053;
  font-weight: 600;
}

.meta {
  color: #6b7280;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.tags span {
  background: #f3f6fb;
  color: #475467;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.card-right {
  text-align: right;
  min-width: 200px;
}

.info-card {
  justify-content: center;
  text-align: center;
  color: #4b5563;
}

.error-card {
  color: #d93025;
}

.status {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 6px;
}

.status.confirmed {
  background: rgba(26, 160, 83, 0.12);
  color: #1aa053;
}

.status.pending {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.status.waitlist {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}

.reminder {
  font-size: 13px;
  color: #6b7280;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.primary,
.ghost {
  border-radius: 999px;
  padding: 10px 20px;
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.ghost {
  border: 1px solid #d2d8e5;
  background: transparent;
  color: #1f2933;
}

.primary {
  background: linear-gradient(120deg, #1aa053, #0a6b3b);
  color: #fff;
}

.primary.sm,
.ghost.sm {
  padding: 8px 16px;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .card {
    flex-direction: column;
  }
  .card-right {
    text-align: left;
  }
}

@media (max-width: 768px) {
  .lae-hero,
  .stats,
  .layout {
    margin: 24px;
  }
  .hero-actions {
    flex-direction: column;
  }
}
</style>
