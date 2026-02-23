<template>
  <dhstyle />
  <div class="lns-page">
    <section class="hero">
      <div>
        <p class="eyebrow">邻里互助中心</p>
        <h1>发布或认领互助任务，让温暖在社区流动</h1>
        <p class="subtitle">
          支持看护、陪诊、跑腿、维修等多种互助场景。可设置时间段、紧急程度与积分奖励，系统会自动匹配合适的志愿者。
        </p>
      </div>
      <button class="primary" @click="openDialog">发起互助</button>
    </section>

    <section class="tasks">
      <article v-for="task in tasks" :key="task.id" class="card">
        <header>
          <div>
            <h3>{{ task.title }}</h3>
            <p>{{ task.category }} · {{ formatTimeRange(task) }}</p>
          </div>
          <span class="badge" :class="priorityClass(task.priority)">{{ priorityLabel(task.priority) }}</span>
        </header>
        <p>{{ task.description }}</p>
        <div class="meta">
          <span><i class="fas fa-map-marker-alt"></i>{{ task.location || '位置待定' }}</span>
          <span><i class="fas fa-user-friends"></i>需求人数：{{ task.volunteerSlots }}</span>
          <span v-if="task.rewardPoints && task.rewardPoints > 0"><i class="fas fa-gift"></i>奖励：{{ task.rewardPoints }} 分</span>
        </div>
        <footer>
          <span>发布人：{{ task.owner || '社区用户' }}</span>
          <button class="ghost" @click="openDetail(task)">查看详情</button>
        </footer>
      </article>
      <p v-if="!tasks.length" class="empty">暂无互助任务，成为第一个发布的人吧。</p>
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
import { onMounted, reactive, ref } from 'vue';
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
  background: #f5f6f8;
}

.lns-page {
  padding-top: 80px;
  color: #111827;
}

.hero {
  margin: 48px;
  padding: 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #361146, #9333ea);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.primary {
  border: none;
  border-radius: 999px;
  padding: 12px 26px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.tasks {
  margin: 0 48px 60px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  border-radius: 22px;
  padding: 20px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.meta i {
  margin-right: 6px;
}

.badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
}

.badge.high {
  background: #dc2626;
}

.badge.medium {
  background: #f97316;
}

.badge.low {
  background: #10b981;
}

.ghost {
  border: 1px solid #d3d9e5;
  background: transparent;
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
}

.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #6b7280;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: grid;
  place-items: center;
  z-index: 2000;
}

.modal {
  background: #fff;
  border-radius: 20px;
  width: min(720px, 92vw);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header,
.modal-footer {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eef2f7;
}

.modal-footer {
  border-top: 1px solid #eef2f7;
  border-bottom: none;
}

.modal-body {
  padding: 16px 20px 8px;
}

.close {
  border: none;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #4b5563;
}

.field input,
.field select,
.field textarea {
  border: 1px solid #dfe3eb;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
}

.muted {
  color: #6b7280;
  margin: 4px 0 12px;
}

.meta.detail {
  flex-direction: column;
  gap: 6px;
}

.submit-msg {
  margin-top: 10px;
  font-size: 13px;
}

.submit-msg.success {
  color: #0f9d58;
}

.submit-msg.error {
  color: #d93025;
}

@media (max-width: 800px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
