<template>
  <dhstyle />
  <div class="lns-page">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">邻里互助中心</p>
        <h1>发布或认领互助任务，让温暖在社区流动</h1>
        <p class="subtitle">
          支持看护、陪诊、跑腿、维修等多种互助场景。可设置时间段、紧急程度与积分奖励，系统会自动匹配合适的志愿者。
        </p>
        <div class="hero-actions">
          <button class="primary" @click="openDialog">
            <i class="fas fa-plus"></i>
            发起互助
          </button>
          <button class="ghost" @click="scrollToTasks">浏览任务</button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat">
          <strong>{{ tasks.length || 12 }}</strong>
          <span>正在进行</span>
        </div>
        <div class="stat">
          <strong>{{ openCount }}</strong>
          <span>等待认领</span>
        </div>
        <div class="stat">
          <strong>{{ urgentCount }}</strong>
          <span>紧急任务</span>
        </div>
      </div>
    </section>

    <div class="filter-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['filter-tab', { active: currentTab === tab.value }]"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
        <span class="count">{{ countBy(tab.value) }}</span>
      </button>
    </div>

    <section class="tasks" id="tasks-list">
      <article v-for="task in displayedTasks" :key="task.id" class="card">
        <header>
          <span class="category-pill">{{ task.category }}</span>
          <span class="badge" :class="priorityClass(task.priority)">{{ priorityLabel(task.priority) }}</span>
        </header>
        <h3>{{ task.title }}</h3>
        <p class="task-desc">{{ task.description }}</p>
        <div class="meta">
          <span><i class="far fa-clock"></i>{{ formatTimeRange(task) }}</span>
          <span><i class="fas fa-location-dot"></i>{{ task.location || '位置待定' }}</span>
          <span><i class="far fa-user"></i>需 {{ task.volunteerSlots }} 人</span>
          <span v-if="task.rewardPoints && task.rewardPoints > 0" class="reward"><i class="fas fa-gift"></i>{{ task.rewardPoints }} 积分</span>
        </div>
        <footer>
          <div class="owner">
            <span class="owner-avatar">{{ (task.owner || '社')[0] }}</span>
            <span>{{ task.owner || '社区用户' }}</span>
          </div>
          <button class="ghost" @click="openDetail(task)">查看详情</button>
        </footer>
      </article>
      <p v-if="!displayedTasks.length" class="empty">
        <i class="far fa-folder-open"></i>
        暂无互助任务，成为第一个发布的人吧。
      </p>
    </section>

    <div v-if="showDialog" class="modal-mask">
      <div class="modal">
        <header class="modal-header">
          <h3>发起互助</h3>
          <button class="close" @click="closeDialog">×</button>
        </header>
        <div class="modal-body">
          <div class="form-grid">
            <label class="field">
              <span>标题 *</span>
              <input v-model="form.title" type="text" placeholder="例如：周末陪诊 · 独居老人" />
            </label>
            <label class="field">
              <span>分类 *</span>
              <select v-model="form.category">
                <option disabled value="">请选择</option>
                <option value="陪诊">陪诊</option>
                <option value="看护">看护</option>
                <option value="跑腿">跑腿</option>
                <option value="维修">维修</option>
                <option value="其他">其他</option>
              </select>
            </label>
            <label class="field">
              <span>开始时间</span>
              <input v-model="form.startTime" type="datetime-local" />
            </label>
            <label class="field">
              <span>结束时间</span>
              <input v-model="form.endTime" type="datetime-local" />
            </label>
            <label class="field">
              <span>地点</span>
              <input v-model="form.location" type="text" placeholder="如：市三医院东门" />
            </label>
            <label class="field">
              <span>需求人数</span>
              <input v-model.number="form.volunteerSlots" type="number" min="1" />
            </label>
            <label class="field">
              <span>优先级</span>
              <select v-model="form.priority">
                <option value="LOW">轻松</option>
                <option value="MEDIUM">一般</option>
                <option value="HIGH">紧急</option>
              </select>
            </label>
            <label class="field">
              <span>积分奖励</span>
              <input v-model.number="form.rewardPoints" type="number" min="0" />
            </label>
          </div>
          <label class="field">
            <span>需求描述</span>
            <textarea v-model="form.description" rows="4" placeholder="说明需要做什么、时间地点等细节"></textarea>
          </label>
          <p v-if="submitMessage" :class="['submit-msg', submitType]">{{ submitMessage }}</p>
        </div>
        <footer class="modal-footer">
          <button class="ghost" @click="closeDialog" :disabled="submitting">取消</button>
          <button class="primary" @click="submitTask" :disabled="submitting">
            {{ submitting ? '提交中...' : '发布' }}
          </button>
        </footer>
      </div>
    </div>

    <div v-if="detailTask" class="modal-mask">
      <div class="modal">
        <header class="modal-header">
          <h3>任务详情</h3>
          <button class="close" @click="detailTask = null">×</button>
        </header>
        <div class="modal-body">
          <h4>{{ detailTask.title }}</h4>
          <p class="muted">{{ detailTask.category }} · {{ formatTimeRange(detailTask) }}</p>
          <p>{{ detailTask.description }}</p>
          <div class="meta detail">
            <span><i class="fas fa-map-marker-alt"></i>{{ detailTask.location || '位置待定' }}</span>
            <span><i class="fas fa-user-friends"></i>需求人数：{{ detailTask.volunteerSlots }}</span>
            <span><i class="fas fa-signal"></i>优先级：{{ priorityLabel(detailTask.priority) }}</span>
            <span v-if="detailTask.rewardPoints"><i class="fas fa-gift"></i>奖励：{{ detailTask.rewardPoints }} 分</span>
          </div>
        </div>
        <footer class="modal-footer">
          <button class="ghost" @click="detailTask = null">关闭</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';

