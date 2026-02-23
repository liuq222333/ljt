<template>
  <div class="add-product-page">
    <dhstyle />
    <div class="container">
      <h1 class="title">发布商品</h1>

      <div class="card">
        <form @submit.prevent="onSubmit">
          <div class="form-row">
            <label>商品标题</label>
            <input class="input" type="text" v-model.trim="form.title" placeholder="请输入标题" />
          </div>

          <div class="form-row">
            <label>商品描述</label>
            <textarea class="textarea" v-model.trim="form.description" placeholder="请输入商品描述"></textarea>
          </div>

          <div class="grid-2">
            <div class="form-row">
              <label>价格 (¥)</label>
              <input class="input" type="number" step="0.01" v-model.number="form.price" placeholder="0.00" />
            </div>
            <div class="form-row">
              <label>库存数量</label>
              <input class="input" type="number" min="0" v-model.number="form.stockQuantity" placeholder="0" />
            </div>
          </div>

          <div class="grid-2">
            <div class="form-row">
              <label>成色</label>
              <select class="input" v-model="form.condition">
                <option value="">请选择</option>
                <option value="全新">全新</option>
                <option value="九成新">九成新</option>
                <option value="七成新">七成新</option>
                <option value="二手">二手</option>
              </select>
            </div>
            <div class="form-row">
              <label>所在地点</label>
              <div class="address-input-row">
                <input class="input" type="text" v-model.trim="form.location" placeholder="城市/区域" />
                <button class="icon-btn" type="button" @click="locateAndFillAddress" :disabled="locating" :title="locating ? '定位中...' : '定位填充'" aria-label="定位填充">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 256 256">
                    <path fill="currentColor" d="M128 60a44 44 0 1 0 44 44a44.05 44.05 0 0 0-44-44m0 64a20 20 0 1 1 20-20a20 20 0 0 1-20 20m0-112a92.1 92.1 0 0 0-92 92c0 77.36 81.64 135.4 85.12 137.83a12 12 0 0 0 13.76 0a259 259 0 0 0 42.18-39C205.15 170.57 220 136.37 220 104a92.1 92.1 0 0 0-92-92m31.3 174.71a249.4 249.4 0 0 1-31.3 30.18a249.4 249.4 0 0 1-31.3-30.18C80 167.37 60 137.31 60 104a68 68 0 0 1 136 0c0 33.31-20 63.37-36.7 82.71"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div class="form-row">
            <label>所属分类</label>
            <select class="input" v-model="form.categoryId">
              <option value="">请选择分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div class="form-row">
            <label>商品图片</label>
            <div class="upload-list">
              <div class="upload-item" v-for="(img, idx) in uploads" :key="img.key || idx">
                <img :src="img.url" alt="预览" @error="onUploadImgError(idx)" />
                <button type="button" class="btn small danger" @click="removeUpload(idx)">删除</button>
              </div>
              <button type="button" class="btn outline" @click="triggerUpload" :disabled="uploading">
                {{ uploading ? '上传中...' : '+ 上传图片' }}
              </button>
              <input ref="fileInput" type="file" accept="image/*" multiple style="display:none" @change="handleFileChange" />
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn primary" :disabled="submitting">{{ submitting ? '提交中...' : '发布' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
  </template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dhstyle from '../../dhstyle/dhstyle.vue'
import { env as ViteEnv } from '@/env'

const router = useRouter()

const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080'
const API_BASE_URL = `${API_BASE}/api`

// 发布商品表单
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
const AMAP_KEY = (
  ViteEnv?.VITE_AMAP_KEY ??
  (import.meta as any)?.env?.VITE_AMAP_KEY ??
  (window as any)?.VITE_AMAP_KEY ??
  ''
)
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo'
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert'
const GEOCODER_FORWARD = 'https://restapi.amap.com/v3/geocode/geo'
const lat = ref<number|null>(null)
const lng = ref<number|null>(null)

const MINIO_BASE = (import.meta.env as any)?.VITE_MINIO_BASE || ''
const uploads = ref<Array<{ key: string; url: string }>>([])
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const submitting = ref(false)

const categories = ref<Array<{ id: string | number; name: string }>>([])

function validate() {
  if (!form.value.title) { ElMessage.error('请输入商品标题'); return false }
  if (!form.value.description) { ElMessage.error('请输入商品描述'); return false }
  if (!form.value.price || form.value.price <= 0) { ElMessage.error('请输入有效价格'); return false }
  if (form.value.stockQuantity < 0) { ElMessage.error('库存数量不能为负'); return false }
  if (!form.value.condition) { ElMessage.error('请选择成色'); return false }
  if (!form.value.location) { ElMessage.error('请输入地点'); return false }
  if (!form.value.categoryId) { ElMessage.error('请选择分类'); return false }
  if (uploads.value.length === 0) { ElMessage.error('���������ϴ�һ��ͼƬ'); return false }
  return true
}

