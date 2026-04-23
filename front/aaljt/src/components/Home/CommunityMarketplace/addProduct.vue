<template>
  <div class="publish-page">
    <dhstyle />

    <div class="publish-shell">
      <CebianTool />

      <main class="publish-main">
        <section class="surface-card publish-hero">
          <div class="publish-hero-copy">
            <p class="section-kicker">发布商品</p>
            <h1>把商品信息整理清楚，再发布出去</h1>
            <p class="section-desc">
              按基础信息、交易信息、位置信息和图片四个区块依次填写，让买家更快判断商品是否合适。
            </p>

            <div class="hero-stat-row">
              <article v-for="item in heroStats" :key="item.label" class="hero-stat-card">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.helper }}</small>
              </article>
            </div>
          </div>

          <div class="hero-action-group">
            <button class="ghost-btn" type="button" @click="router.push({ name: 'CommunityMarketplace' })">
              返回市场
            </button>
            <button class="primary-btn" type="button" @click="openAiDrawer">
              AI 辅助整理
            </button>
          </div>
        </section>

        <div class="publish-grid">
          <form class="publish-form" @submit.prevent="onSubmit">
            <section class="surface-card form-card">
              <header class="card-head">
                <div class="card-head-main">
                  <span class="card-index">01</span>
                  <div>
                    <h2>基础信息</h2>
                    <p>先把商品是什么、适合谁、当前成色说明清楚。</p>
                  </div>
                </div>
              </header>

              <div class="field-grid">
                <label class="field-block">
                  <span>商品标题</span>
                  <input
                    v-model.trim="form.title"
                    class="input"
                    type="text"
                    placeholder="例如：95 新小米平板 6，带原装充电器"
                  />
                </label>

                <label class="field-block full-row">
                  <span>商品描述</span>
                  <textarea
                    v-model.trim="form.description"
                    class="textarea"
                    placeholder="建议写清购买时间、使用情况、转让原因和可交易方式"
                  ></textarea>
                </label>
              </div>
            </section>

            <section class="surface-card form-card">
              <header class="card-head">
                <div class="card-head-main">
                  <span class="card-index">02</span>
                  <div>
                    <h2>交易信息</h2>
                    <p>价格、库存、成色和分类会直接影响买家的第一判断。</p>
                  </div>
                </div>
              </header>

              <div class="field-grid two-column">
                <label class="field-block">
                  <span>价格（¥）</span>
                  <input
                    v-model.number="form.price"
                    class="input"
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                  />
                </label>

                <label class="field-block">
                  <span>库存数量</span>
                  <input
                    v-model.number="form.stockQuantity"
                    class="input"
                    type="number"
                    min="0"
                    placeholder="0"
                  />
                </label>

                <label class="field-block">
                  <span>成色</span>
                  <select v-model="form.condition" class="input">
                    <option value="">请选择</option>
                    <option value="全新">全新</option>
                    <option value="九成新">九成新</option>
                    <option value="七成新">七成新</option>
                    <option value="二手">二手</option>
                  </select>
                </label>

                <label class="field-block">
                  <span>所属分类</span>
                  <select v-model="form.categoryId" class="input">
                    <option value="">请选择分类</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">
                      {{ category.name }}
                    </option>
                  </select>
                </label>
              </div>
            </section>

            <section class="surface-card form-card">
              <header class="card-head">
                <div class="card-head-main">
                  <span class="card-index">03</span>
                  <div>
                    <h2>位置信息</h2>
                    <p>支持手动填写，也可以直接用当前位置自动回填。</p>
                  </div>
                </div>
              </header>

              <div class="address-input-row">
                <input
                  v-model.trim="form.location"
                  class="input"
                  type="text"
                  placeholder="城市 / 区域 / 街道"
                />
                <button
                  class="ghost-btn compact"
                  type="button"
                  :disabled="locating"
                  @click="locateAndFillAddress"
                >
                  {{ locating ? '定位中...' : '定位填充' }}
                </button>
              </div>
              <p class="field-note">
                地址会用于附近推荐和同城展示，建议填写到社区、街道或商圈级别即可。
              </p>
            </section>

            <section class="surface-card form-card">
              <header class="card-head">
                <div class="card-head-main">
                  <span class="card-index">04</span>
                  <div>
                    <h2>商品图片</h2>
                    <p>至少上传 1 张图片，建议主图清晰展示成色与关键细节。</p>
                  </div>
                </div>
              </header>

              <div class="upload-toolbar">
                <div>
                  <strong>已上传 {{ uploads.length }} 张图片</strong>
                  <p>上传顺序会作为展示顺序的一部分，建议先放封面图。</p>
                </div>
                <button class="ghost-btn" type="button" :disabled="uploading" @click="triggerUpload">
                  {{ uploading ? '上传中...' : '上传图片' }}
                </button>
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  multiple
                  class="hidden-input"
                  @change="handleFileChange"
                />
              </div>

              <div v-if="uploads.length" class="upload-list">
                <article v-for="(img, idx) in uploads" :key="img.key || idx" class="upload-item">
                  <img :src="img.url" alt="预览图" @error="onUploadImgError(idx)" />
                  <button class="ghost-btn compact danger" type="button" @click="removeUpload(idx)">删除</button>
                </article>
              </div>

              <div v-else class="upload-empty">
                <strong>还没有上传图片</strong>
                <p>建议至少上传一张封面图和一张细节图，买家会更愿意继续沟通。</p>
              </div>
            </section>

            <section class="surface-card submit-card">
              <div class="submit-summary">
                <div>
                  <strong>确认无误后再发布</strong>
                  <p>买家最先看到的是标题、价格、地点和封面图，信息越清楚，成交效率越高。</p>
                </div>

                <div class="submit-actions">
                  <button
                    class="ghost-btn"
                    type="button"
                    @click="router.push({ name: 'CommunityMarketplace' })"
                  >
                    返回市场
                  </button>
                  <button class="primary-btn" type="submit" :disabled="submitting">
                    {{ submitting ? '提交中...' : '发布商品' }}
                  </button>
                </div>
              </div>
            </section>
          </form>

          <aside class="publish-side">
            <section class="surface-card progress-card">
              <header class="side-card-head">
                <div>
                  <p class="section-kicker">发布进度</p>
                  <h2>检查当前草稿</h2>
                </div>
                <span class="side-card-badge">{{ uploads.length }} 张图</span>
              </header>

              <div class="progress-list">
                <article
                  v-for="item in publishChecklist"
                  :key="item.label"
                  :class="['progress-item', { done: item.done }]"
                >
                  <div>
                    <strong>{{ item.label }}</strong>
                    <p>{{ item.helper }}</p>
                  </div>
                  <span>{{ item.done ? '已完成' : '待完善' }}</span>
                </article>
              </div>
            </section>

            <section class="surface-card ai-helper-card">
              <header class="side-card-head">
                <div>
                  <p class="section-kicker">AI 助手</p>
                  <h2>辅助整理文案</h2>
                </div>
                <button class="ghost-btn compact" type="button" @click="openAiDrawer">打开对话框</button>
              </header>

              <p class="ai-helper-desc">
                AI 只做辅助，帮你优化标题、润色描述和整理卖点，不会自动修改表单内容。
              </p>

              <div class="ai-quick-list">
                <button
                  v-for="prompt in publishAi.quickPrompts"
                  :key="prompt"
                  class="ai-quick-btn"
                  type="button"
                  @click="askAi(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>

              <div class="ai-preview-box">
                <strong>最近会话</strong>
                <p>
                  {{
                    publishAi.latestUserPreview ||
                    '还没有发起过询问，可以先让 AI 帮你优化标题或润色描述。'
                  }}
                </p>
                <small>
                  {{
                    publishAi.latestAgentReplyPreview ||
                    '最近的 AI 回复会显示在这里，方便你继续追问和补充细节。'
                  }}
                </small>
              </div>
            </section>
          </aside>
        </div>
      </main>

      <CommunityMarketplaceAiDrawer
        v-model="publishAi.drawerOpen.value"
        kicker="AI 对话框"
        title="发布助手"
        headline="帮你把商品说清楚"
        helper-text="可以结合当前草稿整理标题、描述、卖点或议价回复，继续追问会保留上下文。"
        empty-text="你好，我是你的发布助手。你可以让我帮你优化标题、润色描述、整理卖点，或者写一段回复买家的话术。"
        placeholder="例如：帮我把这件商品写得更清楚一些"
        :quick-prompts="publishAi.quickPrompts"
        :messages="publishAi.messages.value"
        :input-value="publishAi.input.value"
        :loading="publishAi.loading.value"
        :error="publishAi.error.value"
        @update:input-value="publishAi.input.value = $event"
        @prompt="askAi"
        @send="publishAi.submitMessage"
        @card-click="handleAiCardClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dhstyle from '../../dhstyle/dhstyle.vue'
