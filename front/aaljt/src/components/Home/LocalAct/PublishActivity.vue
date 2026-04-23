<template>
  <dhstyle />
  <div class="lap-page">
    <section class="lap-hero card">
      <div class="hero-main">
        <p class="eyebrow">?????</p>
        <h1>??????</h1>
        <p class="subtitle">
          ?????????????????????????????????????????
        </p>
      </div>
      <div class="hero-side">
        <div class="progress-head">
          <span>?????</span>
          <strong>{{ publishProgress }}%</strong>
        </div>
        <div class="progress-track" role="progressbar" :aria-valuenow="publishProgress" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-fill" :style="{ width: `${publishProgress}%` }"></div>
        </div>
        <p class="progress-tip">{{ progressTip }}</p>
        <div class="hero-actions">
          <button class="btn btn-light" :disabled="saving" @click="handleSubmit('DRAFT')">????</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">
            {{ saving ? '???...' : '????' }}
          </button>
        </div>
      </div>
    </section>

    <div class="lap-layout">
      <section class="panel form-panel">
        <h2>????</h2>
        <div class="form-grid">
          <label class="field">
            <span>????</span>
            <input v-model="form.title" type="text" placeholder="??????? ? ????" />
          </label>
          <label class="field">
            <span>???</span>
            <input v-model="form.subtitle" type="text" placeholder="?????????" />
          </label>
          <label class="field">
            <span>????</span>
            <select v-model="form.category">
              <option disabled value="">?????</option>
              <option v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>????</span>
            <input v-model="form.date" type="date" />
          </label>
          <label class="field">
            <span>????</span>
            <input v-model="form.timeStart" type="time" />
          </label>
          <label class="field">
            <span>????</span>
            <input v-model="form.timeEnd" type="time" />
          </label>
          <label class="field address-field">
            <span>????</span>
            <div class="address-controls">
              <input v-model="form.location" type="text" placeholder="???????????" />
              <button class="btn btn-light sm" type="button" :disabled="locating" @click="locateByAddress">
                {{ locating ? '???...' : '????' }}
              </button>
              <button class="btn btn-light sm" type="button" :disabled="locating" @click="locateCurrent">
                {{ locating ? '???...' : '????' }}
              </button>
            </div>
          </label>
          <label class="field">
            <span>????</span>
            <input v-model.number="form.capacity" type="number" min="1" />
          </label>
          <label class="field">
            <span>????</span>
            <input v-model="form.fee" type="text" placeholder="?? / AA / ??" />
          </label>
        </div>

        <label class="field block-field">
          <span>????</span>
          <textarea
            v-model="form.description"
            rows="5"
            placeholder="????????????????"
          ></textarea>
        </label>

        <div class="field block-field">
          <span>??????</span>
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

        <h2>????</h2>
        <div class="form-grid">
          <label class="field">
            <span>????</span>
            <select v-model="form.registration">
              <option value="auto">???? / ??</option>
              <option value="manual">????</option>
              <option value="external">????</option>
            </select>
          </label>
          <label class="field">
            <span>????</span>
            <select v-model="form.reminder">
              <option value="24h">??? 24 ??</option>
              <option value="3h">??? 3 ??</option>
              <option value="custom">???</option>
            </select>
          </label>
          <label class="field">
            <span>????</span>
            <select v-model="form.checkin">
              <option value="yes">??????</option>
              <option value="no">????</option>
            </select>
          </label>
          <label class="field">
            <span>????</span>
            <select v-model="form.waiting">
              <option value="yes">????</option>
              <option value="no">???</option>
            </select>
          </label>
        </div>

        <div class="form-actions">
          <button class="btn btn-light" :disabled="saving" @click="handleSubmit('DRAFT')">????</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSubmit('PUBLISHED')">????</button>
        </div>
        <p v-if="message" :class="['submit-msg', messageType]">{{ message }}</p>
      </section>

      <aside class="preview-panel">
        <div class="panel preview-card">
          <div class="preview-head">
            <p class="eyebrow">????</p>
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
            <span>?????{{ form.capacity || 0 }}</span>
            <span>???{{ form.checkin === 'yes' ? '??' : '??' }}</span>
          </div>
        </div>

        <div class="panel steps-card">
          <h4>????</h4>
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
          <h4>????</h4>
          <ul>
            <li>???????????????????????</li>
            <li>???? 1200?600 ??????????</li>
            <li>??/??????????????????</li>
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
  { value: 'sport', label: '????' },
  { value: 'eco', label: '????' },
  { value: 'skill', label: '????' },
  { value: 'kids', label: '????' },
  { value: 'market', label: '??/??' }
];

const tagSuggestions = ['????', '????', '??', '????', '??', '??', '????'];

const steps = [
  { title: '????', desc: '?????????????' },
  { title: '????', desc: '????? 1 ???????' },
  { title: '????', desc: '???????????' }
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
  fee: '??',
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
  if (publishProgress.value < 40) return '??????????';
  if (publishProgress.value < 80) return '?????????????';
  return '???????????';
});

const preview = computed(() => ({
  title: form.value.title || '??????',
  date: form.value.date ? new Date(form.value.date).toLocaleDateString('zh-CN') : '????',
  timeRange: form.value.timeStart && form.value.timeEnd ? `${form.value.timeStart} - ${form.value.timeEnd}` : '????',
  location: form.value.location || '????',
  tags: form.value.tags.length ? form.value.tags : ['????', '????'],
  description: form.value.description || '?????????????????'
}));

