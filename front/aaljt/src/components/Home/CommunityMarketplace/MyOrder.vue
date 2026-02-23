<template>
  <div class="orders-page">
    <dhstyle />
    <CebianTool />
    <div class="container">
      <h1 class="title">我的订单</h1>

      <div v-if="loading" class="card" style="text-align:center; padding:20px;">正在加载...</div>
      <div v-else-if="errorMsg" class="card" style="color:#d33; padding:20px;">{{ errorMsg }}</div>
      <div v-else>
        <div v-if="orders.length === 0" class="card" style="padding:20px;">暂无订单</div>
        <div v-else class="orders-grid">
          <div class="order-item" v-for="(o, idx) in orders" :key="idx">
            <img :src="o.cover || FALLBACK_IMG" class="order-cover" @error="onImgError" />
            <div class="order-content">
              <div class="order-head">
                <div class="order-id">订单号：{{ o.orderId }}</div>
                <div class="order-time">下单时间：{{ formatDate(o.createdAt) }}</div>
              </div>
              <div class="order-body">
                <div class="order-meta">
                  <span>商品ID：{{ o.productId }}</span>
                  <span>金额：¥{{ formatPrice(o.totalAmount) }}</span>
                  <span>状态：{{ mapStatus(o.status) }}</span>
                </div>
                <div class="order-recv">
                  <span>收件人：{{ o.receiverName }}</span>
                  <span>电话：{{ o.receiverPhone }}</span>
                  <span>地址：{{ o.receiverAddress }}</span>
                </div>
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
import dhstyle from '../../dhstyle/dhstyle.vue'
import CebianTool from './cebianTool.vue'

interface OrderDTO {
  id: string
  orderId: string
  userId: string
  productId: number
  totalAmount: number
  createdAt: string
  updatedAt: string
  status: number
  deliveryTime?: string
  paymentTime?: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  completedTime?: string
  cancelReason?: string
  cancelledAt?: string
  remark?: string
  cover?: string
}

const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080'
const loading = ref(false)
const errorMsg = ref('')
const orders = ref<OrderDTO[]>([])
const FALLBACK_IMG = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120"><rect width="100%" height="100%" fill="%23f4f5f7"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-size="14">No Image</text></svg>'

function formatPrice(v: number | string) {
  const n = Number(v || 0)
  return n.toFixed(2)
}

function formatDate(v: string) {
  if (!v) return '未知'
  const d = new Date(v)
  if (!isNaN(d.getTime())) return d.toLocaleString('zh-CN', { hour12: false })
  return String(v)
}

function mapStatus(s: number) {
  switch (s) {
    case 1: return '已创建'
    case 2: return '已支付'
    case 3: return '已发货'
    case 4: return '已完成'
    case 5: return '已取消'
    default: return String(s)
  }
}

async function fetchOrders() {
  loading.value = true
  errorMsg.value = ''
  try {
    const userName = localStorage.getItem('username') || ''
    if (!userName) { errorMsg.value = '请先登录'; return }
    const res = await fetch(`${API_BASE}/api/carts/orders?userName=${encodeURIComponent(userName)}`)
    if (!res.ok) {
      const t = await res.text().catch(() => '')
      throw new Error(t || '加载失败')
    }
    const data = await res.json()
    orders.value = Array.isArray(data) ? data.map((d: any) => ({
      id: String(d.id ?? ''),
      orderId: String(d.orderId ?? ''),
      userId: String(d.userId ?? ''),
      productId: Number(d.productId ?? 0),
      totalAmount: Number(d.totalAmount ?? 0),
      createdAt: String(d.createdAt ?? ''),
      updatedAt: String(d.updatedAt ?? ''),
      status: Number(d.status ?? 0),
      deliveryTime: d.deliveryTime ? String(d.deliveryTime) : undefined,
      paymentTime: d.paymentTime ? String(d.paymentTime) : undefined,
      receiverName: String(d.receiverName ?? ''),
      receiverPhone: String(d.receiverPhone ?? ''),
      receiverAddress: String(d.receiverAddress ?? ''),
      completedTime: d.completedTime ? String(d.completedTime) : undefined,
      cancelReason: d.cancelReason ? String(d.cancelReason) : undefined,
      cancelledAt: d.cancelledAt ? String(d.cancelledAt) : undefined,
      remark: d.remark ? String(d.remark) : undefined,
    })) : []
    await resolveCovers()
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrders)

async function resolveCovers() {
  const tasks = orders.value.map(async (o) => {
    if (!o.productId) return
    try {
      const resImg = await fetch(`${API_BASE}/api/products/getProductImage/${o.productId}`)
      if (resImg.ok) {
        const arr = await resImg.json()
        const first = Array.isArray(arr) && arr.length > 0 ? arr[0]?.imageUrl : ''
        if (first) { o.cover = first.startsWith('/') ? `${API_BASE}${first}` : first; return }
      }
      const resProd = await fetch(`${API_BASE}/api/products/getProductById/${o.productId}`)
      if (resProd.ok) {
        const p = await resProd.json()
        const urls = p?.imageUrls
        if (urls) {
          try {
            const parsed = JSON.parse(String(urls))
            const first = Array.isArray(parsed) ? (parsed[0] || '') : ''
            if (first) o.cover = first.startsWith('/') ? `${API_BASE}${first}` : first
          } catch {
            const trimmed = String(urls).trim().replace(/`/g, '')
            o.cover = trimmed.startsWith('/') ? `${API_BASE}${trimmed}` : trimmed
          }
        }
      }
    } catch {}
  })
  await Promise.all(tasks)
}

function onImgError(e: Event) {
  (e.target as HTMLImageElement).src = FALLBACK_IMG
}
</script>

<style scoped>
.orders-page { background: #f4f5f7; min-height: 100vh; }
.container { max-width: 1000px; margin: 90px auto 40px; padding: 0 20px; }
.title { font-size: 24px; font-weight: 700; color: #333; margin-bottom: 16px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,.08); padding: 20px; }
.orders-grid { display: flex; flex-direction: column; gap: 12px; }
.order-item { background: #fff; border-radius: 10px; padding: 12px; box-shadow: 0 2px 8px rgba(0,0,0,.05); display: grid; grid-template-columns: 140px 1fr; gap: 12px; align-items: center; }
.order-cover { width: 140px; height: 105px; object-fit: cover; border-radius: 8px; }
.order-content { display: flex; flex-direction: column; gap: 8px; }
.order-head { display: flex; justify-content: space-between; color: #333; font-weight: 600; }
.order-body { display: flex; flex-direction: column; gap: 6px; }
.order-meta, .order-recv { display: flex; gap: 16px; color: #666; font-size: 13px; flex-wrap: wrap; }
</style>
