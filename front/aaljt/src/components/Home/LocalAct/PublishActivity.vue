<template>
  <dhstyle />
  <div class="lap-page">
    <section class="lap-hero card">
      <div class="hero-main">
        <p class="eyebrow">发布活动</p>
        <h1>把一场社区活动，整理成清晰好参与的入口</h1>
        <p class="subtitle">
          填写活动信息、报名规则和提醒设置，系统会生成活动卡片并同步到本地活动页。
        </p>
      </div>
      <div class="hero-side">
        <div class="progress-head">
          <span>完成度</span>
          <strong>{{ publishProgress }}%</strong>
        </div>
        <div class="progress-track" role="progressbar" :aria-valuenow="publishProgress" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-fill" :style="{ width: `${publishProgress}%` }"></div>
        </div>
        <p class="progress-tip">{{ progressTip }}</p>
        <div class="hero-actions">
          <button class="btn btn-light" :disabled="saving" @click="handleSubmit('DRAFT')">保存草稿</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">
            {{ saving ? '发布中...' : '发布活动' }}
          </button>
        </div>
      </div>
    </section>

    <div class="lap-layout">
      <section class="panel form-panel">
        <h2>基础信息</h2>
        <div class="form-grid">
          <label class="field">
            <span>活动标题</span>
            <input v-model="form.title" type="text" placeholder="例如：旧物再利用市集" />
          </label>
          <label class="field">
            <span>副标题</span>
            <input v-model="form.subtitle" type="text" placeholder="一句话说明活动亮点" />
          </label>
          <label class="field">
            <span>活动分类</span>
            <select v-model="form.category">
              <option disabled value="">请选择分类</option>
              <option v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>活动日期</span>
            <input v-model="form.date" type="date" />
          </label>
          <label class="field">
            <span>开始时间</span>
            <input v-model="form.timeStart" type="time" />
          </label>
          <label class="field">
            <span>结束时间</span>
            <input v-model="form.timeEnd" type="time" />
          </label>
          <label class="field address-field">
            <span>活动地点</span>
            <div class="address-controls">
              <input v-model="form.location" type="text" placeholder="例如：社区广场 A 区" />
              <button class="btn btn-light sm" type="button" :disabled="locating" @click="locateByAddress">
                {{ locating ? '定位中...' : '地址定位' }}
              </button>
              <button class="btn btn-light sm" type="button" :disabled="locating" @click="locateCurrent">
                {{ locating ? '定位中...' : '当前位置' }}
              </button>
            </div>
          </label>
          <label class="field">
            <span>人数上限</span>
            <input v-model.number="form.capacity" type="number" min="1" />
          </label>
          <label class="field">
            <span>费用说明</span>
            <input v-model="form.fee" type="text" placeholder="免费 / AA / 付费" />
          </label>
        </div>

        <label class="field block-field">
          <span>活动介绍</span>
          <textarea
            v-model="form.description"
            rows="5"
            placeholder="说明活动内容、适合人群、参与方式和注意事项"
          ></textarea>
        </label>

        <div class="field block-field">
          <span>活动标签</span>
          <div class="tag-suggestions">
            <button
              v-for="tag in tagSuggestions"
              :key="tag"
              :class="['tag-chip', form.tags.includes(tag) ? 'active' : '']"
              type="button"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <h2>报名设置</h2>
        <div class="form-grid">
          <label class="field">
            <span>报名方式</span>
            <select v-model="form.registration">
              <option value="auto">自动确认 / 站内报名</option>
              <option value="manual">人工审核</option>
              <option value="external">外部链接</option>
            </select>
          </label>
          <label class="field">
            <span>提醒时间</span>
            <select v-model="form.reminder">
              <option value="24h">开始前 24 小时</option>
              <option value="3h">开始前 3 小时</option>
              <option value="custom">自定义</option>
            </select>
          </label>
          <label class="field">
            <span>签到</span>
            <select v-model="form.checkin">
              <option value="yes">开启现场签到</option>
              <option value="no">无需签到</option>
            </select>
          </label>
          <label class="field">
            <span>候补</span>
            <select v-model="form.waiting">
              <option value="yes">开启候补</option>
              <option value="no">不开启</option>
            </select>
          </label>
        </div>

        <div class="form-actions">
          <button class="btn btn-light" :disabled="saving" @click="handleSubmit('DRAFT')">保存草稿</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">发布活动</button>
        </div>
        <p v-if="message" :class="['submit-msg', messageType]">{{ message }}</p>
      </section>

      <aside class="preview-panel">
        <div class="panel preview-card">
          <div class="preview-head">
            <p class="eyebrow">实时预览</p>
            <span class="state-chip">{{ registrationLabel }}</span>
          </div>
          <h3>{{ preview.title }}</h3>
          <p class="preview-meta">
            {{ preview.date }} ? {{ preview.timeRange }}<br />
            {{ preview.location }}
          </p>
          <div class="preview-tags">
            <span v-for="tag in preview.tags" :key="tag">{{ tag }}</span>
          </div>
          <p class="preview-desc">{{ preview.description }}</p>
          <div class="preview-seats">
            <span>人数上限 {{ form.capacity || 0 }}</span>
            <span>签到 {{ form.checkin === 'yes' ? '开启' : '关闭' }}</span>
          </div>
        </div>

        <div class="panel steps-card">
          <h4>发布步骤</h4>
          <ul>
            <li v-for="(step, index) in steps" :key="step.title">
              <span class="dot" :class="{ done: index < completedSteps }"></span>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </li>
          </ul>
        </div>

        <div class="panel tips-card">
          <h4>运营建议</h4>
          <ul>
            <li>标题尽量明确活动对象和参与价值。</li>
            <li>封面建议使用 1200×600 的横向图片。</li>
            <li>时间、地点、取消规则请写清楚，减少报名后沟通成本。</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';