import CebianTool from './cebianTool.vue'
import CommunityMarketplaceAiDrawer from './CommunityMarketplaceAiDrawer.vue'
import { useCommunityMarketplaceAi, type MarketplaceAiCard } from './useCommunityMarketplaceAi'
import { env as ViteEnv } from '@/env'

const router = useRouter()

const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080'
const API_BASE_URL = `${API_BASE}/api`

const form = ref({
  title: '',
  description: '',
  price: 0,
  stockQuantity: 0,
  condition: '',
  location: '',
  categoryId: ''
})

const locating = ref(false)
const AMAP_KEY =
  ViteEnv?.VITE_AMAP_KEY ??
  (import.meta as any)?.env?.VITE_AMAP_KEY ??
  (window as any)?.VITE_AMAP_KEY ??
  ''
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo'
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert'
const GEOCODER_FORWARD = 'https://restapi.amap.com/v3/geocode/geo'
const lat = ref<number | null>(null)
const lng = ref<number | null>(null)

const MINIO_BASE = (import.meta.env as any)?.VITE_MINIO_BASE || ''
const uploads = ref<Array<{ key: string; url: string }>>([])
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const submitting = ref(false)

const categories = ref<Array<{ id: string | number; name: string }>>([])

const selectedCategoryName = computed(() => {
  return categories.value.find((item) => String(item.id) === String(form.value.categoryId))?.name || '未选择'
})

