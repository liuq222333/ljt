<template>
  <section class="admin-page market-products-page">
    <AdminPageHeader eyebrow="运营管理" title="二手市场管理" description="查看商品信息、快速过滤并执行删除处理。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="fetchList">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <label class="filter-field filter-field--search">
          <span>搜索</span>
          <input v-model="keyword" type="text" placeholder="按标题或描述过滤" />
        </label>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="clearSearch">
          清空搜索
        </button>
      </template>
      <template #actions>
        <div class="toolbar-meta">
          <span class="meta-chip">总数 {{ filteredItems.length }}</span>
          <span class="meta-chip">在售 {{ activeCount }}</span>
          <span class="meta-chip">已售 {{ soldCount }}</span>
          <span class="meta-chip">下架 {{ inactiveCount }}</span>
        </div>
      </template>
    </AdminToolbar>

    <AdminPanel title="商品列表" description="当前为全量拉取后在前端过滤，适合日常内容巡检。">
      <div class="table-shell">
        <table class="admin-data-table market-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>商品</th>
              <th>价格</th>
              <th>分类</th>
              <th>状态</th>
              <th>卖家 ID</th>
              <th>发布时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredItems" :key="item.id">
              <td>{{ item.id }}</td>
              <td>
                <div class="product-cell">
                  <img v-if="getFirstImage(item.imageUrls)" :src="getFirstImage(item.imageUrls)" class="thumb" alt="商品图片" />
                  <div v-else class="thumb thumb--placeholder">无图</div>
                  <div class="product-copy">
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.description || '暂无描述' }}</p>
                  </div>
                </div>
              </td>
              <td class="price-cell">¥ {{ formatPrice(item.price) }}</td>
              <td>{{ item.categoryId }}</td>
              <td>
                <span class="state-pill" :class="statusClass(item.status)">
                  {{ item.status || '未知' }}
                </span>
              </td>
              <td>{{ item.sellerId }}</td>
              <td>{{ formatDate(item.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="admin-button admin-button--danger admin-button--small" type="button" @click="deleteItem(item)">
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && filteredItems.length === 0">
              <td colspan="8" class="empty-state">暂无商品数据</td>
            </tr>
            <tr v-if="loading">
              <td colspan="8" class="empty-state">正在加载商品列表...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AdminPanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminPageHeader from './admin/AdminPageHeader.vue'
import AdminPanel from './admin/AdminPanel.vue'
import AdminToolbar from './admin/AdminToolbar.vue'

type Product = {
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

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const items = ref<Product[]>([])
const loading = ref(false)
const keyword = ref('')

const filteredItems = computed(() => {
  if (!keyword.value.trim()) return items.value
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return items.value.filter((item) => {
    return (
      item.title?.toLowerCase().includes(normalizedKeyword) ||
      item.description?.toLowerCase().includes(normalizedKeyword)
    )
  })
})

const activeCount = computed(() => filteredItems.value.filter((item) => statusKey(item.status) === 'active').length)
const soldCount = computed(() => filteredItems.value.filter((item) => statusKey(item.status) === 'sold').length)
const inactiveCount = computed(() => filteredItems.value.filter((item) => statusKey(item.status) === 'inactive').length)

async function fetchList() {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/products/getAllProducts`)
    if (!response.ok) {
      throw new Error('获取商品失败')
    }
    const data = await response.json()
    items.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error(error)
    window.alert('获取商品失败')
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  keyword.value = ''
}

function getFirstImage(jsonValue: string): string | undefined {
  try {
    const images = JSON.parse(jsonValue)
    if (Array.isArray(images) && images.length > 0) {
      return images[0]
    }
  } catch (error) {
    console.error(error)
  }
  return undefined
}

function statusKey(status: string) {
  const raw = (status || '').trim()
  const upper = raw.toUpperCase()

  if (raw === '在售' || upper === 'ACTIVE' || upper === 'ON_SALE') return 'active'
  if (raw === '已售出' || upper === 'SOLD') return 'sold'
  if (raw === '已下架' || upper === 'INACTIVE' || upper === 'OFFLINE') return 'inactive'
  return 'default'
}

function statusClass(status: string) {
  switch (statusKey(status)) {
    case 'active':
      return 'state-pill--info'
    case 'sold':
      return 'state-pill--neutral'
    case 'inactive':
      return 'state-pill--danger'
    default:
      return 'state-pill--warning'
  }
}

function formatPrice(value: number) {
  const price = Number(value)
  if (Number.isNaN(price)) return '-'
  return price.toFixed(2)
}

function formatDate(value: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

async function deleteItem(item: Product) {
  if (!window.confirm(`确认删除商品“${item.title}”（ID: ${item.id}）吗？`)) return

  try {
    const response = await fetch(`${API_BASE}/admin/market/products/${item.id}`, {
      method: 'DELETE',
    })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '删除失败')
    }
    window.alert('商品已删除')
    fetchList()
  } catch (error: any) {
    console.error(error)
    window.alert(error?.message || '删除失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.market-products-page {
  gap: 12px;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.filter-field span {
  white-space: nowrap;
}

.filter-field input {
  min-width: 0;
  height: 32px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  padding: 0 10px;
  font-size: 13px;
  outline: none;
}

.filter-field--search {
  min-width: 280px;
}

.filter-field input:focus {
  border-color: var(--admin-border-strong);
  box-shadow: 0 0 0 3px rgba(37, 50, 68, 0.08);
}

.toolbar-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  color: var(--admin-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.admin-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button:hover {
  border-color: var(--admin-border-strong);
}

.admin-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.admin-button--secondary {
  background: var(--admin-bg-subtle);
}

.admin-button--danger {
  border-color: #e5b3ab;
  background: #fbeeed;
  color: var(--admin-danger);
}

.admin-button--small {
  height: 30px;
  padding: 0 10px;
}

.table-shell {
  overflow: auto;
}

.market-table {
  min-width: 1180px;
}

.product-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 280px;
}

.thumb {
  width: 48px;
  height: 48px;
  border: 1px solid var(--admin-border);
  border-radius: 6px;
  object-fit: cover;
  background: var(--admin-bg-subtle);
  flex-shrink: 0;
}

.thumb--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.product-copy strong {
  display: block;
  color: var(--admin-text-primary);
  font-size: 13px;
  line-height: 1.4;
}

.product-copy p {
  margin: 4px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.price-cell {
  color: #8c2f12;
  font-weight: 700;
  white-space: nowrap;
}

.state-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.state-pill--neutral {
  background: var(--admin-accent-soft);
  border-color: #d9e1ec;
  color: var(--admin-accent);
}

.state-pill--info {
  background: #eef4fb;
  border-color: #c8d7eb;
  color: #315d87;
}

.state-pill--warning {
  background: #fcf8ec;
  border-color: #edd8a5;
  color: var(--admin-warning);
}

.state-pill--danger {
  background: #fbeeed;
  border-color: #efc3bc;
  color: var(--admin-danger);
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-state {
  padding: 28px 12px;
  text-align: center;
  color: var(--admin-text-muted);
}
</style>
