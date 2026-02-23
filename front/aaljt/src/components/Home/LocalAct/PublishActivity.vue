<template>
  <dhstyle />
  <div class="lap-page">
    <section class="lap-hero">
      <div>
        <p class="eyebrow">邻里活动 · 发布</p>
        <h1>把你的活动告诉邻里，尽快找到一起参与的伙伴</h1>
        <p class="subtitle">
          填写活动信息后，将自动展示地点和标签，方便附近的邻居快速查看。保存草稿不会公开，提交后进入审核。
        </p>
        <div class="hero-actions">
          <button class="ghost" :disabled="saving" @click="handleSubmit('DRAFT')">保存草稿</button>
          <button class="primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">
            {{ saving ? '提交中...' : '提交发布' }}
          </button>
        </div>
      </div>
      <div class="hero-progress">
        <span>完成度</span>
        <div class="progress-bar">
          <div class="progress" :style="{ width: `${publishProgress}%` }"></div>
        </div>
        <p class="progress-tip">{{ publishProgress }}% · {{ progressTip }}</p>
      </div>
    </section>

    <div class="lap-layout">
      <section class="form-panel">
        <h2>活动基础信息</h2>
        <div class="form-grid">
          <label class="field">
            <span>活动标题</span>
            <input v-model="form.title" type="text" placeholder="例如：社区夜跑 · 光影健康" />
          </label>
          <label class="field">
            <span>副标题</span>
            <input v-model="form.subtitle" type="text" placeholder="一句话亮点（可选）" />
          </label>
          <label class="field">
            <span>活动类型</span>
            <select v-model="form.category">
              <option disabled value="">请选择类型</option>
              <option v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>举办日期</span>
            <input v-model="form.date" type="date" />
          </label>
          <label class="field inline">
            <span>开始时间</span>
            <input v-model="form.timeStart" type="time" />
          </label>
          <label class="field inline">
            <span>结束时间</span>
            <input v-model="form.timeEnd" type="time" />
          </label>
          <label class="field address-field">
            <span>活动地点</span>
            <div class="address-row">
              <input v-model="form.location" type="text" placeholder="请填写详细地址或集合点" />
              <button class="locate-btn" type="button" :disabled="locating" @click="locateCurrent" aria-label="定位当前位置">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 2.5a.75.75 0 0 1 .75.75v1.74a6.26 6.26 0 0 1 6.26 6.26h1.74a.75.75 0 0 1 0 1.5h-1.74a6.26 6.26 0 0 1-6.26 6.26v1.73a.75.75 0 0 1-1.5 0v-1.73a6.26 6.26 0 0 1-6.26-6.26H3.01a.75.75 0 0 1 0-1.5h1.73A6.26 6.26 0 0 1 11.25 5V3.25A.75.75 0 0 1 12 2.5Zm0 4.51a5 5 0 1 0 0 10.01 5 5 0 0 0 0-10Z" fill="currentColor" />
                  <circle cx="12" cy="12" r="2" fill="currentColor" />
                </svg>
              </button>
            </div>
          </label>
          <label class="field">
            <span>人数上限</span>
            <input v-model.number="form.capacity" type="number" min="1" />
          </label>
          <label class="field">
            <span>费用说明</span>
            <input v-model="form.fee" type="text" placeholder="免费 / AA / 金额" />
          </label>
        </div>

        <div class="field">
          <span>活动介绍</span>
          <textarea
            v-model="form.description"
            rows="5"
            placeholder="填写活动亮点、流程、需携带物品等"
          ></textarea>
        </div>

        <div class="field">
          <span>标签（可选）</span>
          <div class="tag-suggestions">
            <button
              v-for="tag in tagSuggestions"
              :key="tag"
              :class="['tag-chip', form.tags.includes(tag) ? 'active' : '']"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <h2>报名与提醒</h2>
        <div class="form-grid">
          <label class="field">
            <span>报名方式</span>
            <select v-model="form.registration">
              <option value="auto">自动确认 / 即时</option>
              <option value="manual">人工审核</option>
              <option value="external">外部链接</option>
            </select>
          </label>
          <label class="field">
            <span>提醒时间</span>
            <select v-model="form.reminder">
              <option value="24h">活动前 24 小时</option>
              <option value="3h">活动前 3 小时</option>
              <option value="custom">自定义</option>
            </select>
          </label>
          <label class="field">
            <span>需要签到</span>
            <select v-model="form.checkin">
              <option value="yes">需要现场签到</option>
              <option value="no">无需签到</option>
            </select>
          </label>
          <label class="field">
            <span>候补名单</span>
            <select v-model="form.waiting">
              <option value="yes">开启候补</option>
              <option value="no">不开启</option>
            </select>
          </label>
        </div>

        <div class="form-actions">
          <button class="ghost" :disabled="saving" @click="handleSubmit('DRAFT')">保存草稿</button>
          <button class="primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">提交发布</button>
        </div>
        <p v-if="message" :class="['submit-msg', messageType]">{{ message }}</p>
      </section>

      <aside class="preview-panel">
        <div class="preview-card">
          <p class="eyebrow">实时预览</p>
          <h3>{{ preview.title }}</h3>
          <p class="preview-meta">
            {{ preview.date }} · {{ preview.timeRange }}<br />
            {{ preview.location }}
          </p>
          <div class="preview-tags">
            <span v-for="tag in preview.tags" :key="tag">{{ tag }}</span>
          </div>
          <p class="preview-desc">{{ preview.description }}</p>
          <div class="preview-seats">
            <span>人数 {{ form.capacity || 0 }}</span>
            <span>报名方式：{{ registrationLabel }}</span>
          </div>
        </div>

        <div class="steps-card">
          <h4>发布流程</h4>
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

        <div class="tips-card">
          <h4>温馨提示</h4>
          <ul>
            <li>内容需符合平台规范，不得含违规信息。</li>
            <li>建议封面 1200×600 以上清晰图片。</li>
            <li>志愿/公益活动可注明时间段与必备说明。</li>
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