type PublishStatus = 'DRAFT' | 'PUBLISHED';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || '';
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/geo';
const REVERSE_GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo';
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';
const username = ref(localStorage.getItem('username') || '');

const categories = [
  { value: 'sport', label: '运动健康' },
  { value: 'eco', label: '环保共建' },
  { value: 'skill', label: '技能课堂' },
  { value: 'kids', label: '亲子活动' },
  { value: 'market', label: '市集交换' }
];

const tagSuggestions = ['报名中', '适合亲子', '免费', '邻里互助', '环保', '公益', '室内活动'];

const steps = [
  { title: '补充信息', desc: '填写标题、时间、地点和说明' },
  { title: '确认规则', desc: '设置报名、提醒和签到方式' },
  { title: '发布上线', desc: '保存后同步到本地活动页' }
];

const form = ref({
  title: '',
  subtitle: '',
  category: '',
  date: '',
  timeStart: '',
  timeEnd: '',
  location: '',
  capacity: 20,
  fee: '免费',
  description: '',
  tags: [] as string[],
  registration: 'auto',
  reminder: '24h',
  checkin: 'yes',
  waiting: 'yes'
});

const saving = ref(false);
const message = ref('');
const messageType = ref<'success' | 'error'>('success');
const lat = ref<number | null>(null);
const lng = ref<number | null>(null);
const locating = ref(false);

const requiredKeys: (keyof typeof form.value)[] = [
  'title',
  'category',
  'date',
  'timeStart',
  'timeEnd',
  'location',
  'description'
];

const publishProgress = computed(() => {
  const filled = requiredKeys.filter((key) => String(form.value[key]).trim().length > 0).length;
  return Math.round((filled / requiredKeys.length) * 100);
});

const progressTip = computed(() => {
  if (publishProgress.value < 40) return '先补齐标题、分类、时间和地点。';
  if (publishProgress.value < 80) return '再完善介绍和报名设置，活动会更容易被理解。';
  return '信息已经比较完整，可以保存草稿或直接发布。';
});

const preview = computed(() => ({
  title: form.value.title || '社区活动标题',
  date: form.value.date ? new Date(form.value.date).toLocaleDateString('zh-CN') : '待定日期',
  timeRange: form.value.timeStart && form.value.timeEnd ? `${form.value.timeStart} - ${form.value.timeEnd}` : '待定时间',
  location: form.value.location || '待定地点',
  tags: form.value.tags.length ? form.value.tags : ['活动标签', '社区共创'],
  description: form.value.description || '这里会展示活动介绍、参与方式和温馨提示。'
}));

const registrationLabel = computed(() => {
  switch (form.value.registration) {
    case 'manual':
      return '人工审核';
    case 'external':
      return '外部报名';
    default:
      return '自动确认';
  }
});

