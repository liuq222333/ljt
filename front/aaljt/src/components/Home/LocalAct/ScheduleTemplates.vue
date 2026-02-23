<template>
  <dhstyle />
  <div class="lst-page">
    <section class="hero">
      <div>
        <p class="eyebrow">固定日程</p>
        <h1>创建循环活动，让邻里每周都能见面</h1>
        <p class="subtitle">
          适用于夜跑、公益课堂、兴趣社团等需要周期举办的活动。设置一次，系统将自动生成日程、提醒和报名表单。
        </p>
      </div>
      <button class="primary" @click="openDialog">新建日程模板</button>
    </section>

    <section class="content">
      <aside class="sidebar">
        <h3>模板分类</h3>
        <ul>
          <li v-for="cat in categories" :key="cat" :class="{ active: cat === currentCategory }">
            {{ cat }}
          </li>
        </ul>
      </aside>
      <div class="templates">
        <article v-for="template in templates" :key="template.id" class="card">
          <header>
            <h3>{{ template.title }}</h3>
            <span>{{ template.frequency }}</span>
          </header>
          <p>{{ template.summary }}</p>
          <div class="meta">
            <span>地点：{{ template.location }}</span>
            <span>负责人：{{ template.owner }}</span>
          </div>
          <div class="actions">
            <button class="ghost">预览设置</button>
            <button class="primary">应用模板</button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="showDialog" class="modal-mask">
      <div class="modal">
        <header class="modal-header">
          <h3>新建日程模板</h3>
          <button class="close" @click="closeDialog">×</button>
        </header>
        <div class="modal-body">
          <div class="form-grid">
            <label class="field">
              <span>模板名称 *</span>
              <input v-model="templateForm.title" type="text" placeholder="例如：社区夜跑 · 每周二" />
            </label>
            <label class="field">
              <span>模板分类</span>
              <select v-model="templateForm.category">
                <option value="">不限</option>
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </label>
            <label class="field">
              <span>周几 *</span>
              <select v-model.number="templateForm.weekday">
                <option v-for="day in weekdays" :key="day.value" :value="day.value">
                  {{ day.label }}
                </option>
              </select>
            </label>
            <label class="field">
              <span>开始时间 *</span>
              <input v-model="templateForm.startTime" type="time" />
            </label>
            <label class="field">
              <span>结束时间 *</span>
              <input v-model="templateForm.endTime" type="time" />
            </label>
            <label class="field">
              <span>地点</span>
              <input v-model="templateForm.location" type="text" placeholder="集合点/教室/场地" />
            </label>
            <label class="field">
              <span>重复规则</span>
              <input v-model="templateForm.recurrenceRule" type="text" placeholder="如：每周二自动生成" />
            </label>
            <label class="field">
              <span>人数上限</span>
              <input v-model.number="templateForm.capacity" type="number" min="0" />
            </label>
            <label class="field">
              <span>费用类型</span>
              <select v-model="templateForm.feeType">
                <option value="FREE">免费</option>
                <option value="AA">AA</option>
                <option value="PAID">付费</option>
              </select>
            </label>
            <label class="field">
              <span>提醒（分钟）</span>
              <input v-model.number="templateForm.reminderMinutes" type="number" min="0" />
            </label>
          </div>
          <p v-if="submitMessage" :class="['submit-msg', submitType]">{{ submitMessage }}</p>
        </div>
        <footer class="modal-footer">
          <button class="ghost" @click="closeDialog" :disabled="submitting">取消</button>
          <button class="primary" @click="onCreateTemplate" :disabled="submitting">
            {{ submitting ? '提交中...' : '创建模板' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';

type TemplateItem = {
  id: string;
  title: string;
  frequency: string;
  summary: string;
  location: string;
  owner: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const username = ref(localStorage.getItem('username') || '');

const categories = ['全部模板', '运动健康', '志愿互助', '技能课堂', '亲子活动'];
const currentCategory = ref('全部模板');

const templates = ref<TemplateItem[]>([
  {
    id: 't1',
    title: '社区夜跑 · 每周二',
    frequency: '每周二 19:30',
    summary: '由跑者联盟领跑，系统自动限制25人名额并开启签到。',
    location: '中央景观道 · 南门口',
    owner: '跑者联盟'
  },
  {
    id: 't2',
    title: '公益课堂 · 每月第一周',
    frequency: '每月首周周末',
    summary: '邀请专业讲师授课，可自动同步到公共服务栏目。',
    location: '邻里服务中心 · 302',
    owner: '社区发展协会'
  },
  {
    id: 't3',
    title: '亲子共读 · 每周六',
    frequency: '每周六 10:00',
    summary: '适合亲子家庭的阅读分享活动，可直接复用报名表单。',
    location: '溪语书屋',
    owner: '童心共创实验室'
  }
]);

const weekdays = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 0, label: '周日' }
];

const templateForm = reactive({
  title: '',
  category: '',
  weekday: 1,
  startTime: '19:00',
  endTime: '20:00',
  location: '',
  recurrenceRule: '',
  capacity: 20,
  feeType: 'FREE',
  reminderMinutes: 1440
});

const showDialog = ref(false);
const submitting = ref(false);
const submitMessage = ref('');
const submitType = ref<'success' | 'error' | ''>('');

const openDialog = () => {
  submitMessage.value = '';
  submitType.value = '';
  showDialog.value = true;
};

const closeDialog = () => {
  showDialog.value = false;
};

const formatFrequency = () => {
  const weekdayLabel = weekdays.find((d) => d.value === templateForm.weekday)?.label ?? '每周';
  return `${weekdayLabel} ${templateForm.startTime || '--:--'} - ${templateForm.endTime || '--:--'}`;
};

const onCreateTemplate = async () => {
  if (!username.value) {
    submitMessage.value = '请先登录后再创建模板';
    submitType.value = 'error';
    return;
  }
  if (!templateForm.title.trim() || !templateForm.startTime || !templateForm.endTime) {
    submitMessage.value = '请填写必填项：名称、周几、开始/结束时间';
    submitType.value = 'error';
    return;
  }

  submitting.value = true;
  submitMessage.value = '';
  submitType.value = '';
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/schedule-templates`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        title: templateForm.title,
        category: templateForm.category,
        weekday: templateForm.weekday,
        startTime: templateForm.startTime,
        endTime: templateForm.endTime,
        location: templateForm.location,
        recurrenceRule: templateForm.recurrenceRule,
        capacity: templateForm.capacity,
        feeType: templateForm.feeType,
        reminderMinutes: templateForm.reminderMinutes,
        status: 'ACTIVE'
      })
    });

    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || (data && data.code && data.code !== 200)) {
      const msg = (data && (data.message || data.data)) || '创建失败';
      throw new Error(typeof msg === 'string' ? msg : '创建失败');
    }

    const newId = (data && data.data && (data.data.templateId || data.data.id)) || `temp-${Date.now()}`;
    templates.value.unshift({
      id: String(newId),
      title: templateForm.title,
      frequency: formatFrequency(),
      summary: '系统将按此模板自动生成周期日程与提醒。',
      location: templateForm.location || '待定',
      owner: username.value || '我'
    });

    submitMessage.value = '创建成功！';
    submitType.value = 'success';
    setTimeout(() => {
      showDialog.value = false;
    }, 800);
  } catch (err) {
    submitMessage.value = err instanceof Error ? err.message : '创建失败';
    submitType.value = 'error';
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
:global(body) {
  background: #f4f6f8;
}

.lst-page {
  padding-top: 80px;
  color: #111827;
}

.hero {
  margin: 48px;
  padding: 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #0a301a, #1aa053);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.primary {
  border: none;
  border-radius: 999px;
  padding: 12px 28px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.content {
  margin: 0 48px 60px;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
}

.sidebar {
  background: #fff;
  border-radius: 22px;
  padding: 20px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar li {
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  color: #4b5563;
}

.sidebar li.active {
  background: rgba(26, 160, 83, 0.12);
  color: #1aa053;
}

.templates {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  border-radius: 22px;
  padding: 20px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

.ghost {
  border-radius: 999px;
  border: 1px solid #d4dae5;
  background: transparent;
  color: #1f2933;
  padding: 8px 16px;
  cursor: pointer;
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
  padding: 16px 20px 4px;
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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
.field select {
  border: 1px solid #dfe3eb;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
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

@media (max-width: 900px) {
  .content {
    grid-template-columns: 1fr;
  }
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