// Vite will inline env values; use direct access to ensure they are picked up at build time.
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || '';
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/geo';
const REVERSE_GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo';
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';
const username = ref(localStorage.getItem('username') || '');

const categories = [
  { value: 'sport', label: '运动健身' },
  { value: 'eco', label: '环保公益' },
  { value: 'skill', label: '技能分享' },
  { value: 'kids', label: '亲子活动' },
  { value: 'market', label: '市集/展会' }
];

const tagSuggestions = ['志愿时光', '社区友好', '亲子', '技能交流', '环保', '健康', '签到实拍'];

const steps = [
  { title: '填写信息', desc: '补全标题、时间、地点和介绍' },
  { title: '提交审核', desc: '管理员会在 1 个工作日内处理' },
  { title: '成功展示', desc: '通过后自动展示到活动页' }
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
  fee: 'Free',
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

const requiredKeys: (keyof typeof form.value)[] = ['title', 'category', 'date', 'timeStart', 'timeEnd', 'location', 'description'];

const publishProgress = computed(() => {
  const filled = requiredKeys.filter((key) => String(form.value[key]).trim().length > 0).length;
  return Math.round((filled / requiredKeys.length) * 100);
});

const progressTip = computed(() => {
  if (publishProgress.value < 40) return '建议补全时间和地点，提升展示效果';
  if (publishProgress.value < 80) return '再补充标签、介绍和封面';
  return '准备提交发布';
});

const preview = computed(() => ({
  title: form.value.title || '邻里活动标题',
  date: form.value.date ? new Date(form.value.date).toLocaleDateString('zh-CN') : '日期待定',
  timeRange: form.value.timeStart && form.value.timeEnd ? `${form.value.timeStart} - ${form.value.timeEnd}` : '时间待定',
  location: form.value.location || '地点待定',
  tags: form.value.tags.length ? form.value.tags : ['社区活动', '欢迎加入'],
  description: form.value.description || '这里将展示活动亮点、流程和注意事项'
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
    ? form.value.tags.filter((t) => t !== tag)
    : [...form.value.tags, tag];
};

const locateByAddress = async () => {
  if (!AMAP_KEY) {
    message.value = '未配置 VITE_AMAP_KEY';
    messageType.value = 'error';
    return;
  }
  const addr = form.value.location?.trim();
  if (!addr) {
    message.value = '请先填写地址';
    messageType.value = 'error';
    return;
  }
  locating.value = true;
  try {
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&address=${encodeURIComponent(addr)}`);
    const data = await resp.json();
    if (data.status !== '1' || !data.geocodes?.length || !data.geocodes[0]?.location) {
      throw new Error('地址解析失败');
    }
    const parts = String(data.geocodes[0].location).split(',').map(parseFloat);
    if (parts.length !== 2 || parts.some((n) => Number.isNaN(n))) {
      throw new Error('无效坐标');
    }
    lng.value = parts[0];
    lat.value = parts[1];
    message.value = '已解析地址坐标';
    messageType.value = 'success';
  } catch (e: any) {
    message.value = e?.message || '地址解析失败';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const locateCurrent = async () => {
  if (!AMAP_KEY) {
    message.value = '未配置 VITE_AMAP_KEY';
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
        const p = String(convertData.locations).split(',');
        if (p.length === 2) {
          const clng = parseFloat(p[0]);
          const clat = parseFloat(p[1]);
          if (!Number.isNaN(clng) && !Number.isNaN(clat)) {
            useLng = clng;
            useLat = clat;
          }
        }
      }
    } catch (_) {}
    try {
      const resp = await fetch(`${REVERSE_GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`);
      const data = await resp.json();
      if (resp.ok && data.status === '1' && data.regeocode?.formatted_address) {
        form.value.location = data.regeocode.formatted_address;
      }
    } catch (_) {}
    lat.value = useLat;
    lng.value = useLng;
    message.value = '已根据当前位置填充地址与坐标';
    messageType.value = 'success';
  } catch (e: any) {
    const code = Number(e?.code);
    if (code === 1) message.value = '定位权限被拒绝';
    else if (code === 2) message.value = '位置不可用';
    else if (code === 3) message.value = '定位超时';
    else message.value = e?.message || '定位失败';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const handleSubmit = async (status: PublishStatus) => {
  if (!username.value) {
    message.value = '请先登录再发布活动';
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

    message.value = status === 'DRAFT' ? '草稿已保存' : '提交成功，等待审核';
    messageType.value = 'success';
  } catch (err) {
    message.value = err instanceof Error ? err.message : '提交失败，请稍后再试';
    messageType.value = 'error';
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
:global(body) {
  background: #f4f6f8;
}

.lap-page {
  padding-top: 80px;
  padding-bottom: 48px;
  color: #1f2a37;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.lap-hero {
  margin: 48px;
  padding: 32px;
  border-radius: 32px;
  background: linear-gradient(120deg, #092515, #1aa053);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 32px;
  align-items: flex-start;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.3em;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.lap-hero h1 {
  font-size: 36px;
  margin: 12px 0;
}

.subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.hero-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.hero-progress {
  flex: 0 0 280px;
  background: rgba(0, 0, 0, 0.3);
  padding: 20px;
  border-radius: 24px;
}

.hero-progress span {
  font-size: 14px;
  letter-spacing: 0.2em;
}

.progress-tip {
  margin-top: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.progress-bar {
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  height: 10px;
  margin: 12px 0;
  overflow: hidden;
}

.progress {
  height: 100%;
  background: #4ade80;
  border-radius: 12px;
}

.lap-layout {
  margin: 32px 48px 0;
  display: grid;
  grid-template-columns: minmax(0, 2fr) 360px;
  gap: 24px;
  align-items: start;
}

.form-panel,
.preview-panel {
  background: #fff;
  border-radius: 28px;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
  padding: 28px;
}

.form-panel h2 {
  margin-bottom: 18px;
  font-size: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.address-field {
  grid-column: 1 / -1;
  min-width: 320px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #475467;
}

.field input,
.field select,
.field textarea {
  border: 1px solid #dfe3eb;
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: #1aa053;
  outline: none;
}

.inline {
  min-width: 160px;
}

.tag-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  border-radius: 999px;
  border: 1px solid #dfe3eb;
  background: #f7fafc;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
}

.tag-chip.active {
  background: rgba(26, 160, 83, 0.15);
  color: #1aa053;
  border-color: rgba(26, 160, 83, 0.3);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.primary,
.ghost {
  border-radius: 999px;
  padding: 12px 26px;
  font-weight: 600;
  cursor: pointer;
}

.primary {
  border: none;
  background: linear-gradient(120deg, #1aa053, #0a6b3b);
  color: #fff;
}

.ghost {
  border: 1px solid #cfd5e2;
  background: transparent;
  color: #4a5568;
}

.submit-msg {
  margin-top: 12px;
  font-size: 14px;
}

.submit-msg.success {
  color: #0a8754;
}

.submit-msg.error {
  color: #d93025;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-card,
.steps-card,
.tips-card {
  border: 1px solid #edf0f5;
  border-radius: 22px;
  padding: 18px;
}

.preview-meta {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.5;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.preview-tags span {
  background: rgba(26, 160, 83, 0.12);
  color: #1aa053;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.preview-desc {
  font-size: 14px;
  color: #475467;
  line-height: 1.5;
}

.preview-seats {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475467;
  margin-top: 12px;
}

.steps-card ul,
.tips-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.steps-card li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.steps-card .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #d4dbe7;
  margin-top: 6px;
}

.steps-card .dot.done {
  background: #1aa053;
  border-color: #1aa053;
}

.tips-card li {
  font-size: 14px;
  color: #4a5568;
}

.address-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-width: 260px;
}

.address-row input {
  width: 100%;
}

.locate-btn {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid #cfd5e2;
  background: #fff;
  color: #1aa053;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.locate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.locate-btn:hover:not(:disabled) {
  background: rgba(26, 160, 83, 0.08);
  border-color: rgba(26, 160, 83, 0.4);
}

.locate-btn svg {
  width: 20px;
  height: 20px;
}

@media (max-width: 1100px) {
  .lap-hero,
  .lap-layout {
    margin: 32px;
  }
  .lap-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .lap-hero {
    flex-direction: column;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .hero-progress {
    width: 100%;
  }
}
</style>
