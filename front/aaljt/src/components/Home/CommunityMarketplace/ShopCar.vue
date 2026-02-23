<template>
  <div class="cart-page">
    <dhstyle />
    <CebianTool />
    <div class="cart-container">
      <h1 class="page-title">购物车</h1>

      <div v-if="loading" class="state-card loading">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="errorMsg" class="state-card error">
        <p>{{ errorMsg }}</p>
        <button class="btn primary" @click="fetchCart">重试</button>
      </div>
      <div v-else>
        <div v-if="items.length === 0" class="state-card empty">
          <div class="empty-icon">🛒</div>
          <p>购物车空空如也</p>
          <button class="btn secondary" @click="$router.push('/market')">去逛逛</button>
        </div>
        
        <div v-else class="cart-content">
          <div class="cart-list">
            <div class="list-header card">
              <label class="checkbox-wrapper select-all">
                <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll($event)" />
                <span class="checkmark"></span>
                <span class="label-text">全选 ({{ items.length }})</span>
              </label>
              <button class="btn text-danger" @click="deleteSelected" :disabled="selectedCount===0">删除选中</button>
            </div>

            <div class="cart-item card" v-for="(item, idx) in items" :key="idx">
              <label class="checkbox-wrapper item-check">
                <input type="checkbox" v-model="selectedFlags[idx]" />
                <span class="checkmark"></span>
              </label>
              
              <div class="item-thumb">
                <img :src="getFirstImage(item.imageUrls)" @error="onImgError" />
              </div>
              
              <div class="item-main">
                <div class="item-header">
                  <h3 class="item-title">{{ item.title }}</h3>
                  <span v-if="isDownItem(item)" class="badge badge-down">已下架</span>
                </div>
                <div class="item-props">
                  <span class="prop">加入时间：{{ formatDate(item.createdAt) }}</span>
                </div>
                <div class="item-price-row">
                  <span class="price">¥{{ formatPrice(item.price) }}</span>
                  <span class="quantity">x{{ item.quantity }}</span>
                </div>
                
                <div v-if="showDetails[idx]" class="item-extra">
                  <p>图片地址：{{ getFirstImage(item.imageUrls) }}</p>
                  <p v-if="isDownItem(item)" class="text-danger">商品状态异常（下架或库存不足）</p>
                </div>
              </div>

              <div class="item-actions">
                <div class="subtotal">¥{{ formatPrice(item.quantity * item.price) }}</div>
                <div class="action-btns">
                  <button class="btn icon-btn" @click="toggleDetails(idx)" title="详情">
                    <svg viewBox="0 0 24 24" width="18" height="18"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" fill="currentColor"/></svg>
                  </button>
                  <button class="btn icon-btn danger" @click="deleteItem(idx)" title="删除">
                    <svg viewBox="0 0 24 24" width="18" height="18"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" fill="currentColor"/></svg>
                  </button>
                </div>
                <button class="btn sm-primary" v-if="!isDownItem(item)" @click="purchaseItem(idx)">立即购买</button>
              </div>
            </div>
          </div>

          <div class="cart-summary card">
            <div class="summary-row">
              <span>已选商品</span>
              <span class="highlight">{{ selectedCount }} 件</span>
            </div>
            <div class="summary-row">
              <span>总计金额</span>
              <span class="total-price">¥{{ formatPrice(selectedAmount) }}</span>
            </div>
            
            <!-- 优惠与配送占位 -->
            <div class="summary-divider"></div>
            <div class="summary-extra">
              <div class="extra-row">
                <span>配送至</span>
                <span class="link-text">默认地址</span>
              </div>
              <div class="extra-row">
                <span>优惠券</span>
                <span class="link-text">暂无可用</span>
              </div>
            </div>

            <div class="summary-actions">
              <button class="btn primary block" :disabled="selectedCount===0 || hasDownSelected" @click="purchaseSelected">
                结算 ({{ selectedCount }})
              </button>
            </div>
            <p v-if="hasDownSelected" class="error-tip">所选商品包含已下架商品</p>
            
            <div class="service-badges">
              <div class="badge-item">
                <span class="icon">🛡️</span>
                <span>平台担保</span>
              </div>
              <div class="badge-item">
                <span class="icon">⚡</span>
                <span>极速发货</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 猜你喜欢 -->
        <div class="recommend-section">
          <h2 class="section-title">猜你喜欢</h2>
          <div class="recommend-grid">
            <div class="recommend-item card" v-for="item in recommendations" :key="item.id" @click="viewProduct(item)">
              <div class="rec-thumb">
                <img :src="getFirstImage(item.imageUrls)" @error="onImgError" />
              </div>
              <div class="rec-info">
                <div class="rec-title">{{ item.title }}</div>
                <div class="rec-price">¥{{ formatPrice(item.price) }}</div>
                <button class="btn sm-primary rec-btn" @click.stop="addToCart(item)">加入购物车</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router' // Added
