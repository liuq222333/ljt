<template>
  <div class="my-products-page">
    <dhstyle />
    <CebianTool />
    
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">我的商品</h1>
          <p class="page-subtitle">管理您发布的闲置物品</p>
        </div>
        <div class="header-right">
          <button class="btn btn-primary" @click="router.push({ name: 'AddProduct' })">
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px;">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            发布商品
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>正在加载商品...</span>
      </div>

      <div v-else-if="errorMsg" class="error-state">
        {{ errorMsg }}
      </div>

      <div v-else>
        <div v-if="items.length === 0" class="empty-state">
          <div class="empty-icon">📦</div>
          <p>暂无发布的商品</p>
          <button class="btn btn-primary" @click="$router.push({name: 'AddProduct'})">去发布</button>
        </div>

        <div v-else class="products-list">
          <div class="product-card" v-for="(p, idx) in items" :key="p.id">
            <div class="thumb-wrapper">
              <img :src="getFirstImage(p.imageUrls)" class="thumb" @error="onImgError" />
            </div>

            <div class="info-section">
              <div class="product-header">
                <h3 class="product-title">{{ p.title }}</h3>
                <span class="price-tag">¥{{ formatPrice(p.price) }}</span>
              </div>

              <div class="meta-grid">
                <div class="meta-item">
                  <span class="meta-label">库存</span>
                  <span class="meta-value">{{ p.stockQuantity }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">成色</span>
                  <span class="meta-value">{{ p.condition }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">地址</span>
                  <span class="meta-value text-truncate" :title="p.location">{{ p.location }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">发布时间</span>
                  <span class="meta-value">{{ formatDate(p.createdAt) }}</span>
                </div>
              </div>

              <div class="action-bar">
                <div class="action-group">
                  <input class="nd-input small" type="number" v-model.number="edit[idx].delta" min="1" placeholder="数量" />
                  <button class="btn btn-secondary" @click="increaseStock(p.id, edit[idx].delta)">补货</button>
                </div>
                
                <div class="action-group">
                  <input class="nd-input small" type="number" step="0.01" v-model.number="edit[idx].price" placeholder="新价格" />
                  <button class="btn btn-secondary" @click="updatePrice(p.id, edit[idx].price)">调价</button>
                </div>

                <div class="action-group grow">
                  <input class="nd-input" type="text" v-model="edit[idx].location" placeholder="新地址" />
                  <button class="btn btn-secondary" @click="updateLocation(p.id, edit[idx].location)">迁址</button>
                </div>

                <button class="btn btn-danger" @click="takeDown(p.id)">下架</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dhstyle from '../../dhstyle/dhstyle.vue'
import CebianTool from './cebianTool.vue'
import { ElMessage } from 'element-plus'

interface ProductDTO {
  id: number
  sellerId: number
  categoryId: number
  title: string
  description: string
  price: number
  stockQuantity: number
  condition: string
  location: string
  imageUrls: string
  status: string
  createdAt: string
  updatedAt: string
}

const router = useRouter()
const API_BASE_URL = 'http://localhost:8080/api'
const loading = ref(false)
const errorMsg = ref('')
const items = ref<ProductDTO[]>([])
const edit = ref<Array<{ delta: number; price: number; location: string }>>([])

function formatPrice(v: number | string) {
  const n = Number(v || 0)
  return n.toFixed(2)
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

const FALLBACK_IMG = 'https://via.placeholder.com/240x180.png?text=No+Image'

function getFirstImage(imageUrls: string | null | undefined) {
  if (!imageUrls) return FALLBACK_IMG
  const trimmed = String(imageUrls).trim().replace(/`/g, '')
  if (trimmed.startsWith('[')) {
    try {
      const arr = JSON.parse(trimmed)
      const first = Array.isArray(arr) ? (arr[0] || '') : ''
      return first || FALLBACK_IMG
    } catch {
      return FALLBACK_IMG
    }
  }
  return trimmed || FALLBACK_IMG
}

function onImgError(e: Event) {
  (e.target as HTMLImageElement).src = FALLBACK_IMG
}

async function fetchMyProducts() {
  loading.value = true
  errorMsg.value = ''
  try {
    const userName = localStorage.getItem('username') || ''
    if (!userName) { errorMsg.value = '请先登录'; return }
    const res = await fetch(`${API_BASE_URL}/products/myProducts?userName=${encodeURIComponent(userName)}`)
    if (!res.ok) {
      const t = await res.text().catch(() => '')
      throw new Error(t || '加载失败')
    }
    const data = await res.json()
    items.value = Array.isArray(data) ? data.map((d: any) => ({
      id: Number(d.id ?? 0),
      sellerId: Number(d.sellerId ?? 0),
      categoryId: Number(d.categoryId ?? 0),
      title: String(d.title ?? ''),
      description: String(d.description ?? ''),
      price: Number(d.price ?? 0),
      stockQuantity: Number(d.stockQuantity ?? 0),
      condition: String(d.condition ?? ''),
      location: String(d.location ?? ''),
      imageUrls: String(d.imageUrls ?? ''),
      status: String(d.status ?? ''),
      createdAt: String(d.createdAt ?? ''),
      updatedAt: String(d.updatedAt ?? ''),
    })) : []
    edit.value = items.value.map(() => ({ delta: 1, price: 0, location: '' }))
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function takeDown(id: number) {
  const res = await fetch(`${API_BASE_URL}/products/takeDown?productId=${id}`, { method: 'POST' })
  if (res.ok) { ElMessage.success('已下架'); fetchMyProducts() } else { ElMessage.error('下架失败') }
}
async function increaseStock(id: number, delta?: number) {
  const d = Number(delta || 0)
  if (d <= 0) { ElMessage.warning('数量必须为正'); return }
  const res = await fetch(`${API_BASE_URL}/products/increaseStock?productId=${id}&delta=${d}`, { method: 'POST' })
  if (res.ok) { ElMessage.success('库存已更新'); fetchMyProducts() } else { ElMessage.error('库存更新失败') }
}
async function updatePrice(id: number, price?: number) {
  const p = Number(price || 0)
  if (p < 0) { ElMessage.warning('价格不合法'); return }
  const res = await fetch(`${API_BASE_URL}/products/updatePrice?productId=${id}&price=${p}`, { method: 'POST' })
  if (res.ok) { ElMessage.success('价格已更新'); fetchMyProducts() } else { ElMessage.error('价格更新失败') }
}
async function updateLocation(id: number, location?: string) {
  const loc = String(location || '').trim()
  if (!loc) { ElMessage.warning('地址不能为空'); return }
  const res = await fetch(`${API_BASE_URL}/products/updateLocation?productId=${id}&location=${encodeURIComponent(loc)}`, { method: 'POST' })
  if (res.ok) { ElMessage.success('地址已更新'); fetchMyProducts() } else { ElMessage.error('地址更新失败') }
}

onMounted(fetchMyProducts)
</script>

<style scoped>
:root {
  --nd-green: #1AA053;
  --nd-red: #d93025;
}

.my-products-page {
  background: #f9fafb;
  min-height: 100vh;
  padding-bottom: 60px;
  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 100px 24px 20px; /* Top padding for fixed header if any, else spacing */
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  text-align: left;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.page-subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(26, 160, 83, 0.3);
  border-radius: 50%;
  border-top-color: #1AA053;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 80px 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

/* Product List */
.products-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Product Card */
.product-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  gap: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid rgba(0,0,0,0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.thumb-wrapper {
  width: 220px;
  height: 160px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #eee;
}

.thumb {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.info-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.product-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
}

.price-tag {
  font-size: 20px;
  font-weight: 700;
  color: #1AA053; /* Nextdoor Green */
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  background: #f9fafb;
  padding: 16px;
  border-radius: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #666;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* Action Bar */
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-group.grow {
  flex: 1;
}

/* Inputs */
.nd-input {
  height: 36px;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  background: #fff;
  transition: all 0.2s;
  width: 100%;
}

.nd-input.small {
  width: 80px;
  text-align: center;
}

.nd-input:focus {
  border-color: #1AA053;
  box-shadow: 0 0 0 2px rgba(26, 160, 83, 0.1);
  outline: none;
}

/* Buttons */
.btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.btn-primary {
  background: #1AA053;
  color: #fff;
}
.btn-primary:hover {
  background: #158a46;
  box-shadow: 0 4px 12px rgba(26, 160, 83, 0.2);
}

.btn-secondary {
  background: #fff;
  border: 1px solid #e5e5e5;
  color: #333;
}
.btn-secondary:hover {
  background: #f5f5f5;
  border-color: #dcdcdc;
}

.btn-danger {
  background: #fee2e2;
  color: #dc2626;
  margin-left: auto; /* Push to right if possible */
}
.btn-danger:hover {
  background: #fecaca;
}

@media (max-width: 768px) {
  .product-card {
    flex-direction: column;
  }
  
  .thumb-wrapper {
    width: 100%;
    height: 200px;
  }
  
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-group {
    width: 100%;
  }
  
  .nd-input.small {
    width: 100%;
  }
  
  .btn {
    width: 100%;
  }
}
</style>