const registrationLabel = computed(() => {
  switch (form.value.registration) {
    case 'manual':
      return '????';
    case 'external':
      return '????';
    default:
      return '????';
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
    message.value = '??? VITE_AMAP_KEY';
    messageType.value = 'error';
    return;
  }
  const address = form.value.location.trim();
  if (!address) {
    message.value = '??????';
    messageType.value = 'error';
    return;
  }

  locating.value = true;
  try {
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&address=${encodeURIComponent(address)}`);
    const data = await resp.json();
    if (data.status !== '1' || !data.geocodes?.length || !data.geocodes[0]?.location) {
      throw new Error('??????');
    }
    const parts = String(data.geocodes[0].location).split(',').map(parseFloat);
    if (parts.length !== 2 || parts.some((value) => Number.isNaN(value))) {
      throw new Error('????');
    }
    lng.value = parts[0];
    lat.value = parts[1];
    message.value = '??????';
    messageType.value = 'success';
  } catch (error: any) {
    message.value = error?.message || '??????';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const locateCurrent = async () => {
  if (!AMAP_KEY) {
    message.value = '??? VITE_AMAP_KEY';
    messageType.value = 'error';
    return;
  }
  if (!('geolocation' in navigator)) {
    message.value = '??????????';
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
    message.value = '??????????????';
    messageType.value = 'success';
  } catch (error: any) {
    const code = Number(error?.code);
    if (code === 1) message.value = '???????';
    else if (code === 2) message.value = '?????';
    else if (code === 3) message.value = '????';
    else message.value = error?.message || '????';
    messageType.value = 'error';
  } finally {
    locating.value = false;
  }
};

const handleSubmit = async (status: PublishStatus) => {
  if (!username.value) {
    message.value = '?????????';
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

    message.value = status === 'DRAFT' ? '?????' : '?????????';
    messageType.value = 'success';
  } catch (error) {
    message.value = error instanceof Error ? error.message : '??????????';
    messageType.value = 'error';
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
:global(body) {
  background: #f5f6f8;
}

.lap-page {
  padding: 76px 40px 36px;
  color: #1f2937;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.card,
.panel {
  background: #ffffff;
  border: 1px solid #e4e9f1;
  border-radius: 12px;
}

.lap-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
  padding: 20px 22px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #6b7280;
  margin: 0;
}

.lap-hero h1 {
  margin: 10px 0 8px;
  font-size: 30px;
  line-height: 1.25;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  max-width: 660px;
  line-height: 1.6;
  font-size: 14px;
  color: #4b5563;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  background: #fafbfd;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  color: #4b5563;
}

.progress-head strong {
  font-size: 20px;
  color: #2f6ea5;
}

.progress-track {
  height: 8px;
  width: 100%;
  border-radius: 999px;
  background: #e8edf5;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #8cb4db;
}

.progress-tip {
  margin: 0;
  font-size: 12px;
  color: #667085;
}

.hero-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
}

.lap-layout {
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
  align-items: start;
}

.form-panel {
  padding: 16px;
}

.form-panel h2 {
  margin: 2px 0 12px;
  font-size: 16px;
  color: #1f2937;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  margin-bottom: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field span {
  font-size: 13px;
  color: #4b5563;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  border: 1px solid #d9e0ea;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.4;
  color: #1f2937;
  background: #ffffff;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  outline: none;
  border-color: #8ab1d8;
  box-shadow: 0 0 0 2px rgba(138, 177, 216, 0.18);
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
  border: 1px solid #dbe2ec;
  background: #ffffff;
  color: #4b5563;
  border-radius: 8px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
}

.tag-chip.active {
  border-color: #8cb4db;
  background: #eff5fc;
  color: #2f6ea5;
}

.btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #d1d9e6;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  cursor: pointer;
}

.btn:hover:not(:disabled) {
  background: #f5f7fb;
  border-color: #c3ccdb;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-primary {
  background: #8cb4db;
  border-color: #8cb4db;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background: #7ea7cf;
  border-color: #7ea7cf;
}

.btn.sm {
  height: 32px;
  padding: 0 12px;
}

.form-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.submit-msg {
  margin: 10px 0 0;
  font-size: 13px;
}

.submit-msg.success {
  color: #23855c;
}

.submit-msg.error {
  color: #d14343;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-card,
.steps-card,
.tips-card {
  padding: 14px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.state-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #2f6ea5;
  border: 1px solid #cfe0f2;
  background: #f0f6fc;
}

.preview-card h3 {
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}

.preview-meta {
  margin: 8px 0;
  line-height: 1.5;
  font-size: 13px;
  color: #4b5563;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-tags span {
  border: 1px solid #d4dde9;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  color: #475467;
  background: #f8fafc;
}

.preview-desc {
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
  color: #4b5563;
}

.preview-seats {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: #667085;
}

.steps-card h4,
.tips-card h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.steps-card ul,
.tips-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 9px;
}

.steps-card li {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid #c5d0de;
  margin-top: 4px;
}

.dot.done {
  border-color: #79a7d4;
  background: #79a7d4;
}

.steps-card strong {
  display: block;
  font-size: 13px;
  color: #344054;
}

.steps-card p,
.tips-card li {
  margin: 2px 0 0;
  font-size: 12px;
  color: #667085;
  line-height: 1.5;
}

@media (max-width: 1200px) {
  .lap-page {
    padding: 72px 22px 30px;
  }

  .lap-hero,
  .lap-layout {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .address-controls {
    grid-template-columns: 1fr;
  }
}
</style>