import dhstyle from '../../dhstyle/dhstyle.vue'
import CebianTool from './cebianTool.vue'

const router = useRouter() // Added

interface CartItemDTO {
  quantity: number
  price: number
  title: string
  imageUrls: string
  createdAt: string
  productId?: number
  status?: string
  stockQuantity?: number
}

interface Product { // Added
  id: number
  title: string
  price: number
  imageUrls: string
}

const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080'
const items = ref<CartItemDTO[]>([])
const recommendations = ref<Product[]>([]) // Added
const loading = ref(false)
const errorMsg = ref('')

const selectedFlags = ref<boolean[]>([])
const showDetails = ref<boolean[]>([])
const selectedCount = computed(() => selectedFlags.value.filter(Boolean).length)
const selectedAmount = computed(() => items.value.reduce((sum, it, idx) => selectedFlags.value[idx] ? sum + Number(it.price) * Number(it.quantity) : sum, 0))
const isAllSelected = computed(() => items.value.length > 0 && selectedFlags.value.every(Boolean))
function isDownStatus(s?: string) {
  if (s == null) return false
  const v = String(s).trim()
  if (["下架", "已下架", "inactive", "2"].includes(v)) return true
  if (["在售", "出售中", "active", "1"].includes(v)) return false
  return false
}
function isDownItem(it: CartItemDTO) {
  const statusDown = isDownStatus(it.status)
  const stockDown = it.stockQuantity != null ? Number(it.stockQuantity) <= 0 : false
  return statusDown || stockDown
}
const hasDownSelected = computed(() => items.value.some((it, idx) => selectedFlags.value[idx] && isDownItem(it)))

function formatPrice(v: number | string) {
  const n = Number(v || 0)
  return n.toFixed(2)
}

function formatDate(v: string) {
  if (!v) return '未知'
  const d = new Date(v)
  if (!isNaN(d.getTime())) {
    return d.toLocaleString('zh-CN', { hour12: false })
  }
  return String(v)
}

const FALLBACK_ITEM = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="120"><rect width="100%" height="100%" fill="%23f4f5f7"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-size="16">No Image</text></svg>'

