<template>
  <dhstyle />
  <div class="lst-page">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">固定日程</p>
        <h1>创建循环活动，让邻里每周都能见面</h1>
        <p class="subtitle">
          适用于夜跑、公益课堂、兴趣社团等需要周期举办的活动。设置一次，系统将自动生成日程、提醒和报名表单。
        </p>
        <div class="hero-actions">
          <button class="primary" @click="openDialog">
            <i class="fas fa-plus"></i>
            新建模板
          </button>
        </div>
      </div>
      <div class="hero-illustration">
        <div class="calendar-mini">
          <div class="cm-head">
            <span>本周固定日程</span>
            <strong>{{ templates.length }}</strong>
          </div>
          <div class="cm-grid">
            <span v-for="d in ['一', '二', '三', '四', '五', '六', '日']" :key="d" class="cm-day">{{ d }}</span>
            <span
              v-for="i in 7"
              :key="`slot-${i}`"
              :class="['cm-slot', { active: hasTemplateOn(i) }]"
            ></span>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <aside class="sidebar">
        <p class="sidebar-label">模板分类</p>
        <ul>
          <li
            v-for="cat in categories"
            :key="cat"
            :class="{ active: cat === currentCategory }"
            @click="currentCategory = cat"
          >
            <span>{{ cat }}</span>
            <span class="cat-count">{{ cat === '全部模板' ? templates.length : Math.max(1, Math.floor(templates.length / 2)) }}</span>
          </li>
        </ul>
        <div class="sidebar-tip">
          <i class="far fa-lightbulb"></i>
          <p>循环活动建议设置 1440 分钟提醒,让邻里提前一天准备。</p>
        </div>
      </aside>

      <div class="templates">
        <article v-for="template in templates" :key="template.id" class="card">
          <div class="card-head">
            <span class="card-icon"><i class="far fa-calendar-check"></i></span>
            <span class="freq-pill">{{ template.frequency }}</span>
          </div>
          <h3>{{ template.title }}</h3>
          <p>{{ template.summary }}</p>
          <div class="meta">
            <span><i class="fas fa-location-dot"></i>{{ template.location }}</span>
            <span><i class="far fa-user"></i>{{ template.owner }}</span>
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

const hasTemplateOn = (dayIndex: number) => [1, 3, 6].includes(dayIndex);

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
  background: #fafbfc;
}

.lst-page {
  color: #0f172a;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: 32px;
  align-items: center;
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

.hero-illustration {
  display: flex;
  justify-content: center;
}

.calendar-mini {
  width: 100%;
  max-width: 280px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #f8fafc;
}

.cm-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}

.cm-head span {
  font-size: 12px;
  color: #64748b;
}

.cm-head strong {
  font-size: 22px;
  font-weight: 600;
  color: #ff6b2c;
  letter-spacing: -0.02em;
}

.cm-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cm-day {
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  font-weight: 500;
}

.cm-slot {
  height: 28px;
  border-radius: 8px;
  background: #ffffff;
}

.cm-slot.active {
  background: rgba(255, 107, 44, 0.16);
  box-shadow: inset 0 0 0 2px #ff6b2c;
}

.content {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: 96px;
  padding: 22px;
  background: #ffffff;
  border-radius: 18px;
}

.sidebar-label {
  margin: 0 0 14px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
}

.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13.5px;
  color: #475569;
  transition: background 0.18s ease, color 0.18s ease;
}

.sidebar li:hover {
  background: #f8fafc;
}

.sidebar li.active {
  background: rgba(255, 107, 44, 0.08);
  color: #f25a1b;
  font-weight: 500;
}

.cat-count {
  font-size: 11.5px;
  color: #94a3b8;
  font-weight: 500;
}

.sidebar li.active .cat-count {
  color: #ff6b2c;
}

.sidebar-tip {
  margin-top: 22px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  display: flex;
  gap: 10px;
}

.sidebar-tip i {
  flex-shrink: 0;
  margin-top: 2px;
  color: #ff6b2c;
}

.sidebar-tip p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: #64748b;
}

.templates {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 36px rgba(15, 23, 42, 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.card-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 107, 44, 0.1);
  color: #ff6b2c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.freq-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
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

.card > p {
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
  font-size: 12.5px;
  color: #94a3b8;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
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

.actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.ghost {
  border: none;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  padding: 0 16px;
  height: 34px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: background 0.18s ease;
}

.ghost:hover {
  background: #eef2f6;
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.field span {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.field input,
.field select {
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

.field input:focus,
.field select:focus {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
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
  .hero,
  .content {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
  }
}
</style>