const heroStats = computed(() => [
  {
    label: '当前分类',
    value: selectedCategoryName.value,
    helper: '分类越准确，买家越容易搜到'
  },
  {
    label: '已上传图片',
    value: uploads.value.length ? `${uploads.value.length} 张` : '未上传',
    helper: '建议封面图优先展示商品全貌'
  },
  {
    label: '当前位置',
    value: form.value.location || '待填写',
    helper: '用于附近推荐和同城展示'
  }
])

const publishChecklist = computed(() => [
  {
    label: '基础信息',
    helper: form.value.title && form.value.description ? '标题和描述已经准备好' : '补充标题与描述，让买家快速理解商品',
    done: !!form.value.title && !!form.value.description
  },
  {
    label: '交易信息',
    helper:
      form.value.price > 0 && form.value.condition && form.value.categoryId
        ? '价格、成色和分类已设置'
        : '填写价格、成色、库存和分类',
    done: !!form.value.price && form.value.price > 0 && !!form.value.condition && !!form.value.categoryId
  },
  {
    label: '位置信息',
    helper: form.value.location ? '地点已填写，可用于同城展示' : '建议填写社区、街道或商圈',
    done: !!form.value.location
  },
  {
    label: '商品图片',
    helper: uploads.value.length ? `已上传 ${uploads.value.length} 张图片` : '至少上传 1 张清晰图片',
    done: uploads.value.length > 0
  }
])

const publishAi = useCommunityMarketplaceAi({
  apiBase: API_BASE,
  initialAgentMessage: '你好，我可以帮你优化标题、润色描述、整理卖点和议价回复。',
  quickPrompts: [
    '帮我优化当前商品标题',
    '帮我润色当前商品描述',
    '帮我整理 3 个最值得强调的卖点',
    '帮我写一段礼貌但坚定的议价回复'
  ],
  sessionStorageKey: 'community-marketplace-publish-ai',
  buildUserProfile: () => ({
    scene: 'publish-product',
    title: form.value.title,
    description: form.value.description,
    price: form.value.price,
    stockQuantity: form.value.stockQuantity,
    condition: form.value.condition,
    categoryId: form.value.categoryId,
    categoryName: selectedCategoryName.value,
    location: form.value.location,
    imageCount: uploads.value.length
  })
})

function validate() {
  if (!form.value.title) {
    ElMessage.error('请输入商品标题')
    return false
  }
  if (!form.value.description) {
    ElMessage.error('请输入商品描述')
    return false
  }
  if (!form.value.price || form.value.price <= 0) {
    ElMessage.error('请输入有效价格')
    return false
  }
  if (form.value.stockQuantity < 0) {
    ElMessage.error('库存数量不能为负')
    return false
  }
  if (!form.value.condition) {
    ElMessage.error('请选择成色')
    return false
  }
  if (!form.value.location) {
    ElMessage.error('请输入地点')
    return false
  }
  if (!form.value.categoryId) {
    ElMessage.error('请选择分类')
    return false
  }
  if (uploads.value.length === 0) {
    ElMessage.error('请至少上传一张商品图片')
    return false
  }
  return true
}