function getFirstImage(imageUrls: string | null | undefined) {
  if (!imageUrls) return FALLBACK_ITEM
  const trimmed = String(imageUrls).trim().replace(/`/g, '')
  if (trimmed.startsWith('[')) {
    try {
      const arr = JSON.parse(trimmed)
      const first = Array.isArray(arr) ? (arr[0] || '') : ''
      return first ? (String(first).startsWith('/') ? `${API_BASE}${first}` : String(first)) : FALLBACK_ITEM
    } catch {
      const m = trimmed.match(/(https?:\/\/[^"'\]\s]+|\/[\w\-\/\.]+)/)
      if (m) return m[1].startsWith('/') ? `${API_BASE}${m[1]}` : m[1]
    }
  }
  return trimmed.startsWith('/') ? `${API_BASE}${trimmed}` : trimmed
}

function onImgError(e: Event) {
  (e.target as HTMLImageElement).src = FALLBACK_ITEM
}

// ... existing code ...

async function fetchCart() {
  loading.value = true
  errorMsg.value = ''
  try {
    const userName = localStorage.getItem('username') || ''
    if (!userName) { errorMsg.value = '请先登录'; return }
    
    // Fetch Cart
    const res = await fetch(`${API_BASE}/api/carts/getCartItems?userName=${encodeURIComponent(userName)}`)
    if (!res.ok) {
      const t = await res.text().catch(() => '')
      throw new Error(t || '加载失败')
    }
    const data = await res.json()
    items.value = Array.isArray(data) ? data.map((d: any) => ({
      quantity: Number(d.quantity ?? 0),
      price: Number(d.price ?? 0),
      title: String(d.title ?? ''),
      imageUrls: String(d.imageUrls ?? ''),
      createdAt: String(d.createdAt ?? ''),
      productId: d.productId != null ? Number(d.productId) : undefined,
      status: d.status != null ? String(d.status) : undefined,
      stockQuantity: d.stockQuantity != null ? Number(d.stockQuantity) : undefined
    })) : []
    selectedFlags.value = items.value.map(() => false)
    showDetails.value = items.value.map(() => false)
    
    // Fetch Recommendations (Parallel or sequential is fine)
    fetchRecommendations()
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function fetchRecommendations() {
  try {
    const res = await fetch(`${API_BASE}/api/products/getAllProducts`)
    if (res.ok) {
      const data = await res.json()
      if (Array.isArray(data)) {
        // Randomly shuffle or take first 4 different from cart? For now just take first 4
        recommendations.value = data.slice(0, 4)
      }
    }
  } catch (e) {
    console.error('Failed to fetch recommendations', e)
  }
}

function viewProduct(item: Product) {
  router.push(`/product/${item.id}`) // Assuming route exists, or just push to market
}

async function addToCart(item: Product) {
  const userName = localStorage.getItem('username') || ''
  if (!userName) { alert('请先登录'); return }
  const url = `${API_BASE}/api/carts/buyCartItems?userName=${encodeURIComponent(userName)}&productId=${item.id}&quantity=1`
  try {
    const res = await fetch(url, { method: 'POST' })
    if (res.ok) {
      alert('已加入购物车')
      fetchCart() // Refresh cart
    } else {
      alert('添加失败')
    }
  } catch (e) {
    alert('网络错误')
  }
}

onMounted(fetchCart)

function toggleSelectAll(ev: Event) {
  const checked = (ev.target as HTMLInputElement).checked
  selectedFlags.value = items.value.map(() => checked)
}

function toggleDetails(idx: number) {
  showDetails.value[idx] = !showDetails.value[idx]
}

function deleteSelected() {
  if (selectedCount.value === 0) return
  items.value = items.value.filter((_, idx) => !selectedFlags.value[idx])
  selectedFlags.value = items.value.map(() => false)
  showDetails.value = items.value.map(() => false)
}

function purchaseSelected() {
  if (selectedCount.value === 0) return
  if (hasDownSelected.value) { alert('所选包含已下架商品，无法购买'); return }
  const userName = localStorage.getItem('username') || ''
  if (!userName) { alert('请先登录'); return }
  const payload = {
    userName,
    items: items.value
      .map((it, idx) => ({ productId: it.productId, quantity: it.quantity, selected: selectedFlags.value[idx] }))
      .filter(x => x.selected && x.productId && x.quantity > 0)
      .map(x => ({ productId: Number(x.productId), quantity: Number(x.quantity) }))
  }
  if (!payload.items.length) { alert('选中商品缺少可购买的ID或数量'); return }
  fetch(`${API_BASE}/api/carts/buySelected`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(async (res) => {
    if (res.ok) {
      alert(`购买成功：共 ${payload.items.length} 件，金额 ¥${formatPrice(selectedAmount.value)}`)
      await fetchCart()
    } else {
      const msg = await res.text().catch(() => '')
      alert(`购买失败${msg ? `：${msg}` : ''}`)
    }
  })
}

async function purchaseItem(idx: number) {
  const it = items.value[idx]
  const userName = localStorage.getItem('username') || ''
  if (!userName) { alert('请先登录'); return }
  if (!it?.productId) { alert('商品信息缺失'); return }
  const pid = Number(it.productId)
  const qty = Number(it.quantity)
  const url = `${API_BASE}/api/carts/buyCartItems?userName=${encodeURIComponent(userName)}&productId=${pid}&quantity=${qty}`
  const res = await fetch(url, { method: 'POST' })
  if (res.ok) {
    alert('购买成功')
    await fetchCart()
  } else {
    const msg = await res.text().catch(() => '')
    alert(`购买失败${msg ? `：${msg}` : ''}`)
  }
}

async function deleteItem(idx: number) {
  const it = items.value[idx]
  const userName = localStorage.getItem('username') || ''
  if (!userName) { alert('请先登录'); return }
  if (!it?.productId) { alert('商品信息缺失'); return }
  const pid = Number(it.productId)
  const url = `${API_BASE}/api/carts/deleteCartItem?userName=${encodeURIComponent(userName)}&productId=${pid}`
  const res = await fetch(url, { method: 'DELETE' })
  if (res.ok) {
    alert('删除成功')
    await fetchCart()
  } else {
    const msg = await res.text().catch(() => '')
    alert(`删除失败${msg ? `：${msg}` : ''}`)
  }
}
</script>

<style scoped>
:root {
  --nd-green: #1AA053;
  --nd-dark: rgba(0,0,0,.6);
  --nd-bg: #ffffff;
}

.cart-page {
  background-color: #f9f9f9;
  min-height: 100vh;
  padding-top: 80px;
  color: #222;
}

.cart-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
}

/* Cards */
.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  padding: 20px;
  margin-bottom: 16px;
}

/* State Cards */
.state-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.state-card p {
  color: #666;
  margin-bottom: 20px;
}

/* Buttons */
.btn {
  height: 40px;
  padding: 0 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.btn:hover { opacity: 0.9; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn.primary {
  background-color: #1AA053;
  color: #fff;
}
.btn.secondary {
  background-color: #fff;
  border: 1px solid #e5e5e5;
  color: #333;
}
.btn.text-danger {
  background: none;
  color: #d93025;
  padding: 0 10px;
}
.btn.sm-primary {
  height: 32px;
  font-size: 13px;
  background-color: #1AA053;
  color: #fff;
}
.btn.icon-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
}
.btn.icon-btn.danger { color: #d93025; }
.btn.block { width: 100%; }

/* Layout */
.cart-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  align-items: start;
}

/* Checkbox */
.checkbox-wrapper {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}
.checkbox-wrapper input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
}
.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid #ddd;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s;
}
.checkbox-wrapper input:checked ~ .checkmark {
  background-color: #1AA053;
  border-color: #1AA053;
}
.checkbox-wrapper input:checked ~ .checkmark:after {
  content: "";
  position: absolute;
  left: 6px;
  top: 2px;
  width: 4px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.label-text { margin-left: 8px; font-size: 14px; color: #666; }

/* List Header */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

/* Cart Item */
.cart-item {
  display: grid;
  grid-template-columns: auto 100px 1fr auto;
  gap: 16px;
  align-items: center;
  position: relative;
}
.item-thumb {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}
.badge-down { background: #ffebee; color: #d32f2f; }
.item-props {
  font-size: 13px;
  color: #888;
}
.item-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.price { font-size: 16px; font-weight: 600; color: #333; }
.quantity { color: #888; font-size: 13px; }
.item-extra {
  font-size: 12px;
  color: #999;
  background: #f9f9f9;
  padding: 8px;
  border-radius: 4px;
}
.text-danger { color: #d93025; }

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}
.subtotal {
  font-size: 18px;
  font-weight: 700;
  color: #1AA053;
}
.action-btns {
  display: flex;
  gap: 8px;
}

/* Summary Card */
.cart-summary {
  position: sticky;
  top: 100px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}
.highlight { color: #333; font-weight: 600; }
.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #1AA053;
}
.summary-actions { margin-top: 24px; }
.error-tip {
  color: #d93025;
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
}

/* Responsive */
@media (max-width: 768px) {
  .cart-content { grid-template-columns: 1fr; }
  .cart-item {
    grid-template-columns: auto 80px 1fr;
    grid-template-areas: 
      "check thumb main"
      "check thumb actions";
  }
  .item-check { grid-area: check; }
  .item-thumb { grid-area: thumb; width: 80px; height: 80px; }
  .item-main { grid-area: main; }
  .item-actions { 
    grid-area: actions; 
    flex-direction: row; 
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #f0f0f0;
    padding-top: 12px;
    margin-top: 8px;
  }
  .cart-summary { position: static; }
}

/* Recommendations */
.recommend-section { margin-top: 40px; }
.section-title { font-size: 20px; font-weight: 700; margin-bottom: 16px; color: #333; }
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.recommend-item {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  padding: 0; /* Override card padding */
  overflow: hidden;
}
.recommend-item:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.08); }
.rec-thumb { width: 100%; height: 160px; background: #f9f9f9; }
.rec-thumb img { width: 100%; height: 100%; object-fit: cover; }
.rec-info { padding: 12px; flex: 1; display: flex; flex-direction: column; }
.rec-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-price { color: #1AA053; font-weight: 700; font-size: 16px; margin-bottom: 8px; }
.rec-btn { margin-top: auto; width: 100%; opacity: 0; transition: opacity 0.2s; }
.recommend-item:hover .rec-btn { opacity: 1; }

@media (max-width: 1000px) {
  .recommend-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .recommend-grid { grid-template-columns: 1fr; }
}

/* Summary Extra */
.summary-divider { height: 1px; background: #eee; margin: 16px 0; }
.summary-extra { display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: #666; }
.extra-row { display: flex; justify-content: space-between; }
.link-text { color: #1AA053; cursor: pointer; }

/* Service Badges */
.service-badges { margin-top: 20px; display: flex; justify-content: space-around; background: #f9f9f9; padding: 12px; border-radius: 8px; }
.badge-item { display: flex; flex-direction: column; align-items: center; font-size: 12px; color: #666; gap: 4px; }
.badge-item .icon { font-size: 18px; }
</style>