async function loadCategories() {
  try {
    const res = await fetch(`${API_BASE_URL}/categories/getAllCategories`)
    if (res.ok) {
      const data = await res.json()
      const list = Array.isArray(data) ? data : (data.items || data.list || data.data || data.records || data.rows || [])
      categories.value = list.map((c: any) => ({ id: c.id ?? c.categoryId ?? c.Id, name: c.name ?? c.categoryName ?? `分类${String(c.categoryId ?? c.id ?? '')}` }))
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
      const preview = data?.url || data?.publicUrl || (publicUrl && /^https?:\/\//i.test(publicUrl) ? publicUrl : URL.createObjectURL(file))
      uploads.value.push({ key, url: preview || '' })
    } catch (e) {
      ElMessage.error('图片上传出错')
    }
  }
  if (input) input.value = ''
  uploading.value = false
}

function removeUpload(idx: number) {
  uploads.value.splice(idx, 1)
}

function onUploadImgError(idx: number) {
  const item = uploads.value[idx]
  if (item) {
    const fallback = buildPublicUrl(item.key)
    if (fallback && /^https?:\/\//i.test(fallback)) {
      item.url = fallback
    }
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
      const r = await fetch(`${GEOCODER_FORWARD}?key=${AMAP_KEY}&address=${encodeURIComponent(form.value.location)}`)
      const j = await r.json()
      const g = Array.isArray(j?.geocodes) ? j.geocodes[0] : null
      const loc = g?.location ? String(g.location).split(',') : null
      const glng = loc && loc[0] ? parseFloat(loc[0]) : null
      const glat = loc && loc[1] ? parseFloat(loc[1]) : null
      if (glat != null && glng != null && !Number.isNaN(glat) && !Number.isNaN(glng)) {
        lat.value = glat
        lng.value = glng
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
      imageUrls: JSON.stringify(uploads.value.map(u => u.key)),
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
      const t = await res.text().catch(() => '')
      ElMessage.error(t || '发布失败')
      return
    }
    const data = await res.json().catch(() => ({} as any))
    ElMessage.success('发布成功')
    const pid = data?.id ?? data?.productId
    if (pid) {
      router.push({ name: 'ProductDetail', params: { id: pid } })
    } else {
      router.push({ name: 'CommunityMarketplace' })
    }
  } catch (e) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function locateAndFillAddress() {
  if (!AMAP_KEY) { ElMessage.error('未配置地图密钥：请设置 VITE_AMAP_KEY'); return }
  if (!('geolocation' in navigator)) { ElMessage.error('当前浏览器不支持定位'); return }
  locating.value = true
  try {
    const coords = await new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        pos => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        err => reject(err),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      )
    })
    let useLat = coords.latitude
    let useLng = coords.longitude
    alert(`当前定位坐标：纬度 ${useLat.toFixed(6)}, 经度 ${useLng.toFixed(6)}`)
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
            alert(`转换后的坐标：纬度 ${useLat.toFixed(6)}, 经度 ${useLng.toFixed(6)}`)
          }
        }
      }
    } catch {}
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`)
    if (!resp.ok) {
      const t = await resp.text().catch(() => '')
      throw new Error(t || '定位服务错误')
    }
    const data = await resp.json()
    if (data.status !== '1' || !data.regeocode || !data.regeocode.formatted_address) {
      throw new Error('未能解析到地址')
    }
    form.value.location = String(data.regeocode.formatted_address)
    lat.value = useLat
    lng.value = useLng
    ElMessage.success('已根据定位填充地址')
  } catch (e: any) {
    const code = Number(e?.code)
    if (code === 1) ElMessage.error('定位权限被拒绝')
    else if (code === 2) ElMessage.error('位置不可用')
    else if (code === 3) ElMessage.error('定位超时')
    else ElMessage.error(e?.message || '定位失败')
  } finally {
    locating.value = false
  }
}
</script>

<style scoped>
.add-product-page { background: #f4f5f7; min-height: 100vh; }
.container { max-width: 900px; margin: 70px auto 40px; padding: 0 20px; }
.title { font-size: 24px; font-weight: 700; color: #333; margin-bottom: 16px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,.08); padding: 20px; }
.form-row { margin-bottom: 14px; }
.form-row label { display: block; font-size: 13px; color: #666; margin-bottom: 6px; }
.input { width: 92%; height: 40px; border: 1px solid #dcdcdc; border-radius: 8px; padding: 0 12px; font-size: 14px; background: #fff; }
.textarea { width: 100%; min-height: 100px; border: 1px solid #dcdcdc; border-radius: 8px; padding: 10px 12px; font-size: 14px; background: #fff; resize: vertical; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.upload-list { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; }
.upload-item { position: relative; width: 120px; height: 120px; border: 1px dashed #dcdcdc; border-radius: 8px; overflow: hidden; background: #fafafa; display: flex; align-items: center; justify-content: center; }
.upload-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.upload-item .btn.small { position: absolute; bottom: 6px; right: 6px; }
.address-input-row { display: flex; gap: 8px; align-items: center; }
.icon-btn { display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px; border-radius: 6px; border: 1px solid #ddd; background: transparent; color: #333; cursor: pointer; transition: background-color .2s, border-color .2s, color .2s; }
.icon-btn:hover { background-color: #f6f6f6; border-color: #bbb; color: #111; }
.icon-btn[disabled] { opacity: .6; cursor: not-allowed; }
.icon-btn svg { width: 18px; height: 18px; display: block; }
.btn { height: 40px; border-radius: 8px; border: none; padding: 0 16px; font-size: 14px; cursor: pointer; }
.btn.primary { background: #1AA053; color: #fff; }
.btn.outline { background: #fff; border: 1px solid #ddd; color: #333; height: 32px; }
.btn.small { height: 32px; padding: 0 12px; }
.btn.danger { background: #f44336; color: #fff; }
.form-actions { margin-top: 18px; }

@media (max-width: 768px) { .grid-2 { grid-template-columns: 1fr; } .container { margin-top: 80px; } }
</style>