async function loadCategories() {
  try {
    const res = await fetch(`${API_BASE_URL}/categories/getAllCategories`)
    if (res.ok) {
      const data = await res.json()
      const list = Array.isArray(data) ? data : data.items || data.list || data.data || data.records || data.rows || []
      categories.value = list.map((item: any) => ({
        id: item.id ?? item.categoryId ?? item.Id,
        name: item.name ?? item.categoryName ?? `分类${String(item.categoryId ?? item.id ?? '')}`
      }))
    }
  } catch {}
}

onMounted(loadCategories)

const username = computed(() => localStorage.getItem('username') || '')
const token = computed(() => localStorage.getItem('token') || '')

function buildPublicUrl(key: string) {
  if (!key) return ''
  if (/^https?:\/\//i.test(key)) return key
  const base = MINIO_BASE ? MINIO_BASE.replace(/\/+$/, '') : ''
  const cleanKey = key.replace(/^\/+/, '')
  return base ? `${base}/${cleanKey}` : cleanKey
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || uploading.value) return
  const files = Array.from(input.files)
  uploading.value = true
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    try {
      const res = await fetch(`${API_BASE_URL}/products/upload`, {
        method: 'POST',
        body: formData
      })
      if (!res.ok) {
        const msg = await res.text().catch(() => '')
        ElMessage.error(msg || '图片上传失败')
        continue
      }
      const data = await res.json().catch(() => ({} as any))
      const key = data?.key
      if (!key) {
        ElMessage.error('图片上传失败')
        continue
      }
      const publicUrl = buildPublicUrl(key)
      const preview =
        data?.url || data?.publicUrl || (publicUrl && /^https?:\/\//i.test(publicUrl) ? publicUrl : URL.createObjectURL(file))
      uploads.value.push({ key, url: preview || '' })
    } catch {
      ElMessage.error('图片上传出错')
    }
  }
  input.value = ''
  uploading.value = false
}

function removeUpload(idx: number) {
  uploads.value.splice(idx, 1)
}

function onUploadImgError(idx: number) {
  const item = uploads.value[idx]
  if (!item) return
  const fallback = buildPublicUrl(item.key)
  if (fallback && /^https?:\/\//i.test(fallback)) {
    item.url = fallback
  }
}