type Task = {
  id: number;
  title: string;
  category: string;
  description: string;
  location: string;
  volunteerSlots: number;
  priority: string;
  rewardPoints: number;
  status: string;
  startTime?: string;
  endTime?: string;
  owner?: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const username = ref(localStorage.getItem('username') || '');

const tasks = ref<Task[]>([]);
const showDialog = ref(false);
const detailTask = ref<Task | null>(null);
const submitting = ref(false);
const submitMessage = ref('');
const submitType = ref<'success' | 'error' | ''>('');
const currentTab = ref<'all' | 'urgent' | 'open' | 'reward'>('all');

const tabs: Array<{ label: string; value: 'all' | 'urgent' | 'open' | 'reward' }> = [
  { label: '全部任务', value: 'all' },
  { label: '紧急', value: 'urgent' },
  { label: '可认领', value: 'open' },
  { label: '有积分', value: 'reward' }
];

const openCount = computed(() => tasks.value.filter((t) => t.status === 'OPEN' || !t.status).length);
const urgentCount = computed(() => tasks.value.filter((t) => (t.priority || '').toUpperCase() === 'HIGH').length);

const countBy = (value: 'all' | 'urgent' | 'open' | 'reward') => {
  if (value === 'all') return tasks.value.length;
  if (value === 'urgent') return urgentCount.value;
  if (value === 'open') return openCount.value;
  return tasks.value.filter((t) => t.rewardPoints && t.rewardPoints > 0).length;
};

const displayedTasks = computed(() => {
  const list = tasks.value;
  if (currentTab.value === 'urgent') return list.filter((t) => (t.priority || '').toUpperCase() === 'HIGH');
  if (currentTab.value === 'open') return list.filter((t) => t.status === 'OPEN' || !t.status);
  if (currentTab.value === 'reward') return list.filter((t) => t.rewardPoints && t.rewardPoints > 0);
  return list;
});

const scrollToTasks = () => {
  document.getElementById('tasks-list')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const form = reactive({
  title: '',
  category: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  volunteerSlots: 1,
  priority: 'MEDIUM',
  rewardPoints: 0
});

onMounted(() => {
  fetchTasks();
});

const fetchTasks = async () => {
  try {
    const resp = await fetch(`${API_BASE}/api/neighbor-support/tasks`);
    const data = await resp.json();
    if (!resp.ok || (data && data.code && data.code !== 200)) {
      throw new Error((data && data.message) || '获取任务失败');
    }
    tasks.value = (data.data || []).map((item: any) => ({
      id: item.id,
      title: item.title,
      category: item.category,
      description: item.description,
      location: item.location,
      volunteerSlots: item.volunteerSlots || 1,
      priority: item.priority || 'MEDIUM',
      rewardPoints: item.rewardPoints || 0,
      status: item.status || 'OPEN',
      startTime: item.startTime,
      endTime: item.endTime,
      owner: item.owner
    }));
  } catch (err) {
    console.error(err);
  }
};

const openDialog = () => {
  submitMessage.value = '';
  submitType.value = '';
  showDialog.value = true;
};

const closeDialog = () => {
  showDialog.value = false;
};

const openDetail = (task: Task) => {
  detailTask.value = task;
};

const submitTask = async () => {
  if (!username.value) {
    submitMessage.value = '请先登录后再发布互助';
    submitType.value = 'error';
    return;
  }
  if (!form.title.trim() || !form.category) {
    submitMessage.value = '请填写必填项：标题、分类';
    submitType.value = 'error';
    return;
  }

  submitting.value = true;
  submitMessage.value = '';
  submitType.value = '';
  try {
    const resp = await fetch(`${API_BASE}/api/neighbor-support/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        title: form.title,
        category: form.category,
        description: form.description,
        location: form.location,
        startTime: form.startTime ? new Date(form.startTime).toISOString() : '',
        endTime: form.endTime ? new Date(form.endTime).toISOString() : '',
        volunteerSlots: form.volunteerSlots,
        priority: form.priority,
        rewardPoints: form.rewardPoints
      })
    });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || (data && data.code && data.code !== 200)) {
      const msg = (data && (data.message || data.data)) || '发布失败';
      throw new Error(typeof msg === 'string' ? msg : '发布失败');
    }

    submitMessage.value = '发布成功！';
    submitType.value = 'success';
    await fetchTasks();
    setTimeout(() => {
      showDialog.value = false;
    }, 600);
  } catch (err) {
    submitMessage.value = err instanceof Error ? err.message : '发布失败';
    submitType.value = 'error';
  } finally {
    submitting.value = false;
  }
};

const priorityLabel = (p?: string) => {
  switch ((p || '').toUpperCase()) {
    case 'HIGH':
      return '紧急';
    case 'LOW':
      return '轻松';
    default:
      return '一般';
  }
};

const priorityClass = (p?: string) => {
  const val = (p || '').toUpperCase();
  if (val === 'HIGH') return 'high';
  if (val === 'LOW') return 'low';
  return 'medium';
};

const formatTimeRange = (task: Task) => {
  if (!task.startTime && !task.endTime) return '时间待定';
  const start = task.startTime ? new Date(task.startTime).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';
  const end = task.endTime ? new Date(task.endTime).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '';
  return `${start}${end ? ' - ' + end : ''}`;
};
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lns-page {
  color: #0f172a;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: 32px;
  align-items: center;
}

.hero-text {
  max-width: 600px;
}

.subtitle {
  margin: 0;
  max-width: 540px;
}

.hero-actions {
  margin-top: 22px;
  display: flex;
  gap: 10px;
}

.hero-actions .primary i {
  margin-right: 6px;
  font-size: 11px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat {
  padding: 18px 14px;
  border-radius: 16px;
  background: #f8fafc;
  text-align: center;
}

.stat strong {
  display: block;
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  color: #ff6b2c;
  letter-spacing: -0.02em;
}

.stat span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.filter-tabs {
  max-width: 1280px;
  margin: 0 auto 20px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.filter-tabs::-webkit-scrollbar {
  display: none;
}

.filter-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.filter-tab:hover {
  background: #f1f5f9;
}

.filter-tab.active {
  background: #0f172a;
  color: #ffffff;
}

.filter-tab .count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
}

.filter-tab.active .count {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.tasks {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
  background: #ffffff;
  border-radius: 16px;
  cursor: default;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 36px rgba(15, 23, 42, 0.08);
}

.card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.category-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(117, 99, 255, 0.08);
  color: #7563ff;
  font-size: 11.5px;
  font-weight: 500;
}

.card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.task-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  font-size: 12.5px;
  color: #94a3b8;
}

.meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta i {
  width: 12px;
  text-align: center;
  font-size: 11px;
  color: #cbd5e1;
}

.meta .reward {
  color: #ff6b2c;
}

.meta .reward i {
  color: #ff6b2c;
}

.badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
}

.badge.high {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
}

.badge.medium {
  background: rgba(255, 107, 44, 0.1);
  color: #ff6b2c;
}

.badge.low {
  background: rgba(56, 185, 130, 0.1);
  color: #1aa053;
}

.card footer {
  margin-top: 4px;
  padding-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
}

.owner {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #64748b;
}

.owner-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff1ea;
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
  background: #ffffff;
  border-radius: 16px;
}

.empty i {
  font-size: 32px;
  color: #cbd5e1;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.32);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  z-index: 2000;
}

.modal {
  background: #ffffff;
  border-radius: 20px;
  width: min(720px, 92vw);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.16);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 22px 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-footer {
  padding: 18px 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  background: #fafbfc;
}

.modal-body {
  padding: 8px 26px 22px;
  max-height: 60vh;
  overflow-y: auto;
}

.close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 999px;
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.field > span {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  border: none;
  border-radius: 12px;
  padding: 0 14px;
  height: 42px;
  background: #f8fafc;
  font-size: 14px;
  color: #0f172a;
  outline: none;
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.field textarea {
  height: auto;
  min-height: 96px;
  padding: 12px 14px;
  resize: vertical;
  line-height: 1.6;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
}

.muted {
  color: #94a3b8;
  font-size: 13px;
  margin: 4px 0 12px;
}

.meta.detail {
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  margin-top: 10px;
}

.submit-msg {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
}

.submit-msg.success {
  background: rgba(56, 185, 130, 0.08);
  color: #1aa053;
}

.submit-msg.error {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }
}
</style>