const completedSteps = computed(() => {
  if (publishProgress.value >= 90) return steps.length;
  if (publishProgress.value >= 60) return 2;
  return 1;
});

const toggleTag = (tag: string) => {
  form.value.tags = form.value.tags.includes(tag)
    ? form.value.tags.filter((value) => value !== tag)
    : [...form.value.tags, tag];
};

const locateByAddress = async () => {
  if (!AMAP_KEY) {
    message.value = '请先配置 VITE_AMAP_KEY';
    messageType.value = 'error';
    return;
  }
  const address = form.value.location.trim();
  if (!address) {
    message.value = '请先填写活动地点';
    messageType.value = 'error';
    return;
  }

  locating.value = true;
  try {
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&address=${encodeURIComponent(address)}`);
    const data = await resp.json();
    if (data.status !== '1' || !data.geocodes?.length || !data.geocodes[0]?.location) {
      throw new Error('未找到该地址坐标');
    }
    const parts = String(data.geocodes[0].location).split(',').map(parseFloat);
    if (parts.length !== 2 || parts.some((value) => Number.isNaN(value))) {
      throw new Error('坐标解析失败');
    }
    lng.value = parts[0];
    lat.value = parts[1];
    message.value = '地址定位成功';
    messageType.value = 'success';
  } catch (error: any) {
    message.value = error?.message || '地址定位失败';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const locateCurrent = async () => {
  if (!AMAP_KEY) {
    message.value = '请先配置 VITE_AMAP_KEY';
    messageType.value = 'error';
    return;
  }
  if (!('geolocation' in navigator)) {
    message.value = '当前浏览器不支持定位';
    messageType.value = 'error';
    return;
  }

  locating.value = true;
  try {
    const coords = await new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        (err) => reject(err),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    });

    let useLat = coords.latitude;
    let useLng = coords.longitude;

    try {
      const convertResp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${useLng},${useLat}&coordsys=gps`);
      const convertData = await convertResp.json();
      if (convertData.status === '1' && convertData.locations) {
        const points = String(convertData.locations).split(',').map(parseFloat);
        if (points.length === 2 && points.every((value: number) => !Number.isNaN(value))) {
          useLng = points[0];
          useLat = points[1];
        }
      }
    } catch (_) {
      // Fallback to browser-provided coordinates.
    }

    try {
      const reverseResp = await fetch(`${REVERSE_GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`);
      const reverseData = await reverseResp.json();
      if (reverseResp.ok && reverseData.status === '1' && reverseData.regeocode?.formatted_address) {
        form.value.location = reverseData.regeocode.formatted_address;
      }
    } catch (_) {
      // Keep coordinate values even if reverse geocode fails.
    }

    lat.value = useLat;
    lng.value = useLng;
    message.value = '已获取当前位置并尝试回填地址';
    messageType.value = 'success';
  } catch (error: any) {
    const code = Number(error?.code);
    if (code === 1) message.value = '定位权限被拒绝';
    else if (code === 2) message.value = '无法获取当前位置';
    else if (code === 3) message.value = '定位请求超时';
    else message.value = error?.message || '定位失败';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const handleSubmit = async (status: PublishStatus) => {
  if (!username.value) {
    message.value = '请先登录后再发布活动';
    messageType.value = 'error';
    return;
  }

  saving.value = true;
  message.value = '';
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/activities`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        title: form.value.title,
        subtitle: form.value.subtitle,
        category: form.value.category,
        date: form.value.date,
        timeStart: form.value.timeStart,
        timeEnd: form.value.timeEnd,
        location: form.value.location,
        latitude: lat.value,
        longitude: lng.value,
        capacity: form.value.capacity,
        fee: form.value.fee,
        description: form.value.description,
        tags: form.value.tags,
        registration: form.value.registration,
        reminder: form.value.reminder,
        checkin: form.value.checkin,
        waiting: form.value.waiting,
        status
      })
    });

    if (!resp.ok) {
      throw new Error(await resp.text());
    }

    message.value = status === 'DRAFT' ? '草稿已保存' : '活动已发布，等待同步展示';
    messageType.value = 'success';
  } catch (error) {
    message.value = error instanceof Error ? error.message : '活动提交失败';
    messageType.value = 'error';
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
:global(body) {
  background: #fafbfc;
}

.lap-page {
  color: #0f172a;
}

.lap-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 32px;
  padding: 32px 36px;
  align-items: center;
}

.subtitle {
  margin: 14px 0 0;
  max-width: 540px;
  line-height: 1.65;
  font-size: 14.5px;
  color: #64748b;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  border-radius: 16px;
  background: #f8fafc;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.progress-head strong {
  font-size: 24px;
  font-weight: 600;
  color: #ff6b2c;
  letter-spacing: -0.02em;
}

.progress-track {
  height: 6px;
  width: 100%;
  border-radius: 999px;
  background: #ffffff;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #ff6b2c;
  transition: width 0.3s ease;
}

.progress-tip {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.55;
}

.hero-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.lap-layout {
  margin-top: 24px !important;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  align-items: start;
}

.form-panel {
  padding: 32px;
}

.form-panel h2 {
  margin: 0 0 18px;
  padding-bottom: 14px;
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-panel h2::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #ff6b2c;
}

.form-panel h2:not(:first-child) {
  margin-top: 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field span {
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
  min-height: 110px;
  padding: 12px 14px;
  resize: vertical;
  font-family: inherit;
  line-height: 1.65;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
}

.field input::placeholder,
.field textarea::placeholder {
  color: #94a3b8;
}

.address-field,
.block-field {
  grid-column: 1 / -1;
}

.address-controls {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.tag-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  border: none !important;
  background: #f8fafc !important;
  color: #475569 !important;
  border-radius: 999px !important;
  padding: 0 14px !important;
  height: 32px !important;
  font-size: 12.5px !important;
  font-weight: 500 !important;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.tag-chip:hover {
  background: #eef2f6 !important;
}

.tag-chip.active {
  background: rgba(255, 107, 44, 0.1) !important;
  color: #f25a1b !important;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px !important;
  padding: 0 20px !important;
  border-radius: 999px !important;
  border: none !important;
  font-size: 13.5px !important;
  font-weight: 500 !important;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.2s ease, color 0.18s ease;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-primary {
  background: #ff6b2c !important;
  color: #ffffff !important;
}

.btn-primary:hover:not(:disabled) {
  background: #f25a1b !important;
  transform: translateY(-1px);
}

.btn-light {
  background: #f8fafc !important;
  color: #475569 !important;
}

.btn-light:hover:not(:disabled) {
  background: #eef2f6 !important;
  color: #0f172a !important;
}

.btn.sm {
  height: 36px !important;
  padding: 0 14px !important;
  font-size: 12.5px !important;
}

.form-actions {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.submit-msg {
  margin: 14px 0 0;
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

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 96px;
}

.preview-card,
.steps-card,
.tips-card {
  padding: 24px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.state-chip {
  display: inline-flex;
  align-items: center;
  height: 22px !important;
  padding: 0 10px !important;
  border-radius: 999px !important;
  font-size: 11.5px !important;
  font-weight: 500 !important;
}

.preview-card h3 {
  margin: 0;
  font-size: 18px !important;
  font-weight: 600 !important;
  color: #0f172a;
  letter-spacing: -0.015em;
}

.preview-meta {
  margin: 12px 0;
  line-height: 1.65;
  font-size: 13px;
  color: #64748b;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;
}

.preview-tags span {
  border: none;
  border-radius: 999px;
  padding: 0 10px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  font-size: 11.5px;
  font-weight: 500;
  color: #475569;
  background: #f8fafc;
}

.preview-desc {
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
}

.preview-seats {
  margin-top: 12px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.preview-seats span {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  font-size: 12px;
  color: #475569;
}

.steps-card h4,
.tips-card h4 {
  margin: 0 0 16px;
  font-size: 14px !important;
  font-weight: 600 !important;
  color: #0f172a;
}

.steps-card ul,
.tips-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.steps-card li {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 12px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e2e8f0;
  margin-top: 4px;
}

.dot.done {
  background: #ff6b2c;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.16);
}

.steps-card strong {
  display: block;
  font-size: 13.5px;
  font-weight: 500;
  color: #0f172a;
}

.steps-card p,
.tips-card li {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.6;
}

.tips-card li {
  position: relative;
  padding-left: 18px;
}

.tips-card li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #ff6b2c;
}

@media (max-width: 1100px) {
  .lap-hero,
  .lap-layout {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .preview-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .lap-hero {
    padding: 24px 22px;
  }

  .form-panel {
    padding: 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .address-controls {
    grid-template-columns: 1fr;
  }
}
</style>