async function onSubmit() {
  if (!validate()) return
  if (uploading.value) {
    ElMessage.warning('图片上传中，请稍候')
    return
  }
  if (!username.value || !token.value) {
    ElMessage.warning('请先登录')
    router.push('/')
    return
  }
  if ((!lat.value || !lng.value) && AMAP_KEY && form.value.location) {
    try {
      const res = await fetch(`${GEOCODER_FORWARD}?key=${AMAP_KEY}&address=${encodeURIComponent(form.value.location)}`)
      const data = await res.json()
      const geocode = Array.isArray(data?.geocodes) ? data.geocodes[0] : null
      const location = geocode?.location ? String(geocode.location).split(',') : null
      const geocodeLng = location && location[0] ? parseFloat(location[0]) : null
      const geocodeLat = location && location[1] ? parseFloat(location[1]) : null
      if (geocodeLat != null && geocodeLng != null && !Number.isNaN(geocodeLat) && !Number.isNaN(geocodeLng)) {
        lat.value = geocodeLat
        lng.value = geocodeLng
      }
    } catch {}
  }
  submitting.value = true
  try {
    const payload = {
      title: form.value.title,
      description: form.value.description,
      price: Number(form.value.price),
      stockQuantity: Number(form.value.stockQuantity),
      condition: form.value.condition,
      location: form.value.location,
      categoryId: form.value.categoryId,
      imageUrls: JSON.stringify(uploads.value.map((item) => item.key)),
      latitude: lat.value != null ? Number(lat.value) : undefined,
      longitude: lng.value != null ? Number(lng.value) : undefined
    }
    const res = await fetch(`${API_BASE_URL}/products/addProduct`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token.value}`
      },
      body: JSON.stringify(payload)
    })
    if (!res.ok) {
      const text = await res.text().catch(() => '')
      ElMessage.error(text || '发布失败')
      return
    }
    const data = await res.json().catch(() => ({} as any))
    ElMessage.success('发布成功')
    const productId = data?.id ?? data?.productId
    if (productId) {
      router.push({ name: 'ProductDetail', params: { id: productId } })
    } else {
      router.push({ name: 'CommunityMarketplace' })
    }
  } catch {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function locateAndFillAddress() {
  if (!AMAP_KEY) {
    ElMessage.error('未配置地图密钥：请设置 VITE_AMAP_KEY')
    return
  }
  if (!('geolocation' in navigator)) {
    ElMessage.error('当前浏览器不支持定位')
    return
  }
  locating.value = true
  try {
    const coords = await new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position) => resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
        (error) => reject(error),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      )
    })
    let useLat = coords.latitude
    let useLng = coords.longitude
    try {
      const convertResp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${useLng},${useLat}&coordsys=gps`)
      const convertData = await convertResp.json()
      if (convertData.status === '1' && convertData.locations) {
        const parts = String(convertData.locations).split(',')
        if (parts.length === 2) {
          const clng = parseFloat(parts[0])
          const clat = parseFloat(parts[1])
          if (!Number.isNaN(clng) && !Number.isNaN(clat)) {
            useLng = clng
            useLat = clat
          }
        }
      }
    } catch {}
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`)
    if (!resp.ok) {
      const text = await resp.text().catch(() => '')
      throw new Error(text || '定位服务错误')
    }
    const data = await resp.json()
    if (data.status !== '1' || !data.regeocode || !data.regeocode.formatted_address) {
      throw new Error('未能解析到地址')
    }
    form.value.location = String(data.regeocode.formatted_address)
    lat.value = useLat
    lng.value = useLng
    ElMessage.success('已根据定位填充地址')
  } catch (error: any) {
    const code = Number(error?.code)
    if (code === 1) ElMessage.error('定位权限被拒绝')
    else if (code === 2) ElMessage.error('位置不可用')
    else if (code === 3) ElMessage.error('定位超时')
    else ElMessage.error(error?.message || '定位失败')
  } finally {
    locating.value = false
  }
}

function buildDraftContext() {
  const lines = [
    form.value.title ? `标题：${form.value.title}` : '',
    form.value.description ? `描述：${form.value.description}` : '',
    form.value.price > 0 ? `价格：${form.value.price} 元` : '',
    form.value.stockQuantity >= 0 ? `库存：${form.value.stockQuantity}` : '',
    form.value.condition ? `成色：${form.value.condition}` : '',
    form.value.categoryId ? `分类：${selectedCategoryName.value}` : '',
    form.value.location ? `地点：${form.value.location}` : '',
    uploads.value.length ? `图片：${uploads.value.length} 张` : ''
  ].filter(Boolean)
  return lines.join('\n')
}

function openAiDrawer() {
  publishAi.openDrawer()
}

async function askAi(prompt: string) {
  publishAi.openDrawer()
  const draft = buildDraftContext()
  publishAi.prefillInput(draft ? `${prompt}\n\n当前草稿：\n${draft}` : prompt)
  await publishAi.submitMessage()
}

function handleAiCardClick(card: MarketplaceAiCard) {
  if (card.entityType === 'product' && card.entityId) {
    router.push({ name: 'ProductDetail', params: { id: card.entityId } })
  }
}
</script>

<style scoped>
.publish-page {
  --green: #24b55d;
  --green-deep: #1f8f4b;
  --green-soft: #eef8f1;
  --line: #e4ece1;
  --line-strong: #d7e2d4;
  --text-main: #233224;
  --text-sub: #6d7d6d;
  min-height: 100vh;
  background: #f5f7f2;
}

.publish-shell {
  position: relative;
}

.publish-main {
  width: min(1360px, calc(100vw - 116px));
  margin: 0 auto;
  padding: 82px 28px 36px;
  display: grid;
  gap: 18px;
}

.surface-card {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 12px 26px rgba(67, 98, 67, 0.06);
}

.publish-hero {
  padding: 24px 26px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.publish-hero-copy h1 {
  margin: 10px 0 0;
  font-size: 36px;
  line-height: 1.16;
  color: var(--text-main);
}

.section-kicker {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--green-deep);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-desc {
  margin: 14px 0 0;
  max-width: 780px;
  color: var(--text-sub);
  font-size: 15px;
  line-height: 1.8;
}

.hero-stat-row {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat-card {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #edf2ea;
  background: #fbfcfa;
}

.hero-stat-card span,
.hero-stat-card strong,
.hero-stat-card small {
  display: block;
}

.hero-stat-card span {
  font-size: 13px;
  color: #6b7a6c;
}

.hero-stat-card strong {
  margin-top: 10px;
  font-size: 22px;
  color: var(--text-main);
  word-break: break-word;
}

.hero-stat-card small {
  margin-top: 8px;
  color: #92a091;
  line-height: 1.6;
}

.hero-action-group {
  min-width: 160px;
  display: grid;
  gap: 10px;
}

.publish-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 18px;
  align-items: start;
}

.publish-form,
.publish-side {
  display: grid;
  gap: 18px;
}

.publish-side {
  position: sticky;
  top: 84px;
}

.form-card,
.progress-card,
.ai-helper-card,
.submit-card {
  padding: 22px;
}

.card-head,
.side-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-head-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.card-index {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.card-head h2,
.side-card-head h2 {
  margin: 0;
  font-size: 22px;
  color: var(--text-main);
}

.card-head p,
.ai-helper-desc,
.field-note,
.upload-toolbar p,
.upload-empty p,
.submit-summary p,
.progress-item p,
.ai-preview-box p,
.ai-preview-box small {
  margin: 6px 0 0;
  color: var(--text-sub);
  line-height: 1.72;
}

.field-grid {
  margin-top: 18px;
  display: grid;
  gap: 16px;
}

.field-grid.two-column {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block.full-row {
  grid-column: 1 / -1;
}

.field-block > span {
  font-size: 14px;
  font-weight: 600;
  color: #344336;
}

.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--line-strong);
  border-radius: 12px;
  background: #ffffff;
  color: var(--text-main);
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input {
  min-height: 46px;
  padding: 0 14px;
}

.textarea {
  min-height: 132px;
  padding: 12px 14px;
  resize: vertical;
}

.input:focus,
.textarea:focus {
  outline: none;
  border-color: rgba(36, 181, 93, 0.4);
  box-shadow: 0 0 0 3px rgba(36, 181, 93, 0.12);
}

.address-input-row,
.upload-toolbar,
.submit-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.address-input-row .input {
  flex: 1;
}

.upload-toolbar {
  align-items: flex-start;
}

.upload-toolbar strong,
.submit-summary strong,
.ai-preview-box strong,
.progress-item strong {
  color: var(--text-main);
}

.upload-list {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.upload-item {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e7eee3;
  background: #f7faf6;
  min-height: 178px;
}

.upload-item img {
  width: 100%;
  height: 178px;
  object-fit: cover;
  display: block;
}

.upload-item .ghost-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
}

.upload-empty {
  margin-top: 18px;
  padding: 22px;
  border: 1px dashed var(--line-strong);
  border-radius: 14px;
  background: #fbfcfa;
}

.hidden-input {
  display: none;
}

.submit-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.side-card-badge {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.progress-list {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.progress-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #edf2ea;
  background: #fbfcfa;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.progress-item span {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f6f2;
  color: #637164;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  white-space: nowrap;
}

.progress-item.done {
  border-color: rgba(36, 181, 93, 0.16);
  background: #f6fbf7;
}

.progress-item.done span {
  background: var(--green-soft);
  color: var(--green-deep);
}

.ai-quick-list {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.ai-quick-btn {
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid #e4ece1;
  background: #ffffff;
  color: #465547;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.ai-quick-btn:hover,
.ghost-btn:hover {
  border-color: rgba(36, 181, 93, 0.22);
  background: #f6fbf7;
  color: var(--green-deep);
}

.ai-preview-box {
  margin-top: 18px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fbf6;
  border: 1px solid #e8efe4;
}

.primary-btn,
.ghost-btn {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.primary-btn {
  border: 1px solid transparent;
  background: var(--green);
  color: #ffffff;
}

.primary-btn:hover {
  transform: translateY(-1px);
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.ghost-btn {
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #4c5c4d;
}

.ghost-btn.compact {
  min-height: 38px;
  padding: 0 14px;
  font-size: 13px;
}

.ghost-btn.danger {
  border-color: #f0d2d2;
  background: rgba(255, 248, 248, 0.96);
  color: #c55a5a;
}

@media (max-width: 1320px) {
  .publish-grid {
    grid-template-columns: 1fr;
  }

  .publish-side {
    position: static;
  }
}

@media (max-width: 980px) {
  .publish-main {
    width: calc(100vw - 28px);
    padding: 80px 14px 28px;
  }

  .publish-hero,
  .address-input-row,
  .upload-toolbar,
  .submit-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stat-row,
  .field-grid.two-column,
  .upload-list {
    grid-template-columns: 1fr;
  }

  .hero-action-group,
  .submit-actions,
  .address-input-row .input {
    width: 100%;
  }
}
</style>
